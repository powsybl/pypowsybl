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
    voltage_initializer_set_objective_distance,
    VoltageInitializerStatus,
    voltage_initializer_get_status,
    voltage_initializer_get_indicators,
    voltage_initializer_apply_all_modifications,
    run_voltage_initializer,
    JavaHandle
)
from pypowsybl.network import Network


class VoltageInitializerParameters:
    """
    Parameters of a voltage initializer run.
    """

    def __init__(self) -> None:
        self._handle = create_voltage_initializer_params()

    def add_variable_shunt_compensators(self, shunt_id_list: List[str]) -> None:
        '''
        Indicate to voltage initializer that the given shunt compensator has a variable susceptance.

        Args:
            shunt_id_list: List of shunt ids.
        '''
        for id in shunt_id_list:
            voltage_initializer_add_variable_shunt_compensators(self._handle, id)

    def add_constant_q_generators(self, generator_id_list: List[str]) -> None:
        '''
        Indicate to voltage initializer that the given generator have a constant target reactive power.

        Args:
            generator_id_list: List of generator ids.
        '''
        for id in generator_id_list:
            voltage_initializer_add_constant_q_generators(self._handle, id)

    def add_variable_two_windings_transformers(self, transformer_id_list: List[str]) -> None:
        '''
        Indicate to voltage initializer that the given 2wt have a variable ratio.

        Args:
            transformer_id_list: List of transformer ids.
        '''
        for id in transformer_id_list:
            voltage_initializer_add_variable_two_windings_transformers(self._handle, id)

    def add_specific_low_voltage_limits(self, low_limits: List[Tuple[str, bool, float]]) -> None:
        '''
        Indicate to voltage initializer to override the network low voltages limits,
        limit can be given relative to former limit or absolute.
        High limits can be given for the same voltage level ids using
        :func:`~VoltageInitializerParameters.add_specific_high_voltage_limits`
        but it is not necessary to give a high limit as long as each voltage level has its limits
        defined and consistent after overrides (low limit < high limit, low limit > 0...)
        Use this if voltage initializer cannot converge because of infeasibility.

        Args:
            low_limits: A List with elements as (voltage level id, is limit relative, limit value)
        '''
        for voltage_level_id, is_relative, limit in low_limits:
            voltage_initializer_add_specific_low_voltage_limits(self._handle, voltage_level_id, is_relative, limit)

    def add_specific_high_voltage_limits(self, high_limits: List[Tuple[str, bool, float]]) -> None:
        '''
        Indicate to voltage initializer to override the network high voltages limits,
        limit can be given relative to previous limit or absolute.
        Low limits can be given for the same voltage level ids using
        :func:`~VoltageInitializerParameters.add_specific_low_voltage_limits`
        but it is not necessary to give a low limit as long as each voltage level has its limits
        defined and consistent after overrides (low limit < high limit, low limit > 0...)
        Use this if voltage initializer cannot converge because of infeasibility.

        Args:
            high_limits: A List with elements as (voltage level id, is limit relative, limit value)
        '''
        for voltage_level_id, is_relative, limit in high_limits:
            voltage_initializer_add_specific_high_voltage_limits(self._handle, voltage_level_id, is_relative, limit)

    def add_specific_voltage_limits(self, limits: Dict[str, Tuple[float, float]]) -> None:
        '''
        Indicate to voltage initializer to override the network voltages limits.
        Limits are given relative to previous limits.
        Use this if voltage initializer cannot converge because of infeasibility.

        Args:
            limits: A dictionary keys are voltage ids, values are (lower limit, upper limit)
        '''
        for key in limits:
            self.add_specific_low_voltage_limits([(key, True, limits[key][0])])
            self.add_specific_high_voltage_limits([(key, True, limits[key][1])])

    def set_objective(self, objective: VoltageInitializerObjective) -> None:
        '''
        If you use BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT, you also need to call :func:`~VoltageInitializerParameters.set_objective_distance`.

        Args:
            objective: objective function to set for VoltageInitializer.
        '''
        voltage_initializer_set_objective(self._handle, objective)

    def set_objective_distance(self, distance: float) -> None:
        '''
        If you use BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT, you also need to call this function.

        Args:
            distance: is in %.
                        A 0% objective means the model will target lower voltage limit.
                        A 100% objective means the model will target upper voltage limit.
        '''
        voltage_initializer_set_objective_distance(self._handle, distance)


class VoltageInitializerResults:
    """
    Results of a voltage initializer run.
    """

    def __init__(self, result_handle: JavaHandle) -> None:
        self._handle = result_handle
        self._status: VoltageInitializerStatus = voltage_initializer_get_status(self._handle)
        self._indicators: Dict[str, str] = voltage_initializer_get_indicators(self._handle)

    def apply_all_modifications(self, network: Network) -> None:
        '''
        Apply all the modifications voltage initializer found to the network.

        Args:
            network: the network on which the modifications are to be applied.
        '''
        voltage_initializer_apply_all_modifications(self._handle, network._handle)

    @property
    def status(self) -> VoltageInitializerStatus:
        '''
        If the optimisation failed, it can be useful to check the indicators.
        Returns:
            The status of the optimisation
        '''
        return self._status

    @property
    def indicators(self) -> Dict[str, str]:
        '''
        Returns:
            The indicators as a dict of the optimisation
        '''
        return self._indicators


def run(network: Network, params: VoltageInitializerParameters = VoltageInitializerParameters(), debug: bool = False) -> VoltageInitializerResults:
    """
    Run voltage initializer on the network with the given params.

    Args:
        network: Network on which voltage initializer will run
        params: The parameters used to customize the run
        debug: if true, the tmp directory of the voltage initializer run will not be erased.
    """
    result_handle = run_voltage_initializer(debug, network._handle, params._handle)
    return VoltageInitializerResults(result_handle)
