/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.sensitivity;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.reporter.Reporter;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.iidm.network.*;
import com.powsybl.python.commons.CommonObjects;
import com.powsybl.python.contingency.ContingencyContainerImpl;
import com.powsybl.sensitivity.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SensitivityAnalysisContext extends ContingencyContainerImpl {

    private List<SensitivityVariableSet> variableSets = Collections.emptyList();

    static class MatrixInfo {
        private final ContingencyContextType contingencyContextType;

        private final SensitivityFunctionType functionType;

        private final List<String> columnIds;

        private final List<String> rowIds;

        private final List<String> contingencyIds;

        private int offsetData;

        private int offsetColumn;

        MatrixInfo(ContingencyContextType context, SensitivityFunctionType functionType, List<String> columnIds, List<String> rowIds, List<String> contingencyIds) {
            this.contingencyContextType = context;
            this.functionType = functionType;
            this.columnIds = columnIds;
            this.rowIds = rowIds;
            this.contingencyIds = contingencyIds;
        }

        ContingencyContextType getContingencyContextType() {
            return contingencyContextType;
        }

        SensitivityFunctionType getFunctionType() {
            return functionType;
        }

        void setOffsetData(int offset) {
            this.offsetData = offset;
        }

        void setOffsetColumn(int offset) {
            this.offsetColumn = offset;
        }

        int getOffsetData() {
            return offsetData;
        }

        int getOffsetColumn() {
            return offsetColumn;
        }

        List<String> getRowIds() {
            return rowIds;
        }

        List<String> getColumnIds() {
            return columnIds;
        }

        List<String> getContingencyIds() {
            return contingencyIds;
        }

        int getRowCount() {
            return rowIds.size();
        }

        int getColumnCount() {
            return columnIds.size();
        }
    }

    private final Map<String, MatrixInfo> branchFlowFactorsMatrix = new HashMap<>();

    private MatrixInfo busVoltageFactorsMatrix;

    void addBranchFlowFactorMatrix(String matrixId, ContingencyContextType contingencyContextType, List<String> branchesIds,
                                   List<String> variablesIds, List<String> contingencyIds) {
        if (branchFlowFactorsMatrix.containsKey(matrixId)) {
            throw new PowsyblException("Matrix '" + matrixId + "' already exists.");
        }
        MatrixInfo info = new MatrixInfo(contingencyContextType, SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, branchesIds, variablesIds, contingencyIds);
        branchFlowFactorsMatrix.put(matrixId, info);
    }

    void addBranchFlowFactorMatrix(String matrixId, List<String> branchesIds, List<String> variablesIds) {
        addBranchFlowFactorMatrix(matrixId, ContingencyContextType.ALL, branchesIds, variablesIds, Collections.emptyList());
    }

    void addPreContingencyBranchFlowFactorMatrix(String matrixId, List<String> branchesIds, List<String> variablesIds) {
        addBranchFlowFactorMatrix(matrixId, ContingencyContextType.NONE, branchesIds, variablesIds, Collections.emptyList());
    }

    void addPostContingencyBranchFlowFactorMatrix(String matrixId, List<String> branchesIds, List<String> variablesIds, List<String> contingencies) {
        addBranchFlowFactorMatrix(matrixId, ContingencyContextType.SPECIFIC, branchesIds, variablesIds, contingencies);
    }

    void addBranchFactorMatrix(String matrixId, List<String> branchesIds, List<String> variablesIds,
                                   List<String> contingencies, ContingencyContextType contingencyContextType,
                                   SensitivityFunctionType sensitivityFunctionType) {
        if (branchFlowFactorsMatrix.containsKey(matrixId)) {
            throw new PowsyblException("Matrix '" + matrixId + "' already exists.");
        }
        MatrixInfo info = new MatrixInfo(contingencyContextType, sensitivityFunctionType, branchesIds, variablesIds, contingencies);
        branchFlowFactorsMatrix.put(matrixId, info);
    }

    public void setVariableSets(List<SensitivityVariableSet> variableSets) {
        this.variableSets = Objects.requireNonNull(variableSets);
    }

    void setBusVoltageFactorMatrix(List<String> busVoltageIds, List<String> targetVoltageIds) {
        busVoltageFactorsMatrix = new MatrixInfo(ContingencyContextType.ALL, SensitivityFunctionType.BUS_VOLTAGE, busVoltageIds, targetVoltageIds, Collections.emptyList());
    }

    private static Injection<?> getInjection(Network network, String injectionId) {
        Injection<?> injection = network.getGenerator(injectionId);
        if (injection == null) {
            injection = network.getLoad(injectionId);
        }
        if (injection == null) {
            injection = network.getLccConverterStation(injectionId);
        }
        if (injection == null) {
            injection = network.getVscConverterStation(injectionId);
        }
        return injection;
    }

    List<MatrixInfo> prepareMatrices() {
        List<MatrixInfo> matrices = new ArrayList<>();
        int offsetData = 0;
        int offsetColumns = 0;

        for (MatrixInfo matrix : branchFlowFactorsMatrix.values()) {
            matrix.setOffsetData(offsetData);
            matrix.setOffsetColumn(offsetColumns);
            matrices.add(matrix);
            offsetData += matrix.getColumnCount() * matrix.getRowCount();
            offsetColumns += matrix.getColumnCount();
        }

        if (busVoltageFactorsMatrix != null) {
            busVoltageFactorsMatrix.setOffsetData(offsetData);
            busVoltageFactorsMatrix.setOffsetColumn(offsetColumns);
            matrices.add(busVoltageFactorsMatrix);
        }
        return matrices;
    }

    int getTotalNumberOfMatrixFactors(List<MatrixInfo> matrices) {
        int count = 0;
        for (MatrixInfo matrix : matrices) {
            count += matrix.getColumnCount() * matrix.getRowCount();
        }
        return count;
    }

    int getTotalNumberOfMatrixFactorsColumns(List<MatrixInfo> matrices) {
        int count = 0;
        for (MatrixInfo matrix : matrices) {
            count += matrix.getColumnCount();
        }
        return count;
    }

    SensitivityAnalysisResultContext run(Network network, SensitivityAnalysisParameters sensitivityAnalysisParameters, String provider, Reporter reporter) {
        List<Contingency> contingencies = createContingencies(network);

        List<MatrixInfo> matrices = prepareMatrices();

        Map<String, SensitivityVariableSet> variableSetsById = variableSets.stream().collect(Collectors.toMap(SensitivityVariableSet::getId, e -> e));

        SensitivityFactorReader factorReader = handler -> {

            for (MatrixInfo matrix : matrices) {
                List<String> columns = matrix.getColumnIds();
                List<String> rows = matrix.getRowIds();
                List<ContingencyContext> contingencyContexts = new ArrayList<>();
                if (matrix.getContingencyContextType() == ContingencyContextType.ALL) {
                    contingencyContexts.add(ContingencyContext.all());
                } else if (matrix.getContingencyContextType() == ContingencyContextType.NONE) {
                    contingencyContexts.add(ContingencyContext.none());
                } else {
                    for (String c : matrix.getContingencyIds()) {
                        contingencyContexts.add(ContingencyContext.specificContingency(c));
                    }
                }

                switch (matrix.getFunctionType()) {
                    case BRANCH_ACTIVE_POWER_1 -> {
                        for (String variableId : rows) {
                            Injection<?> injection = getInjection(network, variableId);
                            for (String branchId : columns) {
                                Branch<?> branch = network.getBranch(branchId);
                                if (branch == null) {
                                    throw new PowsyblException("Branch '" + branchId + "' not found");
                                }
                                if (injection != null) {
                                    for (ContingencyContext cCtx : contingencyContexts) {
                                        handler.onFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, branchId,
                                                SensitivityVariableType.INJECTION_ACTIVE_POWER, variableId,
                                                false, cCtx);
                                    }
                                } else {
                                    TwoWindingsTransformer twt = network.getTwoWindingsTransformer(variableId);
                                    if (twt != null) {
                                        if (twt.getPhaseTapChanger() == null) {
                                            throw new PowsyblException("Transformer '" + variableId + "' is not a phase shifter");
                                        }
                                        for (ContingencyContext cCtx : contingencyContexts) {
                                            handler.onFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, branchId,
                                                    SensitivityVariableType.TRANSFORMER_PHASE, variableId,
                                                    false, cCtx);
                                        }
                                    } else {
                                        HvdcLine hvdcLine = network.getHvdcLine(variableId);
                                        if (hvdcLine != null) {
                                            for (ContingencyContext cCtx : contingencyContexts) {
                                                handler.onFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, branchId,
                                                        SensitivityVariableType.HVDC_LINE_ACTIVE_POWER, variableId,
                                                        false, cCtx);
                                            }
                                        } else {
                                            if (variableSetsById.containsKey(variableId)) {
                                                for (ContingencyContext cCtx : contingencyContexts) {
                                                    handler.onFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, branchId,
                                                            SensitivityVariableType.INJECTION_ACTIVE_POWER, variableId,
                                                            true, cCtx);
                                                }
                                            } else {
                                                throw new PowsyblException("Variable '" + variableId + "' not found");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    case BUS_VOLTAGE -> {
                        for (String targetVoltageId : rows) {
                            for (String busVoltageId : columns) {
                                handler.onFactor(SensitivityFunctionType.BUS_VOLTAGE, busVoltageId,
                                        SensitivityVariableType.BUS_TARGET_VOLTAGE, targetVoltageId, false, ContingencyContext.all());
                            }
                        }
                    }
                    case BRANCH_REACTIVE_POWER_1, BRANCH_REACTIVE_POWER_2, BRANCH_REACTIVE_POWER_3, BRANCH_CURRENT_1, BRANCH_CURRENT_2, BRANCH_CURRENT_3 -> {
                        for (String targetVoltageId : rows) {
                            for (String branchId : columns) {
                                Branch<?> branch = network.getBranch(branchId);
                                if (branch == null) {
                                    throw new PowsyblException("Branch '" + branchId + "' not found");
                                }
                                handler.onFactor(matrix.getFunctionType(), branchId,
                                        SensitivityVariableType.BUS_TARGET_VOLTAGE, targetVoltageId, false, ContingencyContext.all());
                            }
                        }
                    }
                }
            }
        };

        int baseCaseValueSize = getTotalNumberOfMatrixFactors(matrices);
        double[] baseCaseValues = new double[baseCaseValueSize];
        double[][] valuesByContingencyIndex = new double[contingencies.size()][baseCaseValueSize];

        int totalColumnsCount = getTotalNumberOfMatrixFactorsColumns(matrices);
        double[] baseCaseReferences = new double[totalColumnsCount];
        double[][] referencesByContingencyIndex = new double[contingencies.size()][totalColumnsCount];

        NavigableMap<Integer, MatrixInfo> factorIndexMatrixMap = new TreeMap<>();
        for (MatrixInfo m : matrices) {
            factorIndexMatrixMap.put(m.getOffsetData(), m);
        }

        SensitivityResultWriter valueWriter = new SensitivityResultWriter() {
            @Override
            public void writeSensitivityValue(int factorContext, int contingencyIndex, double value, double functionReference) {
                int factorIndex = factorContext;
                MatrixInfo m = factorIndexMatrixMap.floorEntry(factorIndex).getValue();

                int columnIdx = m.getOffsetColumn() + (factorIndex - m.getOffsetData()) % m.getColumnCount();
                if (contingencyIndex != -1) {
                    valuesByContingencyIndex[contingencyIndex][factorIndex] = value;
                    referencesByContingencyIndex[contingencyIndex][columnIdx] = functionReference;
                } else {
                    baseCaseValues[factorIndex] = value;
                    baseCaseReferences[columnIdx] = functionReference;
                }
            }

            @Override
            public void writeContingencyStatus(int i, SensitivityAnalysisResult.Status status) {

            }
        };

        SensitivityAnalysis.find(provider)
                .run(network,
                        network.getVariantManager().getWorkingVariantId(),
                        factorReader,
                        valueWriter,
                        contingencies,
                        variableSets,
                        sensitivityAnalysisParameters,
                        CommonObjects.getComputationManager(),
                        (reporter == null) ? Reporter.NO_OP : reporter);

        Map<String, double[]> valuesByContingencyId = new HashMap<>(contingencies.size());
        Map<String, double[]> referencesByContingencyId = new HashMap<>(contingencies.size());
        for (int contingencyIndex = 0; contingencyIndex < contingencies.size(); contingencyIndex++) {
            Contingency contingency = contingencies.get(contingencyIndex);
            valuesByContingencyId.put(contingency.getId(), valuesByContingencyIndex[contingencyIndex]);
            referencesByContingencyId.put(contingency.getId(), referencesByContingencyIndex[contingencyIndex]);
        }

        return new SensitivityAnalysisResultContext(branchFlowFactorsMatrix,
                busVoltageFactorsMatrix,
                baseCaseValues,
                valuesByContingencyId,
                baseCaseReferences,
                referencesByContingencyId);
    }

}
