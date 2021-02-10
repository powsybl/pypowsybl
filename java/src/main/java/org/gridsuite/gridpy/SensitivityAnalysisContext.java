/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.gridpy;

import com.powsybl.commons.PowsyblException;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlowParameters;
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
            BranchFlow branchFlow = new BranchFlow(branchId, branch.getName(), branchId);
            for (int row = 0; row < injectionsOrTransfosIds.size(); row++) {
                String injectionOrTransfoId = injectionsOrTransfosIds.get(row);
                Generator generator = network.getGenerator(injectionOrTransfoId);
                if (generator != null) {
                    InjectionIncrease injectionIncrease = new InjectionIncrease(injectionOrTransfoId, generator.getName(), injectionOrTransfoId);
                    factors.add(new IndexedBranchFlowPerInjectionIncrease(branchFlow, injectionIncrease, row, column));
                } else {
                    TwoWindingsTransformer twt = network.getTwoWindingsTransformer(injectionOrTransfoId);
                    if (twt != null) {
                        if (twt.getPhaseTapChanger() == null) {
                            throw new PowsyblException("Transformer '" + injectionOrTransfoId + "' is not a phase shifter");
                        }
                        PhaseTapChangerAngle phaseTapChangerAngle = new PhaseTapChangerAngle(injectionOrTransfoId, twt.getName(), injectionOrTransfoId);
                        factors.add(new IndexedBranchFlowPerPSTAngle(branchFlow, phaseTapChangerAngle, row, column));
                    } else {
                        throw new PowsyblException("Injection or transformer '" + injectionOrTransfoId + "' not found");
                    }
                }
            }
        }
        return factors;
    }

    SensitivityAnalysisResultContext run(Network network, LoadFlowParameters loadFlowParameters) {
        SensitivityAnalysisParameters sensitivityAnalysisParameters = SensitivityAnalysisParameters.load();
        sensitivityAnalysisParameters.setLoadFlowParameters(loadFlowParameters);
        List<Contingency> contingencies = createContingencies(network);
        List<SensitivityFactor> factors = createFactors(network);
        SensitivityAnalysisResult result = SensitivityAnalysis.run(network, VariantManagerConstants.INITIAL_VARIANT_ID,
            n -> factors, contingencies, sensitivityAnalysisParameters, LocalComputationManager.getDefault());
        SensitivityAnalysisResultContext resultContext = null;
        if (result.isOk()) {
            Collection<SensitivityValue> sensitivityValues = result.getSensitivityValues();
            Map<String, List<SensitivityValue>> sensitivityValuesByContingencyId = result.getSensitivityValuesContingencies();
            int columnCount = branchsIds.size();
            int rowCount = injectionsOrTransfosIds.size();
            resultContext = new SensitivityAnalysisResultContext(rowCount, columnCount, sensitivityValues, sensitivityValuesByContingencyId);
        }
        return resultContext;
    }
}
