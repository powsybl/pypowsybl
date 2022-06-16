/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.sensitivity;

import com.powsybl.commons.PowsyblException;
import com.powsybl.python.commons.PyPowsyblApiHeader;
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

    private final Map<String, SensitivityAnalysisContext.MatrixInfo> branchFlowFactorsMatrix;

    private final SensitivityAnalysisContext.MatrixInfo busVoltageFactorsMatrix;

    private final double[] baseCaseValues;

    private final Map<String, double[]> valuesByContingencyId;

    private final double[] baseCaseReferences;

    private final Map<String, double[]> referencesByContingencyId;

    public SensitivityAnalysisResultContext(Map<String, SensitivityAnalysisContext.MatrixInfo> branchFlowFactorsMatrix,
                                            SensitivityAnalysisContext.MatrixInfo busVoltageFactorsMatrix,
                                            double[] baseCaseValues, Map<String, double[]> valuesByContingencyId,
                                            double[] baseCaseReferences, Map<String, double[]> referencesByContingencyId) {
        this.branchFlowFactorsMatrix = branchFlowFactorsMatrix;
        this.busVoltageFactorsMatrix = busVoltageFactorsMatrix;
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

    private SensitivityAnalysisContext.MatrixInfo getBranchFlowFactorsMatrix(String matrixId) {
        SensitivityAnalysisContext.MatrixInfo m = branchFlowFactorsMatrix.get(matrixId);
        if (m == null) {
            throw new PowsyblException("Matrix '" + matrixId + "' not found");
        }
        return m;
    }

    private SensitivityAnalysisContext.MatrixInfo getBusVoltageFactoryMatrix() {
        SensitivityAnalysisContext.MatrixInfo m = busVoltageFactorsMatrix;
        if (m == null) {
            throw new PowsyblException("bus voltage sensitivity matrix does not exist");
        }
        return m;
    }

    public PyPowsyblApiHeader.MatrixPointer createBranchFlowsSensitivityMatrix(String matrixId, String contingencyId) {
        SensitivityAnalysisContext.MatrixInfo m = getBranchFlowFactorsMatrix(matrixId);
        return createDoubleMatrix(() -> getValues(contingencyId), m.getOffsetData(), m.getRowCount(), m.getColumnCount());
    }

    public PyPowsyblApiHeader.MatrixPointer createBusVoltagesSensitivityMatrix(String contingencyId) {
        SensitivityAnalysisContext.MatrixInfo m = getBusVoltageFactoryMatrix();
        return createDoubleMatrix(() -> getValues(contingencyId), m.getOffsetData(), m.getRowCount(), m.getColumnCount());
    }

    public PyPowsyblApiHeader.MatrixPointer createReferenceFlowsActivePower(String matrixId, String contingencyId) {
        SensitivityAnalysisContext.MatrixInfo m = getBranchFlowFactorsMatrix(matrixId);
        return createDoubleMatrix(() -> getReferences(contingencyId), m.getOffsetColumn(), 1, m.getColumnCount());
    }

    public PyPowsyblApiHeader.MatrixPointer createReferenceVoltages(String contingencyId) {
        return createDoubleMatrix(() -> getReferences(contingencyId), busVoltageFactorsMatrix.getOffsetData(),
                busVoltageFactorsMatrix.getRowCount(), busVoltageFactorsMatrix.getColumnCount());
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
