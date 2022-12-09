#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pathlib
import logging
import pandas as pd 
import pytest

import pypowsybl as pp

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent.joinpath('data')

def test_demo():
    network = pp.network.create_eurostag_tutorial_example1_network()
    parameters = pp.flowdecomposition.Parameters()
    branch_ids = ['NHV1_NHV2_1', 'NHV1_NHV2_2']
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_single_element_contingencies(branch_ids) \
        .add_monitored_elements(branch_ids, branch_ids)
    df = flow_decomposition.run(network, parameters)
    print(df)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_fr'
        ],
        data=[
            ['NHV1_NHV2_1'            , 'NHV1_NHV2_1',            '', 'FR', 'BE', 302.444049, 300.0, 0.0, 0.0, 0.0, 300.0, 0.0],
            ['NHV1_NHV2_1_NHV1_NHV2_2', 'NHV1_NHV2_1', 'NHV1_NHV2_2', 'FR', 'BE', 610.562161, 600.0, 0.0, 0.0, 0.0, 600.0, 0.0],
            ['NHV1_NHV2_2'            , 'NHV1_NHV2_2',            '', 'FR', 'BE', 302.444049, 300.0, 0.0, 0.0, 0.0, 300.0, 0.0],
            ['NHV1_NHV2_2_NHV1_NHV2_1', 'NHV1_NHV2_2', 'NHV1_NHV2_1', 'FR', 'BE', 610.562161, 600.0, 0.0, 0.0, 0.0, 600.0, 0.0],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_demo_one_by_one():
    network = pp.network.create_eurostag_tutorial_example1_network()
    parameters = pp.flowdecomposition.Parameters()
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_precontingency_monitored_elements('NHV1_NHV2_1') \
        .add_precontingency_monitored_elements(['NHV1_NHV2_2'])
    df = flow_decomposition.run(network, parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_fr'
        ],
        data=[
            ['NHV1_NHV2_1', 'NHV1_NHV2_1', '', 'FR', 'BE', 302.444049, 300.0, 0.0, 0.0, 0.0, 300.0, 0.0],
            ['NHV1_NHV2_2', 'NHV1_NHV2_2', '', 'FR', 'BE', 302.444049, 300.0, 0.0, 0.0, 0.0, 300.0, 0.0],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_run_no_parameters():
    net = pp.network.load(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct'))
    net.update_phase_tap_changers(id="BLOAD 11 BLOAD 12 2", tap=1)
    flow_decomposition = pp.flowdecomposition.create_decomposition().add_precontingency_monitored_elements(['FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 12 1'])
    df = flow_decomposition.run(net)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_fr'],
        data=[
            ['FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 11 1', '', 'FR', 'BE', 192.390656, 188.652703,  29.015809, 163.652703, 0.0, -2.007905, -2.007905],
            ['FGEN  11 BLOAD 12 1', 'FGEN  11 BLOAD 12 1', '', 'FR', 'BE', -76.189072, -88.652703, -87.047428, 163.652703, 0.0,  6.023714,  6.023714],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_run_full_integration():
    net = pp.network.load(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct'))
    net.update_phase_tap_changers(id="BLOAD 11 BLOAD 12 2", tap=1)
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True,
        losses_compensation_epsilon=pp.flowdecomposition.Parameters.DISABLE_LOSSES_COMPENSATION_EPSILON,
        sensitivity_epsilon=pp.flowdecomposition.Parameters.DISABLE_SENSITIVITY_EPSILON,
        rescale_enabled=True,
        dc_fallback_enabled_after_ac_divergence=True)
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_precontingency_monitored_elements(['BLOAD 11 BLOAD 12 2', 'FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 12 1'])
    df = flow_decomposition.run(net, parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_fr'],
        data=[
            ['BLOAD 11 BLOAD 12 2', 'BLOAD 11 BLOAD 12 2', '', 'BE', 'BE', -160.00594493625374, -168.54299036226615,  27.730133478072496, 156.40133330888222, -24.11086055331822, 0.0              , -0.014661297382767557],
            ['FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 11 1', '', 'FR', 'BE',  192.39065600179342,  200.6712560368467 ,  27.81857394392333 , 156.90014831777725,   0.0             , 7.68659503747561 , -0.014661297382767557],
            ['FGEN  11 BLOAD 12 1', 'FGEN  11 BLOAD 12 1', '', 'FR', 'BE',  -76.18907198080873,  -84.72530847149157, -87.04742845831291 , 155.51999087872588,   0.0             , 7.674711445291424,  0.04179811510434703 ],
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
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True)
    branch_id = 'DB000011 DF000011 1'
    contingency_id = 'DD000011 DF000011 1'
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_single_element_contingencies(contingency_id) \
        .add_postcontingency_monitored_elements(branch_id, contingency_id)
    df = flow_decomposition.run(network, parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_de', 'loop_flow_from_fr'
        ],
        data=[
            ['DB000011 DF000011 1_DD000011 DF000011 1', 'DB000011 DF000011 1', 'DD000011 DF000011 1', 'DE', 'DE', -1276.371381, -1269.931599, 360.886938, -5.59417, 842.900073, 9.928691, 0.0, 61.810068],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_with_N1_custom_name():
    network = pp.network.load(DATA_DIR.joinpath('19700101_0000_FO4_UX1.uct'))
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True)
    branch_id = 'DB000011 DF000011 1'
    contingency_element_id = 'DD000011 DF000011 1'
    contingency_id = 'contingency_DD000011 DF000011 1'
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_single_element_contingencies(contingency_element_id, lambda s: f"contingency_{s}") \
        .add_postcontingency_monitored_elements(branch_id, contingency_id)
    df = flow_decomposition.run(network, parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_de', 'loop_flow_from_fr'
        ],
        data=[
            ['DB000011 DF000011 1_contingency_DD000011 DF000011 1', 'DB000011 DF000011 1', 'contingency_DD000011 DF000011 1', 'DE', 'DE', -1276.371381, -1269.931599, 360.886938, -5.59417, 842.900073, 9.928691, 0.0, 61.810068],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_with_N1_and_N():
    network = pp.network.load(DATA_DIR.joinpath('19700101_0000_FO4_UX1.uct'))
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True)
    branch_id = 'DB000011 DF000011 1'
    contingency_id = 'DD000011 DF000011 1'
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_single_element_contingencies(contingency_id) \
        .add_postcontingency_monitored_elements(branch_id, contingency_id) \
        .add_precontingency_monitored_elements(branch_id)
    df = flow_decomposition.run(network, parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_de', 'loop_flow_from_fr'
        ],
        data=[
            ['DB000011 DF000011 1'                    , 'DB000011 DF000011 1', ''                   , 'DE', 'DE', -304.241406 ,  -300.419652, 253.886474, -3.935538,   -7.764527e-10, 6.984903, 0.0, 43.483813],
            ['DB000011 DF000011 1_DD000011 DF000011 1', 'DB000011 DF000011 1', 'DD000011 DF000011 1', 'DE', 'DE', -1276.371381, -1269.931599, 360.886938, -5.59417 ,  842.900073    , 9.928691, 0.0, 61.810068],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_with_N2():
    network = pp.network.load(DATA_DIR.joinpath('19700101_0000_FO4_UX1.uct'))
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True)
    branch_id = 'DB000011 DF000011 1'
    contingency_element_id1 = 'FB000011 FD000011 1'
    contingency_element_id2 = 'FB000021 FD000021 1'
    contingency_id = 'FB000011 FD000011 1_FB000021 FD000021 1'
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_multiple_elements_contingency(elements_ids=[contingency_element_id1, contingency_element_id2]) \
        .add_postcontingency_monitored_elements(branch_id, contingency_id)
    df = flow_decomposition.run(network, parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_de', 'loop_flow_from_fr'
        ],
        data=[
            ['DB000011 DF000011 1_FB000011 FD000011 1_FB000021 FD000021 1', 'DB000011 DF000011 1', 'FB000011 FD000011 1_FB000021 FD000021 1', 'DE', 'DE', -408.23599, -406.204353, 300.398354, -3.398902, -7.500596e-10, 7.919862, 0.0, 101.285039],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_with_N2_custom_name():
    network = pp.network.load(DATA_DIR.joinpath('19700101_0000_FO4_UX1.uct'))
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True)
    branch_id = 'DB000011 DF000011 1'
    contingency_element_id1 = 'FB000011 FD000011 1'
    contingency_element_id2 = 'FB000021 FD000021 1'
    contingency_id = 'my N-2 contingency'
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_multiple_elements_contingency(elements_ids=[contingency_element_id1, contingency_element_id2], contingency_id=contingency_id) \
        .add_postcontingency_monitored_elements(branch_id, contingency_id)
    df = flow_decomposition.run(network, parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_de', 'loop_flow_from_fr'
        ],
        data=[
            ['DB000011 DF000011 1_my N-2 contingency', 'DB000011 DF000011 1', 'my N-2 contingency', 'DE', 'DE', -408.23599, -406.204353, 300.398354, -3.398902, -7.500596e-10, 7.919862, 0.0, 101.285039],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_with_N1_N2_and_N():
    network = pp.network.load(DATA_DIR.joinpath('19700101_0000_FO4_UX1.uct'))
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
    df = flow_decomposition.run(network, parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_de', 'loop_flow_from_fr'
        ],
        data=[
            ['DB000011 DF000011 1'                                        , 'DB000011 DF000011 1',                                        '', 'DE', 'DE',  -304.241406,  -300.419652, 253.886474, -3.935538, -7.764527e-10,   6.984903,  0.000000e+00,  43.483813],
            ['DB000011 DF000011 1_DD000011 DF000011 1'                    , 'DB000011 DF000011 1',                     'DD000011 DF000011 1', 'DE', 'DE', -1276.371381, -1269.931599, 360.886938, -5.594170,  8.429001e+02,   9.928691,  0.000000e+00,  61.810068],
            ['DB000011 DF000011 1_FB000011 FD000011 1'                    , 'DB000011 DF000011 1',                     'FB000011 FD000011 1', 'DE', 'DE',  -351.682830,  -347.251431, 275.882353, -3.579647, -7.097789e-10,   4.929379,  0.000000e+00,  70.019345],
            ['DB000011 DF000011 1_FB000011 FD000011 1_FB000021 FD000021 1', 'DB000011 DF000011 1', 'FB000011 FD000011 1_FB000021 FD000021 1', 'DE', 'DE',  -408.235990,  -406.204353, 300.398354, -3.398902, -7.500596e-10,   7.919862,  0.000000e+00, 101.285039],
            ['DB000011 DF000011 1_FB000021 FD000021 1'                    , 'DB000011 DF000011 1',                     'FB000021 FD000021 1', 'DE', 'DE',  -328.997790,  -326.303676, 264.210344, -3.893257, -8.114256e-10,   9.071770,  0.000000e+00,  56.914820],
            ['DD000011 DF000011 1'                                        , 'DD000011 DF000011 1',                                        '', 'DE', 'DE', -1152.114761, -1150.209826, 126.943237, -1.967769,  1.000000e+03,   3.492451,  0.000000e+00,  21.741906],
            ['DD000011 DF000011 1_FB000011 FD000011 1'                    , 'DD000011 DF000011 1',                     'FB000011 FD000011 1', 'DE', 'DE', -1175.834546, -1173.625715, 137.941177, -1.789823,  1.000000e+03,   2.464690,  0.000000e+00,  35.009673],
            ['DD000011 DF000011 1_FB000011 FD000011 1_FB000021 FD000021 1', 'DD000011 DF000011 1', 'FB000011 FD000011 1_FB000021 FD000021 1', 'DE', 'DE', -1204.110021, -1203.102176, 150.199177, -1.699451,  1.000000e+03,   3.959931,  0.000000e+00,  50.642520],
            ['DD000011 DF000011 1_FB000021 FD000021 1'                    , 'DD000011 DF000011 1',                     'FB000021 FD000021 1', 'DE', 'DE', -1164.492469, -1163.151838, 132.105172, -1.946629,  1.000000e+03,   4.535885,  0.000000e+00,  28.457410],
            ['FB000011 FD000011 1'                                        , 'FB000011 FD000011 1',                                        '', 'FR', 'FR',  -434.708616,  -432.267648, 203.026817,  3.284953,  2.449288e+02, -18.972937,  6.172911e-10,   0.000000],
            ['FB000011 FD000011 1_DD000011 DF000011 1'                    , 'FB000011 FD000011 1',                     'DD000011 DF000011 1', 'FR', 'FR',  -525.114854,  -522.242562, 212.956924,  3.131025,  2.466296e+02, -18.699741,  7.822478e+01,   0.000000],
            ['FB000011 FD000011 1_FB000021 FD000021 1'                    , 'FB000011 FD000011 1',                     'FB000021 FD000021 1', 'FR', 'FR',  -563.897114,  -566.326691, 256.496405,  3.503932,  3.144909e+02,  -8.164593,  4.357865e-10,   0.000000],
            ['FB000021 FD000021 1'                                        , 'FB000021 FD000021 1',                                        '', 'FR', 'FR',  -252.451244,  -262.257776, 104.601785,  0.428385,  1.360834e+02,  21.144208, -3.550333e-10,   0.000000],
            ['FB000021 FD000021 1_DD000011 DF000011 1'                    , 'FB000021 FD000021 1',                     'DD000011 DF000011 1', 'FR', 'FR',  -281.760393,  -292.997465, 107.994380,  0.375795,  1.366645e+02,  21.237545,  2.672529e+01,   0.000000],
            ['FB000021 FD000021 1_FB000011 FD000011 1'                    , 'FB000021 FD000021 1',                     'FB000011 FD000011 1', 'FR', 'FR',  -332.710857,  -345.125041, 143.522757,  1.058122,  1.830371e+02,  17.507027, -2.366729e-10,   0.000000],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_add_monitored_element_no_contingency():
    network = pp.network.create_eurostag_tutorial_example1_network()
    parameters = pp.flowdecomposition.Parameters()
    branch_ids = ['NHV1_NHV2_1', 'NHV1_NHV2_2']
    flow_decomposition = pp.flowdecomposition.create_decomposition() \
        .add_monitored_elements(branch_ids)
    df = flow_decomposition.run(network, parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_fr'
        ],
        data=[
            ['NHV1_NHV2_1', 'NHV1_NHV2_1', '', 'FR', 'BE', 302.444049, 300.0, 0.0, 0.0, 0.0, 300.0, 0.0],
            ['NHV1_NHV2_2', 'NHV1_NHV2_2', '', 'FR', 'BE', 302.444049, 300.0, 0.0, 0.0, 0.0, 300.0, 0.0],
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
    test_flow_decomposition_with_N1()
    test_flow_decomposition_with_N1_custom_name()
    test_flow_decomposition_with_N1_and_N()
    test_flow_decomposition_with_N2()
    test_flow_decomposition_with_N2_custom_name()
    test_flow_decomposition_with_N1_N2_and_N()
    test_flow_decomposition_add_monitored_element_no_contingency()
