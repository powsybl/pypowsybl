# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from pypowsybl.network import Network
from pypowsybl import _pypowsybl as _pp
from pypowsybl.report import ReportNode
from .event_mapping import EventMapping
from .model_mapping import ModelMapping
from .simulation_result import SimulationResult
from .output_variable_mapping import OutputVariableMapping


class Simulation:  # pylint: disable=too-few-public-methods
    def __init__(self) -> None:
        self._handle = _pp.create_dynamic_simulation_context()

    def run(self,
            network: Network,
            model_mapping: ModelMapping,
            event_mapping: EventMapping,
            timeseries_mapping: OutputVariableMapping,
            start: int,
            stop: int,
            report_node: ReportNode = None
            ) -> SimulationResult:
        """Run the dynawo simulation"""
        return SimulationResult(
            _pp.run_dynamic_model(
                self._handle,
                network._handle, # pylint: disable=protected-access
                model_mapping._handle, # pylint: disable=protected-access
                event_mapping._handle, # pylint: disable=protected-access
                timeseries_mapping._handle, # pylint: disable=protected-access
                start,
                stop,
                None if report_node is None else report_node._report_node) # pylint: disable=protected-access
        )
