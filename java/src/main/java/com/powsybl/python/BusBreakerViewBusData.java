package com.powsybl.python;

import com.powsybl.iidm.network.Bus;

import java.util.Objects;

public class BusBreakerViewBusData {
    private final Bus busBreakerViewBus;
    private final Bus busViewBus;

    public BusBreakerViewBusData(Bus busBreakerViewBus, Bus busViewBus) {
        this.busBreakerViewBus = Objects.requireNonNull(busBreakerViewBus);
        this.busViewBus = busViewBus;
    }

    public String getId() {
        return busBreakerViewBus.getId();
    }

    public String getName() {
        return busBreakerViewBus.getOptionalName().orElse("");
    }

    public String getBusViewBusId() {
        return busViewBus != null ? busViewBus.getId() : "";
    }
}
