package com.powsybl.python;

import com.powsybl.iidm.network.Switch;

import java.util.Objects;

public class NodeBreakerViewSwitchContext {
    private final Switch switchContext;
    private final int node1;
    private final int node2;

    public NodeBreakerViewSwitchContext(Switch switchContext, int node1, int node2) {
        this.switchContext = Objects.requireNonNull(switchContext);
        this.node1 = node1;
        this.node2 = node2;
    }

    public Switch getSwitchContext() {
        return switchContext;
    }

    public int getNode1() {
        return node1;
    }

    public int getNode2() {
        return node2;
    }
}
