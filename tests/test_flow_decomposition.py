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
            ['DB000011 DF000011 1_DD000011 DF000011 1', 'DB000011 DF000011 1', 'DD000011 DF000011 1', 'DE', 'DE', -1276.371381, -1269.931599, 267.200673, 133.910589, -5.59417, 842.900073, -0.4290292, 0.0,  31.943461],
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
            ['DB000011 DF000011 1_contingency_DD000011 DF000011 1', 'DB000011 DF000011 1', 'contingency_DD000011 DF000011 1', 'DE', 'DE', -1276.371381, -1269.931599, 267.200673, 133.910589, -5.59417, 842.900073, -0.4290292, 0.0,  31.943461],
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
            ['DB000011 DF000011 1'                    , 'DB000011 DF000011 1',                    '', 'DE', 'DE',  -304.241406,  -300.419652, 187.977534,  94.207032, -3.935538, -7.764527e-10, -0.3018250, 0.0, 22.47244768],
            ['DB000011 DF000011 1_DD000011 DF000011 1', 'DB000011 DF000011 1', 'DD000011 DF000011 1', 'DE', 'DE', -1276.371381, -1269.931599, 267.200673, 133.910589, -5.59417, 842.900073, -0.4290292, 0.0,  31.943461],
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
            ['DB000011 DF000011 1_FB000011 FD000011 1_FB000021 FD000021 1', 'DB000011 DF000011 1', 'FB000011 FD000011 1_FB000021 FD000021 1', 'DE', 'DE', -408.23599, -406.204353, 221.356569, 138.258178, -3.398902, -7.500596e-10, 1.62672509, 0.0, 48.361782],
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
            ['DB000011 DF000011 1_my N-2 contingency', 'DB000011 DF000011 1', 'my N-2 contingency', 'DE', 'DE', -408.23599, -406.204353, 221.356569, 138.258178, -3.398902, -7.500596e-10, 1.62672509, 0.0, 48.361782],
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
            ['DB000011 DF000011 1'                                        , 'DB000011 DF000011 1',                                        '', 'DE', 'DE',  -304.241406,  -300.419652, 187.977534,  94.207032, -3.935538, -7.764527e-10, -0.3018250, 0.0, 22.47244768],
            ['DB000011 DF000011 1_DD000011 DF000011 1'                    , 'DB000011 DF000011 1',                     'DD000011 DF000011 1', 'DE', 'DE', -1276.371381, -1269.931599, 267.200673, 133.910589, -5.59417, 842.900073, -0.4290292, 0.0,  31.943461],
            ['DB000011 DF000011 1_FB000011 FD000011 1'                    , 'DB000011 DF000011 1',                     'FB000011 FD000011 1', 'DE', 'DE',  -351.682830,  -347.251431, 203.42173294, 119.009883, -3.579647, -7.097789e-10, -1.698409206,  0.000000e+00, 30.097870961],
            ['DB000011 DF000011 1_FB000011 FD000011 1_FB000021 FD000021 1', 'DB000011 DF000011 1', 'FB000011 FD000011 1_FB000021 FD000021 1', 'DE', 'DE',  -408.23599, -406.204353, 221.356569, 138.258178, -3.398902, -7.500596e-10, 1.62672509, 0.0, 48.361782],
            ['DB000011 DF000011 1_FB000021 FD000021 1'                    , 'DB000011 DF000011 1',                     'FB000021 FD000021 1', 'DE', 'DE',  -328.997790,  -326.303676, 195.6430251117, 100.997030, -3.893257, -8.114256e-10, 1.86332498,  0.000000e+00,  31.69355321],
            ['DD000011 DF000011 1'                                        , 'DD000011 DF000011 1',                                        '', 'DE', 'DE', -1152.114761, -1150.209826, 93.98876746,  47.103516, -1.967769,  999.999999, -0.1509125,  0.000000e+00,  11.23622384],
            ['DD000011 DF000011 1_FB000011 FD000011 1'                    , 'DD000011 DF000011 1',                     'FB000011 FD000011 1', 'DE', 'DE', -1175.834546, -1173.625715, 101.710866471,  59.504941, -1.789823,  999.999999, -0.849204603,  0.000000e+00,  15.04893548],
            ['DD000011 DF000011 1_FB000011 FD000011 1_FB000021 FD000021 1', 'DD000011 DF000011 1', 'FB000011 FD000011 1_FB000021 FD000021 1', 'DE', 'DE', -1204.110021, -1203.102176, 110.6782849,  69.129089, -1.699451,  999.999999, 0.8133625487,  0.000000e+00,   24.1808912],
            ['DD000011 DF000011 1_FB000021 FD000021 1'                    , 'DD000011 DF000011 1',                     'FB000021 FD000021 1', 'DE', 'DE', -1164.492469, -1163.151838, 97.821512,  50.498515, -1.946629,  999.999999, 0.931662493,  0.000000e+00,  15.8467766],
            ['FB000011 FD000011 1'                                        , 'FB000011 FD000011 1',                                        '', 'FR', 'FR',  -434.708616,  -432.267648, 142.5533540, 228.935782,  3.284953,  70.384338709, -12.8907798,  6.172911e-10,   0.000000],
            ['FB000011 FD000011 1_DD000011 DF000011 1'                    , 'FB000011 FD000011 1',                     'DD000011 DF000011 1', 'FR', 'FR',  -525.114854,  -522.242562, 149.90560498, 232.620444,  3.131025,  71.263289881, -12.902584,  7.822478e+01,   0.000000],
            ['FB000011 FD000011 1_FB000021 FD000021 1'                    , 'FB000011 FD000011 1',                     'FB000021 FD000021 1', 'FR', 'FR',  -563.897114,  -566.326691, 182.25461070, 264.102673,  3.503932,  118.14246878, -1.6769924,  4.357865e-10,   0.000000],
            ['FB000021 FD000021 1'                                        , 'FB000021 FD000021 1',                                        '', 'FR', 'FR',  -252.451244,  -262.257776, 77.6669963,  68.796482,  0.428385,  93.428541750,  21.9373706, -3.550333e-10,   0.000000],
            ['FB000021 FD000021 1_DD000011 DF000011 1'                    , 'FB000021 FD000021 1',                     'DD000011 DF000011 1', 'FR', 'FR',  -281.760393,  -292.997465, 80.1788732,  70.055337,  0.375795,  93.728833071,  21.9333375,  2.672529e+01,   0.000000],
            ['FB000021 FD000021 1_FB000011 FD000011 1'                    , 'FB000021 FD000021 1',                     'FB000011 FD000011 1', 'FR', 'FR',  -332.710857,  -345.125041, 104.994987, 112.684296,  1.058122,  106.92147301,   19.466161, -2.366729e-10,   0.000000],
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
            ['BB000011 BD000011 1'                      ,                       'BB000011 BD000011 1', '', 'BE', 'BE',   -61.712936,   -63.129227,  -87.792294479,   92.380889, -44.372397, 93.3857083,    0.000000,  4.214968e-09,   9.52732088],
            ['BB000011 BF000012 1'                      ,                       'BB000011 BF000012 1', '', 'BE', 'BE',  -344.300482,  -335.158953,   35.00337287,   57.819439,  66.255835,  176.36117147,    0.000000, -3.200711e-09,   -0.28086599],
            ['BD000011 BD000021 1'                      ,                       'BD000011 BD000021 1', '', 'BE', 'BE',   -82.158277,   -78.650256, -97.4423629,   -2.812351,  -0.685516,  165.4981885994,    0.000000,  3.158192e-09,   14.0922977],
            ['BD000011 BF000011 1'                      ,                       'BD000011 BF000011 1', '', 'BE', 'BE',  -387.515046,  -391.150005,  157.79904,   23.257990, -39.154607,  259.33663455,    0.000000, -1.061630e-08,  -10.0890528],
            ['BD000021 BF000021 1'                      ,                       'BD000021 BF000021 1', '', 'BE', 'BE',    23.443248,    18.529564,  -82.557882,  -18.474791,   6.837469,  100.2477523,    0.000000,  2.443898e-09,   12.4770168],
            ['BF000011 BF000012 1'                      ,                       'BF000011 BF000012 1', '', 'BE', 'BE',  -344.300482,  -335.158953,   35.0033728,   57.819439,  66.255835,  176.3611714,    0.000000, -3.200626e-09,   -0.28086599],
            ['BF000011 BF000021 1'                      ,                       'BF000011 BF000021 1', '', 'BE', 'BE',  -543.762597,  -538.136259, -188.54038910,  -35.604976, -19.989130,  740.24924766,    0.000000, -2.640466e-10,   42.021507],
            ['DB000011 DD000011 1'                      ,                       'DB000011 DD000011 1', '', 'DE', 'DE',   847.885239,   849.790174, -93.988767463,  -47.103516,   1.967769,  1.000000e+03,   0.150912522,  0.000000e+00,    -11.23622384],
            ['DB000011 DF000011 1'                      ,                       'DB000011 DF000011 1', '', 'DE', 'DE',  -304.241406,  -300.419652,  187.9775349,   94.207032,  -3.935538, -7.764527e-10,  -0.301825044,  0.000000e+00,  22.47244768],
            ['DD000011 DF000011 1'                      ,                       'DD000011 DF000011 1', '', 'DE', 'DE', -1152.114761, -1150.209826,  93.988767463,   47.103516,  -1.967769,  999.9999999,  -0.1509125,  0.000000e+00,   11.2362238],
            ['FB000011 FB000022 1'                      ,                       'FB000011 FB000022 1', '', 'FR', 'FR',  -615.614465,  -622.667529,  169.438010,   44.420517,  -1.637176,  307.8778652,   102.568313, -2.080569e-09,    0.000000],
            ['FB000011 FD000011 1'                      ,                       'FB000011 FD000011 1', '', 'FR', 'FR',  -434.708616,  -432.267648,  142.553354,  228.935782,   3.284953,  70.3843387,  -12.8907798,  6.172911e-10,    0.000000],
            ['FB000011 FF000011 1'                      ,                       'FB000011 FF000011 1', '', 'FR', 'FR',  -956.483973,  -954.845098,  706.146680,  -14.042812,   2.189969,  269.145114,  -8.59385321, -9.411565e-09,    0.000000],
            ['FB000021 FD000021 1'                      ,                       'FB000021 FD000021 1', '', 'FR', 'FR',  -252.451244,  -262.257776,  77.666996,   68.796482,   0.428385,  93.4285417,   21.937370681, -3.550333e-10,    0.000000],
            ['FD000011 FD000021 1'                      ,                       'FD000011 FD000021 1', '', 'FR', 'FR',  -252.451244,  -262.257776,  77.6669963,   68.796482,   0.428385,  93.4285417,   21.937370681, -3.550333e-10,    0.000000],
            ['FD000011 FF000011 1'                      ,                       'FD000011 FF000011 1', '', 'FR', 'FR',  -521.758013,  -522.577451,  563.593326, -242.978594,  -1.094984,  198.7607759,  4.29692663, -1.002883e-08,    0.000000],
            ['FD000011 FF000011 2'                      ,                       'FD000011 FF000011 2', '', 'FR', 'FR',  -521.758013,  -522.577451,  563.593326, -242.978594,  -1.094984,  198.7607759,  4.29692663, -1.002883e-08,    0.000000],
            ['XBD00011 BD000011 1 + XBD00011 DB000011 1', 'XBD00011 BD000011 1 + XBD00011 DB000011 1', '', 'BE', 'DE',   121.821917,   124.685261,  171.516848,  -33.155274,   2.951653,  0.000000e+00,   0.226368779, -8.995130e-09,  -16.854335],
            ['XBD00012 BD000011 1 + XBD00012 DB000011 1', 'XBD00012 BD000011 1 + XBD00012 DB000011 1', '', 'BE', 'DE',   121.821917,   124.685261,  171.516848,  -33.155274,   2.951653,  0.000000e+00,   0.226368779, -8.995130e-09,  -16.854335],
            ['XBF00011 BF000011 1 + XBF00011 FB000011 1', 'XBF00011 BF000011 1 + XBF00011 FB000011 1', '', 'BE', 'FR',  -775.578124,  -764.445217,  679.2620239,  170.472453,   7.112098,  0.000000e+00, -124.0529462, -6.713719e-09,  31.6515882141],
            ['XBF00021 BF000021 1 + XBF00021 FB000021 1', 'XBF00021 BF000021 1 + XBF00021 FB000021 1', '', 'BE', 'FR',  -234.032855,  -242.462652,  169.3858368,   44.108499,  -0.604396,  0.000000e+00,   62.2528419, -1.954547e-09,  -32.6801298],
            ['XBF00022 BF000021 1 + XBF00022 FB000022 1', 'XBF00022 BF000021 1 + XBF00022 FB000022 1', '', 'BE', 'FR',  -234.032855,  -242.462652,  169.3858368,   44.108499,  -0.604396,  0.000000e+00,   62.2528419, -1.954547e-09,  -32.6801298],
            ['XDF00011 DF000011 1 + XDF00011 FD000011 1', 'XDF00011 DF000011 1 + XDF00011 FD000011 1', '', 'DE', 'FR', -1156.356167, -1150.629478, 906.966302,  216.310548,  -5.903306,  0.000000e+00,  -0.45273757, -2.032039e-08, 33.70867153],
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
            ['XBD00011 BD000011 1 + XBD00011 DB000011 1', 'XBD00011 BD000011 1 + XBD00011 DB000011 1', '', 'BE', 'DE',   121.821917,   124.685261,  171.516848, -33.155274,  2.951653, 0.0,   0.2263687, -8.995130e-09,  -16.8543357],
            ['XBD00012 BD000011 1 + XBD00012 DB000011 1', 'XBD00012 BD000011 1 + XBD00012 DB000011 1', '', 'BE', 'DE',   121.821917,   124.685261,  171.516848, -33.155274,  2.951653, 0.0,   0.2263687, -8.995130e-09,   -16.8543357],
            ['XBF00011 BF000011 1 + XBF00011 FB000011 1', 'XBF00011 BF000011 1 + XBF00011 FB000011 1', '', 'BE', 'FR',  -775.578124,  -764.445217,  679.262023, 170.472453,  7.112098, 0.0, -124.05294623, -6.713719e-09,   31.6515882],
            ['XBF00021 BF000021 1 + XBF00021 FB000021 1', 'XBF00021 BF000021 1 + XBF00021 FB000021 1', '', 'BE', 'FR',  -234.032855,  -242.462652,  169.38583,  44.108499, -0.604396, 0.0,   62.25284, -1.954547e-09,   -32.6801298],
            ['XBF00022 BF000021 1 + XBF00022 FB000022 1', 'XBF00022 BF000021 1 + XBF00022 FB000022 1', '', 'BE', 'FR',  -234.032855,  -242.462652,  169.38583,  44.108499, -0.604396, 0.0,   62.25284, -1.954547e-09,   -32.6801298],
            ['XDF00011 DF000011 1 + XDF00011 FD000011 1', 'XDF00011 DF000011 1 + XDF00011 FD000011 1', '', 'DE', 'FR', -1156.356167, -1150.629478, 906.966302, 216.310548, -5.903306, 0.0,  -0.4527375, -2.032039e-08,  33.7086715],
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
            ['BB000011 BB000021 1'                      ,                       'BB000011 BB000021 1', '', 'BE', 'BE',  -206.013417,  -198.288180,  -52.788921600050585,    -49.799671,  21.883438,  269.7468798714898, 0.0,  1.014676e-09, 9.246454891873562],
            ['BB000011 BD000011 1'                      ,                       'BB000011 BD000011 1', '', 'BE', 'BE',   -61.712936,   -63.129227,  -87.79229447909327,     92.380889, -44.372397,   93.38570839607306,  0.0,  4.214968e-09, 9.527320884854145],
            ['BB000011 BF000012 1'                      ,                       'BB000011 BF000012 1', '', 'BE', 'BE',  -344.300482,  -335.158953, 35.00337287904162,     57.819439,  66.255835,   176.3611714754164,    0.0, -3.200711e-09, -0.2808659929808548],
            ['BB000021 BD000021 1'                      ,                       'BB000021 BD000021 1', '', 'BE', 'BE',   -58.715028,   -60.120692, -14.884480437124456,     15.662440,  -7.522985,   65.250436242648,    0.0,  7.147278e-10, 1.6152809556946937],
            ['BB000021 BF000021 1'                      ,                       'BB000021 BF000021 1', '', 'BE', 'BE',   -35.271555,   -41.591128, 67.67340203717542,     34.137231, -14.360453, -34.99731611413792,     0.0, -1.729177e-09, -10.861735847568204],
            ['BD000011 BD000021 1'                      ,                       'BD000011 BD000021 1', '', 'BE', 'BE',   -82.158277,   -78.650256, -97.4423629114242,     -2.812351,  -0.685516,   165.49818859943383,   0.0,  3.158192e-09, 14.0922977589576],
            ['BD000011 BF000011 1'                      ,                       'BD000011 BF000011 1', '', 'BE', 'BE',  -387.515046,  -391.150005, 157.79904023717668,     23.257990, -39.154607,   259.33663455475977,  0.0, -1.061630e-08, -10.089052870815799],
            ['BD000021 BF000021 1'                      ,                       'BD000021 BF000021 1', '', 'BE', 'BE',    23.443248,    18.529564, -82.55788247429986,    -18.474791,   6.837469,   100.2477523567859,   0.0,  2.443898e-09, 12.477016803262893],
            ['BF000011 BF000012 1'                      ,                       'BF000011 BF000012 1', '', 'BE', 'BE',  -344.300482,  -335.158953, 35.00337287904178,     57.819439,  66.255835,   176.3611714754164,    0.0, -3.200626e-09, -0.2808659929808055],
            ['BF000011 BF000021 1'                      ,                       'BF000011 BF000021 1', '', 'BE', 'BE',  -543.762597,  -538.136259, -188.5403891062046,    -35.604976, -19.989130,   740.2492476695082,   0.0, -2.640466e-10, 42.021507061321806],
            ['DB000011 DD000011 1'                      ,                       'DB000011 DD000011 1', '', 'DE', 'DE',   847.885239,   849.790174, -93.9887674630349,    -47.103516,   1.967769,   1000.0000000003879,   0.1509125222859815,  0.000000e+00, -11.236223841529872],
            ['DB000011 DF000011 1'                      ,                       'DB000011 DF000011 1', '', 'DE', 'DE',  -304.241406,  -300.419652, 187.97753492606984,  94.207032,  -3.935538, -7.761400411254726e-10,   -0.3018250445719346,  0.000000e+00, 22.47244768305975],
            ['DD000011 DF000011 1'                      ,                       'DD000011 DF000011 1', '', 'DE', 'DE', -1152.114761, -1150.209826, 93.98876746303495,     47.103516,  -1.967769,   999.9999999996118,    -0.1509125222859531,  0.000000e+00, 11.23622384152988],
            ['FB000011 FB000022 1'                      ,                       'FB000011 FB000022 1', '', 'FR', 'FR',  -615.614465,  -622.667529, 169.43801057120407,     44.420517,  -1.637176,   307.8778652019389,   102.56831317580728, -2.080569e-09, 0.0],
            ['FB000011 FD000011 1'                      ,                       'FB000011 FD000011 1', '', 'FR', 'FR',  -434.708616,  -432.267648, 142.5533540378168,    228.935782,   3.284953,   70.3843387093495,     -12.890779843776393, 6.172911e-10, 0.0],
            ['FB000011 FF000011 1'                      ,                       'FB000011 FF000011 1', '', 'FR', 'FR',  -956.483973,  -954.845098, 706.146680450982,    -14.042812,   2.189969,   269.14511470671783,    -8.593853212283491, -9.411565e-09, 0.0],
            ['FB000021 FD000021 1'                      ,                       'FB000021 FD000021 1', '', 'FR', 'FR',  -252.451244,  -262.257776, 77.66699638261046,     68.796482,   0.428385,   93.42854175083555,    21.93737068122535, -3.550333e-10, 0.0],
            ['FD000011 FD000021 1'                      ,                       'FD000011 FD000021 1', '', 'FR', 'FR',  -252.451244,  -262.257776, 77.66699638261044,     68.796482,   0.428385,   93.42854175083556,    21.937370681225325, -3.550333e-10, 0.0],
            ['FD000011 FF000011 1'                      ,                       'FD000011 FF000011 1', '', 'FR', 'FR',  -521.758013,  -522.577451, 563.5933264131652,   -242.978594,  -1.094984,   198.76077599736834,   4.29692663149288, -1.002883e-08, 0.0],
            ['FD000011 FF000011 2'                      ,                       'FD000011 FF000011 2', '', 'FR', 'FR',  -521.758013,  -522.577451, 563.5933264131652,   -242.978594,  -1.094984,   198.76077599736834,   4.29692663149288, -1.002883e-08, 0.0],
            ['XBD00011 BD000011 1 + XBD00011 DB000011 1', 'XBD00011 BD000011 1 + XBD00011 DB000011 1', '', 'BE', 'DE',   121.821917,   124.685261, 171.51684881384688,    -33.155274,   2.951653,   0.0,   0.2263687796261351,    -8.99490260053426e-09, -16.854335757313862],
            ['XBD00012 BD000011 1 + XBD00012 DB000011 1', 'XBD00012 BD000011 1 + XBD00012 DB000011 1', '', 'BE', 'DE',   121.821917,   124.685261, 171.51684881384688,    -33.155274,   2.951653,   0.0,   0.2263687796261351,   -8.99490260053426e-09, -16.854335757313862],
            ['XBF00011 BF000011 1 + XBF00011 FB000011 1', 'XBF00011 BF000011 1 + XBF00011 FB000011 1', '', 'BE', 'FR',  -775.578124,  -764.445217, 679.2620239175948,    170.472453,   7.112098,   0.0, -124.0529462,     -6.713719358231174e-09, 31.651588214128555],
            ['XBF00021 BF000021 1 + XBF00021 FB000021 1', 'XBF00021 BF000021 1 + XBF00021 FB000021 1', '', 'BE', 'FR',  -234.032855,  -242.462652, 169.3858368088401,     44.108499,  -0.604396,   0.0,   62.252841929,     -1.9545041141100228e-09, -32.68012985607644],
            ['XBF00022 BF000021 1 + XBF00022 FB000022 1', 'XBF00022 BF000021 1 + XBF00022 FB000022 1', '', 'BE', 'FR',  -234.032855,  -242.462652, 169.3858368088401,     44.108499,  -0.604396,   0.0,   62.252841929,     -1.9545041141100228e-09, -32.68012985607644],
            ['XDF00011 DF000011 1 + XDF00011 FD000011 1', 'XDF00011 DF000011 1 + XDF00011 FD000011 1', '', 'DE', 'FR', -1156.356167, -1150.629478, 906.9663024059035,    216.310548,  -5.903306,   0.0,  -0.45273757,     -2.0320612748037092e-08, 33.708671534551655],
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
            ['BB000011 BB000021 1'                                      ,                       'BB000011 BB000021 1',                '', 'BE', 'BE',  -206.013417,  -198.288180, -52.788921600050585,  -49.799671,  21.883438, 269.7468798714898,     0.0,  1.014676e-09,                    9.246454891873562],
            ['BB000011 BD000011 1'                                      ,                       'BB000011 BD000011 1',                '', 'BE', 'BE',   -61.712936,   -63.129227, -87.79229447909327,   92.380889, -44.372397,  93.38570839607306,     0.0,  4.214968e-09,                    9.527320884854145],
            ['BB000011 BF000012 1'                                      ,                       'BB000011 BF000012 1',                '', 'BE', 'BE',  -344.300482,  -335.158953, 35.00337287904162,   57.819439,  66.255835,   176.3611714754164,     0.0, -3.200711e-09,                    -0.2808659929808548],
            ['BD000011 BD000021 1'                                      ,                       'BD000011 BD000021 1',                '', 'BE', 'BE',   -82.158277,   -78.650256, -97.4423629114242,   -2.812351,  -0.685516,   165.49818859943383,    0.0,  3.158192e-09,                    14.0922977589576],
            ['BD000011 BF000011 1'                                      ,                       'BD000011 BF000011 1',                '', 'BE', 'BE',  -387.515046,  -391.150005, 157.79904023717668,   23.257990, -39.154607,  259.33663455475977,    0.0, -1.061630e-08,                    -10.089052870815799],
            ['BD000021 BF000021 1'                                      ,                       'BD000021 BF000021 1',                '', 'BE', 'BE',    23.443248,    18.529564, -82.55788247429986,  -18.474791,   6.837469,  100.2477523567859,     0.0,  2.443898e-09,                    12.477016803262893],
            ['BF000011 BF000012 1'                                      ,                       'BF000011 BF000012 1',                '', 'BE', 'BE',  -344.300482,  -335.158953, 35.00337287904178,   57.819439,  66.255835,   176.3611714754164,     0.0, -3.200626e-09,                    -0.2808659929808055],
            ['BF000011 BF000021 1'                                      ,                       'BF000011 BF000021 1',                '', 'BE', 'BE',  -543.762597,  -538.136259, -188.5403891062046,  -35.604976, -19.989130,  740.2492476695082,     0.0, -2.640466e-10,                    42.021507061321806],
            ['DB000011 DD000011 1'                                      ,                       'DB000011 DD000011 1',                '', 'DE', 'DE',   847.885239,   849.790174, -93.9887674630349,  -47.103516,   1.967769,   1000.0000000003879,    0.1509125222859815,     0.000000e+00,  -11.236223841529872],
            ['DB000011 DF000011 1'                                      ,                       'DB000011 DF000011 1',                '', 'DE', 'DE',  -304.241406,  -300.419652, 187.97753492606984,   94.207032,  -3.935538,  -7.761400411254726e-10, -0.3018250445719346,     0.000000e+00, 22.47244768305975],
            ['DD000011 DF000011 1'                                      ,                       'DD000011 DF000011 1',                '', 'DE', 'DE', -1152.114761, -1150.209826, 93.98876746303495,   47.103516,  -1.967769,   999.9999999996118,     -0.1509125222859531,     0.000000e+00, 11.23622384152988],
            ['DD000011 DF000011 1_N-1 contingency'                      ,                       'DD000011 DF000011 1', 'N-1 contingency', 'DE', 'DE',  -766.672743,  -766.666667, -208.333333338933,  -25.000000,   0.000000,   1000.0000000063851,    2.5351134524953522e-09,  0.000000e+00, -3.3206637439775477e-09],
            ['FB000011 FB000022 1'                                      ,                       'FB000011 FB000022 1',                '', 'FR', 'FR',  -615.614465,  -622.667529, 169.43801057120407,   44.420517,  -1.637176,  307.8778652019389,     102.56831317580728, -2.080569e-09,     0.0],
            ['FB000011 FB000022 1_N-1 contingency'                      ,                       'FB000011 FB000022 1', 'N-1 contingency', 'FR', 'FR',  -818.188897,  -829.786516, 332.69642256603584,   83.357483,  -2.699801,  313.9455924930057,     102.48681817730379, -5.738258e-09,     0.0],
            ['FB000011 FD000011 1'                                      ,                       'FB000011 FD000011 1',                '', 'FR', 'FR',  -434.708616,  -432.267648, 142.5533540378168,  228.935782,   3.284953,   70.3843387093495,      -12.890779843776393,  6.172911e-10,    0.0],
            ['FB000011 FF000011 1'                                      ,                       'FB000011 FF000011 1',                '', 'FR', 'FR',  -956.483973,  -954.845098, 706.146680450982,  -14.042812,   2.189969,    269.14511470671783,    -8.593853212283491, -9.411565e-09,     0.0],
            ['FB000021 FD000021 1'                                      ,                       'FB000021 FD000021 1',                '', 'FR', 'FR',  -252.451244,  -262.257776, 77.66699638261046,   68.796482,   0.428385,   93.42854175083555,     21.93737068122535, -3.550333e-10,      0.0],
            ['FD000011 FD000021 1'                                      ,                       'FD000011 FD000021 1',                '', 'FR', 'FR',  -252.451244,  -262.257776, 77.66699638261044,   68.796482,   0.428385,   93.42854175083556,     21.937370681225325, -3.550333e-10,     0.0],
            ['FD000011 FF000011 1'                                      ,                       'FD000011 FF000011 1',                '', 'FR', 'FR',  -521.758013,  -522.577451, 563.5933264131652, -242.978594,  -1.094984,   198.76077599736834,    4.29692663149288, -1.002883e-08,       0.0],
            ['FD000011 FF000011 2'                                      ,                       'FD000011 FF000011 2',                '', 'FR', 'FR',  -521.758013,  -522.577451, 563.5933264131652, -242.978594,  -1.094984,   198.76077599736834,    4.29692663149288, -1.002883e-08,       0.0],
            ['XBD00011 BD000011 1 + XBD00011 DB000011 1'                , 'XBD00011 BD000011 1 + XBD00011 DB000011 1',                '', 'BE', 'DE',   121.821917,   124.685261, 171.51684881384688,  -33.155274,   2.951653,  0.0,   0.2263687796261351,      -8.99490260053426e-09,        -16.854335757313862],
            ['XBD00012 BD000011 1 + XBD00012 DB000011 1'                , 'XBD00012 BD000011 1 + XBD00012 DB000011 1',                '', 'BE', 'DE',   121.821917,   124.685261, 171.51684881384688,  -33.155274,   2.951653,  0.0,   0.2263687796261351,      -8.99490260053426e-09,        -16.854335757313862],
            ['XBF00011 BF000011 1 + XBF00011 FB000011 1'                , 'XBF00011 BF000011 1 + XBF00011 FB000011 1',                '', 'BE', 'FR',  -775.578124,  -764.445217, 679.2620239175948,  170.472453,   7.112098,   0.0, -124.05294623186687,      -6.713719358231174e-09,        31.651588214128555],
            ['XBF00021 BF000021 1 + XBF00021 FB000021 1'                , 'XBF00021 BF000021 1 + XBF00021 FB000021 1',                '', 'BE', 'FR',  -234.032855,  -242.462652, 169.3858368088401,   44.108499,  -0.604396,   0.0,   62.252841929783,      -1.9545041141100228e-09,         -32.68012985607644],
            ['XBF00022 BF000021 1 + XBF00022 FB000022 1'                , 'XBF00022 BF000021 1 + XBF00022 FB000022 1',                '', 'BE', 'FR',  -234.032855,  -242.462652, 169.3858368088401,   44.108499,  -0.604396,   0.0,   62.252841929783,      -1.9545041141100228e-09,         -32.68012985607644],
            ['XDF00011 DF000011 1 + XDF00011 FD000011 1'                , 'XDF00011 DF000011 1 + XDF00011 FD000011 1',                '', 'DE', 'FR', -1156.356167, -1150.629478, 906.9663024059035,  216.310548,  -5.903306,   0.0,  -0.45273757446,      -2.0320612748037092e-08,           33.708671534551655],
            ['XDF00011 DF000011 1 + XDF00011 FD000011 1_N-2 contingency', 'XDF00011 DF000011 1 + XDF00011 FD000011 1', 'N-2 contingency', 'DE', 'FR', -1537.987831, -1517.908196, 1246.1320519416695,  301.139570,  -3.348742,  0., -73.4151688,       -2.388298980804393e-08,                47.4004847656334],
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
