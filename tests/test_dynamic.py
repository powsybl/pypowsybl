#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pypowsybl as pp
import pypowsybl.dynamic as dyn
import pytest
import pandas as pd
import pypowsybl.network as pn

@pytest.fixture(autouse=True)
def set_up():
    pp.set_config_read(False)

def test_categories_information():
    model_mapping = dyn.ModelMapping()
    assert model_mapping.get_categories_names()
    info_df = model_mapping.get_categories_information()
    assert info_df.loc['SimplifiedGenerator']['attribute'] == 'index : static_id (str), parameter_set_id (str), model_name (str)'
    assert info_df.loc['SimplifiedGenerator']['description'] == 'Simplified generator not synchronized with the network frequency'
    assert info_df.loc['PhaseShifterP']['attribute'] == 'index : dynamic_model_id (str), parameter_set_id (str), model_name (str), transformer (str)'
    assert info_df.loc['PhaseShifterP']['description'] == 'Phase shifter P'
    assert info_df.loc['TapChangerBlocking']['attribute'] == '[dataframe "Tcb"] index : dynamic_model_id (str), parameter_set_id (str), model_name (str) / [dataframe "Transformers"] index : dynamic_model_id (str), transformer_id (str) / [dataframe "U measurement 1"] index : dynamic_model_id (str), measurement_point_id (str) / [dataframe "U measurement 2"] index : dynamic_model_id (str), measurement_point_id (str) / [dataframe "U measurement 3"] index : dynamic_model_id (str), measurement_point_id (str) / [dataframe "U measurement 4"] index : dynamic_model_id (str), measurement_point_id (str) / [dataframe "U measurement 5"] index : dynamic_model_id (str), measurement_point_id (str)'
    assert info_df.loc['TapChangerBlocking']['description'] == 'Tap changer blocking automation system'

def test_add_mapping():
    model_mapping = dyn.ModelMapping()
    # Equipments
    model_mapping.add_base_load(static_id='LOAD', parameter_set_id='lab', model_name='LoadPQ')
    model_mapping.add_load_one_transformer(static_id='LOAD', parameter_set_id='lt', model_name='LoadOneTransformer')
    model_mapping.add_load_one_transformer_tap_changer(static_id='LOAD', parameter_set_id='lt_tc', model_name='LoadOneTransformerTapChanger')
    model_mapping.add_load_two_transformers(static_id='LOAD', parameter_set_id='ltt', model_name='LoadTwoTransformers')
    model_mapping.add_load_two_transformers_tap_changers(static_id='LOAD', parameter_set_id='ltt_tc', model_name='LoadTwoTransformersTapChangers')
    model_mapping.add_base_generator(static_id='GEN', parameter_set_id='gen', model_name='GeneratorFictitious')
    model_mapping.add_synchronized_generator(static_id='GEN', parameter_set_id='sgen', model_name='GeneratorPVFixed')
    model_mapping.add_synchronous_generator(static_id='GEN', parameter_set_id='ssgen', model_name='GeneratorSynchronousThreeWindings')
    model_mapping.add_wecc(static_id='GEN', parameter_set_id='wecc', model_name='WT4BWeccCurrentSource')
    model_mapping.add_grid_forming_converter(static_id='GEN', parameter_set_id='gf', model_name='GridFormingConverterMatchingControl')
    model_mapping.add_signal_n_generator(static_id='GEN', parameter_set_id='signal_n', model_name='GeneratorPVSignalN')
    model_mapping.add_hvdc_p(static_id='HVDC_LINE', parameter_set_id='hvdc_p', model_name='HvdcPV')
    model_mapping.add_hvdc_vsc(static_id='HVDC_LINE', parameter_set_id='hvdc_vsc', model_name='HvdcVSCDanglingP')
    model_mapping.add_base_transformer(static_id='TFO', parameter_set_id='tfo', model_name='TransformerFixedRatio')
    model_mapping.add_base_static_var_compensator(static_id='SVARC', parameter_set_id='svarc', model_name='StaticVarCompensatorPV')
    model_mapping.add_shunt(static_id='SHUNT', parameter_set_id='sh', model_name='ShuntB')
    model_mapping.add_base_line(static_id='LINE', parameter_set_id='l', model_name='Line')
    model_mapping.add_base_bus(static_id='BUS', parameter_set_id='bus', model_name='Bus')
    model_mapping.add_infinite_bus(static_id='BUS', parameter_set_id='inf_bus', model_name='InfiniteBus')
    model_mapping.add_inertial_grid(static_id='GEN', parameter_set_id='in_grid', model_name='InertialGrid')
    # Dynamic automation systems
    model_mapping.add_overload_management_system(dynamic_model_id='DM_OV', parameter_set_id='ov', controlled_branch='LINE1',
                                                 i_measurement='LINE2', i_measurement_side='TWO', model_name='OverloadManagementSystem')
    model_mapping.add_two_level_overload_management_system(dynamic_model_id='DM_TOV', parameter_set_id='tov',
                                                            controlled_branch= 'LINE1',
                                                            i_measurement_1='LINE1', i_measurement_1_side='TWO',
                                                            i_measurement_2='LINE2', i_measurement_2_side='ONE',
                                                            model_name='TwoLevelsOverloadManagementSystem')
    model_mapping.add_under_voltage_automation_system(dynamic_model_id='DM_UV', parameter_set_id='psi',
                                                      generator='GEN', model_name='UnderVoltage')
    model_mapping.add_phase_shifter_i_automation_system(dynamic_model_id='DM_PS_I', parameter_set_id='psi',
                                                        transformer='TRA', model_name='PhaseShifterI')
    model_mapping.add_phase_shifter_p_automation_system(dynamic_model_id='DM_PS_P', parameter_set_id='psp',
                                                        transformer='TRA', model_name='PhaseShifterP')
    model_mapping.add_phase_shifter_blocking_i_automation_system(dynamic_model_id='DM_PSB_I', parameter_set_id='psb',
                                                                 phase_shifter_id='PSI', model_name='PhaseShifterBlockingI')
    model_mapping.add_tap_changer_automation_system(dynamic_model_id='DM_TC', parameter_set_id='tc', static_id='LOAD',
                                                    side='HIGH_VOLTAGE', model_name='TapChangerAutomaton')
    # Equipment with default model name and dynamic id
    model_mapping.add_base_load(static_id='LOAD', parameter_set_id='lab')
    # Equipment model from Supported models
    model_name = model_mapping.get_supported_models('Load')[0]
    model_mapping.add_base_load(static_id='LOAD', parameter_set_id='lab', model_name=model_name)
    # Dynamic model from category name
    model_mapping.add_dynamic_model(category_name='Load', static_id='LOAD', parameter_set_id='lab', model_name='LoadPQ')


