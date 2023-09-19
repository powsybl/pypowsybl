# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Dict, List, Optional
import numpy as np
import pandas as pd
from pypowsybl import _pypowsybl
from .dc_sensitivity_analysis_result import DcSensitivityAnalysisResult


class AcSensitivityAnalysisResult(DcSensitivityAnalysisResult):
    """
    Represents the result of a AC sensitivity analysis.

    The result contains computed values (so called "reference" values) and sensitivity values
    of requested factors, on the base case and on post contingency states.
    """

    def __init__(self, result_context_ptr: _pypowsybl.JavaHandle, branches_ids: Dict[str, List[str]],
                 branch_data_frame_index: Dict[str, List[str]],
                 bus_ids: List[str], target_voltage_ids: List[str]):
        DcSensitivityAnalysisResult.__init__(self, result_context_ptr, branches_ids, branch_data_frame_index)
        self.bus_ids = bus_ids
        self.target_voltage_ids = target_voltage_ids

    def get_bus_voltages_sensitivity_matrix(self, contingency_id: str = None) -> Optional[pd.DataFrame]:
        """
        Get the matrix of bus voltages sensitivities on the base case or on post contingency state.

        Args:
            contingency_id: ID of the contingency
        Returns:
            the matrix of sensitivities
        """
        matrix = _pypowsybl.get_bus_voltages_sensitivity_matrix(self.result_context_ptr,
                                                                '' if contingency_id is None else contingency_id)
        if matrix is None:
            return None

        data = np.array(matrix, copy=False)
        return pd.DataFrame(data=data, columns=self.bus_ids, index=self.target_voltage_ids)

    def get_reference_voltages(self, contingency_id: str = None) -> Optional[pd.DataFrame]:
        """
        The values of bus voltages on the base case or on post contingency state.

        Args:
            contingency_id: ID of the contingency
        Returns:
            the values of bus voltages
        """
        matrix = _pypowsybl.get_reference_voltages(self.result_context_ptr,
                                                   '' if contingency_id is None else contingency_id)
        if matrix is None:
            return None

        data = np.array(matrix, copy=False)
        return pd.DataFrame(data=data, columns=self.bus_ids, index=['reference_voltages'])
