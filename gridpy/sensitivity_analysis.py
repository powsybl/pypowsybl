#
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _gridpy

from gridpy.loadflow import Parameters
from gridpy.network import Network
from gridpy.util import ContingencyContainer
from typing import List
import numpy as np


class SensitivityAnalysisResult:
    def __init__(self, result_context):
        self._result_context = result_context

    def get_sensitivity_matrix(self):
        return np.array(_gridpy.get_sensitivity_matrix(self._result_context, ''), copy = False)

    def get_post_contingency_sensitivity_matrix(self, contingency_id: str):
        return np.array(_gridpy.get_sensitivity_matrix(self._result_context, contingency_id), copy = False)


class SensitivityAnalysis(ContingencyContainer):
    def __init__(self, ptr):
        ContingencyContainer.__init__(self, ptr)

    def set_factor_matrix(self, branches_ids: List[str], injections_or_transformers_ids: List[str]):
        _gridpy.set_factor_matrix(self.ptr, branches_ids, injections_or_transformers_ids)

    def run_dc(self, network: Network, parameters: Parameters = Parameters()) -> SensitivityAnalysisResult:
        return SensitivityAnalysisResult(_gridpy.run_sensitivity_analysis(self.ptr, network.ptr, parameters))


def create() -> SensitivityAnalysis:
    return SensitivityAnalysis(_gridpy.create_sensitivity_analysis())
