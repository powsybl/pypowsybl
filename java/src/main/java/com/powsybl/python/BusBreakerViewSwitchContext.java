package com.powsybl.python;

import com.powsybl.iidm.network.Switch;

import java.util.Objects;

public class BusBreakerViewSwitchContext {
    private final Switch switchContext;
    private final String busId1;
    private final String busId2;

    public BusBreakerViewSwitchContext(Switch switchContext, String busId1, String busId2) {
        this.switchContext = Objects.requireNonNull(switchContext);
        this.busId1 = busId1;
        this.busId2 = busId2;
    }

    public Switch getSwitchContext() {
        return switchContext;
    }

    public String getBusId1() {
        return busId1;
    }

    public String getBusId2() {
        return busId2;
    }
}
