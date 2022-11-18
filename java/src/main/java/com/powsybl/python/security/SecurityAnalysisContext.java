/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.security;

import com.powsybl.commons.reporter.Reporter;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.CommonObjects;
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

    SecurityAnalysisResult run(Network network, SecurityAnalysisParameters securityAnalysisParameters, String provider, Reporter reporter) {
        ContingenciesProvider contingencies = this::createContingencies;
        SecurityAnalysisReport report = SecurityAnalysis.find(provider)
                .run(
                        network,
                        network.getVariantManager().getWorkingVariantId(),
                        contingencies,
                        securityAnalysisParameters,
                        CommonObjects.getComputationManager(),
                        new LimitViolationFilter(),
                        new DefaultLimitViolationDetector(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        monitors,
                        (reporter == null) ? Reporter.NO_OP : reporter
                );
        return report.getResult();
    }

    void addMonitor(StateMonitor monitor) {
        monitors.add(monitor);
    }
}