def test_dynamic_dataframe():
    network = pp.network.create_ieee9()
    model_mapping = dyn.ModelMapping()

    load_mapping_df = pd.DataFrame(
        index=pd.Series(name='static_id', data=network.get_loads().index),
        data={
            'parameter_set_id': 'LAB',
            'model_name': 'LoadPQ'
        }
    )
    model_mapping.add_base_load(load_mapping_df)

    generator_mapping_df = pd.DataFrame(
        index=pd.Series(name='static_id', data=network.get_generators().index),
        data={
            'parameter_set_id': 'GSTWPR',
            'model_name': 'GeneratorSynchronousThreeWindings'
        }
    )
    model_mapping.add_synchronous_generator(generator_mapping_df)

    tcb_df = pd.DataFrame.from_records(
        index='dynamic_model_id',
        columns=['dynamic_model_id', 'parameter_set_id', 'model_name'],
        data=[('DM_TCB', 'tcb', 'TapChangerBlockingAutomaton')])
    tfo_df = pd.DataFrame.from_records(
        index='dynamic_model_id',
        columns=['dynamic_model_id', 'transformer_id'],
        data=[('DM_TCB', 'TFO1'),
              ('DM_TCB', 'TFO2'),
              ('DM_TCB', 'TFO3')])
    measurement1_df = pd.DataFrame.from_records(
        index='dynamic_model_id',
        columns=['dynamic_model_id', 'measurement_point_id'],
        data=[('DM_TCB', 'B1'),
              ('DM_TCB', 'BS1')])
    measurement2_df = pd.DataFrame.from_records(
        index='dynamic_model_id',
        columns=['dynamic_model_id', 'measurement_point_id'],
        data=[('DM_TCB', 'B4')])
    model_mapping.add_tap_changer_blocking_automation_system(tcb_df, tfo_df, measurement1_df, measurement2_df)

def test_events_information():
    event_mapping = dyn.EventMapping()
    info_df = event_mapping.get_events_information()
    assert info_df.loc['ActivePowerVariation']['description'] == 'Active power variation on generator or load'
    assert info_df.loc['ActivePowerVariation']['attribute'] == 'index : static_id (str), start_time (double), delta_p (double)'
    assert info_df.loc['ReferenceVoltageVariation']['description'] == 'Reference voltage variation on synchronous/synchronized generator'
    assert info_df.loc['ReferenceVoltageVariation']['attribute'] == 'index : static_id (str), start_time (double), delta_u (double)'


def test_add_event():
    event_mapping = dyn.EventMapping()
    event_mapping.add_disconnection(static_id='GEN', start_time=5)
    event_mapping.add_disconnection(static_id='LINE', start_time=3.3, disconnect_only='TWO')
    event_mapping.add_active_power_variation(static_id='LOAD', start_time=14, delta_p=2)
    event_mapping.add_reactive_power_variation(static_id='LOAD', start_time=15, delta_q=3)
    event_mapping.add_reference_voltage_variation(static_id='GEN', start_time=16, delta_u=4)
    event_mapping.add_node_fault(static_id='BUS', start_time=12, fault_time=2, r_pu=0.1, x_pu=0.2)
    # Event model from event name
    event_mapping.add_event_model(event_name='ActivePowerVariation', static_id='GEN', start_time=1.5, delta_p=5)


