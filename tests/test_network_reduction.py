#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import pytest
import re

import pypowsybl as pp
import pathlib

TEST_DIR = pathlib.Path(__file__).parent


@pytest.fixture(autouse=True)
def setUp():
    pp.set_config_read(False)


def test_reduce_by_voltage():
    n = pp.network.create_eurostag_tutorial_example1_network()
    pp.loadflow.run_ac(n)
    assert 4 == len(n.get_buses())
    n.reduce_by_voltage_range(v_min=240, v_max=400)
    assert 2 == len(n.get_buses())


def test_reduce_by_ids():
    n = pp.network.create_eurostag_tutorial_example1_network()
    pp.loadflow.run_ac(n)
    assert 4 == len(n.get_buses())
    n.reduce_by_ids(ids=['P2'])
    assert 2 == len(n.get_buses())


def test_reduce_by_subnetwork():
    n = pp.network.create_eurostag_tutorial_example1_network()
    pp.loadflow.run_ac(n)
    assert 4 == len(n.get_buses())
    n.reduce_by_ids_and_depths(vl_depths=[('VLGEN', 1), ('VLLOAD', 1)])
    assert 4 == len(n.get_buses())

def test_deprecated_reduce():
    n = pp.network.create_eurostag_tutorial_example1_network()
    pp.loadflow.run_ac(n)
    assert 4 == len(n.get_buses())
    with pytest.warns(DeprecationWarning, match=re.escape("reduce is deprecated, use `reduce_by_voltage_range`")):
        n.reduce(v_min=240, v_max=400)
    assert 2 == len(n.get_buses())