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
from pypowsybl._pypowsybl import PreContingencyResult, PostContingencyResult
from pypowsybl.utils import create_data_frame_from_series_array


class SecurityAnalysisResult:
    """
    The result of a sensitivity analysis.
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        self._handle = handle
        self._pre_contingency_result = _pypowsybl.get_pre_contingency_result(self._handle)
        post_contingency_results = _pypowsybl.get_post_contingency_results(self._handle)
        self._post_contingency_results = {}
        for result in post_contingency_results:
            if result.contingency_id:
                self._post_contingency_results[result.contingency_id] = result
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

    def get_table(self) -> PrettyTable:
        table = PrettyTable()
        table.field_names = ["Contingency ID", "Status", "Equipment ID", "Equipment name", "Limit type", "Limit",
                             "Limit name", "Acceptable duration", "Limit reduction", "Value", "Side"]
        for contingency_id, post_contingency_result in self._post_contingency_results.items():
            table.add_row([contingency_id, post_contingency_result.status.name, '', '', '', '', '', '', '', '', ''])
            for limit_violation in post_contingency_result.limit_violations:
                table.add_row(['', '',
                               limit_violation.subject_id,
                               limit_violation.subject_name,
                               limit_violation.limit_type.name,
                               f'{limit_violation.limit:.1f}',
                               limit_violation.limit_name,
                               limit_violation.acceptable_duration,
                               limit_violation.limit_reduction,
                               f'{limit_violation.value:.1f}',
                               limit_violation.side.name])
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
