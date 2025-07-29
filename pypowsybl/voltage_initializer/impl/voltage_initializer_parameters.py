# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0

from typing import Dict, List, Tuple
from pypowsybl._pypowsybl import (
    create_voltage_initializer_params,
    voltage_initializer_add_variable_shunt_compensators,
    voltage_initializer_add_constant_q_generators,
    voltage_initializer_add_variable_two_windings_transformers,
    voltage_initializer_add_specific_low_voltage_limits,
    voltage_initializer_add_specific_high_voltage_limits,
    VoltageInitializerObjective,
    VoltageInitializerLogLevelAmpl,
    VoltageInitializerLogLevelSolver,
    VoltageInitializerReactiveSlackBusesMode,
    voltage_initializer_set_objective,
    voltage_initializer_set_objective_distance,
    voltage_initializer_set_log_level_ampl,
    voltage_initializer_set_log_level_solver,
    voltage_initializer_set_reactive_slack_buses_mode,
    voltage_initializer_set_min_plausible_low_voltage_limit,
    voltage_initializer_set_max_plausible_high_voltage_limit,
    voltage_initializer_set_active_power_variation_rate,
    voltage_initializer_set_min_plausible_active_power_threshold,
    voltage_initializer_set_low_impedance_threshold,
    voltage_initializer_set_min_nominal_voltage_ignored_bus,
    voltage_initializer_set_min_nominal_voltage_ignored_voltage_bounds,
    voltage_initializer_set_max_plausible_power_limit,
    voltage_initializer_set_high_active_power_default_limit,
    voltage_initializer_set_low_active_power_default_limit,
    voltage_initializer_set_default_minimal_qp_range,
    voltage_initializer_set_default_qmax_pmax_ratio,
    voltage_initializer_set_default_variable_scaling_factor,
    voltage_initializer_set_default_constraint_scaling_factor,
    voltage_initializer_set_reactive_slack_variable_scaling_factor,
    voltage_initializer_set_twt_ratio_variable_scaling_factor)

