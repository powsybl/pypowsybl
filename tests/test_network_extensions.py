#
# Copyright (c) 2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import numpy as np
import pandas as pd
import pathlib
import pytest

import pypowsybl.network
import pypowsybl.network as pn

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent / 'data'


def test_extensions():
    assert 'activePowerControl' in pn.get_extensions_names()
    no_extensions_network = pn.create_eurostag_tutorial_example1_network()
    assert no_extensions_network.get_extensions('activePowerControl').empty
    n = pn._create_network('eurostag_tutorial_example1_with_apc_extension')
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 1
    assert generators_extensions['participate']['GEN']
    assert generators_extensions['droop']['GEN'] == pytest.approx(1.1, abs=1e-3)
    assert np.isnan(generators_extensions['participation_factor']['GEN'])
    assert np.isnan(generators_extensions['max_target_p']['GEN'])
    assert np.isnan(generators_extensions['min_target_p']['GEN'])
    assert n.get_extensions('hvdcOperatorActivePowerRange').empty


def test_update_extensions():
    n = pn._create_network('eurostag_tutorial_example1_with_apc_extension')
    n.update_extensions('activePowerControl', pd.DataFrame.from_records(index='id', data=[
        {'id': 'GEN', 'droop': 1.2, 'participation_factor': 1.5, 'max_target_p': 900., 'min_target_p': 200.}
    ]))
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 1
    assert generators_extensions['participate']['GEN']
    assert generators_extensions['droop']['GEN'] == pytest.approx(1.2, abs=1e-3)
    assert generators_extensions['participation_factor']['GEN'] == pytest.approx(1.5, abs=1e-3)
    assert generators_extensions['max_target_p']['GEN'] == pytest.approx(900., abs=1e-3)
    assert generators_extensions['min_target_p']['GEN'] == pytest.approx(200., abs=1e-3)
    n.update_extensions('activePowerControl',
                        id='GEN', droop=1.4, participation_factor=1.8, max_target_p=800., min_target_p=150.)
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 1
    assert generators_extensions['participate']['GEN']
    assert generators_extensions['droop']['GEN'] == pytest.approx(1.4, abs=1e-3)
    assert generators_extensions['participation_factor']['GEN'] == pytest.approx(1.8, abs=1e-3)
    assert generators_extensions['max_target_p']['GEN'] == pytest.approx(800., abs=1e-3)
    assert generators_extensions['min_target_p']['GEN'] == pytest.approx(150., abs=1e-3)


def test_remove_extensions():
    n = pn._create_network('eurostag_tutorial_example1_with_apc_extension')
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 1
    n.remove_extensions('activePowerControl', ['GEN', 'GEN2'])
    assert n.get_extensions('activePowerControl').empty


def test_create_extensions():
    n = pn._create_network('eurostag_tutorial_example1')
    n.create_extensions('activePowerControl', pd.DataFrame.from_records(index='id', data=[
        {'id': 'GEN', 'droop': 1.2, 'participate': True, 'participation_factor': 1.5,
         'max_target_p': 900., 'min_target_p': 200.}
    ]))
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 1
    assert generators_extensions['participate']['GEN']
    assert generators_extensions['droop']['GEN'] == pytest.approx(1.2, abs=1e-3)
    assert generators_extensions['participation_factor']['GEN'] == pytest.approx(1.5, abs=1e-3)
    assert generators_extensions['max_target_p']['GEN'] == pytest.approx(900., abs=1e-3)
    assert generators_extensions['min_target_p']['GEN'] == pytest.approx(200., abs=1e-3)

    n.create_extensions('activePowerControl', id='GEN2', droop=1.3, participate=False)
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 2
    assert not generators_extensions['participate']['GEN2']
    assert generators_extensions['droop']['GEN2'] == pytest.approx(1.3, abs=1e-3)
    assert np.isnan(generators_extensions['participation_factor']['GEN2'])
    assert np.isnan(generators_extensions['max_target_p']['GEN2'])
    assert np.isnan(generators_extensions['min_target_p']['GEN2'])


