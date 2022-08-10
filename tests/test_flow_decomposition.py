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

def test_flow_decomposition_run_no_parameters():
    net = pp.network.load(str(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct')))
    net.update_phase_tap_changers(id="BLOAD 11 BLOAD 12 2", tap=1)
    df=pp.flowdecomposition.run(net)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'commercial_flow', 'pst_flow', 'loop_flow_from_be', 'loop_flow_from_fr', 'ac_reference_flow', 'dc_reference_flow', 'country1', 'country2'],
        data=[
            ['FGEN  11 BLOAD 11 1_InitialState', 'FGEN  11 BLOAD 11 1', 'InitialState',  29.015809, 163.652703, -2.007905, -2.007905, 192.390656, 188.652703, 'FR', 'BE'],
            ['FGEN  11 BLOAD 12 1_InitialState', 'FGEN  11 BLOAD 12 1', 'InitialState', -87.047428, 163.652703,  6.023714,  6.023714, -76.189072, -88.652703, 'FR', 'BE'],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_run_full_integration():
    net = pp.network.load(str(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct')))
    net.update_phase_tap_changers(id="BLOAD 11 BLOAD 12 2", tap=1)
    parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True,
        losses_compensation_epsilon=pp.flowdecomposition.Parameters.DISABLE_LOSSES_COMPENSATION_EPSILON,
        sensitivity_epsilon=pp.flowdecomposition.Parameters.DISABLE_SENSITIVITY_EPSILON,
        rescale_enabled=True,
        branch_selection_strategy=pp.flowdecomposition.BranchSelectionStrategy.ZONE_TO_ZONE_PTDF_CRITERIA,
        contingency_strategy=pp.flowdecomposition.ContingencyStrategy.AUTO_CONTINGENCY)
    df=pp.flowdecomposition.run(net, parameters=parameters)
    expected = pd.DataFrame.from_records(
        index=['xnec_id'],
        columns=['xnec_id', 'branch_id', 'contingency_id', 'commercial_flow', 'pst_flow', 'loop_flow_from_be', 'loop_flow_from_fr', 'ac_reference_flow', 'dc_reference_flow', 'country1', 'country2'],
        data=[
            ['BLOAD 11 BLOAD 12 2_FGEN  11 BLOAD 12 1', 'BLOAD 11 BLOAD 12 2', 'FGEN  11 BLOAD 12 1',  16.251468284764158,  91.7052365827749,  -24.038740818033162,   -0.0037090564131254666,  -83.91425499309278,  -83.92993647911071, 'BE', 'BE'],
            ['BLOAD 11 BLOAD 12 2_InitialState',        'BLOAD 11 BLOAD 12 2', 'InitialState',         27.730133478072496, 156.40133330888222, -24.11086055331822,    -0.014661297382767557,  -160.00594493625374, -168.54299036226615, 'BE', 'BE'],
            ['FGEN  11 BLOAD 11 1_FGEN  11 BLOAD 12 1', 'FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 12 1',  16.773601649552976,  94.65157736298661,   4.631557002925231,   -0.0037090564131254666,  116.05302695905169,  115.97664549460403, 'FR', 'BE'],
            ['FGEN  11 BLOAD 11 1_InitialState',        'FGEN  11 BLOAD 11 1', 'InitialState',         27.81857394392333,  156.90014831777725,   7.68659503747561,    -0.014661297382767557,   192.39065600179342,  200.6712560368467,  'FR', 'BE'],
            ['FGEN  11 BLOAD 12 1_FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 12 1', 'FGEN  11 BLOAD 11 1', -86.99448447540858,  202.9955999051578,    0.007444347574602772, 0.007444347574613789,   116.01600412489843,  115.9766418423117,  'FR', 'BE'],
            ['FGEN  11 BLOAD 12 1_InitialState',        'FGEN  11 BLOAD 12 1', 'InitialState',        -87.04742845831291,  155.51999087872588,   7.674711445291424,    0.04179811510434703,    -76.18907198080873,  -84.72530847149157, 'FR', 'BE'],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)

def test_flow_decomposition_parameters():
    net = pp.network.load(str(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct')))
    net.update_phase_tap_changers(id="BLOAD 11 BLOAD 12 2", tap=1)
    parameters = pp.flowdecomposition.Parameters()

    # Testing setting independently every attributes
    attributes = {
        'save_intermediates': [True, False],
        'enable_losses_compensation': [True, False],
        'losses_compensation_epsilon': [-1, 1e-3, 1e-5],
        'sensitivity_epsilon': [-1, 1e-3, 1e-5],
        'rescale_enabled': [True, False],
        'branch_selection_strategy': [pp.flowdecomposition.BranchSelectionStrategy.ONLY_INTERCONNECTIONS, pp.flowdecomposition.BranchSelectionStrategy.ZONE_TO_ZONE_PTDF_CRITERIA],
        'contingency_strategy': [pp.flowdecomposition.ContingencyStrategy.ONLY_N_STATE, pp.flowdecomposition.ContingencyStrategy.AUTO_CONTINGENCY],
    }

    for attribute, values in attributes.items():
        for value in values:
            parameters = pp.flowdecomposition.Parameters(**dict([(attribute, value)]))
            assert value == getattr(parameters, attribute)

            parameters = pp.flowdecomposition.Parameters()
            setattr(parameters, attribute, value)
            assert value == getattr(parameters, attribute)
            print(parameters)

            df=pp.flowdecomposition.run(net, parameters)
            print(df)

if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG)
    logging.getLogger('powsybl').setLevel(logging.INFO) # If logging is enable, it freezes your terminal with `python test/test_flow_decomposition.py`, sigterm the python process...
    test_flow_decomposition_parameters()
    print('\n\n\n\n')
    test_flow_decomposition_run_no_parameters()
    print('\n\n\n\n')
    test_flow_decomposition_run_full_integration()
