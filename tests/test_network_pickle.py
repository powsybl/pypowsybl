#
# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import pathlib
import pickle
import tempfile

import pandas as pd
import pytest

import pypowsybl as pp


@pytest.fixture(autouse=True)
def setUp():
    pp.set_config_read(False)


def test_pickle():
    n = pp.network.create_eurostag_tutorial_example1_network()
    buses = n.get_buses(all_attributes=True)
    with tempfile.TemporaryDirectory() as tmp_dir_name:
        tmp_dir_path = pathlib.Path(tmp_dir_name)
        data_file = tmp_dir_path.joinpath('data.pkl')
        with open(data_file, 'wb') as f:
            pickle.dump(n, f)
        with open(data_file, 'rb') as f:
            n2 = pickle.load(f)
            buses2 = n2.get_buses(all_attributes=True)
            pd.testing.assert_frame_equal(buses, buses2, check_dtype=False)