class VoltageInitializerParameters:
    """
    Parameters of a voltage initializer run.
    """

    def __init__(self) -> None:
        self._handle = create_voltage_initializer_params()

    def add_variable_shunt_compensators(self, shunt_id_list: List[str]) -> None:
        """
        Indicate to voltage initializer that the given shunt compensator has a variable susceptance.

        Args:
            shunt_id_list: List of shunt ids.
        """
        for shunt_id in shunt_id_list:
            voltage_initializer_add_variable_shunt_compensators(self._handle, shunt_id)

    def add_constant_q_generators(self, generator_id_list: List[str]) -> None:
        """
        Indicate to voltage initializer that the given generator have a constant target reactive power.

        Args:
            generator_id_list: List of generator ids.
        """
        for generator_id in generator_id_list:
            voltage_initializer_add_constant_q_generators(self._handle, generator_id)

    def add_variable_two_windings_transformers(self, transformer_id_list: List[str]) -> None:
        """
        Indicate to voltage initializer that the given 2wt have a variable ratio.

        Args:
            transformer_id_list: List of transformer ids.
        """
        for transformer_id in transformer_id_list:
            voltage_initializer_add_variable_two_windings_transformers(self._handle, transformer_id)

    def add_specific_low_voltage_limits(self, low_limits: List[Tuple[str, bool, float]]) -> None:
        """
        Indicate to voltage initializer to override the network low voltages limits,
        limit can be given relative to former limit or absolute.
        High limits can be given for the same voltage level ids using
        :func:`~VoltageInitializerParameters.add_specific_high_voltage_limits`
        but it is not necessary to give a high limit as long as each voltage level has its limits
        defined and consistent after overrides (low limit < high limit, low limit > 0...)
        Use this if voltage initializer cannot converge because of infeasibility.

        Args:
            low_limits: A List with elements as (voltage level id, is limit relative, limit value)
        """
        for voltage_level_id, is_relative, limit in low_limits:
            voltage_initializer_add_specific_low_voltage_limits(self._handle, voltage_level_id, is_relative, limit)

    def add_specific_high_voltage_limits(self, high_limits: List[Tuple[str, bool, float]]) -> None:
        """
        Indicate to voltage initializer to override the network high voltages limits,
        limit can be given relative to previous limit or absolute.
        Low limits can be given for the same voltage level ids using
        :func:`~VoltageInitializerParameters.add_specific_low_voltage_limits`
        but it is not necessary to give a low limit as long as each voltage level has its limits
        defined and consistent after overrides (low limit < high limit, low limit > 0...)
        Use this if voltage initializer cannot converge because of infeasibility.

        Args:
            high_limits: A List with elements as (voltage level id, is limit relative, limit value)
        """
        for voltage_level_id, is_relative, limit in high_limits:
            voltage_initializer_add_specific_high_voltage_limits(self._handle, voltage_level_id, is_relative, limit)

    def add_specific_voltage_limits(self, limits: Dict[str, Tuple[float, float]]) -> None:
        """
        Indicate to voltage initializer to override the network voltages limits.
        Limits are given relative to previous limits.
        Use this if voltage initializer cannot converge because of infeasibility.

        Args:
            limits: A dictionary keys are voltage ids, values are (lower limit, upper limit)
        """
        for key in limits:
            self.add_specific_low_voltage_limits([(key, True, limits[key][0])])
            self.add_specific_high_voltage_limits([(key, True, limits[key][1])])

    def set_objective(self, objective: VoltageInitializerObjective) -> None:
        """
        If you use BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT, you also need to call
         :func:`~VoltageInitializerParameters.set_objective_distance`.

        Args:
            objective: objective function to set for VoltageInitializer.
        """
        voltage_initializer_set_objective(self._handle, objective)

    def set_objective_distance(self, distance: float) -> None:
        """
        If you use BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT, you also need to call this function.

        Args:
            distance: is in %.
                        A 0% objective means the model will target lower voltage limit.
                        A 100% objective means the model will target upper voltage limit.
        """
        voltage_initializer_set_objective_distance(self._handle, distance)

    def set_log_level_ampl(self, log_level_ampl: VoltageInitializerLogLevelAmpl) -> None:
        """
        Changes the log level of AMPL printings.

        log_level_ampl can be:
         - DEBUG
         - INFO
         - WARNING
         - ERROR

        Args:
            log_level_ampl: the log level.
        """
        voltage_initializer_set_log_level_ampl(self._handle, log_level_ampl)

    def set_log_level_solver(self, log_level_solver: VoltageInitializerLogLevelSolver) -> None:
        """
        Changes the log level of non-linear optimization solver printings.

        log_level_solver can be:
         - NOTHING
         - ONLY_RESULTS
         - EVERYTHING

        Args:
            log_level_solver: the log level.
        """
        voltage_initializer_set_log_level_solver(self._handle, log_level_solver)

    def set_reactive_slack_buses_mode(self, reactive_slack_buses_mode: VoltageInitializerReactiveSlackBusesMode) -> None:
        """
        Changes the log level of non-linear optimization solver printings.

        log_level_solver can be:
         - NOTHING
         - ONLY_RESULTS
         - EVERYTHING

        Args:
            log_level_solver: the log level.
        """
        voltage_initializer_set_reactive_slack_buses_mode(self._handle, reactive_slack_buses_mode)

    def set_min_plausible_low_voltage_limit(self, min_plausible_low_voltage_level: float) -> None:
        """
        Changes the minimal plausible value for low voltage limits (in p.u.) in ACOPF solving.
        
        Args:
            min_plausible_low_voltage_level: is >= 0.
        """
        voltage_initializer_set_min_plausible_low_voltage_limit(self._handle, min_plausible_low_voltage_level)

    def set_max_plausible_high_voltage_limit(self, max_plausible_high_voltage_limit: float) -> None:
        """
        Changes the maximal plausible value for high voltage limits (in p.u.) in ACOPF solving.
        
        Args:
            max_plausible_high_voltage_limit: is > 0.
        """
        voltage_initializer_set_max_plausible_high_voltage_limit(self._handle, max_plausible_high_voltage_limit)

    def set_active_power_variation_rate(self, active_power_variation_rate: float) -> None:
        """
        Changes the weight to favor more/less minimization of active power produced by generators.
    
        Args:
            active_power_variation_rate: is >= 0 and =< 1.
                        A 0 active_power_variation_rate means the model will minimize the sum of generations.
                        A 1 active_power_variation_rate means the model will minimize the sum of squared differences between target and value.
        """
        voltage_initializer_set_active_power_variation_rate(self._handle, active_power_variation_rate)

    def set_min_plausible_active_power_threshold(self, min_plausible_active_power_threshold: float) -> None:
        """
        Changes the threshold of active and reactive power considered as null in the optimization.
        
        Args:
            min_plausible_active_power_threshold: is >= 0.
        """
        voltage_initializer_set_min_plausible_active_power_threshold(self._handle, min_plausible_active_power_threshold)

    def set_low_impedance_threshold(self, low_impedance_threshold: float) -> None:
        """
        Changes the threshold of impedance considered as null.
        
        Args:
            low_impedance_threshold: is >= 0.
        """
        voltage_initializer_set_low_impedance_threshold(self._handle, low_impedance_threshold)

    def set_min_nominal_voltage_ignored_bus(self, min_nominal_voltage_ignored_bus: float) -> None:
        """
        Changes the threshold used to ignore voltage levels with nominal voltage lower than it. 
        
        Args:
            min_nominal_voltage_ignored_bus: is >= 0.
        """
        voltage_initializer_set_min_nominal_voltage_ignored_bus(self._handle, min_nominal_voltage_ignored_bus)

    def set_min_nominal_voltage_ignored_voltage_bounds(self, min_nominal_voltage_ignored_voltage_bounds: float) -> None:
        """
        Changes the threshold used to replace voltage limits of voltage levels with nominal voltage lower than it.
        
        Args:
            min_nominal_voltage_ignored_voltage_bounds: is >= 0.
        """
        voltage_initializer_set_min_nominal_voltage_ignored_voltage_bounds(self._handle, min_nominal_voltage_ignored_voltage_bounds)

    def set_max_plausible_power_limit(self, max_plausible_power_limit: float) -> None:
        """
        Changes the threshold defining the maximum active and reactive power considered in correction of generator limits.
        
        Args:
            max_plausible_power_limit: is > 0.
        """
        voltage_initializer_set_max_plausible_power_limit(self._handle, max_plausible_power_limit)

    def set_high_active_power_default_limit(self, high_active_power_default_limit: float) -> None:
        """
        Changes the threshold used for the correction of high active power limit of generators.
        
        Args:
            high_active_power_default_limit: is > 0.
        """
        voltage_initializer_set_high_active_power_default_limit(self._handle, high_active_power_default_limit)

    def set_low_active_power_default_limit(self, low_active_power_default_limit: float) -> None:
        """
        Changes the threshold used for the correction of low active power limit of generators.
        
        Args:
            low_active_power_default_limit: is > 0.
        """
        voltage_initializer_set_low_active_power_default_limit(self._handle, low_active_power_default_limit)

    def set_default_minimal_qp_range(self, default_minimal_qp_range: float) -> None:
        """
        Changes the threshold used to fix active (resp. reactive) power of generators with 
        active (resp. reactive) power limits that are closer than it.
        
        Args:
            default_minimal_qp_range: is >= 0.
        """
        voltage_initializer_set_default_minimal_qp_range(self._handle, default_minimal_qp_range)

    def set_default_qmax_pmax_ratio(self, default_qmax_pmax_ratio: float) -> None:
        """
        Changes the ratio used to calculate threshold for corrections of high/low reactive power limits.
        
        Args:
            default_qmax_pmax_ratio: is > 0.
        """
        voltage_initializer_set_default_qmax_pmax_ratio(self._handle, default_qmax_pmax_ratio)

    def set_default_variable_scaling_factor(self, default_variable_scaling_factor: float) -> None:
        """
        Changes the scaling of all variables (except reactive slacks and transformer ratios) before ACOPF solving

        Args:
            default_variable_scaling_factor: is > 0.
                        Default scaling factor
        """
        voltage_initializer_set_default_variable_scaling_factor(self._handle, default_variable_scaling_factor)

    def set_default_constraint_scaling_factor(self, default_constraint_scaling_factor: float) -> None:
        """
        Changes the scaling factor applied to all the constraints before ACOPF solving

        Args:
            default_constraint_scaling_factor: is >= 0.
        """
        voltage_initializer_set_default_constraint_scaling_factor(self._handle, default_constraint_scaling_factor)

    def set_reactive_slack_variable_scaling_factor(self, reactive_slack_variable_scaling_factor: float) -> None:
        """
        Changes the scaling factor applied to all reactive slacks variables before ACOPF solving

        Args:
            reactive_slack_variable_scaling_factor: is > 0.
        """
        voltage_initializer_set_reactive_slack_variable_scaling_factor(self._handle, reactive_slack_variable_scaling_factor)

    def set_twt_ratio_variable_scaling_factor(self, twt_ratio_variable_scaling_factor: float) -> None:
        """
        Changes the scaling factor applied to all transformer ratio variables before ACOPF solving

        Args:
            twt_ratio_variable_scaling_factor: is > 0.
        """
        voltage_initializer_set_twt_ratio_variable_scaling_factor(self._handle, twt_ratio_variable_scaling_factor)
