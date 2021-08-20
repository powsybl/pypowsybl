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

    Map<String, SensitivityAnalysisContext.MatrixInfo> branchFlowFactorsMatrix;

    SensitivityAnalysisContext.MatrixInfo busVoltageFactorsMatrix;

    Map<String, SensitivityAnalysisContext.MatrixInfo> preContingencyFactorMatrix;

    Map<String, SensitivityAnalysisContext.MatrixInfo> postContingencyFactorMatrix;

    private final double[] baseCaseValues;

    private final Map<String, double[]> valuesByContingencyId;

    private final double[] baseCaseReferences;

    private final Map<String, double[]> referencesByContingencyId;

    public SensitivityAnalysisResultContext(Map<String, SensitivityAnalysisContext.MatrixInfo> branchFlowFactorsMatrix,
                                              SensitivityAnalysisContext.MatrixInfo busVoltageFactorsMatrix,
                                              Map<String, SensitivityAnalysisContext.MatrixInfo> preContingencyFactorMatrix,
                                              Map<String, SensitivityAnalysisContext.MatrixInfo> postContingencyFactorMatrix,
                                              double[] baseCaseValues, Map<String, double[]> valuesByContingencyId,
                                              double[] baseCaseReferences, Map<String, double[]> referencesByContingencyId) {
        this.branchFlowFactorsMatrix = branchFlowFactorsMatrix;
        this.busVoltageFactorsMatrix = busVoltageFactorsMatrix;
        this.preContingencyFactorMatrix = preContingencyFactorMatrix;
        this.postContingencyFactorMatrix = postContingencyFactorMatrix;
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

    public PyPowsyblApiHeader.MatrixPointer createBranchFlowsSensitivityMatrix(String matrixId, String contingencyId) {
        SensitivityAnalysisContext.MatrixInfo m = this.branchFlowFactorsMatrix.get(matrixId);
        return createDoubleMatrix(() -> getValues(contingencyId), m.getOffsetData(), m.getRowCount(), m.getColumnCount());
    }

    public PyPowsyblApiHeader.MatrixPointer createPreContingencyBranchFlowsSensitivityMatrix(String matrixId) {
        SensitivityAnalysisContext.MatrixInfo m = this.preContingencyFactorMatrix.get(matrixId);
        return createDoubleMatrix(() -> getValues(""), m.getOffsetData(), m.getRowCount(), m.getColumnCount());
    }

    public PyPowsyblApiHeader.MatrixPointer createPostContingencyBranchFlowsSensitivityMatrix(String matrixId, String contingency) {
        SensitivityAnalysisContext.MatrixInfo m = this.postContingencyFactorMatrix.get(matrixId);
        return createDoubleMatrix(() -> getValues(contingency), m.getOffsetData(), m.getRowCount(), m.getColumnCount());
    }

    public PyPowsyblApiHeader.MatrixPointer createBranchFlowsSensitivityMatrix(final String contingencyId) {
        return createBranchFlowsSensitivityMatrix("default", contingencyId);
    }

    public PyPowsyblApiHeader.MatrixPointer createBusVoltagesSensitivityMatrix(String contingencyId) {
        return createDoubleMatrix(() -> getValues(contingencyId), this.busVoltageFactorsMatrix.getOffsetData(),
                this.busVoltageFactorsMatrix.getRowCount(), this.busVoltageFactorsMatrix.getColumnCount());
    }

    public PyPowsyblApiHeader.MatrixPointer createReferenceFlows(String contingencyId) {
        return createReferenceFlowsActivePower("default", contingencyId);
    }

    public PyPowsyblApiHeader.MatrixPointer createReferenceFlowsActivePower(String matrixId, String contingencyId) {
        SensitivityAnalysisContext.MatrixInfo m = this.branchFlowFactorsMatrix.get(matrixId);
        return createDoubleMatrix(() -> getReferences(contingencyId), m.getOffsetColumn(), 1, m.getColumnCount());
    }

    public PyPowsyblApiHeader.MatrixPointer createReferenceVoltages(String contingencyId) {
        return createDoubleMatrix(() -> getReferences(contingencyId), this.busVoltageFactorsMatrix.getOffsetData(),
                this.busVoltageFactorsMatrix.getRowCount(), this.busVoltageFactorsMatrix.getColumnCount());
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
