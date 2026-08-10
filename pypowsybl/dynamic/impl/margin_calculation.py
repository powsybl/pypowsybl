# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Optional
import pandas as pd
from pypowsybl import _pypowsybl
from pypowsybl.network import Network
from pypowsybl.report import ReportNode
from pypowsybl.security import ContingencyContainer
from pypowsybl.utils import create_data_frame_from_series_array
from .model_mapping import ModelMapping
from .loads_variation import LoadsVariationMapping
from .margin_calculation_parameters import MarginCalculationParameters


class MarginCalculationResult:
    """
    The result of a margin calculation.
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        self._handle = handle
        self._load_increase_results = create_data_frame_from_series_array(
            _pypowsybl.get_margin_calculation_load_increase_results(self._handle))
        self._scenario_results = create_data_frame_from_series_array(
            _pypowsybl.get_margin_calculation_scenario_results(self._handle))

    @property
    def load_increase_results(self) -> pd.DataFrame:
        """
        The load level and status reached for each load increase, as a dataframe.
        """
        return self._load_increase_results

    @property
    def scenario_results(self) -> pd.DataFrame:
        """
        The status of each contingency scenario, per load level, as a dataframe.
        """
        return self._scenario_results


class MarginCalculation(ContingencyContainer):
    """
    Allows to run a margin calculation on a network, using the Dynawo provider.

    A margin calculation computes, for a set of load variations and contingencies, the
    load level up to which the network stays stable using time-domain simulation.
    """

    def __init__(self) -> None:
        ContingencyContainer.__init__(self, _pypowsybl.create_margin_calculation())

    def run(self,
            network: Network,
            model_mapping: ModelMapping,
            loads_variation_mapping: LoadsVariationMapping,
            parameters: Optional[MarginCalculationParameters] = None,
            report_node: Optional[ReportNode] = None) -> MarginCalculationResult:
        """ Runs a margin calculation.

        Args:
            network:                 Network on which the margin calculation will be computed
            model_mapping:           Mapping of the network equipments to their dynamic models
            loads_variation_mapping: Mapping of the loads variations to apply
            parameters:              Margin calculation parameters
            report_node:             The reporter to be used to create an execution report, default is None (no report)

        Returns:
            A margin calculation result, containing the load increase results and the scenario results
        """
        return MarginCalculationResult(
            _pypowsybl.run_margin_calculation(
                self._handle,
                network._handle,  # pylint: disable=protected-access
                model_mapping._handle,  # pylint: disable=protected-access
                loads_variation_mapping._handle,  # pylint: disable=protected-access
                parameters._to_c_parameters() if parameters is not None else _pypowsybl.MarginCalculationParameters(),  # pylint: disable=protected-access
                None if report_node is None else report_node._report_node))  # pylint: disable=protected-access
