/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.sensitivity.SensitivityValue;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.word.WordFactory;

import java.util.*;
import java.util.function.Function;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SensitivityAnalysisResultContextV1 implements SensitivityAnalysisResultContext {

    private final List<String> functions;

    private final List<String> variables;

    private final Collection<SensitivityValue> sensitivityValues;

    private final Map<String, List<SensitivityValue>> sensitivityValuesByContingencyId;

    private Map<String, Integer> idxByFunction;
    private Map<String, Integer> idxByVariable;

    SensitivityAnalysisResultContextV1(List<String> functions, List<String> variables, Collection<SensitivityValue> sensitivityValues,
                                       Map<String, List<SensitivityValue>> sensitivityValuesByContingencyId) {
        this.functions = Objects.requireNonNull(functions);
        this.variables = Objects.requireNonNull(variables);
        this.sensitivityValues = Collections.unmodifiableCollection(sensitivityValues);
        this.sensitivityValuesByContingencyId = sensitivityValuesByContingencyId;
    }

    private Collection<SensitivityValue> getSensitivityValues(String contingencyId) {
        return contingencyId.isEmpty() ? sensitivityValues : sensitivityValuesByContingencyId.get(contingencyId);
    }

    @Override
    public PyPowsyblApiHeader.MatrixPointer createSensitivityMatrix(String contingencyId) {
        Collection<SensitivityValue> values = getSensitivityValues(contingencyId);
        if (values.isEmpty()) {
            return WordFactory.nullPointer();
        }
        double[] sensitivityValues = reorder(values, SensitivityValue::getValue);
        CDoublePointer valuePtr = UnmanagedMemory.calloc(variables.size() * functions.size() * SizeOf.get(CDoublePointer.class));
        for (int i = 0; i < sensitivityValues.length; i++) {
            valuePtr.addressOf(i).write(sensitivityValues[i]);
        }
        PyPowsyblApiHeader.MatrixPointer matrixPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.MatrixPointer.class));
        matrixPtr.setRowCount(variables.size());
        matrixPtr.setColumnCount(functions.size());
        matrixPtr.setValues(valuePtr);
        return matrixPtr;
    }

    private double[] reorder(Collection<SensitivityValue> sensitivityValues, Function<SensitivityValue, Double> converter) {
        buildIdxMaps();
        double[] values = new double[variables.size() * functions.size()];
        Arrays.fill(values, Double.NaN);
        for (SensitivityValue value : sensitivityValues) {
            int idxRow = idxByVariable.get(value.getFactor().getVariable().getId());
            int idxCol = idxByFunction.get(value.getFactor().getFunction().getId());
            int arrIdx = idxRow * idxByFunction.size() + idxCol;
            values[arrIdx] = converter.apply(value);
        }
        return values;
    }

    private void buildIdxMaps() {
        if (idxByFunction == null) {
            idxByFunction = new HashMap<>();
            idxByVariable = new HashMap<>();
            for (int i = 0; i < functions.size(); i++) {
                idxByFunction.put(functions.get(i), i);
            }
            for (int i = 0; i < variables.size(); i++) {
                idxByVariable.put(variables.get(i), i);
            }
        }
    }

    @Override
    public PyPowsyblApiHeader.MatrixPointer createReferenceFlows(String contingencyId) {
        Collection<SensitivityValue> sensitivityValues = getSensitivityValues(contingencyId);
        if (sensitivityValues == null || sensitivityValues.isEmpty()) {
            return WordFactory.nullPointer();
        }
        final double[] values = reorder(sensitivityValues, SensitivityValue::getFunctionReference);
        CDoublePointer valuePtr = UnmanagedMemory.calloc(functions.size() * SizeOf.get(CDoublePointer.class));
        for (int i = 0; i < values.length; i++) {
            valuePtr.addressOf(i).write(values[i]);
        }
        PyPowsyblApiHeader.MatrixPointer matrixPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.MatrixPointer.class));
        matrixPtr.setRowCount(1);
        matrixPtr.setColumnCount(functions.size());
        matrixPtr.setValues(valuePtr);
        return matrixPtr;
    }
}
