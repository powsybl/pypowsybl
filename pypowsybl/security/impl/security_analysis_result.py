# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Dict
import pandas as pd
from prettytable import PrettyTable
from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import PreContingencyResult, PostContingencyResult, OperatorStrategyResult, LimitViolationArray
from pypowsybl.utils import create_data_frame_from_series_array


class SecurityAnalysisResult:
    """
    The result of a security analysis.
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        self._handle = handle
        self._pre_contingency_result = _pypowsybl.get_pre_contingency_result(self._handle)
        post_contingency_results = _pypowsybl.get_post_contingency_results(self._handle)
        operator_strategy_results = _pypowsybl.get_operator_strategy_results(self._handle)
        self._post_contingency_results = {}
        self._operator_strategy_results = {}
        for result in post_contingency_results:
            if result.contingency_id:
                self._post_contingency_results[result.contingency_id] = result
        for result in operator_strategy_results:
            self._operator_strategy_results[result.operator_strategy_id] = result
        self._limit_violations = create_data_frame_from_series_array(_pypowsybl.get_limit_violations(self._handle))

    @property
    def pre_contingency_result(self) -> PreContingencyResult:
        """
        Result for the pre-contingency state.
        """
        return self._pre_contingency_result

    @property
    def post_contingency_results(self) -> Dict[str, PostContingencyResult]:
        """
        Results for the contingencies, as a dictionary contingency ID -> result.
        """
        return self._post_contingency_results

    def find_post_contingency_result(self, contingency_id: str) -> PostContingencyResult:
        """
        Result for the specified contingency.

        Returns:
            Result for the specified contingency.
        """
        result = self._post_contingency_results[contingency_id]
        if not result:
            raise KeyError(f'Contingency {contingency_id} not found')
        return result

    @property
    def operator_strategy_results(self) -> Dict[str, OperatorStrategyResult]:
        """
        Results for the operator strategies, as a dictionary operator strategy ID -> result.
        """

        return self._operator_strategy_results

    def find_operator_strategy_results(self, operator_strategy_id: str) -> OperatorStrategyResult:
        """
        Result for the specified operator strategy

        Returns:
            Result for the specified operator strategy.
        """
        result = self._operator_strategy_results[operator_strategy_id]
        if not result:
            raise KeyError(f'Operator strategy {operator_strategy_id} not found')
        return result

    def get_table(self) -> PrettyTable:
        table = PrettyTable()
        table.field_names = ["Contingency ID", "Operator strategy ID", "Status", "Equipment ID", "Equipment name", "Limit type", "Limit",
                             "Limit name", "Acceptable duration", "Limit reduction", "Value", "Side"]
        def print_limit_violation(limit_violations: LimitViolationArray) -> None:
            for limit_violation in limit_violations:
                table.add_row(['', '', '',
                               limit_violation.subject_id,
                               limit_violation.subject_name,
                               limit_violation.limit_type.name,
                               f'{limit_violation.limit:.1f}',
                               limit_violation.limit_name,
                               limit_violation.acceptable_duration,
                               limit_violation.limit_reduction,
                               f'{limit_violation.value:.1f}',
                               limit_violation.side.name])
        for contingency_id, post_contingency_result in self._post_contingency_results.items():
            table.add_row([contingency_id, '', post_contingency_result.status.name, '', '', '', '', '', '', '', '', ''])
            print_limit_violation(post_contingency_result.limit_violations)

        for operator_strategy_id, operator_strategy_result in self._operator_strategy_results.items():
            table.add_row(['', operator_strategy_id, operator_strategy_result.status.name, '', '', '', '', '', '', '', '', ''])
            print_limit_violation(operator_strategy_result.limit_violations)
        return table

    @property
    def limit_violations(self) -> pd.DataFrame:
        """
        All limit violations in a dataframe representation.
        """
        return self._limit_violations

    @property
    def branch_results(self) -> pd.DataFrame:
        """
        Results (P, Q, I) for monitored branches.
        """
        return create_data_frame_from_series_array(_pypowsybl.get_branch_results(self._handle))

    @property
    def bus_results(self) -> pd.DataFrame:
        """
        Bus results (voltage angle and magnitude) for monitored voltage levels.
        """
        return create_data_frame_from_series_array(_pypowsybl.get_bus_results(self._handle))

    @property
    def three_windings_transformer_results(self) -> pd.DataFrame:
        """
        Results (P, Q, I) for monitored three winding transformers.
        """
        return create_data_frame_from_series_array(_pypowsybl.get_three_windings_transformer_results(self._handle))