def test_add_event_dataframe():
    event_mapping = dyn.EventMapping()
    event_mapping_df = pd.DataFrame(
        index=pd.Series(name='static_id', data=['GEN', 'LOAD']),
        data={
            'start_time': [10, 15],
            'delta_p': [2, 4]
        },
    )
    event_mapping.add_active_power_variation(event_mapping_df)


def test_add_output_variables():
    variables = dyn.OutputVariableMapping()
    variables.add_dynamic_model_curves('test_dyn_load_id_1', 'load_QPu')
    variables.add_dynamic_model_curves('test_dyn_load_id_1', ['load_PPu', 'load_QPu'])
    variables.add_standard_model_curves('test_gen_id_1', 'generator_UStatorPu')
    variables.add_standard_model_curves('test_gen_id_1', ['generator_UStatorPu', 'voltageRegulator_EfdPu'])
    variables.add_dynamic_model_final_state_values('test_dyn_load_id_2', 'load_PPu')
    variables.add_dynamic_model_final_state_values('test_dyn_load_id_2', ['load_PPu', 'load_QPu'])
    variables.add_standard_model_final_state_values('test_bus_id_2', 'Upu_value')
    variables.add_standard_model_final_state_values('test_bus_id_2', ['Upu_value', 'U_value'])


def test_default_parameters():
    parameters = dyn.Parameters()
    assert 0.0 == parameters.start_time
    assert 10.0 == parameters.stop_time
    assert not parameters.provider_parameters


def test_parameters():
    dynawo_param = {
        'solver.type': 'IDA',
        'precision': '1e-5'
    }
    parameters = dyn.Parameters(start_time=20, stop_time=100, provider_parameters=dynawo_param)
    assert 20.0 == parameters.start_time
    assert 100.0 == parameters.stop_time
    assert 'IDA'== parameters.provider_parameters['solver.type']
    assert '1e-5' == parameters.provider_parameters['precision']


def test_synchronous_generator_properties():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'synchronousGeneratorProperties'
    element_id = 'GH1'

    extensions = n.get_extensions(extension_name)
    assert extensions.empty

    n.create_extensions(extension_name, id=element_id, numberOfWindings="THREE_WINDINGS",
                        governor="Proportional", voltageRegulator="Proportional", pss="",
                        auxiliaries=True, internalTransformer=False, rpcl="RPCL1",
                        aggregated=False, qlim=False)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.numberOfWindings == "THREE_WINDINGS"
    assert e.governor == "Proportional"
    assert e.voltageRegulator == "Proportional"
    assert e.pss == ""
    assert e.auxiliaries == True
    assert e.internalTransformer == False
    assert e.rpcl == "RPCL1"
    assert e.uva == "LOCAL"
    assert e.aggregated == False
    assert e.qlim == False

    n.update_extensions(extension_name, id=element_id, numberOfWindings="FOUR_WINDINGS",
                        governor="ProportionalIntegral", voltageRegulator="ProportionalIntegral", pss="Pss",
                        auxiliaries=False, internalTransformer=True, rpcl="RPCL2",
                        uva="DISTANT", aggregated=True, qlim=True)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.numberOfWindings == "FOUR_WINDINGS"
    assert e.governor == "ProportionalIntegral"
    assert e.voltageRegulator == "ProportionalIntegral"
    assert e.pss == "Pss"
    assert e.auxiliaries == False
    assert e.internalTransformer == True
    assert e.rpcl == "RPCL2"
    assert e.uva == "DISTANT"
    assert e.aggregated == True
    assert e.qlim == True

    n.remove_extensions(extension_name, [element_id])
    assert n.get_extensions(extension_name).empty

def test_synchronized_generator_properties():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'synchronizedGeneratorProperties'
    element_id = 'GH1'

    extensions = n.get_extensions(extension_name)
    assert extensions.empty

    n.create_extensions(extension_name, id=element_id,
                        type="PV", rpcl2=True)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.type == "PV"
    assert e.rpcl2 == True

    n.update_extensions(extension_name, id=element_id, type="PfQ", rpcl2=False)
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.type == "PfQ"
    assert e.rpcl2 == False

    n.remove_extensions(extension_name, [element_id])
    assert n.get_extensions(extension_name).empty

def test_generator_connection_level_properties():
    n = pn.create_four_substations_node_breaker_network()
    extension_name = 'generatorConnectionLevel'
    element_id = 'GH1'

    extensions = n.get_extensions(extension_name)
    assert extensions.empty

    n.create_extensions(extension_name, id=element_id, level='TSO')
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.level == 'TSO'

    n.update_extensions(extension_name, id=element_id, level='DSO')
    e = n.get_extensions(extension_name).loc[element_id]
    assert e.level == 'DSO'

    n.remove_extensions(extension_name, [element_id])
    assert n.get_extensions(extension_name).empty
