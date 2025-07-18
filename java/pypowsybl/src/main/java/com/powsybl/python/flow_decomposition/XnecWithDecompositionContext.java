/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.flow_decomposition;

import com.powsybl.flow_decomposition.DecomposedFlow;
import com.powsybl.flow_decomposition.DecomposedFlowBuilder;
import com.powsybl.flow_decomposition.FlowPartition;
import com.powsybl.flow_decomposition.NetworkUtil;
import com.powsybl.iidm.network.Country;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class XnecWithDecompositionContext extends DecomposedFlow {
    public XnecWithDecompositionContext(DecomposedFlow decomposedFlow) {
        super(new DecomposedFlowBuilder().withBranchId(decomposedFlow.getBranchId())
                .withContingencyId(decomposedFlow.getContingencyId())
                .withCountry1(decomposedFlow.getCountry1())
                .withCountry2(decomposedFlow.getCountry2())
                .withAcTerminal1ReferenceFlow(decomposedFlow.getAcTerminal1ReferenceFlow())
                .withAcTerminal2ReferenceFlow(decomposedFlow.getAcTerminal2ReferenceFlow())
                .withDcReferenceFlow(decomposedFlow.getDcReferenceFlow())
                .withFlowPartition(new FlowPartition(decomposedFlow.getInternalFlow(),
                        decomposedFlow.getAllocatedFlow(),
                        decomposedFlow.getLoopFlows(),
                        decomposedFlow.getPstFlow(),
                        decomposedFlow.getXNodeFlow())));
    }

    public String getCountry1String() {
        return getCountry1().toString();
    }

    public String getCountry2String() {
        return getCountry2().toString();
    }

    public static Map<String, ToDoubleFunction<XnecWithDecompositionContext>> getLoopFlowsFunctionMap(Set<Country> zoneSet) {
        TreeMap<String, ToDoubleFunction<XnecWithDecompositionContext>> loopFlows = new TreeMap<>();
        zoneSet.forEach(country -> loopFlows.put(getColumnPep8Name(country), decomposedFlow -> decomposedFlow.getLoopFlow(country)));
        return loopFlows;
    }

    private static String getColumnPep8Name(Country country) {
        return NetworkUtil.getLoopFlowIdFromCountry(country).replace(" ", "_").toLowerCase();
    }
}