def test_entsoe_area():
    network = pn.load(str(DATA_DIR / 'germanTsos.uct'))
    area = network.get_extensions('entsoeArea').loc['D4NEUR']
    assert area.code == 'D4'

    network.update_extensions('entsoeArea', id='D4NEUR', code='FR')
    e = network.get_extensions('entsoeArea').loc['D4NEUR']
    assert e.code == 'FR'

    network.remove_extensions('entsoeArea', ['D4NEUR'])
    assert network.get_extensions('entsoeArea').empty

    network.create_extensions('entsoeArea', id='D4NEUR', code='D4')
    e = network.get_extensions('entsoeArea').loc['D4NEUR']
    assert e.code == 'D4'


def test_entsoe_category():
    network = pn._create_network('eurostag_tutorial_example1_with_entsoe_category')
    gen = network.get_extensions('entsoeCategory').loc['GEN']
    assert gen.code == 5

    network.update_extensions('entsoeCategory', id='GEN', code=6)
    e = network.get_extensions('entsoeCategory').loc['GEN']
    assert e.code == 6

    network.remove_extensions('entsoeCategory', ['GEN'])
    assert network.get_extensions('entsoeCategory').empty

    network.create_extensions('entsoeCategory', id='GEN', code=5)
    e = network.get_extensions('entsoeCategory').loc['GEN']
    assert e.code == 5


def test_hvdc_angle_droop_active_power_control():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'hvdcAngleDroopActivePowerControl'
    element_id = 'HVDC1'

    extensions = n.get_extensions(extension_name)
    assert extensions.empty

    n.create_extensions(extension_name, id=element_id, droop=0.1, p0=200, enabled=True)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.droop == pytest.approx(0.1, abs=1e-3)
    assert e.p0 == pytest.approx(200, abs=1e-3)
    assert e.enabled == True

    n.update_extensions(extension_name, id=element_id, droop=0.15, p0=210, enabled=False)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.droop == pytest.approx(0.15, abs=1e-3)
    assert e.p0 == pytest.approx(210, abs=1e-3)
    assert e.enabled == False

    n.remove_extensions(extension_name, [element_id])
    assert n.get_extensions(extension_name).empty


def test_hvdc_operator_active_power_range():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'hvdcOperatorActivePowerRange'
    element_id = 'HVDC1'

    extensions = n.get_extensions(extension_name)
    assert extensions.empty

    n.create_extensions(extension_name, id=element_id, opr_from_cs1_to_cs2=0.1, opr_from_cs2_to_cs1=0.2)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.opr_from_cs1_to_cs2 == pytest.approx(0.1, abs=1e-3)
    assert e.opr_from_cs2_to_cs1 == pytest.approx(0.2, abs=1e-3)

    n.update_extensions(extension_name, id=element_id, opr_from_cs1_to_cs2=0.15, opr_from_cs2_to_cs1=0.25)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.opr_from_cs1_to_cs2 == pytest.approx(0.15, abs=1e-3)
    assert e.opr_from_cs2_to_cs1 == pytest.approx(0.25, abs=1e-3)

    n.remove_extensions(extension_name, [element_id])
    assert n.get_extensions(extension_name).empty


def test_load_detail():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'detail'
    element_id = 'LD1'
    extensions = n.get_extensions(extension_name)
    assert extensions.empty

    n.create_extensions(extension_name, id=element_id, fixed_p0=200, variable_p0=20,
                        fixed_q0=100, variable_q0=10)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.fixed_p0 == 200
    assert e.variable_p0 == 20
    assert e.fixed_q0 == 100
    assert e.variable_q0 == 10

    n.update_extensions(extension_name, id=element_id, fixed_p0=210, variable_p0=25,
                        fixed_q0=110, variable_q0=15)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.fixed_p0 == 210
    assert e.variable_p0 == 25
    assert e.fixed_q0 == 110
    assert e.variable_q0 == 15

    n.remove_extensions(extension_name, [element_id])
    assert n.get_extensions(extension_name).empty


