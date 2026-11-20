/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.sensitivity;

import com.powsybl.commons.PowsyblException;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.WordFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class SensitivityAnalysisResultContext {

    private final Map<String, SensitivityAnalysisContext.MatrixInfo> factorsMatrix;

    private final double[] baseCaseValues;

    private final Map<String, double[]> valuesByContingencyId;

    private final double[] baseCaseReferences;

    private final Map<String, double[]> referencesByContingencyId;

    public SensitivityAnalysisResultContext(Map<String, SensitivityAnalysisContext.MatrixInfo> factorsMatrix,
                                            double[] baseCaseValues, Map<String, double[]> valuesByContingencyId,
                                            double[] baseCaseReferences, Map<String, double[]> referencesByContingencyId) {
        this.factorsMatrix = factorsMatrix;
        this.baseCaseValues = baseCaseValues;
        this.valuesByContingencyId = valuesByContingencyId;
        this.baseCaseReferences = baseCaseReferences;
        this.referencesByContingencyId = referencesByContingencyId;
    }

    private double[] getValues(String contingencyId) {
        return contingencyId.isEmpty() ? baseCaseValues : valuesByContingencyId.get(contingencyId);
    }

    private double[] getReferences(String contingencyId) {
        return contingencyId.isEmpty() ? baseCaseReferences : referencesByContingencyId.get(contingencyId);
    }

    private SensitivityAnalysisContext.MatrixInfo getFactorsMatrix(String matrixId) {
        SensitivityAnalysisContext.MatrixInfo m = factorsMatrix.get(matrixId);
        if (m == null) {
            throw new PowsyblException("Matrix '" + matrixId + "' not found");
        }
        return m;
    }

    public PyPowsyblApiHeader.MatrixPointer createSensitivityMatrix(String matrixId, String contingencyId) {
        SensitivityAnalysisContext.MatrixInfo m = getFactorsMatrix(matrixId);
        return createDoubleMatrix(() -> getValues(contingencyId), m.getOffsetData(), m.getRowCount(), m.getColumnCount());
    }

    public PyPowsyblApiHeader.MatrixPointer createReferenceMatrix(String matrixId, String contingencyId) {
        SensitivityAnalysisContext.MatrixInfo m = getFactorsMatrix(matrixId);
        return createDoubleMatrix(() -> getReferences(contingencyId), m.getOffsetColumn(), 1, m.getColumnCount());
    }

    private static PyPowsyblApiHeader.MatrixPointer createDoubleMatrix(Supplier<double[]> srcSupplier, int srcPos, int matRow, int matCol) {
        final double[] sources = srcSupplier.get();
        if (sources == null) {
            return WordFactory.nullPointer();
        }
        int cellCount = matRow * matCol;
        CDoublePointer valuePtr = UnmanagedMemory.malloc(cellCount * SizeOf.get(CDoublePointer.class));
        ByteBuffer buffer = CTypeConversion.asByteBuffer(valuePtr, cellCount * Double.BYTES).order(ByteOrder.nativeOrder());
        buffer.asDoubleBuffer().put(sources, srcPos, cellCount);
        PyPowsyblApiHeader.MatrixPointer matrixPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.MatrixPointer.class));
        matrixPtr.setRowCount(matRow);
        matrixPtr.setColumnCount(matCol);
        matrixPtr.setValues(valuePtr);
        return matrixPtr;
    }
}
