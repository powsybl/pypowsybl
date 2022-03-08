/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.word.WordFactory;

import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class SensitivityAnalysisResultContext {

    private final int flowRowCount;

    private final int flowColCount;

    private final int voltageRowCount;

    private final int voltageColCount;

    private final double[] baseCaseValues;

    private final Map<String, double[]> valuesByContingencyId;

    private final double[] baseCaseReferences;

    private final Map<String, double[]> referencesByContingencyId;

    public SensitivityAnalysisResultContext(int flowRowCount, int flowColCount, int voltageRowCount, int voltageColCount,
                                            double[] baseCaseValues, Map<String, double[]> valuesByContingencyId,
                                            double[] baseCaseReferences, Map<String, double[]> referencesByContingencyId) {
        this.flowRowCount = flowRowCount;
        this.flowColCount = flowColCount;
        this.voltageRowCount = voltageRowCount;
        this.voltageColCount = voltageColCount;
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

    public PyPowsyblApiHeader.MatrixPointer createBranchFlowsSensitivityMatrix(String contingencyId) {
        return createDoubleMatrix(() -> getValues(contingencyId), 0, flowRowCount, flowColCount);
    }

    public PyPowsyblApiHeader.MatrixPointer createBusVoltagesSensitivityMatrix(String contingencyId) {
        return createDoubleMatrix(() -> getValues(contingencyId), flowRowCount * flowColCount, voltageRowCount, voltageColCount);
    }

    public PyPowsyblApiHeader.MatrixPointer createReferenceFlows(String contingencyId) {
        return createDoubleMatrix(() -> getReferences(contingencyId), 0, 1, flowColCount);
    }

    public PyPowsyblApiHeader.MatrixPointer createReferenceVoltages(String contingencyId) {
        return createDoubleMatrix(() -> getReferences(contingencyId), flowColCount, voltageRowCount, voltageColCount);
    }

    private static PyPowsyblApiHeader.MatrixPointer createDoubleMatrix(Supplier<double[]> srcSupplier, int srcPos, int matRow, int matCol) {
        final double[] sources = srcSupplier.get();
        if (sources == null) {
            return WordFactory.nullPointer();
        }
        double[] values = new double[matRow * matCol];
        System.arraycopy(sources, srcPos, values, 0, values.length);
        return doubleArrToMatrix(values, matRow, matCol);
    }

    private static PyPowsyblApiHeader.MatrixPointer doubleArrToMatrix(double[] values, int rowCount, int colCount) {
        if (values.length != rowCount * colCount) {
            throw new IllegalArgumentException("Matrix(" + rowCount + "*" + colCount + ") is not suitable for arrays size:" + values.length);
        }
        CDoublePointer valuePtr = UnmanagedMemory.calloc(rowCount * colCount * SizeOf.get(CDoublePointer.class));
        for (int i = 0; i < colCount * rowCount; i++) {
            valuePtr.addressOf(i).write(values[i]);
        }
        PyPowsyblApiHeader.MatrixPointer matrixPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.MatrixPointer.class));
        matrixPtr.setRowCount(rowCount);
        matrixPtr.setColumnCount(colCount);
        matrixPtr.setValues(valuePtr);
        return matrixPtr;
    }
}
