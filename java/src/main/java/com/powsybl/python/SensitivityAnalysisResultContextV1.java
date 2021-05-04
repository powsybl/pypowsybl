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

    private final List<String> flowFunctions;
    private final List<String> flowVariables;

    private final List<String> voltageFunctions;
    private final List<String> voltageVariables;

    private final Collection<SensitivityValue> sensitivityValues;

    private final Map<String, List<SensitivityValue>> sensitivityValuesByContingencyId;

    private Map<String, Integer> idxByFlowFunction;
    private Map<String, Integer> idxByFlowVariable;
    private Map<String, Integer> idxByVoltageFunction;
    private Map<String, Integer> idxByVoltageVariable;

    SensitivityAnalysisResultContextV1(List<String> flowFunctions, List<String> flowVariables,
                                       List<String> voltageFunctions, List<String> voltageVariables,
                                       Collection<SensitivityValue> sensitivityValues,
                                       Map<String, List<SensitivityValue>> sensitivityValuesByContingencyId) {
        this.flowFunctions = Objects.requireNonNull(flowFunctions);
        this.flowVariables = Objects.requireNonNull(flowVariables);
        this.voltageFunctions = Objects.requireNonNull(voltageFunctions);
        this.voltageVariables = Objects.requireNonNull(voltageVariables);
        this.sensitivityValues = Collections.unmodifiableCollection(sensitivityValues);
        this.sensitivityValuesByContingencyId = sensitivityValuesByContingencyId;
    }

    private Collection<SensitivityValue> getSensitivityValues(String contingencyId) {
        return contingencyId.isEmpty() ? sensitivityValues : sensitivityValuesByContingencyId.get(contingencyId);
    }

    @Override
    public PyPowsyblApiHeader.MatrixPointer createBranchFlowsSensitivityMatrix(String contingencyId) {
        Collection<SensitivityValue> sensitivityValues = getSensitivityValues(contingencyId);
        if (sensitivityValues == null || sensitivityValues.isEmpty()) {
            return WordFactory.nullPointer();
        }
        final double[] values = reorderFlows(sensitivityValues, SensitivityValue::getValue);
        CDoublePointer valuePtr = UnmanagedMemory.calloc(flowVariables.size() * flowFunctions.size() * SizeOf.get(CDoublePointer.class));
        for (int i = 0; i < values.length; i++) {
            valuePtr.addressOf(i).write(values[i]);
        }
        PyPowsyblApiHeader.MatrixPointer matrixPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.MatrixPointer.class));
        matrixPtr.setRowCount(flowVariables.size());
        matrixPtr.setColumnCount(flowFunctions.size());
        matrixPtr.setValues(valuePtr);
        return matrixPtr;
    }

    private double[] reorderFlows(Collection<SensitivityValue> sensitivityValues, Function<SensitivityValue, Double> converter) {
        buildFlowsIdxMaps();
        double[] values = new double[flowVariables.size() * flowFunctions.size()];
        Arrays.fill(values, Double.NaN);
        for (SensitivityValue value : sensitivityValues) {
            int idxRow = idxByFlowVariable.get(value.getFactor().getVariable().getId());
            int idxCol = idxByFlowFunction.get(value.getFactor().getFunction().getId());
            int arrIdx = idxRow * idxByFlowFunction.size() + idxCol;
            values[arrIdx] = converter.apply(value);
        }
        return values;
    }

    private double[] reoderVoltages(Collection<SensitivityValue> sensitivityValues, Function<SensitivityValue, Double> converter) {
        buildVoltagesIdxMaps();
        double[] values = new double[voltageVariables.size() * voltageFunctions.size()];
        Arrays.fill(values, Double.NaN);
        for (SensitivityValue value : sensitivityValues) {
            int idxRow = idxByVoltageVariable.get(value.getFactor().getVariable().getId());
            int idxCol = idxByVoltageFunction.get(value.getFactor().getFunction().getId());
            int arrIdx = idxRow * idxByVoltageFunction.size() + idxCol;
            values[arrIdx] = converter.apply(value);
        }
        return values;
    }

    private void buildFlowsIdxMaps() {
        if (idxByFlowFunction == null) {
            idxByFlowFunction = new HashMap<>();
            idxByFlowVariable = new HashMap<>();
            for (int i = 0; i < flowFunctions.size(); i++) {
                idxByFlowFunction.put(flowFunctions.get(i), i);
            }
            for (int i = 0; i < flowVariables.size(); i++) {
                idxByFlowVariable.put(flowVariables.get(i), i);
            }
        }
    }

    private void buildVoltagesIdxMaps() {
        if (idxByVoltageFunction == null) {
            idxByVoltageFunction = new HashMap<>();
            idxByVoltageVariable = new HashMap<>();
        }
        for (int i = 0; i < voltageFunctions.size(); i++) {
            idxByVoltageFunction.put(voltageFunctions.get(i), i);
        }
        for (int i = 0; i < voltageVariables.size(); i++) {
            idxByVoltageVariable.put(voltageVariables.get(i), i);
        }
    }

    @Override
    public PyPowsyblApiHeader.MatrixPointer createBusVoltagesSensitivityMatrix(String contingencyId) {
        // TODO currently this part is not tested yet by sensi2
        Collection<SensitivityValue> sensitivityValues = getSensitivityValues(contingencyId);
        if (sensitivityValues == null || sensitivityValues.isEmpty()) {
            return WordFactory.nullPointer();
        }
        final double[] values = reoderVoltages(sensitivityValues, SensitivityValue::getValue);
        CDoublePointer valuePtr = UnmanagedMemory.calloc(voltageVariables.size() * voltageFunctions.size() * SizeOf.get(CDoublePointer.class));
        for (int i = 0; i < values.length; i++) {
            valuePtr.addressOf(i).write(values[i]);
        }
        PyPowsyblApiHeader.MatrixPointer matrixPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.MatrixPointer.class));
        matrixPtr.setRowCount(voltageVariables.size());
        matrixPtr.setColumnCount(voltageFunctions.size());
        matrixPtr.setValues(valuePtr);
        return matrixPtr;
    }

    @Override
    public PyPowsyblApiHeader.MatrixPointer createReferenceFlows(String contingencyId) {
        Collection<SensitivityValue> sensitivityValues = getSensitivityValues(contingencyId);
        if (sensitivityValues == null || sensitivityValues.isEmpty()) {
            return WordFactory.nullPointer();
        }
        final double[] values = reorderFlows(sensitivityValues, SensitivityValue::getFunctionReference);
        CDoublePointer valuePtr = UnmanagedMemory.calloc(flowFunctions.size() * SizeOf.get(CDoublePointer.class));
        for (int i = 0; i < values.length; i++) {
            valuePtr.addressOf(i).write(values[i]);
        }
        PyPowsyblApiHeader.MatrixPointer matrixPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.MatrixPointer.class));
        matrixPtr.setRowCount(1);
        matrixPtr.setColumnCount(flowFunctions.size());
        matrixPtr.setValues(valuePtr);
        return matrixPtr;
    }

    @Override
    public PyPowsyblApiHeader.MatrixPointer createReferenceVoltages(String contingencyId) {
        Collection<SensitivityValue> sensitivityValues = getSensitivityValues(contingencyId);
        if (sensitivityValues == null || sensitivityValues.isEmpty()) {
            return WordFactory.nullPointer();
        }
        final double[] values = reoderVoltages(sensitivityValues, SensitivityValue::getFunctionReference);
        CDoublePointer valuePtr = UnmanagedMemory.calloc(voltageFunctions.size() * SizeOf.get(CDoublePointer.class));
        for (int i = 0; i < values.length; i++) {
            valuePtr.addressOf(i).write(values[i]);
        }
        PyPowsyblApiHeader.MatrixPointer matrixPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.MatrixPointer.class));
        matrixPtr.setRowCount(1);
        matrixPtr.setColumnCount(voltageFunctions.size());
        matrixPtr.setValues(valuePtr);
        return matrixPtr;
    }
}
