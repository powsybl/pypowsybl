package com.powsybl.python.flow_decomposition;

import com.powsybl.flow_decomposition.DecomposedFlow;
import com.powsybl.iidm.network.Country;
import com.powsybl.flow_decomposition.NetworkUtil;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;

public class DecomposedFlowContext extends DecomposedFlow {

    private final String xnecId;

    public DecomposedFlowContext(String xnecId, DecomposedFlow decomposedFlow) {
        super(decomposedFlow.getLoopFlows(), decomposedFlow.getAllocatedFlow(), decomposedFlow.getPstFlow(),
            decomposedFlow.getAcReferenceFlow(), decomposedFlow.getDcReferenceFlow());
        this.xnecId = xnecId;
    }

    public String getXnecId() {
        return xnecId;
    }

    public static Map<String, ToDoubleFunction<DecomposedFlowContext>> getLoopFlowsFunctionMap(Set<Country> zoneSet) {
        TreeMap<String, ToDoubleFunction<DecomposedFlowContext>> loopFlows = new TreeMap<>();
        zoneSet.stream().sorted().forEach(country -> loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(country), decomposedFlow -> decomposedFlow.getLoopFlow(country)));
        return loopFlows;
    }
}
