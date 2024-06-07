# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0

import pypowsybl as pp
import pypowsybl.voltage_initializer as va
import pytest

@pytest.fixture(autouse=True)
def set_up():
    pp.set_config_read(False)

def test_parameters():
    params = va.VoltageInitializerParameters()
    params.add_variable_shunt_compensators(["shunt1", "shunt2"])
    params.add_constant_q_generators(["gen1", "gen2"])
    params.add_variable_two_windings_transformers(["twt1", "twt2"])

    params.add_specific_voltage_limits({"vl_id": (0.5, 1.2)})

    params.add_specific_low_voltage_limits([("vl_id", True, 0.5)])
    params.add_specific_high_voltage_limits([("vl_id", True, 1.2)])
    params.add_specific_low_voltage_limits([("vl_id_2", False, 380)])
    params.add_specific_high_voltage_limits([("vl_id_3", False, 420)])
    params.add_specific_low_voltage_limits([("vl_id_4", False, 380)])
    params.add_specific_high_voltage_limits([("vl_id_4", True, 2.3)])

    params.set_objective(va.VoltageInitializerObjective.SPECIFIC_VOLTAGE_PROFILE)
    params.set_objective_distance(1.3)
    params.set_log_level_ampl(va.VoltageInitializerLogLevelAmpl.ERROR)
    params.set_log_level_solver(va.VoltageInitializerLogLevelSolver.EVERYTHING)
    params.set_reactive_slack_buses_mode(va.VoltageInitializerReactiveSlackBusesMode.NO_GENERATION)
    params.set_min_plausible_low_voltage_limit(0.45)
    params.set_max_plausible_high_voltage_limit(1.2)
    params.set_active_power_variation_rate(0.5)
    params.set_max_plausible_power_limit(7800)
    params.set_min_plausible_active_power_threshold(1)
    params.set_low_impedance_threshold(1e-5)
    params.set_min_nominal_voltage_ignored_bus(0.5)
    params.set_min_nominal_voltage_ignored_voltage_bounds(1)
    params.set_high_active_power_default_limit(950)
    params.set_low_active_power_default_limit(0.5)
    params.set_default_minimal_qp_range(0.45)
    params.set_default_qmax_pmax_ratio(0.45)

    params.set_default_variable_scaling_factor(1.1)
    params.set_default_constraint_scaling_factor(0.8)
    params.set_reactive_slack_variable_scaling_factor(0.2)
    params.set_twt_ratio_variable_scaling_factor(0.002)


@pytest.mark.skip(reason="CI doesn't have a Ampl and Knitro runtime.")
def test_runner():
    from pypowsybl import network, voltage_initializer as v_init
    params = v_init.VoltageInitializerParameters()
    n = network.create_eurostag_tutorial_example1_network()
    some_gen_id = n.get_generators().iloc[0].name
    params.add_constant_q_generators([some_gen_id])
    some_2wt_id = n.get_2_windings_transformers().iloc[0].name
    params.add_variable_two_windings_transformers([some_2wt_id])

    params.add_algorithm_param({"foo": "bar", "bar": "bar2"})
    params.set_objective(v_init.VoltageInitializerObjective.SPECIFIC_VOLTAGE_PROFILE)

    results = v_init.run(n, params, True)
    results.apply_all_modifications(n)

    assert results.status == v_init.VoltageInitializerStatus.OK
    assert len(results.indicators) == 78
