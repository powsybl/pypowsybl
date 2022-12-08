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

def test_demo():
    network = pp.network.create_eurostag_tutorial_example1_network()
    parameters = pp.flowdecomposition.Parameters()
    flow_decomposition = pp.flowdecomposition.create_decomposition()
    flow_decomposition.add_precontingency_monitored_elements(['NHV1_NHV2_1', 'NHV1_NHV2_2'])
    df = flow_decomposition.run(network, parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_fr'
        ],
        data=[
            ['NHV1_NHV2_1', 'NHV1_NHV2_1', 'FR', 'BE', 302.444049, 300.0, 0.0, 0.0, 0.0, 300.0, 0.0],
            ['NHV1_NHV2_2', 'NHV1_NHV2_2', 'FR', 'BE', 302.444049, 300.0, 0.0, 0.0, 0.0, 300.0, 0.0],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_demo_one_by_one():
    network = pp.network.create_eurostag_tutorial_example1_network()
    parameters = pp.flowdecomposition.Parameters()
    flow_decomposition = pp.flowdecomposition.create_decomposition()
    flow_decomposition.add_precontingency_monitored_elements('NHV1_NHV2_1')
    flow_decomposition.add_precontingency_monitored_elements('NHV1_NHV2_2')
    df = flow_decomposition.run(network, parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_fr'
        ],
        data=[
            ['NHV1_NHV2_1', 'NHV1_NHV2_1', 'FR', 'BE', 302.444049, 300.0, 0.0, 0.0, 0.0, 300.0, 0.0],
            ['NHV1_NHV2_2', 'NHV1_NHV2_2', 'FR', 'BE', 302.444049, 300.0, 0.0, 0.0, 0.0, 300.0, 0.0],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_run_no_parameters():
    net = pp.network.load(str(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct')))
    net.update_phase_tap_changers(id="BLOAD 11 BLOAD 12 2", tap=1)
    flow_decomposition = pp.flowdecomposition.create_decomposition()
    flow_decomposition.add_precontingency_monitored_elements(['FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 12 1'])
    df = flow_decomposition.run(net)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_fr'],
        data=[
            ['FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 11 1', 'FR', 'BE', 192.390656, 188.652703,  29.015809, 163.652703, 0.0, -2.007905, -2.007905],
            ['FGEN  11 BLOAD 12 1', 'FGEN  11 BLOAD 12 1', 'FR', 'BE', -76.189072, -88.652703, -87.047428, 163.652703, 0.0,  6.023714,  6.023714],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_run_full_integration():
    net = pp.network.load(str(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct')))
    net.update_phase_tap_changers(id="BLOAD 11 BLOAD 12 2", tap=1)
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True,
        losses_compensation_epsilon=pp.flowdecomposition.Parameters.DISABLE_LOSSES_COMPENSATION_EPSILON,
        sensitivity_epsilon=pp.flowdecomposition.Parameters.DISABLE_SENSITIVITY_EPSILON,
        rescale_enabled=True,
        dc_fallback_enabled_after_ac_divergence=True)
    flow_decomposition = pp.flowdecomposition.create_decomposition()
    flow_decomposition.add_precontingency_monitored_elements(['BLOAD 11 BLOAD 12 2', 'FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 12 1'])
    df = flow_decomposition.run(net, parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'country1', 'country2', 'ac_reference_flow', 'dc_reference_flow', 'commercial_flow', 'pst_flow', 'internal_flow', 'loop_flow_from_be', 'loop_flow_from_fr'],
        data=[
            ['BLOAD 11 BLOAD 12 2', 'BLOAD 11 BLOAD 12 2', 'BE', 'BE', -160.00594493625374, -168.54299036226615,  27.730133478072496, 156.40133330888222, -24.11086055331822, 0.0              , -0.014661297382767557],
            ['FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 11 1', 'FR', 'BE',  192.39065600179342,  200.6712560368467 ,  27.81857394392333 , 156.90014831777725,   0.0             , 7.68659503747561 , -0.014661297382767557],
            ['FGEN  11 BLOAD 12 1', 'FGEN  11 BLOAD 12 1', 'FR', 'BE',  -76.18907198080873,  -84.72530847149157, -87.04742845831291 , 155.51999087872588,   0.0             , 7.674711445291424,  0.04179811510434703 ],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_parameters():
    net = pp.network.load(str(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct')))
    net.update_phase_tap_changers(id="BLOAD 11 BLOAD 12 2", tap=1)
    parameters = pp.flowdecomposition.Parameters()

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

if __name__ == "__main__":
    # If you set your .itools/config.yml with a custom Voltage Init mode, it will break the tests !
    logging.basicConfig(level=logging.DEBUG)
    logging.getLogger('powsybl').setLevel(logging.DEBUG)
    test_demo()
    test_demo_one_by_one()
    test_flow_decomposition_parameters()
    test_flow_decomposition_run_no_parameters()
    test_flow_decomposition_run_full_integration()
