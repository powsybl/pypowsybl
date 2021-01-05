#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _gridpy
from gridpy.network import Network
from gridpy.util import ObjectHandle
from typing import List


class SecurityAnalysisResult:
    def __init__(self, results):
        self._post_contingency_result = {}
        for result in results:
            if result.contingency_id:
                self._post_contingency_result[result.contingency_id] = result
            else:
                self._pre_contingency_result = result

    @property
    def pre_contingency_result(self):
        return self._pre_contingency_result

    @property
    def post_contingency_results(self):
        return self._post_contingency_result.values()

    @property
    def contingencies(self):
        return self._post_contingency_result.keys()

    def find_post_contingency_result(self, contingency_id: str):
        result = self._post_contingency_result[contingency_id]
        if not result:
            raise Exception("Contingency '%s' not found".format(contingency_id))
        return result


class SecurityAnalysis(ObjectHandle):
    def __init__(self, ptr):
        ObjectHandle.__init__(self, ptr)

    def run(self, network: Network):
        return SecurityAnalysisResult(_gridpy.run_security_analysis(self.ptr, network.ptr))

    def add_contingency(self, id: str, elements_ids: List[str]):
        _gridpy.add_contingency_to_security_analysis(self.ptr, id, elements_ids)

    def add_contingency(self, id: str, first_element_id: str, *other_elements_ids: str):
        _gridpy.add_contingency_to_security_analysis(self.ptr, id, [first_element_id] + list(other_elements_ids))


def create():
    return SecurityAnalysis(_gridpy.create_security_analysis())
