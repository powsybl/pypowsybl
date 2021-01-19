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
from gridpy.util import ObjectHandle
from typing import List
import numpy as np
import pandas as pd


class SensitivityAnalysisResult(ObjectHandle):
    def __init__(self, result_context_ptr, branches_ids: List[str], injections_or_transformers_ids: List[str]):
        ObjectHandle.__init__(self, result_context_ptr)
        self.result_context_ptr = result_context_ptr
        self.branches_ids = branches_ids
        self.injections_or_transformers_ids = injections_or_transformers_ids

    def get_sensitivity_matrix(self):
        return self.get_post_contingency_sensitivity_matrix('')

    def get_post_contingency_sensitivity_matrix(self, contingency_id: str):
        m = _gridpy.get_sensitivity_matrix(self.result_context_ptr, contingency_id)
        if m is None:
            return None
        else:
            data = np.array(m, copy=False)
            return pd.DataFrame(data=data, columns=self.branches_ids, index=self.injections_or_transformers_ids)

    def get_reference_flows(self):
        return self.get_post_contingency_reference_flows('')

    def get_post_contingency_reference_flows(self, contingency_id: str):
        m = _gridpy.get_reference_flows(self.result_context_ptr, contingency_id)
        if m is None:
            return None
        else:
            data = np.array(m, copy=False)
            return pd.DataFrame(data=data, columns=self.branches_ids, index=['reference_flows'])


class SensitivityAnalysis(ContingencyContainer):
    def __init__(self, ptr):
        ContingencyContainer.__init__(self, ptr)
        self.branches_ids = None
        self.injections_or_transformers_ids = None

    def set_factor_matrix(self, branches_ids: List[str], injections_or_transformers_ids: List[str]):
        _gridpy.set_factor_matrix(self.ptr, branches_ids, injections_or_transformers_ids)
        self.branches_ids = branches_ids
        self.injections_or_transformers_ids = injections_or_transformers_ids

    def run_dc(self, network: Network, parameters: Parameters = Parameters()) -> SensitivityAnalysisResult:
        return SensitivityAnalysisResult(_gridpy.run_sensitivity_analysis(self.ptr, network.ptr, parameters),
                                         branches_ids=self.branches_ids, injections_or_transformers_ids=self.injections_or_transformers_ids)


def create() -> SensitivityAnalysis:
    return SensitivityAnalysis(_gridpy.create_sensitivity_analysis())
