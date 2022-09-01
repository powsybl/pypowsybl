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
public class XnecWithDecompositionContext extends DecomposedFlow {
    private final String branchId;

    public XnecWithDecompositionContext(Map.Entry<String, DecomposedFlow> entry) {
        super(entry.getValue().getLoopFlows(),
            entry.getValue().getInternalFlow(),
            entry.getValue().getAllocatedFlow(),
            entry.getValue().getPstFlow(),
            entry.getValue().getAcReferenceFlow(),
            entry.getValue().getDcReferenceFlow(),
            entry.getValue().getCountry1(),
            entry.getValue().getCountry2());
        this.branchId = entry.getKey();
    }

    public String getId() {
        return getBranchId() + "_" + getVariantId();
    }

    public String getBranchId() {
        return branchId;
    }

    public String getVariantId() {
        return "InitialState"; //TODO replace this. Done like this now as we do not manage contingencies for V1.0 of flow decomposition.
    }

    public String getCountry1String() {
        return super.getCountry1().toString();
    }

    public String getCountry2String() {
        return super.getCountry2().toString();
    }

    public static Map<String, ToDoubleFunction<XnecWithDecompositionContext>> getLoopFlowsFunctionMap(Set<Country> zoneSet) {
        TreeMap<String, ToDoubleFunction<XnecWithDecompositionContext>> loopFlows = new TreeMap<>();
        zoneSet.stream().sorted().forEach(country -> loopFlows.put(getColumnPep8Name(country), decomposedFlow -> decomposedFlow.getLoopFlow(country)));
        return loopFlows;
    }

    private static String getColumnPep8Name(Country country) {
        return NetworkUtil.getLoopFlowIdFromCountry(country).replace(" ", "_").toLowerCase();
    }
}
