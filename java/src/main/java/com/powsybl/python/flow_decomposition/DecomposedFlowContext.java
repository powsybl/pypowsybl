/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.flow_decomposition;

import com.powsybl.flow_decomposition.DecomposedFlow;
import com.powsybl.iidm.network.Country;
import com.powsybl.flow_decomposition.NetworkUtil;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class DecomposedFlowContext extends DecomposedFlow {

    private final String xnecId;

    public DecomposedFlowContext(String xnecId, DecomposedFlow decomposedFlow) {
        super(decomposedFlow.getLoopFlows(), decomposedFlow.getAllocatedFlow(), decomposedFlow.getPstFlow(),
            decomposedFlow.getAcReferenceFlow(), decomposedFlow.getDcReferenceFlow(), decomposedFlow.getCountries());
        this.xnecId = xnecId;
    }

    public String getXnecId() {
        return xnecId;
    }

    public String getCountry1() {
        return getCountries().getFirst().toString();
    }

    public String getCountry2() {
        return getCountries().getSecond().toString();
    }

    public static Map<String, ToDoubleFunction<DecomposedFlowContext>> getLoopFlowsFunctionMap(Set<Country> zoneSet) {
        TreeMap<String, ToDoubleFunction<DecomposedFlowContext>> loopFlows = new TreeMap<>();
        zoneSet.stream().sorted().forEach(country -> loopFlows.put(getColumnPep8Name(country), decomposedFlow -> decomposedFlow.getLoopFlow(country)));
        return loopFlows;
    }

    private static String getColumnPep8Name(Country country) {
        return NetworkUtil.getLoopFlowIdFromCountry(country).replace(" ", "_").toLowerCase();
    }
}
