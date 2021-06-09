/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.commons.PowsyblException;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.sensi.*;
import com.powsybl.sensitivity.*;
import com.powsybl.sensitivity.factors.BranchFlowPerInjectionIncrease;
import com.powsybl.sensitivity.factors.BranchFlowPerPSTAngle;
import com.powsybl.sensitivity.factors.BusVoltagePerTargetV;
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.functions.BusVoltage;
import com.powsybl.sensitivity.factors.variables.InjectionIncrease;
import com.powsybl.sensitivity.factors.variables.PhaseTapChangerAngle;
import com.powsybl.sensitivity.factors.variables.TargetVoltage;

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

    private List<SensitivityFactor> createFactors(Network network) {
        List<SensitivityFactor> factors = new ArrayList<>();
        createBranchFlowFactors(network, factors);
        createBusVoltageFactorMatrix(network, factors);
        return factors;
    }

    private void createBranchFlowFactors(Network network, List<SensitivityFactor> factors) {
        if (branchFlowBranchesIds == null) {
            return;
        }
        for (int column = 0; column < branchFlowBranchesIds.size(); column++) {
            String branchId = branchFlowBranchesIds.get(column);
            Branch branch = network.getBranch(branchId);
            if (branch == null) {
                throw new PowsyblException("Branch '" + branchId + "' not found");
            }
            BranchFlow branchFlow = new BranchFlow(branchId, branch.getNameOrId(), branchId);
            for (int row = 0; row < branchFlowVariablesIds.size(); row++) {
                String injectionOrTransfoId = branchFlowVariablesIds.get(row);
                Injection<?> injection = getInjection(network, injectionOrTransfoId);
                if (injection != null) {
                    InjectionIncrease injectionIncrease = new InjectionIncrease(injectionOrTransfoId, injection.getNameOrId(), injectionOrTransfoId);
                    factors.add(new BranchFlowPerInjectionIncrease(branchFlow, injectionIncrease));
                } else {
                    TwoWindingsTransformer twt = network.getTwoWindingsTransformer(injectionOrTransfoId);
                    if (twt != null) {
                        if (twt.getPhaseTapChanger() == null) {
                            throw new PowsyblException("Transformer '" + injectionOrTransfoId + "' is not a phase shifter");
                        }
                        PhaseTapChangerAngle phaseTapChangerAngle = new PhaseTapChangerAngle(injectionOrTransfoId, twt.getNameOrId(), injectionOrTransfoId);
                        factors.add(new BranchFlowPerPSTAngle(branchFlow, phaseTapChangerAngle));
                    } else {
                        throw new PowsyblException("Injection or transformer '" + injectionOrTransfoId + "' not found");
                    }
                }
            }
        }
    }

    private void createBusVoltageFactorMatrix(Network network, List<SensitivityFactor> factors) {
        if (targetVoltageEquipmentsIds == null) {
            return;
        }
        for (int column = 0; column < busVoltageEquipmentsIds.size(); column++) {
            String busId = busVoltageEquipmentsIds.get(column);
            Bus bus = network.getBusView().getBus(busId);
            if (bus == null) {
                throw new PowsyblException("Bus '" + busId + "' not found");
            }
            BusVoltage busVoltage = new BusVoltage(busId, busId, new IdBasedBusRef(busId));
            for (int row = 0; row < targetVoltageEquipmentsIds.size(); row++) {
                String targetVoltageId = targetVoltageEquipmentsIds.get(row);
                TargetVoltage targetVoltage = new TargetVoltage(targetVoltageId, targetVoltageId, targetVoltageId);
                factors.add(new BusVoltagePerTargetV(busVoltage, targetVoltage));
            }
        }
    }

    SensitivityAnalysisResultContext run(Network network, LoadFlowParameters loadFlowParameters, String provider) {
        if ("OpenSensitivityAnalysis".equals(provider)) {
            return runV2(network, loadFlowParameters);
        } else {
            return runV1(network, loadFlowParameters, provider);
        }
    }

    private SensitivityAnalysisResultContextV1 runV1(Network network, LoadFlowParameters loadFlowParameters, String provider) {
        SensitivityAnalysisParameters sensitivityAnalysisParameters = SensitivityAnalysisParameters.load();
        sensitivityAnalysisParameters.setLoadFlowParameters(loadFlowParameters);
        List<Contingency> contingencies = createContingencies(network);
        List<SensitivityFactor> factors = createFactors(network);
        SensitivityAnalysisResult result = SensitivityAnalysis.find(provider).run(network, VariantManagerConstants.INITIAL_VARIANT_ID,
            n -> factors, contingencies, sensitivityAnalysisParameters, LocalComputationManager.getDefault());
        SensitivityAnalysisResultContextV1 resultContext = null;
        if (result.isOk()) {
            Collection<SensitivityValue> sensitivityValues = result.getSensitivityValues();
            Map<String, List<SensitivityValue>> sensitivityValuesByContingencyId = result.getSensitivityValuesContingencies();
            resultContext = new SensitivityAnalysisResultContextV1(branchFlowBranchesIds, branchFlowVariablesIds,
                                                                busVoltageEquipmentsIds, targetVoltageEquipmentsIds,
                                                                sensitivityValues, sensitivityValuesByContingencyId);
        }
        return resultContext;
    }

    private SensitivityAnalysisResultContext runV2(Network network, LoadFlowParameters loadFlowParameters) {
        SensitivityAnalysisParameters sensitivityAnalysisParameters = SensitivityAnalysisParameters.load();
        sensitivityAnalysisParameters.setLoadFlowParameters(loadFlowParameters);
        List<Contingency> contingencies = createContingencies(network);

        int branchFlowMatrixColumnCount = branchFlowBranchesIds != null ? branchFlowBranchesIds.size() : 0;
        int branchFlowMatrixRowCount = branchFlowVariablesIds != null ? branchFlowVariablesIds.size() : 0;

        // second matrix offset
        int busVoltageMatrixSerializedOffset = branchFlowMatrixColumnCount * branchFlowMatrixRowCount;
        int busVoltageMatrixColCount = busVoltageEquipmentsIds != null ? busVoltageEquipmentsIds.size() : 0;
        int busVoltageMatrixRowCount = targetVoltageEquipmentsIds != null ? targetVoltageEquipmentsIds.size() : 0;

        Map<String, SensitivityVariableSet> variableSetsById = variableSets.stream().collect(Collectors.toMap(SensitivityVariableSet::getId, e -> e));

        ContingencyContext contingencyContext = ContingencyContext.createAllContingencyContext();
        SensitivityFactorReader factorReader = handler -> {
            for (int column = 0; column < branchFlowMatrixColumnCount; column++) {
                String branchId = branchFlowBranchesIds.get(column);
                Branch branch = network.getBranch(branchId);
                if (branch == null) {
                    throw new PowsyblException("Branch '" + branchId + "' not found");
                }
                for (int row = 0; row < branchFlowMatrixRowCount; row++) {
                    int index = column + branchFlowMatrixColumnCount * row;
                    String variableId = branchFlowVariablesIds.get(row);
                    Injection<?> injection = getInjection(network, variableId);
                    if (injection != null) {
                        handler.onFactor(index, SensitivityFunctionType.BRANCH_ACTIVE_POWER, branchId,
                                SensitivityVariableType.INJECTION_ACTIVE_POWER, variableId,
                                false, contingencyContext);
                    } else {
                        TwoWindingsTransformer twt = network.getTwoWindingsTransformer(variableId);
                        if (twt != null) {
                            if (twt.getPhaseTapChanger() == null) {
                                throw new PowsyblException("Transformer '" + variableId + "' is not a phase shifter");
                            }
                            handler.onFactor(index, SensitivityFunctionType.BRANCH_ACTIVE_POWER, branchId,
                                    SensitivityVariableType.TRANSFORMER_PHASE, variableId,
                                    false, contingencyContext);
                        } else {
                            if (variableSetsById.containsKey(variableId)) {
                                handler.onFactor(index, SensitivityFunctionType.BRANCH_ACTIVE_POWER, branchId,
                                        SensitivityVariableType.INJECTION_ACTIVE_POWER, variableId,
                                        true, contingencyContext);
                            } else {
                                throw new PowsyblException("Variable '" + variableId + "' not found");
                            }
                        }
                    }
                }
            }
            for (int column = 0; column < busVoltageMatrixColCount; column++) {
                final String busVoltageId = busVoltageEquipmentsIds.get(column);
                for (int row = 0; row < busVoltageMatrixRowCount; row++) {
                    int index = busVoltageMatrixSerializedOffset + column + busVoltageMatrixColCount * row;
                    final String targetVoltageId = targetVoltageEquipmentsIds.get(row);
                    handler.onFactor(index, SensitivityFunctionType.BUS_VOLTAGE, busVoltageId,
                            SensitivityVariableType.BUS_TARGET_VOLTAGE, targetVoltageId, false, contingencyContext);
                }
            }
        };
        int baseCaseValueSize = branchFlowMatrixColumnCount * branchFlowMatrixRowCount + busVoltageMatrixColCount * busVoltageMatrixRowCount;
        double[] baseCaseValues = new double[baseCaseValueSize];
        double[][] valuesByContingencyIndex = new double[contingencies.size()][baseCaseValueSize];
        double[] baseCaseReferences = new double[branchFlowMatrixColumnCount + busVoltageMatrixColCount];
        double[][] referencesByContingencyIndex = new double[contingencies.size()][branchFlowMatrixColumnCount + busVoltageMatrixColCount];
        SensitivityValueWriter valueWriter = (factorContext, contingencyId, contingencyIndex, value, functionReference) -> {
            int factorIndex = (Integer) factorContext;
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

        new OpenSensitivityAnalysisProvider().run(network, contingencies, variableSets,
                sensitivityAnalysisParameters, factorReader, valueWriter);

        Map<String, double[]> valuesByContingencyId = new HashMap<>(contingencies.size());
        Map<String, double[]> referencesByContingencyId = new HashMap<>(contingencies.size());
        for (int contingencyIndex = 0; contingencyIndex < contingencies.size(); contingencyIndex++) {
            Contingency contingency = contingencies.get(contingencyIndex);
            valuesByContingencyId.put(contingency.getId(), valuesByContingencyIndex[contingencyIndex]);
            referencesByContingencyId.put(contingency.getId(), referencesByContingencyIndex[contingencyIndex]);
        }

        return new SensitivityAnalysisResultContextV2(branchFlowMatrixRowCount, branchFlowMatrixColumnCount,
                                                    busVoltageMatrixRowCount, busVoltageMatrixColCount,
                                                    baseCaseValues, valuesByContingencyId,
                                                    baseCaseReferences, referencesByContingencyId);
    }
}
