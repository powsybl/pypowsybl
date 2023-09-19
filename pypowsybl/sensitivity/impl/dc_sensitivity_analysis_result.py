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

TO_REMOVE = 'TO_REMOVE'


class DcSensitivityAnalysisResult:
    """
    Represents the result of a DC sensitivity analysis.

    The result contains computed values (so called "reference" values) and sensitivity values
    of requested factors, on the base case and on post contingency states.
    """

    def __init__(self,
                 result_context_ptr: _pypowsybl.JavaHandle,
                 branches_ids: Dict[str, List[str]],
                 branch_data_frame_index: Dict[str, List[str]]):
        self._handle = result_context_ptr
        self.result_context_ptr = result_context_ptr
        self.branches_ids = branches_ids
        self.branch_data_frame_index = branch_data_frame_index

    def get_branch_flows_sensitivity_matrix(self, matrix_id: str = 'default', contingency_id: str = None) -> Optional[
        pd.DataFrame]:
        """
        Get the matrix of branch flows sensitivities on the base case or on post contingency state.

        If contingency_id is None, returns the base case matrix.

        Args:
            matrix_id:      ID of the matrix
            contingency_id: ID of the contingency
        Returns:
            the matrix of branch flows sensitivities
        """
        matrix = _pypowsybl.get_branch_flows_sensitivity_matrix(self.result_context_ptr, matrix_id,
                                                                '' if contingency_id is None else contingency_id)
        if matrix is None:
            return None

        data = np.array(matrix, copy=False)

        df = pd.DataFrame(data=data, columns=self.branches_ids[matrix_id],
                          index=self.branch_data_frame_index[matrix_id])

        # substract second power transfer zone to first one
        i = 0
        while i < len(self.branch_data_frame_index[matrix_id]):
            if self.branch_data_frame_index[matrix_id][i] == TO_REMOVE:
                df.iloc[i - 1] = df.iloc[i - 1] - df.iloc[i]
            i += 1

        # remove rows corresponding to power transfer second zone
        return df.drop([TO_REMOVE], errors='ignore')

    def get_reference_flows(self, matrix_id: str = 'default', contingency_id: str = None) -> Optional[pd.DataFrame]:
        """
        The branches active power flows on the base case or on post contingency state.

        Args:
            matrix_id:      ID of the matrix
            contingency_id: ID of the contingency
        Returns:
            the branches active power flows
        """
        matrix = _pypowsybl.get_reference_flows(self.result_context_ptr, matrix_id,
                                                '' if contingency_id is None else contingency_id)
        if matrix is None:
            return None
        data = np.array(matrix, copy=False)
        return pd.DataFrame(data=data, columns=self.branches_ids[matrix_id], index=['reference_flows'])
