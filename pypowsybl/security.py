#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _pypowsybl
from _pypowsybl import ContingencyResult
from _pypowsybl import LimitViolation
from pypowsybl.network import Network
from pypowsybl.loadflow import Parameters
from pypowsybl.util import ContingencyContainer
from prettytable import PrettyTable


ContingencyResult.__repr__ = lambda self: f"{self.__class__.__name__}("\
                                          f"contingency_id={self.contingency_id!r}"\
                                          f", status={self.status.name}"\
                                          f", limit_violations=[{len(self.limit_violations)}]"\
                                          f")"

LimitViolation.__repr__ = lambda self: f"{self.__class__.__name__}("\
                                       f"subject_id={self.subject_id!r}"\
                                       f", subject_name={self.subject_name!r}"\
                                       f", limit_type={self.limit_type.name}"\
                                       f", limit={self.limit!r}"\
                                       f", limit_name={self.limit_name!r}"\
                                       f", acceptable_duration={self.acceptable_duration!r}"\
                                       f", limit_reduction={self.limit_reduction!r}"\
                                       f", value={self.value!r}"\
                                       f", side={self.side.name}"\
                                       f")"


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


class SecurityAnalysis(ContingencyContainer):
    def __init__(self, ptr):
        ContingencyContainer.__init__(self, ptr)

    def run_ac(self, network: Network, parameters: Parameters = Parameters(), provider = 'OpenLoadFlow') -> SecurityAnalysisResult:
        return SecurityAnalysisResult(_pypowsybl.run_security_analysis(self.ptr, network.ptr, parameters, provider))


def create_analysis() -> SecurityAnalysis:
    return SecurityAnalysis(_pypowsybl.create_security_analysis())