def test_measurements():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'measurements'
    element_id = 'LD1'
    extensions = n.get_extensions(extension_name)
    assert extensions.empty

    n.create_extensions(extension_name, id='measurement1', element_id=element_id, type='CURRENT', value=100,
                        standard_deviation=2, valid=True)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.id == 'measurement1'
    assert e.type == 'CURRENT'
    assert e.side == ''
    assert e.standard_deviation == 100.0
    assert e.value == 2.0
    assert e.valid
    n.create_extensions(extension_name, id=['measurement2', 'measurement3'], element_id=[element_id, element_id],
                        type=['REACTIVE_POWER', 'ACTIVE_POWER'], value=[200, 180], standard_deviation=[21, 23],
                        valid=[True, True])
    e = n.get_extensions(extension_name).loc[element_id]
    expected = pd.DataFrame(index=pd.Series(name='element_id', data=['LD1', 'LD1']),
                            columns=['id', 'type', 'side', 'standard_deviation', 'value', 'valid'],
                            data=[['measurement2', 'REACTIVE_POWER', '', 200, 21, True],
                                  ['measurement3', 'ACTIVE_POWER', '', 180, 23, True]])
    pd.testing.assert_frame_equal(expected, e, check_dtype=False)
    n.remove_extensions(extension_name, [element_id])
    assert n.get_extensions(extension_name).empty


def test_injection_observability():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'injectionObservability'
    element_id = 'LD1'
    extensions = n.get_extensions(extension_name)
    assert extensions.empty

    n.create_extensions(extension_name, id=element_id, observable=False)
    e = n.get_extensions(extension_name).loc[element_id]
    assert not e.observable
    assert np.isnan(e.p_standard_deviation)
    assert not e.p_redundant
    assert np.isnan(e.q_standard_deviation)
    assert not e.q_redundant
    assert np.isnan(e.v_standard_deviation)
    assert not e.v_redundant

    n.create_extensions(extension_name, id=element_id, observable=True, p_standard_deviation=200, p_redundant=True,
                        q_standard_deviation=150, q_redundant=True, v_standard_deviation=400, v_redundant=True)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.observable
    assert e.p_standard_deviation == 200
    assert e.p_redundant
    assert e.q_standard_deviation == 150
    assert e.q_redundant
    assert e.v_standard_deviation == 400
    assert e.v_redundant

    n.remove_extensions(extension_name, [element_id])
    assert n.get_extensions(extension_name).empty


def test_branch_observability():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'branchObservability'
    element_id = 'TWT'
    extensions = n.get_extensions(extension_name)
    assert extensions.empty

    n.create_extensions(extension_name, id=element_id, observable=False)
    e = n.get_extensions(extension_name).loc[element_id]
    assert np.isnan(e.p1_standard_deviation)
    assert not e.p1_redundant
    assert np.isnan(e.p2_standard_deviation)
    assert not e.p2_redundant
    assert np.isnan(e.q1_standard_deviation)
    assert not e.q1_redundant
    assert np.isnan(e.q2_standard_deviation)
    assert not e.q2_redundant

    n.create_extensions(extension_name, id=element_id, observable=True, p1_standard_deviation=195, p1_redundant=True,
                        p2_standard_deviation=200, p2_redundant=True, q1_standard_deviation=190,
                        q1_redundant=True, q2_standard_deviation=205, q2_redundant=True)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.p1_standard_deviation == 195
    assert e.p1_redundant
    assert e.p2_standard_deviation == 200
    assert e.p2_redundant
    assert e.q1_standard_deviation == 190
    assert e.q1_redundant
    assert e.q2_standard_deviation == 205
    assert e.q2_redundant

    n.remove_extensions(extension_name, [element_id])
    assert n.get_extensions(extension_name).empty


def test_connectable_position():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'position'

    # twt
    element_id = 'TWT'
    extensions = n.get_extensions(extension_name)
    assert extensions.empty
    n.create_extensions(extension_name, id=[element_id, element_id, element_id], side=['ONE', 'TWO', 'THREE'],
                        order=[1, 2, 3], feeder_name=['test', 'test1', 'test2'],
                        direction=['TOP', 'BOTTOM', 'UNDEFINED'])
    e = n.get_extensions(extension_name).loc[element_id]
    assert all([a == b for a, b in zip(e.side.values, ['ONE', 'TWO', 'THREE'])])
    assert all([a == b for a, b in zip(e.feeder_name.values, ['test', 'test1', 'test2'])])
    assert all([a == b for a, b in zip(e.direction.values, ['TOP', 'BOTTOM', 'UNDEFINED'])])
    assert all([a == b for a, b in zip(e.order.values, [1, 2, 3])])

    n.remove_extensions(extension_name, [element_id])
    assert n.get_extensions(extension_name).empty

    # load
    n.create_extensions(extension_name, id="LD1", order=3, feeder_name='test', direction='UNDEFINED')
    e = n.get_extensions(extension_name).loc["LD1"]
    assert e.order == 3
    assert e.feeder_name == 'test'
    assert e.direction == 'UNDEFINED'
    assert e.side == ''


