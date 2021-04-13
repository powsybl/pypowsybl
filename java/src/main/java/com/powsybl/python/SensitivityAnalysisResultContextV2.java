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

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class SensitivityAnalysisResultContextV2 implements SensitivityAnalysisResultContext {

    private final int rowCount;

    private final int columnCount;

    private final double[] baseCaseValues;

    private final Map<String, double[]> valuesByContingencyId;

    private final double[] baseCaseReferenceFlows;

    private final Map<String, double[]> referenceFlowsByContingencyId;

    public SensitivityAnalysisResultContextV2(int rowCount, int columnCount, double[] baseCaseValues, Map<String, double[]> valuesByContingencyId,
                                              double[] baseCaseReferenceFlows, Map<String, double[]> referenceFlowsByContingencyId) {
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.baseCaseValues = baseCaseValues;
        this.valuesByContingencyId = valuesByContingencyId;
        this.baseCaseReferenceFlows = baseCaseReferenceFlows;
        this.referenceFlowsByContingencyId = referenceFlowsByContingencyId;
    }

    private double[] getValues(String contingencyId) {
        return contingencyId.isEmpty() ? baseCaseValues : valuesByContingencyId.get(contingencyId);
    }

    private double[] getReferenceFlows(String contingencyId) {
        return contingencyId.isEmpty() ? baseCaseReferenceFlows : referenceFlowsByContingencyId.get(contingencyId);
    }

    @Override
    public PyPowsyblApiHeader.MatrixPointer createSensitivityMatrix(String contingencyId) {
        double[] values = getValues(contingencyId);
        if (values != null) {
            CDoublePointer valuePtr = UnmanagedMemory.calloc(rowCount * columnCount * SizeOf.get(CDoublePointer.class));
            for (int i = 0; i < values.length; i++) {
                valuePtr.addressOf(i).write(values[i]);
            }
            PyPowsyblApiHeader.MatrixPointer matrixPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.MatrixPointer.class));
            matrixPtr.setRowCount(rowCount);
            matrixPtr.setColumnCount(columnCount);
            matrixPtr.setValues(valuePtr);
            return matrixPtr;
        }
        return WordFactory.nullPointer();

    }

    @Override
    public PyPowsyblApiHeader.MatrixPointer createReferenceFlows(String contingencyId) {
        double[] referenceFlows = getReferenceFlows(contingencyId);
        if (referenceFlows != null) {
            CDoublePointer valuePtr = UnmanagedMemory.calloc(columnCount * SizeOf.get(CDoublePointer.class));
            for (int i = 0; i < referenceFlows.length; i++) {
                valuePtr.addressOf(i).write(referenceFlows[i]);
            }
            PyPowsyblApiHeader.MatrixPointer matrixPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.MatrixPointer.class));
            matrixPtr.setRowCount(1);
            matrixPtr.setColumnCount(columnCount);
            matrixPtr.setValues(valuePtr);
            return matrixPtr;
        }
        return WordFactory.nullPointer();
    }
}
