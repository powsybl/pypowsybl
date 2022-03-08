/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.security.LimitViolationFilter;
import com.powsybl.security.SecurityAnalysis;
import com.powsybl.security.SecurityAnalysisParameters;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.detectors.DefaultLimitViolationDetector;
import com.powsybl.security.monitor.StateMonitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SecurityAnalysisContext extends AbstractContingencyContainer {

    private final List<StateMonitor> monitors = new ArrayList<>();

    SecurityAnalysisResult run(Network network, LoadFlowParameters loadFlowParameters, String provider) {
        SecurityAnalysis.Runner runner = SecurityAnalysis.find(provider);
        SecurityAnalysisParameters securityAnalysisParameters = SecurityAnalysisParameters.load();
        securityAnalysisParameters.setLoadFlowParameters(loadFlowParameters);
        List<Contingency> contingencies = createContingencies(network);
        return runner
            .run(network, network.getVariantManager().getWorkingVariantId(), n -> contingencies, securityAnalysisParameters,
                    LocalComputationManager.getDefault(), new LimitViolationFilter(), new DefaultLimitViolationDetector(),
                Collections.emptyList(), monitors)
            .getResult();
    }

    void addMonitor(StateMonitor monitor) {
        monitors.add(monitor);
    }
}
