/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.sensitivity;

import com.powsybl.commons.PowsyblException;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.Util;

import java.util.Map;

/**
 * Result of a reverse-mode (adjoint / VJP) sensitivity run: {@code θ̄ = dL/dvariable} per declared factor
 * matrix, one value per variable (row) in declaration order, exposed as a {@code rowCount × 1} column — the
 * reverse-mode dual of {@link SensitivityAnalysisResultContext#createSensitivityMatrix}.
 *
 * <p>Holds the answers, not the ingredients: {@link SensitivityAnalysisContext#runAdjoint} has already joined
 * OpenLoadFlow's gradient — keyed by {@code (variableType, variableId)} — onto each matrix's rows, so how a
 * row is keyed does not leak into this type.</p>
 */
public class SensitivityAnalysisAdjointResultContext {

    private final Map<String, double[]> gradientByMatrixId;

    public SensitivityAnalysisAdjointResultContext(Map<String, double[]> gradientByMatrixId) {
        this.gradientByMatrixId = gradientByMatrixId;
    }

    /** dL/dvariable for one declared factor matrix (used by tests; the C entry point returns it as {@link #createGradient}). */
    double[] gradientValues(String matrixId) {
        double[] values = gradientByMatrixId.get(matrixId);
        if (values == null) {
            throw new PowsyblException("Factor matrix '" + matrixId + "' not found");
        }
        return values;
    }

    public PyPowsyblApiHeader.MatrixPointer createGradient(String matrixId) {
        double[] values = gradientValues(matrixId);
        return Util.createMatrix(values, values.length, 1);
    }
}
