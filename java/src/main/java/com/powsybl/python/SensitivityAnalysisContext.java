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
import com.powsybl.sensitivity.factors.functions.BranchFlow;
import com.powsybl.sensitivity.factors.variables.InjectionIncrease;
import com.powsybl.sensitivity.factors.variables.PhaseTapChangerAngle;

import java.util.*;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SensitivityAnalysisContext extends AbstractContingencyContainer {

    static class IndexedBranchFlowPerInjectionIncrease extends BranchFlowPerInjectionIncrease implements IndexedSensitivityFactor {

        private final int row;

        private final int column;

        IndexedBranchFlowPerInjectionIncrease(BranchFlow sensitivityFunction, InjectionIncrease sensitivityVariable,
                                              int row, int column) {
            super(sensitivityFunction, sensitivityVariable);
            this.row = row;
            this.column = column;
        }

        @Override
        public int getRow() {
            return row;
        }

        @Override
        public int getColumn() {
            return column;
        }
    }

    static class IndexedBranchFlowPerPSTAngle extends BranchFlowPerPSTAngle implements IndexedSensitivityFactor {

        private final int row;

        private final int column;

        IndexedBranchFlowPerPSTAngle(BranchFlow sensitivityFunction, PhaseTapChangerAngle sensitivityVariable,
                                     int row, int column) {
            super(sensitivityFunction, sensitivityVariable);
            this.row = row;
            this.column = column;
        }

        @Override
        public int getRow() {
            return row;
        }

        @Override
        public int getColumn() {
            return column;
        }
    }

    private List<String> branchsIds;

    private List<String> injectionsOrTransfosIds;

    void setFactorMatrix(List<String> branchsIds, List<String> injectionsOrTransfosIds) {
        this.branchsIds = Objects.requireNonNull(branchsIds);
        this.injectionsOrTransfosIds = Objects.requireNonNull(injectionsOrTransfosIds);
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
        if (branchsIds == null) {
            return Collections.emptyList();
        }
        List<SensitivityFactor> factors = new ArrayList<>();
        for (int column = 0; column < branchsIds.size(); column++) {
            String branchId = branchsIds.get(column);
            Branch branch = network.getBranch(branchId);
            if (branch == null) {
                throw new PowsyblException("Branch '" + branchId + "' not found");
            }
            BranchFlow branchFlow = new BranchFlow(branchId, branch.getNameOrId(), branchId);
            for (int row = 0; row < injectionsOrTransfosIds.size(); row++) {
                String injectionOrTransfoId = injectionsOrTransfosIds.get(row);
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
        return factors;
    }

    SensitivityAnalysisResultContext run(Network network, LoadFlowParameters loadFlowParameters, String provider) {
        if ("OpenLoadFlow".equals(provider)) {
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
            int columnCount = branchsIds != null ? branchsIds.size() : 0;
            int rowCount = injectionsOrTransfosIds != null ? injectionsOrTransfosIds.size() : 0;
            resultContext = new SensitivityAnalysisResultContextV1(rowCount, columnCount, sensitivityValues, sensitivityValuesByContingencyId);
        }
        return resultContext;
    }

    private SensitivityAnalysisResultContext runV2(Network network, LoadFlowParameters loadFlowParameters) {
        SensitivityAnalysisParameters sensitivityAnalysisParameters = SensitivityAnalysisParameters.load();
        sensitivityAnalysisParameters.setLoadFlowParameters(loadFlowParameters);
        List<Contingency> contingencies = createContingencies(network);

        int columnCount = branchsIds != null ? branchsIds.size() : 0;
        int rowCount = injectionsOrTransfosIds != null ? injectionsOrTransfosIds.size() : 0;

        SensitivityFactorReader factorReader = handler -> {
            if (branchsIds == null) {
                return;
            }
            for (int column = 0; column < columnCount; column++) {
                String branchId = branchsIds.get(column);
                Branch branch = network.getBranch(branchId);
                if (branch == null) {
                    throw new PowsyblException("Branch '" + branchId + "' not found");
                }
                for (int row = 0; row < rowCount; row++) {
                    int index = column + columnCount * row;
                    String injectionOrTransfoId = injectionsOrTransfosIds.get(row);
                    Injection<?> injection = getInjection(network, injectionOrTransfoId);
                    if (injection != null) {
                        handler.onSimpleFactor(index, SensitivityFunctionType.BRANCH_ACTIVE_POWER, branchId,
                                SensitivityVariableType.INJECTION_ACTIVE_POWER, injectionOrTransfoId);
                    } else {
                        TwoWindingsTransformer twt = network.getTwoWindingsTransformer(injectionOrTransfoId);
                        if (twt != null) {
                            if (twt.getPhaseTapChanger() == null) {
                                throw new PowsyblException("Transformer '" + injectionOrTransfoId + "' is not a phase shifter");
                            }
                            handler.onSimpleFactor(index, SensitivityFunctionType.BRANCH_ACTIVE_POWER, branchId,
                                    SensitivityVariableType.TRANSFORMER_PHASE, injectionOrTransfoId);
                        } else {
                            throw new PowsyblException("Injection or transformer '" + injectionOrTransfoId + "' not found");
                        }
                    }
                }
            }
        };

        double[] baseCaseValues = new double[rowCount * columnCount];
        double[][] valuesByContingencyIndex = new double[contingencies.size()][rowCount * columnCount];
        double[] baseCaseReferenceFlows = new double[columnCount];
        double[][] referenceFlowsByContingencyIndex = new double[contingencies.size()][columnCount];
        SensitivityValueWriter valueWriter = (factorContext, contingencyId, contingencyIndex, value, functionReference) -> {
            int factorIndex = (Integer) factorContext;
            int columnIndex = factorIndex % columnCount;
            if (contingencyIndex != -1) {
                valuesByContingencyIndex[contingencyIndex][factorIndex] = value;
                referenceFlowsByContingencyIndex[contingencyIndex][columnIndex] = functionReference;
            } else {
                baseCaseValues[factorIndex] = value;
                baseCaseReferenceFlows[columnIndex] = functionReference;
            }
        };

        new OpenSensitivityAnalysisProvider().run(network, VariantManagerConstants.INITIAL_VARIANT_ID, contingencies,
                sensitivityAnalysisParameters, factorReader, valueWriter);

        Map<String, double[]> valuesByContingencyId = new HashMap<>(contingencies.size());
        Map<String, double[]> referenceFlowsByContingencyId = new HashMap<>(contingencies.size());
        for (int contingencyIndex = 0; contingencyIndex < contingencies.size(); contingencyIndex++) {
            Contingency contingency = contingencies.get(contingencyIndex);
            valuesByContingencyId.put(contingency.getId(), valuesByContingencyIndex[contingencyIndex]);
            referenceFlowsByContingencyId.put(contingency.getId(), referenceFlowsByContingencyIndex[contingencyIndex]);
        }

        return new SensitivityAnalysisResultContextV2(rowCount, columnCount, baseCaseValues, valuesByContingencyId,
                baseCaseReferenceFlows, referenceFlowsByContingencyId);
    }
}
