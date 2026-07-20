/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynamicsimulation.EventModelsSupplier;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.CommonObjects;
import com.powsybl.python.contingency.ContingencyContainerImpl;
import com.powsybl.security.SecurityAnalysisReport;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.dynamic.DynamicSecurityAnalysis;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisRunParameters;
import com.powsybl.security.monitor.StateMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the inputs of a dynamic security analysis (contingencies inherited from
 * {@link ContingencyContainerImpl} plus monitored elements) and runs it through the
 * {@link DynamicSecurityAnalysis} API, reusing the dynamic simulation model/event suppliers.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class DynamicSecurityAnalysisContext extends ContingencyContainerImpl {

    private final List<StateMonitor> monitors = new ArrayList<>();

    SecurityAnalysisResult run(Network network,
                               DynamicModelsSupplier dynamicModelsSupplier,
                               EventModelsSupplier eventModelsSupplier,
                               DynamicSecurityAnalysisParameters dynamicSecurityAnalysisParameters,
                               String provider,
                               ReportNode reportNode) {
        ContingenciesProvider contingencies = this::createContingencies;
        DynamicSecurityAnalysisRunParameters runParameters = new DynamicSecurityAnalysisRunParameters()
                .setDynamicSecurityAnalysisParameters(dynamicSecurityAnalysisParameters)
                .setEventModelsSupplier(eventModelsSupplier)
                .setMonitors(monitors)
                .setComputationManager(CommonObjects.getComputationManager())
                .setReportNode(reportNode == null ? ReportNode.NO_OP : reportNode);
        SecurityAnalysisReport report = DynamicSecurityAnalysis.find(provider)
                .run(network,
                        network.getVariantManager().getWorkingVariantId(),
                        dynamicModelsSupplier,
                        contingencies,
                        runParameters);
        return report.getResult();
    }

    void addMonitor(StateMonitor monitor) {
        monitors.add(monitor);
    }
}
