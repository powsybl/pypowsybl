# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Dict, List
import numpy as np
import pandas as pd
from pypowsybl import _pypowsybl
from .sensitivity_analysis_result import DEFAULT_MATRIX_ID, process_ptdf_rows


class AcSensitivityAnalysisAdjointResult:
    """
    Result of a reverse-mode (adjoint / VJP) AC sensitivity analysis: the gradient ``dL/dvariable`` of a
    scalar loss ``L`` w.r.t. every declared variable, obtained by contracting the sensitivity matrix ``S``
    with the output cotangents ``dL/dfunction`` — ``theta_bar = S^T . y_bar`` — without materialising ``S``.
    """

    def __init__(self, result_context_ptr: _pypowsybl.JavaHandle,
                 variable_ids: Dict[str, List[str]]):
        self._handle = result_context_ptr
        self.result_context_ptr = result_context_ptr
        # The variable (row) ids of each factor matrix, which index its gradient. The forward result calls
        # the same lists function_data_frame_index, which is where they come from: despite that name they
        # hold the matrix's VARIABLES, since those are the rows of the sensitivity matrix.
        self.variable_ids = variable_ids

    def get_gradient(self, matrix_id: str = DEFAULT_MATRIX_ID) -> pd.Series:
        """
        The adjoint gradient ``dL/dvariable`` for a given factor matrix, as a vector over its variables.

        Args:
            matrix_id: ID of the factor matrix whose variables' gradient to read, as used when the factors
                       were declared
        Returns:
            a Series of ``dL/dvariable`` indexed by the variable ids
        """
        matrix = _pypowsybl.get_gradient(self.result_context_ptr, matrix_id)
        rows = self.variable_ids[matrix_id]
        df = pd.DataFrame(data=np.array(matrix, copy=False), columns=['value'], index=rows)  # shape [rowCount, 1]
        return process_ptdf_rows(df, rows)['value'].rename('dL/dvariable')
