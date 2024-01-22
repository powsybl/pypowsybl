# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import warnings
from typing import Dict, List, Optional
import pandas as pd
from pypowsybl import _pypowsybl
from .sensitivity_analysis_result import SensitivityAnalysisResult, DEFAULT_MATRIX_ID


class DcSensitivityAnalysisResult(SensitivityAnalysisResult):
    """
    Represents the result of a DC sensitivity analysis.

    The result contains computed values (so called "reference" values) and sensitivity values
    of requested factors, on the base case and on post contingency states.
    """

    def __init__(self,
                 result_context_ptr: _pypowsybl.JavaHandle,
                 functions_ids: Dict[str, List[str]],
                 function_data_frame_index: Dict[str, List[str]]):
        SensitivityAnalysisResult.__init__(self, result_context_ptr, functions_ids, function_data_frame_index)

    def get_branch_flows_sensitivity_matrix(self, matrix_id: str = DEFAULT_MATRIX_ID, contingency_id: str = None) -> Optional[
        pd.DataFrame]:
        """
        .. deprecated:: 1.1.0
          Use :meth:`get_sensitivity_matrix` instead.

        Get the matrix of branch flows sensitivities on the base case or on post contingency state.

        If contingency_id is None, returns the base case matrix.

        Args:
            matrix_id:      ID of the matrix
            contingency_id: ID of the contingency
        Returns:
            the matrix of branch flows sensitivities
        """
        warnings.warn("get_branch_flows_sensitivity_matrix is deprecated, use get_sensitivity_matrix instead",
                      DeprecationWarning)
        return self.get_sensitivity_matrix(matrix_id, contingency_id)

    def get_reference_flows(self, matrix_id: str = DEFAULT_MATRIX_ID, contingency_id: str = None) -> Optional[pd.DataFrame]:
        """
        .. deprecated:: 1.1.0
          Use :meth:`get_reference_matrix` instead.

        The branches active power flows on the base case or on post contingency state.

        Args:
            matrix_id:      ID of the matrix
            contingency_id: ID of the contingency
        Returns:
            the branches active power flows
        """
        warnings.warn("get_reference_flows is deprecated, use get_reference_matrix instead",
                      DeprecationWarning)
        return self.get_reference_matrix(matrix_id, contingency_id, 'reference_flows')
