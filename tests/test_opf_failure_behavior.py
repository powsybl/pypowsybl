#
# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import platform

import pandas as pd
import pytest

import pypowsybl as pp
from pypowsybl.opf.impl.opf import OptimalPowerFlowParameters

if platform.system() == 'Darwin' and platform.machine() == 'x86_64':
    pytest.skip("No version compatible with x86_64 macOS.", allow_module_level=True)


def test_run_ac_does_not_write_back_network_on_infeasible_solve():
    """red->green: run_ac used to overwrite the network with the infeasible solve's stopping
    point even while returning False. Every bus pinned to an impossible 1.5 pu box: IPOPT cannot
    satisfy it on a real network with fixed injections, so the solve is genuinely infeasible."""
    network = pp.network.create_ieee14()
    before = network.get_buses(attributes=['v_mag'])['v_mag'].copy()

    opf_parameters = OptimalPowerFlowParameters(default_voltage_bounds=(1.5, 1.5))
    converged = pp.opf.run_ac(network, opf_parameters)

    after = network.get_buses(attributes=['v_mag'])['v_mag']
    assert not converged
    pd.testing.assert_series_equal(before, after)


def test_run_ac_still_writes_back_network_on_a_successful_solve():
    """trap: an ordinary, successful solve must still update the network. Guards the fix above
    from over-reaching into the success path."""
    network = pp.network.create_ieee14()
    before = network.get_buses(attributes=['v_mag'])['v_mag'].copy()

    converged = pp.opf.run_ac(network)

    after = network.get_buses(attributes=['v_mag'])['v_mag']
    assert converged
    assert (before - after).abs().max() > 0.01
