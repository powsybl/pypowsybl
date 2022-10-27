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
    assert n.get_extensions('hvdcOperatorActivePowerRange').empty


def test_update_extensions():
    n = pn._create_network('eurostag_tutorial_example1_with_apc_extension')
    n.update_extensions('activePowerControl', pd.DataFrame.from_records(index='id', data=[
        {'id': 'GEN', 'droop': 1.2}
    ]))
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 1
    assert generators_extensions['participate']['GEN']
    assert generators_extensions['droop']['GEN'] == pytest.approx(1.2, abs=1e-3)
    n.update_extensions('activePowerControl', id='GEN', droop=1.4)
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 1
    assert generators_extensions['participate']['GEN']
    assert generators_extensions['droop']['GEN'] == pytest.approx(1.4, abs=1e-3)


def test_remove_extensions():
    n = pn._create_network('eurostag_tutorial_example1_with_apc_extension')
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 1
    n.remove_extensions('activePowerControl', ['GEN', 'GEN2'])
    assert n.get_extensions('activePowerControl').empty


def test_create_extensions():
    n = pn._create_network('eurostag_tutorial_example1')
    n.create_extensions('activePowerControl', pd.DataFrame.from_records(index='id', data=[
        {'id': 'GEN', 'droop': 1.2, 'participate': True}
    ]))
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 1
    assert generators_extensions['participate']['GEN']
    assert generators_extensions['droop']['GEN'] == pytest.approx(1.2, abs=1e-3)

    n.create_extensions('activePowerControl', id='GEN2', droop=1.3, participate=False)
    generators_extensions = n.get_extensions('activePowerControl')
    assert len(generators_extensions) == 2
    assert not generators_extensions['participate']['GEN2']
    assert generators_extensions['droop']['GEN2'] == pytest.approx(1.3, abs=1e-3)


def test_merged_xnode():
    network = pn.load(str(DATA_DIR / 'uxTestGridForMerging.uct'))
    merged_x_nodes = network.get_extensions('mergedXnode')
    x = merged_x_nodes.loc['BBBBBB11 XXXXXX11 1 + FFFFFF11 XXXXXX11 1']
    assert x.code == 'XXXXXX11'
    assert (x.line1, x.line2) == ('BBBBBB11 XXXXXX11 1', 'FFFFFF11 XXXXXX11 1')
    assert (x.r_dp, x.x_dp, x.g1_dp, x.g2_dp, x.b1_dp, x.b2_dp) == (0.5, 0.5, 0.5, 0.5, 0.5, 0.5)

    network.update_extensions('mergedXnode', id='BBBBBB11 XXXXXX11 1 + FFFFFF11 XXXXXX11 1', code='XXXXXX15',
                              line1='BBBBBB11 XXXXXX11 1', line2='FFFFFF11 XXXXXX11 1',
                              r_dp=0.6, x_dp=0.6, g1_dp=0.6, g2_dp=0.6, b1_dp=0.6, b2_dp=0.6,
                              p1=0, q1=0, p2=0, q2=0)
    x = network.get_extensions('mergedXnode').loc['BBBBBB11 XXXXXX11 1 + FFFFFF11 XXXXXX11 1']
    assert x.code == 'XXXXXX15'
    assert (x.line1, x.line2) == ('BBBBBB11 XXXXXX11 1', 'FFFFFF11 XXXXXX11 1')
    assert (x.r_dp, x.x_dp, x.g1_dp, x.g2_dp, x.b1_dp, x.b2_dp) == (0.6, 0.6, 0.6, 0.6, 0.6, 0.6)
    assert (x.p1, x.q1, x.p2, x.q2) == (0, 0, 0, 0)

    network.remove_extensions('mergedXnode', ['BBBBBB11 XXXXXX11 1 + FFFFFF11 XXXXXX11 1',
                                              'BBBBBB11 XXXXXX12 1 + FFFFFF11 XXXXXX12 1'])
    assert network.get_extensions('mergedXnode').empty

    network.create_extensions('mergedXnode', id='BBBBBB11 XXXXXX11 1 + FFFFFF11 XXXXXX11 1', code='XXXXXX11',
                              line1='BBBBBB11 XXXXXX11 1', line2='FFFFFF11 XXXXXX11 1',
                              r_dp=0.4, x_dp=0.4, g1_dp=0.4, g2_dp=0.4, b1_dp=0.4, b2_dp=0.4,
                              p1=0, q1=0, p2=0, q2=0)
    x = network.get_extensions('mergedXnode').loc['BBBBBB11 XXXXXX11 1 + FFFFFF11 XXXXXX11 1']
    assert x.code == 'XXXXXX11'
    assert (x.line1, x.line2) == ('BBBBBB11 XXXXXX11 1', 'FFFFFF11 XXXXXX11 1')
    assert (x.r_dp, x.x_dp, x.g1_dp, x.g2_dp, x.b1_dp, x.b2_dp) == (0.4, 0.4, 0.4, 0.4, 0.4, 0.4)
    assert (x.p1, x.q1, x.p2, x.q2) == (0, 0, 0, 0)


def test_xnode():
    network = pn.load(str(DATA_DIR / 'simple-eu-xnode.uct'))
    x = network.get_extensions('xnode').loc['NNL2AA1  XXXXXX11 1']
    assert x.code == 'XXXXXX11'

    network.update_extensions('xnode', id='NNL2AA1  XXXXXX11 1', code='XXXXXX12')
    e = network.get_extensions('xnode').loc['NNL2AA1  XXXXXX11 1']
    assert e.code == 'XXXXXX12'

    network.remove_extensions('xnode', ['NNL2AA1  XXXXXX11 1'])
    assert network.get_extensions('xnode').empty

    network.create_extensions('xnode', id='NNL2AA1  XXXXXX11 1', code='XXXXXX13')
    e = network.get_extensions('xnode').loc['NNL2AA1  XXXXXX11 1']
    assert e.code == 'XXXXXX13'


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