def test_busbar_section_position():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'busbarSectionPosition'
    element_id = 'S1VL1_BBS'
    extensions = n.get_extensions(extension_name)
    assert extensions.empty
    n.create_extensions(extension_name, id=element_id, busbar_index=1, section_index=2)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.busbar_index == 1
    assert e.section_index == 1
    n.remove_extensions(extension_name, [element_id])
    assert n.get_extensions(extension_name).empty


def test_identifiable_short_circuit():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'identifiableShortCircuit'
    extensions = n.get_extensions(extension_name)
    assert extensions.empty
    element_id = 'S1VL1'
    n.create_extensions(extension_name, id=element_id, ip_min=3.2, ip_max=5.1)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.ip_min == 3.2
    assert e.ip_max == 5.1
    assert e.equipment_type == 'VOLTAGE_LEVEL'


def test_generator_short_circuit():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'generatorShortCircuit'
    extensions = n.get_extensions(extension_name)
    assert extensions.empty
    element_id = 'GH1'
    n.create_extensions(extension_name, id=element_id, direct_sub_trans_x=9.2, direct_trans_x=2.1,
                        step_up_transformer_x=5)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.direct_sub_trans_x == 9.2
    assert e.direct_trans_x == 2.1
    assert e.step_up_transformer_x == 5


def test_slack_terminal():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'slackTerminal'
    voltage_level_id = 'S1VL2'
    element_id = 'GH1'
    extensions = n.get_extensions(extension_name)
    assert extensions.empty

    n.create_extensions(extension_name, voltage_level_id=voltage_level_id, element_id=element_id)
    e = n.get_extensions(extension_name).loc[voltage_level_id]
    assert e.element_id == element_id
    assert e.bus_id == 'S1VL2_0'

    n.remove_extensions(extension_name, [voltage_level_id])
    assert n.get_extensions(extension_name).empty

    n.create_extensions(extension_name, voltage_level_id=voltage_level_id, bus_id='S1VL2_0')
    e = n.get_extensions(extension_name).loc[voltage_level_id]
    assert e.element_id == 'S1VL2_BBS1'
    assert e.bus_id == 'S1VL2_0'


def test_slack_terminal_bus_breaker():
    n = pn.create_eurostag_tutorial_example1_network()
    n.create_extensions('slackTerminal', voltage_level_id='VLHV1', element_id='NHV1')
    e = n.get_extensions('slackTerminal').loc['VLHV1']
    assert e.element_id == 'NHV1_NHV2_1'   # because there is no terminal associated to buses, not so natural,
                                           # but powsybl-core works this way
    assert e.bus_id == 'VLHV1_0'  # the corresponding "bus view" bus

def test_coordinated_reactive_control():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'coordinatedReactiveControl'
    n.create_extensions(extension_name, generator_id='GH1', q_percent=0.8)
    e = n.get_extensions(extension_name).loc['GH1']
    assert e.q_percent == 0.8
    extensions_information = pypowsybl.network.get_extensions_information()
    assert extensions_information.loc[extension_name]['detail'] == 'it allow to specify the percent of the coordinated reactive control that comes from a generator'
    assert extensions_information.loc[extension_name]['attributes'] == 'index : generator_id (str), q_percent (float)'

def test_standby_automaton():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'standbyAutomaton'
    n.create_extensions(extension_name, id='SVC', standby=True, b0=0.1, low_voltage_threshold=200, low_voltage_setpoint=215,
                        high_voltage_threshold=235, high_voltage_setpoint=225)
    e = n.get_extensions(extension_name).loc['SVC']
    assert e.standby
    pytest.approx(e.b0, 0.1)
    assert e.low_voltage_threshold == 200
    assert e.low_voltage_setpoint == 215
    assert e.high_voltage_threshold == 235
    assert e.high_voltage_setpoint == 225

    extensions_information = pypowsybl.network.get_extensions_information()
    assert extensions_information.loc[extension_name]['detail'] == 'allow to manage standby mode for static var compensator'
    assert extensions_information.loc[extension_name]['attributes'] == 'index : id (str), standby (boolean), b0 (double),' \
                                                                       ' low_voltage_threshold (double), low_voltage_setpoint ' \
                                                                       '(double), high_voltage_threshold (double), ' \
                                                                       'high_voltage_setpoint (double)'

