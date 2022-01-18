package com.powsybl.python;

import com.powsybl.iidm.network.Bus;

public class BusBreakerViewBusContext {
    private final Bus busBreakerViewBus;
    private final Bus busViewBus;

    public BusBreakerViewBusContext(Bus busBreakerViewBus, Bus busViewBus) {
        this.busBreakerViewBus = busBreakerViewBus;
        this.busViewBus = busViewBus;
    }

    public Bus getBusBreakerViewBus() {
        return busBreakerViewBus;
    }

    public Bus getBusViewBus() {
        return busViewBus;
    }
}
