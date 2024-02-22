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
    voltage_initializer_set_objective,
    voltage_initializer_set_objective_distance)


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