def test_secondary_voltage_control():
    n = pn.create_eurostag_tutorial_example1_network()
    extension_name = 'secondaryVoltageControl'
    zones_df = pd.DataFrame.from_records(
            index='name',
            columns=['name', 'target_v', 'bus_ids'],
            data=[('zone_test', 400, 'NHV1')])
    units_df = pd.DataFrame.from_records(
                index='unit_id',
                columns=['unit_id', 'participate', 'zone_name'],
                data=[('GEN', True, 'zone_test')])
    n.create_extensions(extension_name, [zones_df, units_df])

    e1 = n.get_extensions(extension_name, "zones").loc['zone_test']
    assert e1.target_v == 400
    assert e1.bus_ids == "NHV1"
    e2 = n.get_extensions(extension_name, "units").loc['GEN']
    assert e2.participate
    assert e2.zone_name == 'zone_test'

    n.update_extensions(extension_name, table_name='zones', df=pd.DataFrame.from_records(index="name", data=[{'name': 'zone_test', 'target_v': 225}]))
    e1 = n.get_extensions(extension_name, "zones").loc['zone_test']
    assert e1.target_v == 225
    n.update_extensions(extension_name, table_name='units', df=pd.DataFrame.from_records(index="unit_id", data=[{'unit_id': 'GEN', 'participate': False}]))
    e2 = n.get_extensions(extension_name, "units").loc['GEN']
    assert e2.participate == False


def test_geo_data():
    n = pn.load(str(DATA_DIR.joinpath('MicroGridTestConfiguration_T4_BE_BB_Complete_v2.zip')), {'iidm.import.cgmes.post-processors': 'cgmesGLImport'})
    substation_expected = pd.DataFrame.from_records(index='id',
                                         data=[{'id': '87f7002b-056f-4a6a-a872-1744eea757e3', 'latitude': 51.3251, 'longitude': 4.25926},
                                               {'id': '37e14a0f-5e34-4647-a062-8bfd9305fa9d', 'latitude': 50.8038, 'longitude': 4.30089}])
    pd.testing.assert_frame_equal(n.get_extensions('substationPosition'), substation_expected)
    line_expected = pd.DataFrame.from_records(data=[{'id': 'b58bf21a-096a-4dae-9a01-3f03b60c24c7', 'num': 0, 'latitude': 50.8035, 'longitude': 4.30113},
                                                    {'id': 'b58bf21a-096a-4dae-9a01-3f03b60c24c7', 'num': 1, 'latitude': 50.9169, 'longitude': 4.34509},
                                                    {'id': 'b58bf21a-096a-4dae-9a01-3f03b60c24c7', 'num': 2, 'latitude': 51.0448, 'longitude': 4.29565},
                                                    {'id': 'b58bf21a-096a-4dae-9a01-3f03b60c24c7', 'num': 3, 'latitude': 51.1570, 'longitude': 4.38354},
                                                    {'id': 'b58bf21a-096a-4dae-9a01-3f03b60c24c7', 'num': 4, 'latitude': 51.3251, 'longitude': 4.25926},
                                                    {'id': 'ffbabc27-1ccd-4fdc-b037-e341706c8d29', 'num': 0, 'latitude': 50.8035, 'longitude': 4.30113},
                                                    {'id': 'ffbabc27-1ccd-4fdc-b037-e341706c8d29', 'num': 1, 'latitude': 50.9169, 'longitude': 4.34509},
                                                    {'id': 'ffbabc27-1ccd-4fdc-b037-e341706c8d29', 'num': 2, 'latitude': 51.0448, 'longitude': 4.29565},
                                                    {'id': 'ffbabc27-1ccd-4fdc-b037-e341706c8d29', 'num': 3, 'latitude': 51.1570, 'longitude': 4.38354},
                                                    {'id': 'ffbabc27-1ccd-4fdc-b037-e341706c8d29', 'num': 4, 'latitude': 51.3251, 'longitude': 4.25926}])
    # force num column dtype to be int32 like the one generated by pypowsybl and not int64
    # as a consequence index has to be created after
    line_expected = line_expected.astype({'id': str, 'num': np.int32, 'latitude': np.float64, 'longitude': np.float64})
    line_expected.set_index(['id', 'num'], inplace=True)

    pd.testing.assert_frame_equal(n.get_extensions('linePosition'), line_expected)


