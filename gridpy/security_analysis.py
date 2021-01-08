#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _gridpy
from gridpy.network import Network
from gridpy.loadflow import LoadFlowParameters
from gridpy.util import ObjectHandle
from typing import List
from prettytable import PrettyTable


class SecurityAnalysisResult:
    def __init__(self, results):
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


class SecurityAnalysis(ObjectHandle):
    def __init__(self, ptr):
        ObjectHandle.__init__(self, ptr)

    def run_ac(self, network: Network, parameters: LoadFlowParameters = LoadFlowParameters()) -> SecurityAnalysisResult:
        return SecurityAnalysisResult(_gridpy.run_security_analysis(self.ptr, network.ptr, parameters))

    def add_contingency(self, id: str, elements_ids: List[str]):
        _gridpy.add_contingency_to_security_analysis(self.ptr, id, elements_ids)

    def add_contingency(self, id: str, first_element_id: str, *other_elements_ids: str):
        _gridpy.add_contingency_to_security_analysis(self.ptr, id, [first_element_id] + list(other_elements_ids))


def create() -> SecurityAnalysis:
    return SecurityAnalysis(_gridpy.create_security_analysis())
