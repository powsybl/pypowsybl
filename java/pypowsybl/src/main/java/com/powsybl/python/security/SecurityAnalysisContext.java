/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.security;

import com.powsybl.action.Action;
import com.powsybl.action.ActionList;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.CommonObjects;
import com.powsybl.python.contingency.ContingencyContainerImpl;
import com.powsybl.security.*;
import com.powsybl.security.monitor.StateMonitor;
import com.powsybl.security.strategy.OperatorStrategy;
import com.powsybl.security.strategy.OperatorStrategyList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SecurityAnalysisContext extends ContingencyContainerImpl {

    private final List<Action> actions = new ArrayList<>();

    private final List<OperatorStrategy> operatorStrategies = new ArrayList<>();

    private final List<StateMonitor> monitors = new ArrayList<>();

    SecurityAnalysisResult run(Network network, SecurityAnalysisParameters securityAnalysisParameters, String provider, ReportNode reportNode) {
        ContingenciesProvider contingencies = this::createContingencies;
        SecurityAnalysisRunParameters runParameters = new SecurityAnalysisRunParameters()
                .setSecurityAnalysisParameters(securityAnalysisParameters)
                .setComputationManager(CommonObjects.getComputationManager())
                .setOperatorStrategies(operatorStrategies)
                .setActions(actions)
                .setMonitors(monitors)
                .setReportNode(reportNode == null ? ReportNode.NO_OP : reportNode);
        SecurityAnalysisReport report = SecurityAnalysis.find(provider)
                .run(network, network.getVariantManager().getWorkingVariantId(), contingencies, runParameters);
        return report.getResult();
    }

    void addActionsFromJsonFile(Path path) {
        if (Files.exists(path)) {
            ActionList actionList;
            actionList = ActionList.readJsonFile(path);
            actions.addAll(actionList.getActions());
        } else {
            throw new SecurityException("No actions found in " + path);
        }
    }

    void addOperatorStrategiesFromJsonFile(Path path) {
        if (Files.exists(path)) {
            OperatorStrategyList operatorStrategyList;
            operatorStrategyList = OperatorStrategyList.read(path);
            operatorStrategies.addAll(operatorStrategyList.getOperatorStrategies());
        } else {
            throw new SecurityException("No operatorStrategies found in " + path);
        }
    }

    void addAction(Action action) {
        actions.add(action);
    }

    void addOperatorStrategy(OperatorStrategy strategy) {
        operatorStrategies.add(strategy);
    }

    void addMonitor(StateMonitor monitor) {
        monitors.add(monitor);
    }
}
