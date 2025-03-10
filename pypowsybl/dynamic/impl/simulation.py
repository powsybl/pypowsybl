# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import List
from pandas import DataFrame
from pypowsybl.network import Network
from pypowsybl import _pypowsybl as _pp
from pypowsybl.utils import create_data_frame_from_series_array
from pypowsybl.report import ReportNode
from .event_mapping import EventMapping
from .model_mapping import ModelMapping
from .simulation_result import SimulationResult
from .output_variable_mapping import OutputVariableMapping
from .parameters import Parameters


class Simulation:  # pylint: disable=too-few-public-methods
    def __init__(self) -> None:
        self._handle = _pp.create_dynamic_simulation_context()

    def run(self,
            network: Network,
            model_mapping: ModelMapping,
            event_mapping: EventMapping,
            timeseries_mapping: OutputVariableMapping,
            parameters: Parameters = None,
            report_node: ReportNode = None
            ) -> SimulationResult:
        """Run the dynawo simulation"""
        return SimulationResult(
                _pp.run_dynamic_simulation(
                    self._handle,
                    network._handle, # pylint: disable=protected-access
                    model_mapping._handle, # pylint: disable=protected-access
                    event_mapping._handle, # pylint: disable=protected-access
                    timeseries_mapping._handle, # pylint: disable=protected-access
                    parameters._to_c_parameters() if parameters is not None else _pp.DynamicSimulationParameters(), # pylint: disable=protected-access
                    None if report_node is None else report_node._report_node) # pylint: disable=protected-access
        )


    @staticmethod
    def get_provider_parameters_names() -> List[str]:
        """
        Get list of parameters for Dynawo provider.

        Returns:
            the list of Dynawo's parameters
        """
        return _pp.get_dynamic_simulation_provider_parameters_names()

    @staticmethod
    def get_provider_parameters() -> DataFrame:
        """
        Supported dynamic simulation specific parameters for a given provider.

        Returns:
            dynamic simulation parameters dataframe

        Examples:
           .. doctest::

               >>> parameters = pp.dynamic.Simulation.get_provider_parameters()
               >>> parameters['description']['solver.type']
               'Solver used in the simulation'
               >>> parameters['type']['solver.type']
               'STRING'
               >>> parameters['default']['solver.type']
               'SIM'
               >>> parameters['possible_values']['solver.type']
               '[SIM, IDA]'
        """
        series_array = _pp.create_dynamic_simulation_provider_parameters_series_array()
        return create_data_frame_from_series_array(series_array)
