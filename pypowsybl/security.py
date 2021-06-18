#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# iicense, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
from typing import List
from pypowsybl.util import create_data_frame_from_series_array as _create_data_frame_from_series_array

import _pypowsybl
from _pypowsybl import ContingencyResult
from _pypowsybl import LimitViolation
from _pypowsybl import LimitType
from _pypowsybl import Side
from _pypowsybl import ContingencyContextType
from pypowsybl.network import Network
from pypowsybl.loadflow import Parameters
from pypowsybl.util import ContingencyContainer
from prettytable import PrettyTable

ContingencyResult.__repr__ = lambda self: f"{self.__class__.__name__}(" \
                                          f"contingency_id={self.contingency_id!r}" \
                                          f", status={self.status.name}" \
                                          f", limit_violations=[{len(self.limit_violations)}]" \
                                          f")"

LimitViolation.__repr__ = lambda self: f"{self.__class__.__name__}(" \
                                       f"subject_id={self.subject_id!r}" \
                                       f", subject_name={self.subject_name!r}" \
                                       f", limit_type={self.limit_type.name}" \
                                       f", limit={self.limit!r}" \
                                       f", limit_name={self.limit_name!r}" \
                                       f", acceptable_duration={self.acceptable_duration!r}" \
                                       f", limit_reduction={self.limit_reduction!r}" \
                                       f", value={self.value!r}" \
                                       f", side={self.side.name}" \
                                       f")"


class SecurityAnalysisResult:
    def __init__(self, result):
        self.ptr = result
        results = _pypowsybl.get_security_analysis_result(result)
        self._post_contingency_results = {}
        for result in results:
            if result.contingency_id:
                self._post_contingency_results[result.contingency_id] = result
            else:
                self._pre_contingency_result = result

    @property
    def pre_contingency_result(self):
        return self._pre_contingency_result

    @property
    def post_contingency_results(self):
        return self._post_contingency_results.values()

    def find_post_contingency_result(self, contingency_id: str):
        result = self._post_contingency_results[contingency_id]
        if not result:
            raise Exception("Contingency '%s' not found".format(contingency_id))
        return result

    def get_table(self):
        table = PrettyTable()
        table.field_names = ["Contingency ID", "Status", "Equipment ID", "Equipment name", "Limit type", "Limit",
                             "Limit name", "Acceptable duration", "Limit reduction", "Value", "Side"]
        for contingency_id in self._post_contingency_results:
            post_contingency_result = self._post_contingency_results[contingency_id]
            table.add_row([contingency_id, post_contingency_result.status.name, '', '', '', '', '', '', '', '', ''])
            for limit_violation in post_contingency_result.limit_violations:
                table.add_row(['', '',
                               limit_violation.subject_id,
                               limit_violation.subject_name,
                               limit_violation.limit_type.name,
                               "{:.1f}".format(limit_violation.limit),
                               limit_violation.limit_name,
                               limit_violation.acceptable_duration,
                               limit_violation.limit_reduction,
                               "{:.1f}".format(limit_violation.value),
                               limit_violation.side.name])
        return table

    def get_branch_results(self):
        return _create_data_frame_from_series_array(_pypowsybl.get_branch_results(self.ptr))

    def get_bus_results(self):
        return _create_data_frame_from_series_array(_pypowsybl.get_bus_results(self.ptr))

    def get_three_windings_transformer_results(self):
        return _create_data_frame_from_series_array(_pypowsybl.get_three_windings_transformer_results(self.ptr))


class SecurityAnalysis(ContingencyContainer):
    def __init__(self, ptr):
        ContingencyContainer.__init__(self, ptr)

    def run_ac(self, network: Network, parameters: Parameters = Parameters(),
               provider='OpenSecurityAnalysis') -> SecurityAnalysisResult:
        return SecurityAnalysisResult(_pypowsybl.run_security_analysis(self.ptr, network.ptr, parameters, provider))

    def add_monitored_elements(self, contingency_context_type: ContingencyContextType,
                           branch_ids: List[str] = None,
                           voltage_level_ids: List[str] = None,
                           three_windings_transformer_ids: List[str] = None,
                           contingencies=None):

        if three_windings_transformer_ids is None:
            three_windings_transformer_ids = list()
        if branch_ids is None:
            branch_ids = list()
        if voltage_level_ids is None:
            voltage_level_ids = list()
        if contingencies is None:
            contingencies = ['']
        elif type(contingencies) == str:
            contingencies = [contingencies]
        _pypowsybl.add_monitored_elements(self.ptr, contingency_context_type, branch_ids, voltage_level_ids,
                                          three_windings_transformer_ids, contingencies)


def create_analysis() -> SecurityAnalysis:
    return SecurityAnalysis(_pypowsybl.create_security_analysis())
