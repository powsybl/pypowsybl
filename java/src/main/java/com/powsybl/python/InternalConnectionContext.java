package com.powsybl.python;

import com.powsybl.iidm.network.VoltageLevel;

import java.util.Objects;

public class InternalConnectionContext {

    private final VoltageLevel.NodeBreakerView.InternalConnection internalConnection;
    private final int index;

    public InternalConnectionContext(VoltageLevel.NodeBreakerView.InternalConnection internalConnection, int index) {
        this.index = index;
        this.internalConnection = Objects.requireNonNull(internalConnection);
    }

    public int getIndex() {
        return index;
    }

    public VoltageLevel.NodeBreakerView.InternalConnection getInternalConnection() {
        return internalConnection;
    }
}