def test_cgmes_metadata_extension():
    n = pn.create_eurostag_tutorial_example1_network()
    extension_name = 'cgmesMetadataModels'
    cgmes_id = 'sshId'
    metadata = pd.DataFrame.from_records(index='id',
                                        data=[('sshId', 'STEADY_STATE_HYPOTHESIS', 'SSH description', 1,
                                               'http://powsybl.org', 'steady-state-hypothesis',
                                               'ssh-dependency1/ssh-dependency2', '')],
                                         columns=['id', 'cgmes_subset', 'description', 'version',
                                                  'modeling_authority_set', 'profiles', 'dependent_on', 'supersedes'])

    n.create_extensions(extension_name, [metadata])

    # force num column dtype to be int32 like the one generated by pypowsybl and not int64
    # as a consequence index has to be created after
    metadata.reset_index(inplace=True)
    metadata = metadata.astype({'id': str, 'cgmes_subset' : str, 'description' : str, 'version' : np.int32, 'modeling_authority_set' : str,
                                'profiles' : str, 'dependent_on' : str, 'supersedes' : str})
    metadata.set_index(['id'], inplace=True)

    #test get_extensions
    pd.testing.assert_frame_equal(n.get_extensions(extension_name), metadata)


def test_reference_priorities():
    network = pn.create_eurostag_tutorial_example1_network()
    assert network.get_extensions('referencePriorities').empty

    network.create_extensions('referencePriorities', id='GEN', priority=1)
    e = network.get_extensions('referencePriorities')
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['GEN']),
        columns=['priority'],
        data=[[1]])
    pd.testing.assert_frame_equal(expected, e, check_dtype=False)

    network.create_extensions('referencePriorities', id='LOAD', priority=2)
    e = network.get_extensions('referencePriorities')
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['GEN', 'LOAD']),
        columns=['priority'],
        data=[[1], [2]])
    pd.testing.assert_frame_equal(expected, e, check_dtype=False)

    network.update_extensions('referencePriorities', id=['GEN', 'LOAD'], priority=[3, 4])
    e = network.get_extensions('referencePriorities')
    expected = pd.DataFrame(
        index=pd.Series(name='id', data=['GEN', 'LOAD']),
        columns=['priority'],
        data=[[3], [4]])
    pd.testing.assert_frame_equal(expected, e, check_dtype=False)

    network.remove_extensions('referencePriorities', ['GEN', 'LOAD'])
    assert network.get_extensions('referencePriorities').empty


