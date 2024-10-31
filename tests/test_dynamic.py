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


@pytest.fixture(autouse=True)
def set_up():
    pp.set_config_read(False)


def test_add_mapping():
    static_id = 'test_id'
    dynamic_id = 'test_dynamic_id'
    parameter_id = 'test_parameter'
    model_mapping = dyn.ModelMapping()
    # Equipments
    model_mapping.add_base_load(static_id='LOAD', parameter_set_id='lab', dynamic_model_id='DM_LOAD', model_name='LoadPQ')
    model_mapping.add_load_one_transformer(static_id='LOAD', parameter_set_id='lt', dynamic_model_id='DM_LT', model_name='LoadOneTransformer')
    model_mapping.add_load_one_transformer_tap_changer(static_id='LOAD', parameter_set_id='lt_tc', dynamic_model_id='DM_LT_TC', model_name='LoadOneTransformerTapChanger')
    model_mapping.add_load_two_transformers(static_id='LOAD', parameter_set_id='ltt', dynamic_model_id='DM_LTT', model_name='LoadTwoTransformers')
    model_mapping.add_load_two_transformers_tap_changers(static_id='LOAD', parameter_set_id='ltt_tc', dynamic_model_id='DM_LTT_TC', model_name='LoadTwoTransformersTapChangers')
    model_mapping.add_base_generator(static_id='GEN', parameter_set_id='gen', dynamic_model_id='DM_GEN', model_name='GeneratorFictitious')
    model_mapping.add_synchronized_generator(static_id='GEN', parameter_set_id='sgen', dynamic_model_id='DM_SYNCH_GEN', model_name='GeneratorPVFixed')
    model_mapping.add_synchronous_generator(static_id='GEN', parameter_set_id='ssgen', dynamic_model_id='DM_SYNCHRONOUS_GEN', model_name='GeneratorSynchronousThreeWindings')
    model_mapping.add_wecc(static_id='GEN', parameter_set_id='wecc', dynamic_model_id='DM_WECC', model_name='WT4BWeccCurrentSource')
    model_mapping.add_grid_forming_converter(static_id='GEN', parameter_set_id='gf', dynamic_model_id='DM_GF', model_name='GridFormingConverterMatchingControl')
    model_mapping.add_signal_n_generator(static_id='GEN', parameter_set_id='signal_n', dynamic_model_id='DM_SIGNAL_N', model_name='GeneratorPVSignalN')
    model_mapping.add_hvdc_p(static_id='HVDC_LINE', parameter_set_id='hvdc_p', dynamic_model_id='DM_HVDC_P', model_name='HvdcPV')
    model_mapping.add_hvdc_vsc(static_id='HVDC_LINE', parameter_set_id='hvdc_vsc', dynamic_model_id='DM_HVDC_VSC', model_name='HvdcVSCDanglingP')
    model_mapping.add_base_transformer(static_id='TFO', parameter_set_id='tfo', dynamic_model_id='DM_TFO', model_name='TransformerFixedRatio')
    model_mapping.add_base_static_var_compensator(static_id='SVARC', parameter_set_id='svarc', dynamic_model_id='DM_SVARC', model_name='StaticVarCompensatorPV')
    model_mapping.add_base_line(static_id='LINE', parameter_set_id='l', dynamic_model_id='DM_LINE', model_name='Line')
    model_mapping.add_base_bus(static_id='BUS', parameter_set_id='bus', dynamic_model_id='DM_BUS', model_name='Bus')
    model_mapping.add_infinite_bus(static_id='BUS', parameter_set_id='inf_bus', dynamic_model_id='DM_INF_BUS', model_name='InfiniteBus')
    # Dynamic automation systems
    model_mapping.add_overload_management_system(dynamic_model_id='DM_OV', parameter_set_id='ov', controlled_branch='LINE1',
                                                 i_measurement='LINE2', i_measurement_side='TWO', model_name='OverloadManagementSystem')
    model_mapping.add_two_levels_overload_management_system(dynamic_model_id='DM_TOV', parameter_set_id='tov',
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
    model_mapping.add_tap_changer_blocking_automation_system(dynamic_model_id='DM_TCB', parameter_set_id='tcb',
                                                             transformers='TRA', u_measurements='BUS',
                                                             model_name='TapChangerBlockingAutomaton')
    # Equipment with default model name and dynamic id
    model_mapping.add_base_load(static_id='LOAD', parameter_set_id='lab')
    # Equipment model from Supported models
    model_name = model_mapping.get_supported_models(dyn.DynamicMappingType.BASE_LOAD)[0]
    model_mapping.add_base_load(static_id='LOAD', parameter_set_id='lab', dynamic_model_id='DM_LOAD', model_name=model_name)


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
    generator_mapping_df = pd.DataFrame(
        index=pd.Series(name='static_id', data=network.get_generators().index),
        data={
            'parameter_set_id': 'GSTWPR',
            'model_name': 'GeneratorSynchronousThreeWindings'
        }
    )

    model_mapping.add_base_load(load_mapping_df)
    model_mapping.add_synchronous_generator(generator_mapping_df)


def test_add_event():
    event_mapping = dyn.EventMapping()
    event_mapping.add_disconnection(static_id='GEN', start_time=5)
    event_mapping.add_disconnection(static_id='LINE', start_time=3.3, disconnect_only='TWO')
    event_mapping.add_active_power_variation(static_id='LOAD', start_time=14, delta_p=2)
    event_mapping.add_node_fault(static_id='BUS', start_time=12, fault_time=2, r_pu=0.1, x_pu=0.2)


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


def test_add_curve():
    timeseries = dyn.CurveMapping()
    timeseries.add_curves('test_load_id_1', ['load_PPu', 'load_QPu'])
    timeseries.add_curve('test_load_id_2', 'load_PPu')
