/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.reporter.Reporter;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SensitivityAnalysisContext extends AbstractContingencyContainer {

    private List<SensitivityVariableSet> variableSets = Collections.emptyList();

    private List<String> branchFlowBranchesIds;

    private List<String> branchFlowVariablesIds;

    private List<String> busVoltageEquipmentsIds;

    private List<String> targetVoltageEquipmentsIds;

    void setBranchFlowFactorMatrix(List<String> branchesIds, List<String> variablesIds) {
        this.branchFlowBranchesIds = Objects.requireNonNull(branchesIds);
        this.branchFlowVariablesIds = Objects.requireNonNull(variablesIds);
    }

    public void setVariableSets(List<SensitivityVariableSet> variableSets) {
        this.variableSets = Objects.requireNonNull(variableSets);
    }

    void setBusVoltageFactorMatrix(List<String> busVoltageIds, List<String> targetVoltageIds) {
        busVoltageEquipmentsIds = Objects.requireNonNull(busVoltageIds);
        targetVoltageEquipmentsIds = Objects.requireNonNull(targetVoltageIds);
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

    SensitivityAnalysisResultContext run(Network network, LoadFlowParameters loadFlowParameters, String provider) {
        SensitivityAnalysisParameters sensitivityAnalysisParameters = PyPowsyblConfiguration.isReadConfig() ? SensitivityAnalysisParameters.load() : new SensitivityAnalysisParameters();
        sensitivityAnalysisParameters.setLoadFlowParameters(loadFlowParameters);
        List<Contingency> contingencies = createContingencies(network);

        int branchFlowMatrixColumnCount = branchFlowBranchesIds != null ? branchFlowBranchesIds.size() : 0;
        int branchFlowMatrixRowCount = branchFlowVariablesIds != null ? branchFlowVariablesIds.size() : 0;

        // second matrix offset
        int busVoltageMatrixSerializedOffset = branchFlowMatrixColumnCount * branchFlowMatrixRowCount;
        int busVoltageMatrixColCount = busVoltageEquipmentsIds != null ? busVoltageEquipmentsIds.size() : 0;
        int busVoltageMatrixRowCount = targetVoltageEquipmentsIds != null ? targetVoltageEquipmentsIds.size() : 0;

        Map<String, SensitivityVariableSet> variableSetsById = variableSets.stream().collect(Collectors.toMap(SensitivityVariableSet::getId, e -> e));

        ContingencyContext contingencyContext = ContingencyContext.all();
        SensitivityFactorReader factorReader = handler -> {

            for (int row = 0; row < branchFlowMatrixRowCount; row++) {
                String variableId = branchFlowVariablesIds.get(row);
                Injection<?> injection = getInjection(network, variableId);
                for (int column = 0; column < branchFlowMatrixColumnCount; column++) {
                    String branchId = branchFlowBranchesIds.get(column);
                    Branch branch = network.getBranch(branchId);
                    if (branch == null) {
                        throw new PowsyblException("Branch '" + branchId + "' not found");
                    }
                    if (injection != null) {
                        handler.onFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER, branchId,
                                SensitivityVariableType.INJECTION_ACTIVE_POWER, variableId,
                                false, contingencyContext);
                    } else {
                        TwoWindingsTransformer twt = network.getTwoWindingsTransformer(variableId);
                        if (twt != null) {
                            if (twt.getPhaseTapChanger() == null) {
                                throw new PowsyblException("Transformer '" + variableId + "' is not a phase shifter");
                            }
                            handler.onFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER, branchId,
                                    SensitivityVariableType.TRANSFORMER_PHASE, variableId,
                                    false, contingencyContext);
                        } else {
                            if (variableSetsById.containsKey(variableId)) {
                                handler.onFactor(SensitivityFunctionType.BRANCH_ACTIVE_POWER, branchId,
                                        SensitivityVariableType.INJECTION_ACTIVE_POWER, variableId,
                                        true, contingencyContext);
                            } else {
                                throw new PowsyblException("Variable '" + variableId + "' not found");
                            }
                        }
                    }
                }
            }

            for (int row = 0; row < busVoltageMatrixRowCount; row++) {
                final String targetVoltageId = targetVoltageEquipmentsIds.get(row);
                for (int column = 0; column < busVoltageMatrixColCount; column++) {
                    final String busVoltageId = busVoltageEquipmentsIds.get(column);
                    handler.onFactor(SensitivityFunctionType.BUS_VOLTAGE, busVoltageId,
                            SensitivityVariableType.BUS_TARGET_VOLTAGE, targetVoltageId, false, contingencyContext);
                }
            }
        };

        int baseCaseValueSize = branchFlowMatrixColumnCount * branchFlowMatrixRowCount + busVoltageMatrixColCount * busVoltageMatrixRowCount;
        double[] baseCaseValues = new double[baseCaseValueSize];
        double[][] valuesByContingencyIndex = new double[contingencies.size()][baseCaseValueSize];
        double[] baseCaseReferences = new double[branchFlowMatrixColumnCount + busVoltageMatrixColCount];
        double[][] referencesByContingencyIndex = new double[contingencies.size()][branchFlowMatrixColumnCount + busVoltageMatrixColCount];
        SensitivityValueWriter valueWriter = (factorIndex, contingencyIndex, value, functionReference) -> {
            int columnIndex = factorIndex < busVoltageMatrixSerializedOffset ?
                    factorIndex % branchFlowMatrixColumnCount
                    : branchFlowMatrixColumnCount + (factorIndex - busVoltageMatrixSerializedOffset) % busVoltageMatrixColCount;
            if (contingencyIndex != -1) {
                valuesByContingencyIndex[contingencyIndex][factorIndex] = value;
                referencesByContingencyIndex[contingencyIndex][columnIndex] = functionReference;
            } else {
                baseCaseValues[factorIndex] = value;
                baseCaseReferences[columnIndex] = functionReference;
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
                        LocalComputationManager.getDefault(),
                        Reporter.NO_OP);

        Map<String, double[]> valuesByContingencyId = new HashMap<>(contingencies.size());
        Map<String, double[]> referencesByContingencyId = new HashMap<>(contingencies.size());
        for (int contingencyIndex = 0; contingencyIndex < contingencies.size(); contingencyIndex++) {
            Contingency contingency = contingencies.get(contingencyIndex);
            valuesByContingencyId.put(contingency.getId(), valuesByContingencyIndex[contingencyIndex]);
            referencesByContingencyId.put(contingency.getId(), referencesByContingencyIndex[contingencyIndex]);
        }

        return new SensitivityAnalysisResultContext(branchFlowMatrixRowCount, branchFlowMatrixColumnCount,
                                                    busVoltageMatrixRowCount, busVoltageMatrixColCount,
                                                    baseCaseValues, valuesByContingencyId,
                                                    baseCaseReferences, referencesByContingencyId);
    }
}
