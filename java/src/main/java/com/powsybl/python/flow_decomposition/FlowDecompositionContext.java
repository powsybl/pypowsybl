package com.powsybl.python.flow_decomposition;

import java.util.ArrayList;
import java.util.List;

public class FlowDecompositionContext {
    private final List<String> precontingencyMonitoredElements = new ArrayList<>();

    public void addPrecontingencyMonitoredElements(List<String> elementIds) {
        precontingencyMonitoredElements.addAll(elementIds);
    }

    public List<String> getPrecontingencyMonitoredElements() {
        return precontingencyMonitoredElements;
    }
}
