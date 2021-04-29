/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.sensitivity.SensitivityValue;
import org.apache.commons.lang3.NotImplementedException;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.word.WordFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SensitivityAnalysisResultContextV1 implements SensitivityAnalysisResultContext {

    private final int rowCount;

    private final int columnCount;

    private final Collection<SensitivityValue> sensitivityValues;

    private final Map<String, List<SensitivityValue>> sensitivityValuesByContingencyId;

    SensitivityAnalysisResultContextV1(int rowCount, int columnCount, Collection<SensitivityValue> sensitivityValues,
                                       Map<String, List<SensitivityValue>> sensitivityValuesByContingencyId) {
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.sensitivityValues = sensitivityValues;
        this.sensitivityValuesByContingencyId = sensitivityValuesByContingencyId;
    }

    private Collection<SensitivityValue> getSensitivityValues(String contingencyId) {
        return contingencyId.isEmpty() ? sensitivityValues : sensitivityValuesByContingencyId.get(contingencyId);
    }

    @Override
    public PyPowsyblApiHeader.MatrixPointer createBranchFlowsSensitivityMatrix(String contingencyId) {
        Collection<SensitivityValue> sensitivityValues = getSensitivityValues(contingencyId);
        if (sensitivityValues != null) {
            CDoublePointer valuePtr = UnmanagedMemory.calloc(rowCount * columnCount * SizeOf.get(CDoublePointer.class));
            for (SensitivityValue sensitivityValue : sensitivityValues) {
                IndexedSensitivityFactor indexedFactor = (IndexedSensitivityFactor) sensitivityValue.getFactor();
                valuePtr.addressOf(indexedFactor.getRow() * columnCount + indexedFactor.getColumn()).write(sensitivityValue.getValue());
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
    public PyPowsyblApiHeader.MatrixPointer createBusVoltagesSensitivityMatrix(String contingencyId) {
        throw new NotImplementedException("not implemented yet");
    }

    @Override
    public PyPowsyblApiHeader.MatrixPointer createReferenceFlows(String contingencyId) {
        Collection<SensitivityValue> sensitivityValues = getSensitivityValues(contingencyId);
        if (sensitivityValues != null) {
            CDoublePointer valuePtr = UnmanagedMemory.calloc(columnCount * SizeOf.get(CDoublePointer.class));
            for (SensitivityValue sensitivityValue : sensitivityValues) {
                IndexedSensitivityFactor indexedFactor = (IndexedSensitivityFactor) sensitivityValue.getFactor();
                valuePtr.addressOf(indexedFactor.getColumn()).write(sensitivityValue.getFunctionReference());
            }
            PyPowsyblApiHeader.MatrixPointer matrixPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.MatrixPointer.class));
            matrixPtr.setRowCount(1);
            matrixPtr.setColumnCount(columnCount);
            matrixPtr.setValues(valuePtr);
            return matrixPtr;
        }
        return WordFactory.nullPointer();
    }

    @Override
    public PyPowsyblApiHeader.MatrixPointer createReferenceVoltages(String contingencyId) {
        throw new NotImplementedException("not implemented yet");
    }
}
