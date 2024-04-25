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

DEFAULT_REFERENCE_COLUMN_ID = 'reference_values'

DEFAULT_MATRIX_ID = 'default'
TO_REMOVE = 'TO_REMOVE'


class SensitivityAnalysisResult:
    """
    Represents the result of a sensitivity analysis.

    The result contains computed values (so called "reference" values) and sensitivity values
    of requested factors, on the base case and on post contingency states.
    """

    def __init__(self,
                 result_context_ptr: _pypowsybl.JavaHandle,
                 functions_ids: Dict[str, List[str]],
                 function_data_frame_index: Dict[str, List[str]]):
        self._handle = result_context_ptr
        self.result_context_ptr = result_context_ptr
        self.functions_ids = functions_ids
        self.function_data_frame_index = function_data_frame_index

    @staticmethod
    def clean_contingency_id(contingency_id: Optional[str]) -> str:
        return '' if contingency_id is None else contingency_id

    def process_ptdf(self, df: pd.DataFrame, matrix_id: str) -> pd.DataFrame:
        # substract second power transfer zone to first one
        i = 0
        while i < len(self.function_data_frame_index[matrix_id]):
            if self.function_data_frame_index[matrix_id][i] == TO_REMOVE:
                df.iloc[i - 1] = df.iloc[i - 1] - df.iloc[i]
            i += 1
        # remove rows corresponding to power transfer second zone
        return df.drop([TO_REMOVE], errors='ignore')

    def get_sensitivity_matrix(self, matrix_id: str = DEFAULT_MATRIX_ID, contingency_id: str = None) -> Optional[
        pd.DataFrame]:
        """
        Get the matrix of sensitivity values on the base case or on post contingency state.

        If contingency_id is None, returns the base case matrix.

        Args:
            matrix_id:      ID of the matrix
            contingency_id: ID of the contingency
        Returns:
            the matrix of sensitivity values
        """
        matrix = _pypowsybl.get_sensitivity_matrix(self.result_context_ptr, matrix_id, self.clean_contingency_id(contingency_id))
        if matrix is None:
            return None

        data = np.array(matrix, copy=False)

        df = pd.DataFrame(data=data, columns=self.functions_ids[matrix_id],
                          index=self.function_data_frame_index[matrix_id])

        return self.process_ptdf(df, matrix_id) # only used for PTDF

    def get_reference_matrix(self, matrix_id: str = DEFAULT_MATRIX_ID, contingency_id: str = None, reference_column_id: str = DEFAULT_REFERENCE_COLUMN_ID) -> Optional[pd.DataFrame]:
        """
        The reference values on the base case or on post contingency state.

        Args:
            matrix_id:      ID of the matrix
            contingency_id: ID of the contingency
        Returns:
            the reference values
        """
        matrix = _pypowsybl.get_reference_matrix(self.result_context_ptr, matrix_id, self.clean_contingency_id(contingency_id))
        if matrix is None:
            return None

        data = np.array(matrix, copy=False)

        return pd.DataFrame(data=data, columns=self.functions_ids[matrix_id], index=[reference_column_id])
