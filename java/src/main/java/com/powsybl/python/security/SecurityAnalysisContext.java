/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.security;

import com.powsybl.commons.reporter.Reporter;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.python.commons.PyPowsyblConfiguration;
import com.powsybl.python.contingency.ContingencyContainerImpl;
import com.powsybl.security.*;
import com.powsybl.security.detectors.DefaultLimitViolationDetector;
import com.powsybl.security.monitor.StateMonitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SecurityAnalysisContext extends ContingencyContainerImpl {

    private final List<StateMonitor> monitors = new ArrayList<>();

    SecurityAnalysisResult run(Network network, LoadFlowParameters loadFlowParameters, String provider) {
        SecurityAnalysisParameters securityAnalysisParameters = PyPowsyblConfiguration.isReadConfig() ? SecurityAnalysisParameters.load() : new SecurityAnalysisParameters();
        securityAnalysisParameters.setLoadFlowParameters(loadFlowParameters);
        ContingenciesProvider contingencies = this::createContingencies;
        SecurityAnalysisReport report = SecurityAnalysis.find(provider)
                .run(
                    network,
                    network.getVariantManager().getWorkingVariantId(),
                    contingencies,
                    securityAnalysisParameters,
                    LocalComputationManager.getDefault(),
                    new LimitViolationFilter(),
                    new DefaultLimitViolationDetector(),
                    Collections.emptyList(),
                    monitors,
                    Reporter.NO_OP
                );
        return report.getResult();
    }

    void addMonitor(StateMonitor monitor) {
        monitors.add(monitor);
    }
}
