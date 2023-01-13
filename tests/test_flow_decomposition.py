#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pathlib
import logging
import pandas as pd 

import pypowsybl as pp

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent.joinpath('data')

def define_test_load_flow_parameters():
    return pp.loadflow.Parameters(
        voltage_init_mode=pp.loadflow.VoltageInitMode.UNIFORM_VALUES,
        transformer_voltage_control_on=False, 
        no_generator_reactive_limits=False, 
        phase_shifter_regulation_on=False, 
        twt_split_shunt_admittance=False, 
        simul_shunt=False, 
        read_slack_bus=True, 
        write_slack_bus=False, 
        distributed_slack=True, 
        balance_type=pp.loadflow.BalanceType.PROPORTIONAL_TO_GENERATION_P_MAX, 
        dc_use_transformer_ratio=True, 
        countries_to_balance=[], 
        connected_component_mode=pp.loadflow.ConnectedComponentMode.MAIN, 
        provider_parameters={}
        )

def test_demo():
    network = pp.network.create_eurostag_tutorial_example1_network()
    load_flow_parameters = define_test_load_flow_parameters()
    parameters = pp.flowdecomposition.Parameters()
    branch_ids = ['NHV1_NHV2_1', 'NHV1_NHV2_2']
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_single_element_contingencies(branch_ids) \
        .add_monitored_elements(branch_ids, branch_ids)
    df = flow_decomposition.run(network, parameters, load_flow_parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'x_node_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_fr'
        ],
        data=[
            ['NHV1_NHV2_1'            , 'NHV1_NHV2_1',            '', 'FR', 'BE', 302.444049, 300.0, 0.0, 0.0, 0.0, 0.0, 300.0, 0.0],
            ['NHV1_NHV2_1_NHV1_NHV2_2', 'NHV1_NHV2_1', 'NHV1_NHV2_2', 'FR', 'BE', 610.562161, 600.0, 0.0, 0.0, 0.0, 0.0, 600.0, 0.0],
            ['NHV1_NHV2_2'            , 'NHV1_NHV2_2',            '', 'FR', 'BE', 302.444049, 300.0, 0.0, 0.0, 0.0, 0.0, 300.0, 0.0],
            ['NHV1_NHV2_2_NHV1_NHV2_1', 'NHV1_NHV2_2', 'NHV1_NHV2_1', 'FR', 'BE', 610.562161, 600.0, 0.0, 0.0, 0.0, 0.0, 600.0, 0.0],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_demo_one_by_one():
    network = pp.network.create_eurostag_tutorial_example1_network()
    load_flow_parameters = define_test_load_flow_parameters()
    parameters = pp.flowdecomposition.Parameters()
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_precontingency_monitored_elements('NHV1_NHV2_1') \
        .add_precontingency_monitored_elements(['NHV1_NHV2_2'])
    df = flow_decomposition.run(network, parameters, load_flow_parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'x_node_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_fr'
        ],
        data=[
            ['NHV1_NHV2_1', 'NHV1_NHV2_1', '', 'FR', 'BE', 302.444049, 300.0, 0.0, 0.0, 0.0, 0.0, 300.0, 0.0],
            ['NHV1_NHV2_2', 'NHV1_NHV2_2', '', 'FR', 'BE', 302.444049, 300.0, 0.0, 0.0, 0.0, 0.0, 300.0, 0.0],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_run_no_parameters():
    net = pp.network.load(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct'))
    load_flow_parameters = define_test_load_flow_parameters()
    net.update_phase_tap_changers(id="BLOAD 11 BLOAD 12 2", tap=1)
    flow_decomposition = pp.flowdecomposition.create_decomposition().add_precontingency_monitored_elements(['FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 12 1'])
    df = flow_decomposition.run(net, load_flow_parameters=load_flow_parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'x_node_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_fr'],
        data=[
            ['FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 11 1', '', 'FR', 'BE', 192.390656, 188.652703,  29.015809, 0.0, 163.652703, 0.0, -2.007905, -2.007905],
            ['FGEN  11 BLOAD 12 1', 'FGEN  11 BLOAD 12 1', '', 'FR', 'BE', -76.189072, -88.652703, -87.047428, 0.0, 163.652703, 0.0,  6.023714,  6.023714],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_run_full_integration():
    net = pp.network.load(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct'))
    load_flow_parameters = define_test_load_flow_parameters()
    net.update_phase_tap_changers(id="BLOAD 11 BLOAD 12 2", tap=1)
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True,
        losses_compensation_epsilon=pp.flowdecomposition.Parameters.DISABLE_LOSSES_COMPENSATION_EPSILON,
        sensitivity_epsilon=pp.flowdecomposition.Parameters.DISABLE_SENSITIVITY_EPSILON,
        rescale_enabled=True,
        dc_fallback_enabled_after_ac_divergence=True)
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_precontingency_monitored_elements(['BLOAD 11 BLOAD 12 2', 'FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 12 1'])
    df = flow_decomposition.run(net, parameters, load_flow_parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'x_node_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_fr'],
        data=[
            ['BLOAD 11 BLOAD 12 2', 'BLOAD 11 BLOAD 12 2', '', 'BE', 'BE', -160.00594493625374, -168.54299036226615,  27.730133478072496, 0.0, 156.40133330888222, -24.11086055331822, 0.0              , -0.014661297382767557],
            ['FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 11 1', '', 'FR', 'BE',  192.39065600179342,  200.6712560368467 ,  27.81857394392333 , 0.0, 156.90014831777725,   0.0             , 7.68659503747561 , -0.014661297382767557],
            ['FGEN  11 BLOAD 12 1', 'FGEN  11 BLOAD 12 1', '', 'FR', 'BE',  -76.18907198080873,  -84.72530847149157, -87.04742845831291 , 0.0, 155.51999087872588,   0.0             , 7.674711445291424,  0.04179811510434703 ],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_run_demo_user_guide():
    net = pp.network.load(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct'))
    load_flow_parameters = define_test_load_flow_parameters()
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=False,
        losses_compensation_epsilon=pp.flowdecomposition.Parameters.DISABLE_LOSSES_COMPENSATION_EPSILON,
        sensitivity_epsilon=pp.flowdecomposition.Parameters.DISABLE_SENSITIVITY_EPSILON,
        rescale_enabled=False,
        dc_fallback_enabled_after_ac_divergence=True)
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_single_element_contingency('FGEN  11 BLOAD 11 1') \
        .add_postcontingency_monitored_elements(['FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 12 1', 'BLOAD 11 BLOAD 12 2'], ['FGEN  11 BLOAD 11 1']) \
        .add_multiple_elements_contingency(['FGEN  11 BLOAD 11 1', 'BLOAD 11 BLOAD 12 2']) \
        .add_postcontingency_monitored_elements('FGEN  11 BLOAD 12 1', 'FGEN  11 BLOAD 11 1_BLOAD 11 BLOAD 12 2') \
        .add_interconnections_as_monitored_elements() \
        .add_all_branches_as_monitored_elements()
    df = flow_decomposition.run(net, flow_decomposition_parameters=parameters, load_flow_parameters=load_flow_parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'x_node_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_fr'],
        data=[
            ['BLOAD 11 BLOAD 12 2'                                        , 'BLOAD 11 BLOAD 12 2',                                        '', 'BE', 'BE',   3.005666, -25.0,  28.999015, 0.0, -0.0, -1.999508,  0.000000, -1.999508],
            ['BLOAD 11 BLOAD 12 2_FGEN  11 BLOAD 11 1'                    , 'BLOAD 11 BLOAD 12 2',                     'FGEN  11 BLOAD 11 1', 'BE', 'BE',  32.000000,  -0.0,   0.000000, 0.0,  0.0, -0.000000,  0.000000,  0.000000],
            ['FGEN  11 BLOAD 11 1'                                        , 'FGEN  11 BLOAD 11 1',                                        '', 'FR', 'BE',  29.003009,  25.0,  28.999015, 0.0, -0.0,  0.000000, -1.999508, -1.999508],
            ['FGEN  11 BLOAD 12 1'                                        , 'FGEN  11 BLOAD 12 1',                                        '', 'FR', 'BE',  87.009112,  75.0,  86.997046, 0.0,  0.0,  0.000000, -5.998523, -5.998523],
            ['FGEN  11 BLOAD 12 1_FGEN  11 BLOAD 11 1'                    , 'FGEN  11 BLOAD 12 1',                     'FGEN  11 BLOAD 11 1', 'FR', 'BE', 116.016179, 100.0, 115.996062, 0.0,  0.0,  0.000000, -7.998031, -7.998031],
            ['FGEN  11 BLOAD 12 1_FGEN  11 BLOAD 11 1_BLOAD 11 BLOAD 12 2', 'FGEN  11 BLOAD 12 1', 'FGEN  11 BLOAD 11 1_BLOAD 11 BLOAD 12 2', 'FR', 'BE', 100.034531, 100.0, 115.996062, 0.0,  0.0,  0.000000, -7.998031, -7.998031],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_parameters():
    # Testing setting independently every attributes
    attributes = {
        'enable_losses_compensation': [True, False],
        'losses_compensation_epsilon': [-1, 1e-3, 1e-5],
        'sensitivity_epsilon': [-1, 1e-3, 1e-5],
        'rescale_enabled': [True, False],
        'dc_fallback_enabled_after_ac_divergence': [True, False],
        'sensitivity_variable_batch_size' : [100, 1000, 5000, 15000]
    }

    for attribute, values in attributes.items():
        for value in values:
            parameters = pp.flowdecomposition.Parameters(**dict([(attribute, value)]))
            assert value == getattr(parameters, attribute)

            parameters = pp.flowdecomposition.Parameters()
            setattr(parameters, attribute, value)
            assert value == getattr(parameters, attribute)

def test_flow_decomposition_with_N1():
    network = pp.network.load(DATA_DIR.joinpath('19700101_0000_FO4_UX1.uct'))
    load_flow_parameters = define_test_load_flow_parameters()
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True)
    branch_id = 'DB000011 DF000011 1'
    contingency_id = 'DD000011 DF000011 1'
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_single_element_contingencies([contingency_id]) \
        .add_postcontingency_monitored_elements(branch_id, contingency_id)
    df = flow_decomposition.run(network, parameters, load_flow_parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'x_node_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_de', 'loop_flow_from_fr'
        ],
        data=[
            ['DB000011 DF000011 1_DD000011 DF000011 1', 'DB000011 DF000011 1', 'DD000011 DF000011 1', 'DE', 'DE', -1276.371381, -1269.931599, 360.886938, 133.910589, -5.59417, 842.900073, -40.144946, 0.0,  -22.026886],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_with_N1_custom_name():
    network = pp.network.load(DATA_DIR.joinpath('19700101_0000_FO4_UX1.uct'))
    load_flow_parameters = define_test_load_flow_parameters()
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True)
    branch_id = 'DB000011 DF000011 1'
    contingency_element_id = 'DD000011 DF000011 1'
    contingency_id = 'contingency_DD000011 DF000011 1'
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_single_element_contingencies([contingency_element_id], lambda s: f"contingency_{s}") \
        .add_postcontingency_monitored_elements(branch_id, contingency_id)
    df = flow_decomposition.run(network, parameters, load_flow_parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'x_node_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_de', 'loop_flow_from_fr'
        ],
        data=[
            ['DB000011 DF000011 1_contingency_DD000011 DF000011 1', 'DB000011 DF000011 1', 'contingency_DD000011 DF000011 1', 'DE', 'DE', -1276.371381, -1269.931599, 360.886938, 133.910589, -5.59417, 842.900073, -40.144946, 0.0,  -22.026886],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_with_N1_and_N():
    network = pp.network.load(DATA_DIR.joinpath('19700101_0000_FO4_UX1.uct'))
    load_flow_parameters = define_test_load_flow_parameters()
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True)
    branch_id = 'DB000011 DF000011 1'
    contingency_id = 'DD000011 DF000011 1'
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_single_element_contingency(contingency_id) \
        .add_postcontingency_monitored_elements(branch_id, contingency_id) \
        .add_precontingency_monitored_elements(branch_id)
    df = flow_decomposition.run(network, parameters, load_flow_parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'x_node_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_de', 'loop_flow_from_fr'
        ],
        data=[
            ['DB000011 DF000011 1'                    , 'DB000011 DF000011 1',                    '', 'DE', 'DE',  -304.241406,  -300.419652, 253.886474,  94.207032, -3.935538, -7.764527e-10, -28.242249, 0.0, -15.496067],
            ['DB000011 DF000011 1_DD000011 DF000011 1', 'DB000011 DF000011 1', 'DD000011 DF000011 1', 'DE', 'DE', -1276.371381, -1269.931599, 360.886938, 133.910589, -5.594170,  8.429001e+02, -40.144946, 0.0, -22.026886],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_with_N2():
    network = pp.network.load(DATA_DIR.joinpath('19700101_0000_FO4_UX1.uct'))
    load_flow_parameters = define_test_load_flow_parameters()
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True)
    branch_id = 'DB000011 DF000011 1'
    contingency_element_id1 = 'FB000011 FD000011 1'
    contingency_element_id2 = 'FB000021 FD000021 1'
    contingency_id = 'FB000011 FD000011 1_FB000021 FD000021 1'
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_multiple_elements_contingency(elements_ids=[contingency_element_id1, contingency_element_id2]) \
        .add_postcontingency_monitored_elements(branch_id, contingency_id)
    df = flow_decomposition.run(network, parameters, load_flow_parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'x_node_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_de', 'loop_flow_from_fr'
        ],
        data=[
            ['DB000011 DF000011 1_FB000011 FD000011 1_FB000021 FD000021 1', 'DB000011 DF000011 1', 'FB000011 FD000011 1_FB000021 FD000021 1', 'DE', 'DE', -408.23599, -406.204353, 300.398354, 138.258178, -3.398902, -7.500596e-10, -32.382292, 0.0, 3.329015],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_with_N2_custom_name():
    network = pp.network.load(DATA_DIR.joinpath('19700101_0000_FO4_UX1.uct'))
    load_flow_parameters = define_test_load_flow_parameters()
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True)
    branch_id = 'DB000011 DF000011 1'
    contingency_element_id1 = 'FB000011 FD000011 1'
    contingency_element_id2 = 'FB000021 FD000021 1'
    contingency_id = 'my N-2 contingency'
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_multiple_elements_contingency(elements_ids=[contingency_element_id1, contingency_element_id2], contingency_id=contingency_id) \
        .add_postcontingency_monitored_elements(branch_id, contingency_id)
    df = flow_decomposition.run(network, parameters, load_flow_parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'x_node_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_de', 'loop_flow_from_fr'
        ],
        data=[
            ['DB000011 DF000011 1_my N-2 contingency', 'DB000011 DF000011 1', 'my N-2 contingency', 'DE', 'DE', -408.23599, -406.204353, 300.398354, 138.258178, -3.398902, -7.500596e-10, -32.382292, 0.0, 3.329015],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_with_N1_N2_and_N():
    network = pp.network.load(DATA_DIR.joinpath('19700101_0000_FO4_UX1.uct'))
    load_flow_parameters = define_test_load_flow_parameters()
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True)
    branch_id_1 = 'DB000011 DF000011 1'
    branch_id_2 = 'DD000011 DF000011 1'
    branch_id_3 = 'FB000011 FD000011 1'
    branch_id_4 = 'FB000021 FD000021 1'
    branch_id_list = [branch_id_1, branch_id_2, branch_id_3, branch_id_4]
    contingency_n1_id1 = 'DD000011 DF000011 1'
    contingency_n1_id2 = 'FB000011 FD000011 1'
    contingency_n1_id3 = 'FB000021 FD000021 1'
    contingency_element_n2_id1 = 'FB000011 FD000011 1'
    contingency_element_n2_id2 = 'FB000021 FD000021 1'
    contingency_n2_id = 'FB000011 FD000011 1_FB000021 FD000021 1'
    contingency_id_list = [contingency_n1_id1, contingency_n1_id2, contingency_n1_id3, contingency_n2_id]
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_single_element_contingencies([contingency_n1_id1, contingency_n1_id2, contingency_n1_id3]) \
        .add_multiple_elements_contingency([contingency_element_n2_id1, contingency_element_n2_id2]) \
        .add_monitored_elements(branch_id_list, contingency_id_list)
    df = flow_decomposition.run(network, parameters, load_flow_parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'x_node_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_de', 'loop_flow_from_fr'
        ],
        data=[
            ['DB000011 DF000011 1'                                        , 'DB000011 DF000011 1',                                        '', 'DE', 'DE',  -304.241406,  -300.419652, 253.886474,  94.207032, -3.935538, -7.764527e-10, -28.242249,  0.000000e+00, -15.496067],
            ['DB000011 DF000011 1_DD000011 DF000011 1'                    , 'DB000011 DF000011 1',                     'DD000011 DF000011 1', 'DE', 'DE', -1276.371381, -1269.931599, 360.886938, 133.910589, -5.594170,  8.429001e+02, -40.144946,  0.000000e+00, -22.026886],
            ['DB000011 DF000011 1_FB000011 FD000011 1'                    , 'DB000011 DF000011 1',                     'FB000011 FD000011 1', 'DE', 'DE',  -351.682830,  -347.251431, 275.882353, 119.009883, -3.579647, -7.097789e-10, -32.814824,  0.000000e+00, -11.246334],
            ['DB000011 DF000011 1_FB000011 FD000011 1_FB000021 FD000021 1', 'DB000011 DF000011 1', 'FB000011 FD000011 1_FB000021 FD000021 1', 'DE', 'DE',  -408.235990,  -406.204353, 300.398354, 138.258178, -3.398902, -7.500596e-10, -32.382292,  0.000000e+00,   3.329015],
            ['DB000011 DF000011 1_FB000021 FD000021 1'                    , 'DB000011 DF000011 1',                     'FB000021 FD000021 1', 'DE', 'DE',  -328.997790,  -326.303676, 264.210344, 100.997030, -3.893257, -8.114256e-10, -27.193775,  0.000000e+00,  -7.816665],
            ['DD000011 DF000011 1'                                        , 'DD000011 DF000011 1',                                        '', 'DE', 'DE', -1152.114761, -1150.209826, 126.943237,  47.103516, -1.967769,  1.000000e+03, -14.121124,  0.000000e+00,  -7.748034],
            ['DD000011 DF000011 1_FB000011 FD000011 1'                    , 'DD000011 DF000011 1',                     'FB000011 FD000011 1', 'DE', 'DE', -1175.834546, -1173.625715, 137.941177,  59.504941, -1.789823,  1.000000e+03, -16.407412,  0.000000e+00,  -5.623167],
            ['DD000011 DF000011 1_FB000011 FD000011 1_FB000021 FD000021 1', 'DD000011 DF000011 1', 'FB000011 FD000011 1_FB000021 FD000021 1', 'DE', 'DE', -1204.110021, -1203.102176, 150.199177,  69.129089, -1.699451,  1.000000e+03, -16.191146,  0.000000e+00,   1.664508],
            ['DD000011 DF000011 1_FB000021 FD000021 1'                    , 'DD000011 DF000011 1',                     'FB000021 FD000021 1', 'DE', 'DE', -1164.492469, -1163.151838, 132.105172,  50.498515, -1.946629,  1.000000e+03, -13.596888,  0.000000e+00,  -3.908333],
            ['FB000011 FD000011 1'                                        , 'FB000011 FD000011 1',                                        '', 'FR', 'FR',  -434.708616,  -432.267648, 203.026817, 228.935782,  3.284953,  3.922597e+01, -42.205878,  6.172911e-10,   0.000000],
            ['FB000011 FD000011 1_DD000011 DF000011 1'                    , 'FB000011 FD000011 1',                     'DD000011 DF000011 1', 'FR', 'FR',  -525.114854,  -522.242562, 212.956924, 232.620444,  3.131025,  3.861989e+01, -43.310499,  7.822478e+01,   0.000000],
            ['FB000011 FD000011 1_FB000021 FD000021 1'                    , 'FB000011 FD000011 1',                     'FB000021 FD000021 1', 'FR', 'FR',  -563.897114,  -566.326691, 256.496405, 264.102673,  3.503932,  7.899928e+01, -36.775602,  4.357865e-10,   0.000000],
            ['FB000021 FD000021 1'                                        , 'FB000021 FD000021 1',                                        '', 'FR', 'FR',  -252.451244,  -262.257776, 104.601785,  68.796482,  0.428385,  7.780795e+01,  10.623169, -3.550333e-10,   0.000000],
            ['FB000021 FD000021 1_DD000011 DF000011 1'                    , 'FB000021 FD000021 1',                     'DD000011 DF000011 1', 'FR', 'FR',  -281.760393,  -292.997465, 107.994380,  70.055337,  0.375795,  7.760089e+01,  10.245778,  2.672529e+01,   0.000000],
            ['FB000021 FD000021 1_FB000011 FD000011 1'                    , 'FB000021 FD000021 1',                     'FB000011 FD000011 1', 'FR', 'FR',  -332.710857,  -345.125041, 143.522757, 112.684296,  1.058122,  8.532772e+01,   2.532150, -2.366729e-10,   0.000000],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_add_monitored_element_no_contingency():
    network = pp.network.create_eurostag_tutorial_example1_network()
    load_flow_parameters = define_test_load_flow_parameters()
    parameters = pp.flowdecomposition.Parameters()
    branch_ids = ['NHV1_NHV2_1', 'NHV1_NHV2_2']
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_monitored_elements(branch_ids)
    df = flow_decomposition.run(network, parameters, load_flow_parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'x_node_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_fr'
        ],
        data=[
            ['NHV1_NHV2_1', 'NHV1_NHV2_1', '', 'FR', 'BE', 302.444049, 300.0, 0.0, 0.0, 0.0, 0.0, 300.0, 0.0],
            ['NHV1_NHV2_2', 'NHV1_NHV2_2', '', 'FR', 'BE', 302.444049, 300.0, 0.0, 0.0, 0.0, 0.0, 300.0, 0.0],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_add_5perc_ptdf():
    network = pp.network.load(DATA_DIR.joinpath('19700101_0000_FO4_UX1.uct'))
    load_flow_parameters = define_test_load_flow_parameters()
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True)
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_5perc_ptdf_as_monitored_elements()
    df = flow_decomposition.run(network, parameters, load_flow_parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'x_node_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_de', 'loop_flow_from_fr'
        ],
        data=[
            ['BB000011 BD000011 1'                      ,                       'BB000011 BD000011 1', '', 'BE', 'BE',   -61.712936,   -63.129227,  -85.068312,   92.380889, -44.372397,  8.048160e+01,    0.000000,  4.214968e-09,   19.707443],
            ['BB000011 BF000012 1'                      ,                       'BB000011 BF000012 1', '', 'BE', 'BE',  -344.300482,  -335.158953,   20.697401,   57.819439,  66.255835,  1.917461e+02,    0.000000, -3.200711e-09,   -1.359853],
            ['BD000011 BD000021 1'                      ,                       'BD000011 BD000021 1', '', 'BE', 'BE',   -82.158277,   -78.650256, -107.638863,   -2.812351,  -0.685516,  1.614157e+02,    0.000000,  3.158192e-09,   28.371308],
            ['BD000011 BF000011 1'                      ,                       'BD000011 BF000011 1', '', 'BE', 'BE',  -387.515046,  -391.150005,  126.463114,   23.257990, -39.154607,  3.030107e+02,    0.000000, -1.061630e-08,  -22.427148],
            ['BD000021 BF000021 1'                      ,                       'BD000021 BF000021 1', '', 'BE', 'BE',    23.443248,    18.529564,  -93.216212,  -18.474791,   6.837469,  9.835303e+01,    0.000000,  2.443898e-09,   25.030068],
            ['BF000011 BF000012 1'                      ,                       'BF000011 BF000012 1', '', 'BE', 'BE',  -344.300482,  -335.158953,   20.697401,   57.819439,  66.255835,  1.917461e+02,    0.000000, -3.200626e-09,   -1.359853],
            ['BF000011 BF000021 1'                      ,                       'BF000011 BF000021 1', '', 'BE', 'BE',  -543.762597,  -538.136259, -263.717679,  -35.604976, -19.989130,  7.752996e+02,    0.000000, -2.640466e-10,   82.148403],
            ['DB000011 DD000011 1'                      ,                       'DB000011 DD000011 1', '', 'DE', 'DE',   847.885239,   849.790174, -126.943237,  -47.103516,   1.967769,  1.000000e+03,   14.121124,  0.000000e+00,    7.748034],
            ['DB000011 DF000011 1'                      ,                       'DB000011 DF000011 1', '', 'DE', 'DE',  -304.241406,  -300.419652,  253.886474,   94.207032,  -3.935538, -7.764527e-10,  -28.242249,  0.000000e+00,  -15.496067],
            ['DD000011 DF000011 1'                      ,                       'DD000011 DF000011 1', '', 'DE', 'DE', -1152.114761, -1150.209826,  126.943237,   47.103516,  -1.967769,  1.000000e+03,  -14.121124,  0.000000e+00,   -7.748034],
            ['FB000011 FB000022 1'                      ,                       'FB000011 FB000022 1', '', 'FR', 'FR',  -615.614465,  -622.667529,  216.839953,   44.420517,  -1.637176,  2.763605e+02,   86.683777, -2.080569e-09,    0.000000],
            ['FB000011 FD000011 1'                      ,                       'FB000011 FD000011 1', '', 'FR', 'FR',  -434.708616,  -432.267648,  203.026817,  228.935782,   3.284953,  3.922597e+01,  -42.205878,  6.172911e-10,    0.000000],
            ['FB000011 FF000011 1'                      ,                       'FB000011 FF000011 1', '', 'FR', 'FR',  -956.483973,  -954.845098,  897.255973,  -14.042812,   2.189969,  1.392459e+02,  -69.803918, -9.411565e-09,    0.000000],
            ['FB000021 FD000021 1'                      ,                       'FB000021 FD000021 1', '', 'FR', 'FR',  -252.451244,  -262.257776,  104.601785,   68.796482,   0.428385,  7.780795e+01,   10.623169, -3.550333e-10,    0.000000],
            ['FD000011 FD000021 1'                      ,                       'FD000011 FD000021 1', '', 'FR', 'FR',  -252.451244,  -262.257776,  104.601785,   68.796482,   0.428385,  7.780795e+01,   10.623169, -3.550333e-10,    0.000000],
            ['FD000011 FF000011 1'                      ,                       'FD000011 FF000011 1', '', 'FR', 'FR',  -521.758013,  -522.577451,  694.229156, -242.978594,  -1.094984,  1.000199e+02,  -27.598041, -1.002883e-08,    0.000000],
            ['FD000011 FF000011 2'                      ,                       'FD000011 FF000011 2', '', 'FR', 'FR',  -521.758013,  -522.577451,  694.229156, -242.978594,  -1.094984,  1.000199e+02,  -27.598041, -1.002883e-08,    0.000000],
            ['XBD00011 BD000011 1 + XBD00011 DB000011 1', 'XBD00011 BD000011 1 + XBD00011 DB000011 1', '', 'BE', 'DE',   121.821917,   124.685261,  159.585145,  -33.155274,   2.951653,  0.000000e+00,   30.556687, -8.995130e-09,  -35.252949],
            ['XBD00012 BD000011 1 + XBD00012 DB000011 1', 'XBD00012 BD000011 1 + XBD00012 DB000011 1', '', 'BE', 'DE',   121.821917,   124.685261,  159.585145,  -33.155274,   2.951653,  0.000000e+00,   30.556687, -8.995130e-09,  -35.252949],
            ['XBF00011 BF000011 1 + XBF00011 FB000011 1', 'XBF00011 BF000011 1 + XBF00011 FB000011 1', '', 'BE', 'FR',  -775.578124,  -764.445217,  883.442837,  170.472453,   7.112098,  0.000000e+00, -198.693573, -6.713719e-09,  -97.888598],
            ['XBF00021 BF000021 1 + XBF00021 FB000021 1', 'XBF00021 BF000021 1 + XBF00021 FB000021 1', '', 'BE', 'FR',  -234.032855,  -242.462652,  217.863726,   44.108499,  -0.604396,  0.000000e+00,   45.528473, -1.954547e-09,  -64.433650],
            ['XBF00022 BF000021 1 + XBF00022 FB000022 1', 'XBF00022 BF000021 1 + XBF00022 FB000022 1', '', 'BE', 'FR',  -234.032855,  -242.462652,  217.863726,   44.108499,  -0.604396,  0.000000e+00,   45.528473, -1.954547e-09,  -64.433650],
            ['XDF00011 DF000011 1 + XDF00011 FD000011 1', 'XDF00011 DF000011 1 + XDF00011 FD000011 1', '', 'DE', 'FR', -1156.356167, -1150.629478, 1080.829711,  216.310548,  -5.903306,  0.000000e+00,  -23.613373, -2.032039e-08, -116.994101],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)
    
def test_flow_decomposition_add_interconnections():
    network = pp.network.load(DATA_DIR.joinpath('19700101_0000_FO4_UX1.uct'))
    load_flow_parameters = define_test_load_flow_parameters()
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True)
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_interconnections_as_monitored_elements()
    df = flow_decomposition.run(network, parameters, load_flow_parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'x_node_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_de', 'loop_flow_from_fr'
        ],
        data=[
            ['XBD00011 BD000011 1 + XBD00011 DB000011 1', 'XBD00011 BD000011 1 + XBD00011 DB000011 1', '', 'BE', 'DE',   121.821917,   124.685261,  159.585145, -33.155274,  2.951653, 0.0,   30.556687, -8.995130e-09,  -35.252949],
            ['XBD00012 BD000011 1 + XBD00012 DB000011 1', 'XBD00012 BD000011 1 + XBD00012 DB000011 1', '', 'BE', 'DE',   121.821917,   124.685261,  159.585145, -33.155274,  2.951653, 0.0,   30.556687, -8.995130e-09,  -35.252949],
            ['XBF00011 BF000011 1 + XBF00011 FB000011 1', 'XBF00011 BF000011 1 + XBF00011 FB000011 1', '', 'BE', 'FR',  -775.578124,  -764.445217,  883.442837, 170.472453,  7.112098, 0.0, -198.693573, -6.713719e-09,  -97.888598],
            ['XBF00021 BF000021 1 + XBF00021 FB000021 1', 'XBF00021 BF000021 1 + XBF00021 FB000021 1', '', 'BE', 'FR',  -234.032855,  -242.462652,  217.863726,  44.108499, -0.604396, 0.0,   45.528473, -1.954547e-09,  -64.433650],
            ['XBF00022 BF000021 1 + XBF00022 FB000022 1', 'XBF00022 BF000021 1 + XBF00022 FB000022 1', '', 'BE', 'FR',  -234.032855,  -242.462652,  217.863726,  44.108499, -0.604396, 0.0,   45.528473, -1.954547e-09,  -64.433650],
            ['XDF00011 DF000011 1 + XDF00011 FD000011 1', 'XDF00011 DF000011 1 + XDF00011 FD000011 1', '', 'DE', 'FR', -1156.356167, -1150.629478, 1080.829711, 216.310548, -5.903306, 0.0,  -23.613373, -2.032039e-08, -116.994101],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_add_all_branches():
    network = pp.network.load(DATA_DIR.joinpath('19700101_0000_FO4_UX1.uct'))
    load_flow_parameters = define_test_load_flow_parameters()
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True)
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_all_branches_as_monitored_elements()
    df = flow_decomposition.run(network, parameters, load_flow_parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'x_node_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_de', 'loop_flow_from_fr'
        ],
        data=[
            ['BB000011 BB000021 1'                      ,                       'BB000011 BB000021 1', '', 'BE', 'BE',  -206.013417,  -198.288180,  -64.370910,    -49.799671,  21.883438,  2.722277e+02,    0.000000,  1.014676e-09,   18.347590],
            ['BB000011 BD000011 1'                      ,                       'BB000011 BD000011 1', '', 'BE', 'BE',   -61.712936,   -63.129227,  -85.068312,     92.380889, -44.372397,  8.048160e+01,    0.000000,  4.214968e-09,   19.707443],
            ['BB000011 BF000012 1'                      ,                       'BB000011 BF000012 1', '', 'BE', 'BE',  -344.300482,  -335.158953,   20.697401,     57.819439,  66.255835,  1.917461e+02,    0.000000, -3.200711e-09,   -1.359853],
            ['BB000021 BD000021 1'                      ,                       'BB000021 BD000021 1', '', 'BE', 'BE',   -58.715028,   -60.120692,  -14.422651,     15.662440,  -7.522985,  6.306265e+01,    0.000000,  7.147278e-10,    3.341239],
            ['BB000021 BF000021 1'                      ,                       'BB000021 BF000021 1', '', 'BE', 'BE',   -35.271555,   -41.591128,   78.793561,     34.137231, -14.360453, -3.529038e+01,    0.000000, -1.729177e-09,  -21.688829],
            ['BD000011 BD000021 1'                      ,                       'BD000011 BD000021 1', '', 'BE', 'BE',   -82.158277,   -78.650256, -107.638863,     -2.812351,  -0.685516,  1.614157e+02,    0.000000,  3.158192e-09,   28.371308],
            ['BD000011 BF000011 1'                      ,                       'BD000011 BF000011 1', '', 'BE', 'BE',  -387.515046,  -391.150005,  126.463114,     23.257990, -39.154607,  3.030107e+02,    0.000000, -1.061630e-08,  -22.427148],
            ['BD000021 BF000021 1'                      ,                       'BD000021 BF000021 1', '', 'BE', 'BE',    23.443248,    18.529564,  -93.216212,    -18.474791,   6.837469,  9.835303e+01,    0.000000,  2.443898e-09,   25.030068],
            ['BF000011 BF000012 1'                      ,                       'BF000011 BF000012 1', '', 'BE', 'BE',  -344.300482,  -335.158953,   20.697401,     57.819439,  66.255835,  1.917461e+02,    0.000000, -3.200626e-09,   -1.359853],
            ['BF000011 BF000021 1'                      ,                       'BF000011 BF000021 1', '', 'BE', 'BE',  -543.762597,  -538.136259, -263.717679,    -35.604976, -19.989130,  7.752996e+02,    0.000000, -2.640466e-10,   82.148403],
            ['DB000011 DD000011 1'                      ,                       'DB000011 DD000011 1', '', 'DE', 'DE',   847.885239,   849.790174, -126.943237,    -47.103516,   1.967769,  1.000000e+03,   14.121124,  0.000000e+00,    7.748034],
            ['DB000011 DF000011 1'                      ,                       'DB000011 DF000011 1', '', 'DE', 'DE',  -304.241406,  -300.419652,  253.886474,     94.207032,  -3.935538, -7.764527e-10,  -28.242249,  0.000000e+00,  -15.496067],
            ['DD000011 DF000011 1'                      ,                       'DD000011 DF000011 1', '', 'DE', 'DE', -1152.114761, -1150.209826,  126.943237,     47.103516,  -1.967769,  1.000000e+03,  -14.121124,  0.000000e+00,   -7.748034],
            ['FB000011 FB000022 1'                      ,                       'FB000011 FB000022 1', '', 'FR', 'FR',  -615.614465,  -622.667529,  216.839953,     44.420517,  -1.637176,  2.763605e+02,   86.683777, -2.080569e-09,    0.000000],
            ['FB000011 FD000011 1'                      ,                       'FB000011 FD000011 1', '', 'FR', 'FR',  -434.708616,  -432.267648,  203.026817,    228.935782,   3.284953,  3.922597e+01,  -42.205878,  6.172911e-10,    0.000000],
            ['FB000011 FF000011 1'                      ,                       'FB000011 FF000011 1', '', 'FR', 'FR',  -956.483973,  -954.845098,  897.255973,    -14.042812,   2.189969,  1.392459e+02,  -69.803918, -9.411565e-09,    0.000000],
            ['FB000021 FD000021 1'                      ,                       'FB000021 FD000021 1', '', 'FR', 'FR',  -252.451244,  -262.257776,  104.601785,     68.796482,   0.428385,  7.780795e+01,   10.623169, -3.550333e-10,    0.000000],
            ['FD000011 FD000021 1'                      ,                       'FD000011 FD000021 1', '', 'FR', 'FR',  -252.451244,  -262.257776,  104.601785,     68.796482,   0.428385,  7.780795e+01,   10.623169, -3.550333e-10,    0.000000],
            ['FD000011 FF000011 1'                      ,                       'FD000011 FF000011 1', '', 'FR', 'FR',  -521.758013,  -522.577451,  694.229156,   -242.978594,  -1.094984,  1.000199e+02,  -27.598041, -1.002883e-08,    0.000000],
            ['FD000011 FF000011 2'                      ,                       'FD000011 FF000011 2', '', 'FR', 'FR',  -521.758013,  -522.577451,  694.229156,   -242.978594,  -1.094984,  1.000199e+02,  -27.598041, -1.002883e-08,    0.000000],
            ['XBD00011 BD000011 1 + XBD00011 DB000011 1', 'XBD00011 BD000011 1 + XBD00011 DB000011 1', '', 'BE', 'DE',   121.821917,   124.685261,  159.585145,    -33.155274,   2.951653,  0.000000e+00,   30.556687, -8.995130e-09,  -35.252949],
            ['XBD00012 BD000011 1 + XBD00012 DB000011 1', 'XBD00012 BD000011 1 + XBD00012 DB000011 1', '', 'BE', 'DE',   121.821917,   124.685261,  159.585145,    -33.155274,   2.951653,  0.000000e+00,   30.556687, -8.995130e-09,  -35.252949],
            ['XBF00011 BF000011 1 + XBF00011 FB000011 1', 'XBF00011 BF000011 1 + XBF00011 FB000011 1', '', 'BE', 'FR',  -775.578124,  -764.445217,  883.442837,    170.472453,   7.112098,  0.000000e+00, -198.693573, -6.713719e-09,  -97.888598],
            ['XBF00021 BF000021 1 + XBF00021 FB000021 1', 'XBF00021 BF000021 1 + XBF00021 FB000021 1', '', 'BE', 'FR',  -234.032855,  -242.462652,  217.863726,     44.108499,  -0.604396,  0.000000e+00,   45.528473, -1.954547e-09,  -64.433650],
            ['XBF00022 BF000021 1 + XBF00022 FB000022 1', 'XBF00022 BF000021 1 + XBF00022 FB000022 1', '', 'BE', 'FR',  -234.032855,  -242.462652,  217.863726,     44.108499,  -0.604396,  0.000000e+00,   45.528473, -1.954547e-09,  -64.433650],
            ['XDF00011 DF000011 1 + XDF00011 FD000011 1', 'XDF00011 DF000011 1 + XDF00011 FD000011 1', '', 'DE', 'FR', -1156.356167, -1150.629478, 1080.829711,    216.310548,  -5.903306,  0.000000e+00,  -23.613373, -2.032039e-08, -116.994101],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_combine_xnec_providers():
    network = pp.network.load(DATA_DIR.joinpath('19700101_0000_FO4_UX1.uct'))
    load_flow_parameters = define_test_load_flow_parameters()
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True)
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_interconnections_as_monitored_elements() \
        .add_5perc_ptdf_as_monitored_elements() \
        .add_monitored_elements(['BB000011 BB000021 1', 'BB000011 BD000011 1']) \
        .add_single_element_contingency('XDF00011 DF000011 1 + XDF00011 FD000011 1', 'N-1 contingency') \
        .add_monitored_elements(['FB000011 FB000022 1', 'DD000011 DF000011 1'], 'N-1 contingency') \
        .add_multiple_elements_contingency(['XBF00011 BF000011 1 + XBF00011 FB000011 1', 'BD000021 BF000021 1'], 'N-2 contingency') \
        .add_monitored_elements('XDF00011 DF000011 1 + XDF00011 FD000011 1', 'N-2 contingency', pp.flowdecomposition.ContingencyContextType.SPECIFIC) \
        .add_precontingency_monitored_elements('XDF00011 DF000011 1 + XDF00011 FD000011 1')
    df = flow_decomposition.run(network, parameters, load_flow_parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'x_node_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_de', 'loop_flow_from_fr'
        ],
        data=[
            ['BB000011 BB000021 1'                                      ,                       'BB000011 BB000021 1',                '', 'BE', 'BE',  -206.013417,  -198.288180,  -64.370910,  -49.799671,  21.883438,  2.722277e+02,    0.000000,  1.014676e-09,   18.347590],
            ['BB000011 BD000011 1'                                      ,                       'BB000011 BD000011 1',                '', 'BE', 'BE',   -61.712936,   -63.129227,  -85.068312,   92.380889, -44.372397,  8.048160e+01,    0.000000,  4.214968e-09,   19.707443],
            ['BB000011 BF000012 1'                                      ,                       'BB000011 BF000012 1',                '', 'BE', 'BE',  -344.300482,  -335.158953,   20.697401,   57.819439,  66.255835,  1.917461e+02,    0.000000, -3.200711e-09,   -1.359853],
            ['BD000011 BD000021 1'                                      ,                       'BD000011 BD000021 1',                '', 'BE', 'BE',   -82.158277,   -78.650256, -107.638863,   -2.812351,  -0.685516,  1.614157e+02,    0.000000,  3.158192e-09,   28.371308],
            ['BD000011 BF000011 1'                                      ,                       'BD000011 BF000011 1',                '', 'BE', 'BE',  -387.515046,  -391.150005,  126.463114,   23.257990, -39.154607,  3.030107e+02,    0.000000, -1.061630e-08,  -22.427148],
            ['BD000021 BF000021 1'                                      ,                       'BD000021 BF000021 1',                '', 'BE', 'BE',    23.443248,    18.529564,  -93.216212,  -18.474791,   6.837469,  9.835303e+01,    0.000000,  2.443898e-09,   25.030068],
            ['BF000011 BF000012 1'                                      ,                       'BF000011 BF000012 1',                '', 'BE', 'BE',  -344.300482,  -335.158953,   20.697401,   57.819439,  66.255835,  1.917461e+02,    0.000000, -3.200626e-09,   -1.359853],
            ['BF000011 BF000021 1'                                      ,                       'BF000011 BF000021 1',                '', 'BE', 'BE',  -543.762597,  -538.136259, -263.717679,  -35.604976, -19.989130,  7.752996e+02,    0.000000, -2.640466e-10,   82.148403],
            ['DB000011 DD000011 1'                                      ,                       'DB000011 DD000011 1',                '', 'DE', 'DE',   847.885239,   849.790174, -126.943237,  -47.103516,   1.967769,  1.000000e+03,   14.121124,  0.000000e+00,    7.748034],
            ['DB000011 DF000011 1'                                      ,                       'DB000011 DF000011 1',                '', 'DE', 'DE',  -304.241406,  -300.419652,  253.886474,   94.207032,  -3.935538, -7.764527e-10,  -28.242249,  0.000000e+00,  -15.496067],
            ['DD000011 DF000011 1'                                      ,                       'DD000011 DF000011 1',                '', 'DE', 'DE', -1152.114761, -1150.209826,  126.943237,   47.103516,  -1.967769,  1.000000e+03,  -14.121124,  0.000000e+00,   -7.748034],
            ['DD000011 DF000011 1_N-1 contingency'                      ,                       'DD000011 DF000011 1', 'N-1 contingency', 'DE', 'DE',  -766.672743,  -766.666667, -233.333333,  -25.000000,   0.000000,  1.000000e+03,   -6.250000,  0.000000e+00,   31.250000],
            ['FB000011 FB000022 1'                                      ,                       'FB000011 FB000022 1',                '', 'FR', 'FR',  -615.614465,  -622.667529,  216.839953,   44.420517,  -1.637176,  2.763605e+02,   86.683777, -2.080569e-09,    0.000000],
            ['FB000011 FB000022 1_N-1 contingency'                      ,                       'FB000011 FB000022 1', 'N-1 contingency', 'FR', 'FR',  -818.188897,  -829.786516,  411.394636,   83.357483,  -2.699801,  2.553009e+02,   82.433254, -5.738258e-09,    0.000000],
            ['FB000011 FD000011 1'                                      ,                       'FB000011 FD000011 1',                '', 'FR', 'FR',  -434.708616,  -432.267648,  203.026817,  228.935782,   3.284953,  3.922597e+01,  -42.205878,  6.172911e-10,    0.000000],
            ['FB000011 FF000011 1'                                      ,                       'FB000011 FF000011 1',                '', 'FR', 'FR',  -956.483973,  -954.845098,  897.255973,  -14.042812,   2.189969,  1.392459e+02,  -69.803918, -9.411565e-09,    0.000000],
            ['FB000021 FD000021 1'                                      ,                       'FB000021 FD000021 1',                '', 'FR', 'FR',  -252.451244,  -262.257776,  104.601785,   68.796482,   0.428385,  7.780795e+01,   10.623169, -3.550333e-10,    0.000000],
            ['FD000011 FD000021 1'                                      ,                       'FD000011 FD000021 1',                '', 'FR', 'FR',  -252.451244,  -262.257776,  104.601785,   68.796482,   0.428385,  7.780795e+01,   10.623169, -3.550333e-10,    0.000000],
            ['FD000011 FF000011 1'                                      ,                       'FD000011 FF000011 1',                '', 'FR', 'FR',  -521.758013,  -522.577451,  694.229156, -242.978594,  -1.094984,  1.000199e+02,  -27.598041, -1.002883e-08,    0.000000],
            ['FD000011 FF000011 2'                                      ,                       'FD000011 FF000011 2',                '', 'FR', 'FR',  -521.758013,  -522.577451,  694.229156, -242.978594,  -1.094984,  1.000199e+02,  -27.598041, -1.002883e-08,    0.000000],
            ['XBD00011 BD000011 1 + XBD00011 DB000011 1'                , 'XBD00011 BD000011 1 + XBD00011 DB000011 1',                '', 'BE', 'DE',   121.821917,   124.685261,  159.585145,  -33.155274,   2.951653,  0.000000e+00,   30.556687, -8.995130e-09,  -35.252949],
            ['XBD00012 BD000011 1 + XBD00012 DB000011 1'                , 'XBD00012 BD000011 1 + XBD00012 DB000011 1',                '', 'BE', 'DE',   121.821917,   124.685261,  159.585145,  -33.155274,   2.951653,  0.000000e+00,   30.556687, -8.995130e-09,  -35.252949],
            ['XBF00011 BF000011 1 + XBF00011 FB000011 1'                , 'XBF00011 BF000011 1 + XBF00011 FB000011 1',                '', 'BE', 'FR',  -775.578124,  -764.445217,  883.442837,  170.472453,   7.112098,  0.000000e+00, -198.693573, -6.713719e-09,  -97.888598],
            ['XBF00021 BF000021 1 + XBF00021 FB000021 1'                , 'XBF00021 BF000021 1 + XBF00021 FB000021 1',                '', 'BE', 'FR',  -234.032855,  -242.462652,  217.863726,   44.108499,  -0.604396,  0.000000e+00,   45.528473, -1.954547e-09,  -64.433650],
            ['XBF00022 BF000021 1 + XBF00022 FB000022 1'                , 'XBF00022 BF000021 1 + XBF00022 FB000022 1',                '', 'BE', 'FR',  -234.032855,  -242.462652,  217.863726,   44.108499,  -0.604396,  0.000000e+00,   45.528473, -1.954547e-09,  -64.433650],
            ['XDF00011 DF000011 1 + XDF00011 FD000011 1'                , 'XDF00011 DF000011 1 + XDF00011 FD000011 1',                '', 'DE', 'FR', -1156.356167, -1150.629478, 1080.829711,  216.310548,  -5.903306,  0.000000e+00,  -23.613373, -2.032039e-08, -116.994101],
            ['XDF00011 DF000011 1 + XDF00011 FD000011 1_N-2 contingency', 'XDF00011 DF000011 1 + XDF00011 FD000011 1', 'N-2 contingency', 'DE', 'FR', -1537.987831, -1517.908196, 1520.113996,  301.139570,  -3.348742,  0.000000e+00, -132.425930, -2.388299e-08, -167.570698],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)


if __name__ == "__main__":
    # If you set your .itools/config.yml with a custom Voltage Init mode, it will break the tests !
    logging.basicConfig(level=logging.DEBUG)
    logging.getLogger('powsybl').setLevel(logging.DEBUG)
    pd.set_option('display.max_rows', None)
    pd.set_option('display.max_columns', None)
    pd.set_option('display.width', 2000)
    pd.set_option('display.max_colwidth', None)
    test_demo()
    test_demo_one_by_one()
    test_flow_decomposition_parameters()
    test_flow_decomposition_run_no_parameters()
    test_flow_decomposition_run_full_integration()
    test_flow_decomposition_run_demo_user_guide()
    test_flow_decomposition_with_N1()
    test_flow_decomposition_with_N1_custom_name()
    test_flow_decomposition_with_N1_and_N()
    test_flow_decomposition_with_N2()
    test_flow_decomposition_with_N2_custom_name()
    test_flow_decomposition_with_N1_N2_and_N()
    test_flow_decomposition_add_monitored_element_no_contingency()
    test_flow_decomposition_add_5perc_ptdf()
    test_flow_decomposition_add_interconnections()
    test_flow_decomposition_add_all_branches()
    test_flow_decomposition_combine_xnec_providers()