def test_get_extensions_information():
    extensions_information = pypowsybl.network.get_extensions_information()
    assert extensions_information.loc['cgmesMetadataModels']['detail'] == 'Provides information about CGMES metadata models'
    assert extensions_information.loc['cgmesMetadataModels']['attributes'] == ('index : id (str), cgmes_subset (str), description (str), ' \
                                                                            'version (int), modeling_authority_set (str), profiles (str), ' \
                                                                            'dependent_on (str), supersedes (str) ')
    assert extensions_information.loc['measurements']['detail'] == 'Provides measurement about a specific equipment'
    assert extensions_information.loc['measurements']['attributes'] == 'index : element_id (str),id (str), type (str), ' \
                                                                       'standard_deviation (float), value (float), valid (bool)'
    assert extensions_information.loc['branchObservability']['detail'] == 'Provides information about the observability of a branch'
    assert extensions_information.loc['branchObservability']['attributes'] == 'index : id (str), observable (bool), ' \
                                                                              'p1_standard_deviation (float), p1_redundant (bool), ' \
                                                                              'p2_standard_deviation (float), p2_redundant (bool), ' \
                                                                              'q1_standard_deviation (float), q1_redundant (bool), ' \
                                                                              'q2_standard_deviation (float), q2_redundant (bool)'
    assert extensions_information.loc['hvdcAngleDroopActivePowerControl']['detail'] == 'Active power control mode based on an offset in MW and a droop in MW/degree'
    assert extensions_information.loc['hvdcAngleDroopActivePowerControl']['attributes'] == 'index : id (str), droop (float), p0 (float), enabled (bool)'
    assert extensions_information.loc['injectionObservability']['detail'] == 'Provides information about the observability of a injection'
    assert extensions_information.loc['injectionObservability'][
               'attributes'] == 'index : id (str), observable (bool), p_standard_deviation (float), p_redundant (bool), q_standard_deviation (float), q_redundant (bool), v_standard_deviation (float), v_redundant (bool)'
    assert extensions_information.loc['detail']['detail'] == 'Provides active power setpoint and reactive power setpoint for a load'
    assert extensions_information.loc['detail'][
               'attributes'] == 'index : id (str), fixed_p (float), variable_p (float), fixed_q (float), variable_q (float)'
    assert extensions_information.loc['hvdcOperatorActivePowerRange']['detail'] == ''
    assert extensions_information.loc['hvdcOperatorActivePowerRange']['attributes'] == 'index : id (str), opr_from_cs1_to_cs2 (float), opr_from_cs2_to_cs1 (float)'
    assert extensions_information.loc['activePowerControl']['detail'] == 'Provides information about the participation of generators to balancing'
    assert extensions_information.loc['activePowerControl']['attributes'] == 'index : id (str), participate (bool), droop (float), participation_factor (float), max_target_p (float), min_target_p (float)'
    assert extensions_information.loc['entsoeCategory']['detail'] == 'Provides Entsoe category code for a generator'
    assert extensions_information.loc['entsoeCategory']['attributes'] == 'index : id (str), code (int)'
    assert extensions_information.loc['entsoeArea']['detail'] == 'Provides Entsoe geographical code for a substation'
    assert extensions_information.loc['entsoeArea']['attributes'] == 'index : id (str), code (str)'
    assert extensions_information.loc['generatorShortCircuit']['detail'] == 'it contains the transitory reactance of a generator needed to compute short circuit. A subtransitory reactance can also be contained'
    assert extensions_information.loc['generatorShortCircuit']['attributes'] == 'index : id (str), direct_sub_trans_x (float), direct_trans_x (float), step_up_transformer_x (float)'
    assert extensions_information.loc['identifiableShortCircuit']['detail'] == 'it contains max and min values of current allowed during short circuit on a network element'
    assert extensions_information.loc['identifiableShortCircuit']['attributes'] == 'index : id (str), equipment_type (str), ip_min (float), ip_max (float)'
    assert extensions_information.loc['position']['detail'] == 'it gives the position of a connectable relative to other equipments in the network'
    assert extensions_information.loc['position']['attributes'] == 'index : id (str), side (str), order (int), feeder_name (str), direction (str)'
    assert extensions_information.loc['slackTerminal']['detail'] == 'a terminal that determines the slack bus for loadflow analysis'
    assert extensions_information.loc['slackTerminal']['attributes'] == 'index : voltage_level_id (str), element_id (str), bus_id (str)'
    assert extensions_information.loc['busbarSectionPosition']['detail'] == 'Position information about the BusbarSection'
    assert extensions_information.loc['busbarSectionPosition']['attributes'] == 'index : id (str), busbar_index (int), section_index (int)'
    assert extensions_information.loc['secondaryVoltageControl']['detail'] == 'Provides information about the secondary voltage control zones and units, in two distinct dataframes.'
    assert extensions_information.loc['secondaryVoltageControl']['attributes'] == '[dataframe "zones"] index : name (str), target_v (float), bus_ids (str) / [dataframe "units"] index : unit_id (str), participate (bool), zone_name (str)'
    assert extensions_information.loc['substationPosition']['attributes'] == 'index : id (str), latitude (float), longitude (float)'
    assert extensions_information.loc['linePosition']['attributes'] == 'index : id (str), num (int), latitude (float), longitude (float)'
    assert extensions_information.loc['referencePriorities']['detail'] == 'Defines the angle reference generator, busbar section or load of a power flow calculation, i.e. which bus will be used with a zero-voltage angle.'
    assert extensions_information.loc['referencePriorities']['attributes'] == 'index : id (str), priority (int)'
