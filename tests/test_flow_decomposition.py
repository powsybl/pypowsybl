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
    #logging.basicConfig(level=logging.DEBUG)
    #logging.getLogger('powsybl').setLevel(logging.DEBUG) # If logging is enable, it freezes your terminal with `python test/test_flow_decomposition.py`, sigterm the python process...
    test_flow_decomposition_run_no_parameters()
    test_flow_decomposition_parameters()
