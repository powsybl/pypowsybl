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
import pypowsybl.opf.impl.model.opf_model as opf_model_module
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


def test_run_ac_restores_per_unit_when_the_solve_raises(monkeypatch):
    """red->green: run() sets network.per_unit = True through NetworkCache and only reset it on
    the last line, so any exception between the two left the caller's network
    stuck in per-unit mode. Forced through a stubbed Model.optimize(), not a malformed network -
    the point is the teardown, not the error."""
    real_create_model = opf_model_module.create_model

    def broken_create_model(solver_type, solver_options):
        model = real_create_model(solver_type, solver_options)
        def raising_optimize():
            raise RuntimeError("stubbed solver failure")
        model.optimize = raising_optimize
        return model

    monkeypatch.setattr(opf_model_module, 'create_model', broken_create_model)

    network = pp.network.create_ieee14()
    with pytest.raises(RuntimeError, match="stubbed solver failure"):
        pp.opf.run_ac(network)

    assert network.per_unit is False


def test_run_ac_leaves_per_unit_false_after_a_normal_run():
    """trap: an ordinary, successful solve must still end with per_unit is False. Guards the fix
    above from over-reaching (e.g. never setting it back to True in the first place)."""
    network = pp.network.create_ieee14()
    assert pp.opf.run_ac(network)
    assert network.per_unit is False
