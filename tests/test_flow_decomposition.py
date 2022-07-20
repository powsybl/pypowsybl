#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pathlib
import pandas as pd 

import pypowsybl as pp
import pypowsybl.flowdecomposition as fd

TEST_DIR = pathlib.Path(__file__).parent
DATA_DIR = TEST_DIR.parent / 'data'

def test_run_fd():
    net = pp.network.load(str(DATA_DIR / 'NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct'))
    df=fd.run(net)
    expected = pd.DataFrame.from_records(
        index=['XNEC id'],
        columns=['XNEC id', 'Allocated Flow', 'PST Flow', 'Loop Flow from BE', 'Loop Flow from FR', 'Reference AC Flow', 'Reference DC Flow'],
        data=[
            ['FGEN  11 BLOAD 11 1',  29.015809, 163.652703, -2.007905, -2.007905, 192.390656, 188.652703],
            ['FGEN  11 BLOAD 12 1', -87.047428, 163.652703,  6.023714,  6.023714, -76.189072, -88.652703],
        ])
    pd.testing.assert_frame_equal(expected, df, check_dtype=False)
    