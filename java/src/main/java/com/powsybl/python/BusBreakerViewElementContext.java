package com.powsybl.python;

import com.powsybl.iidm.network.ConnectableType;

import java.util.Optional;

public class BusBreakerViewElementContext {
    private final String elementId;
    private final String busId;
    private final ConnectableType type;
    private final Optional<SideEnum> side;

    public BusBreakerViewElementContext(ConnectableType type, String busId, String elementId) {
        this(type, busId, elementId, Optional.empty());
    }

    public BusBreakerViewElementContext(ConnectableType type, String busId, String elementId, Optional<SideEnum> side) {
        this.type = type;
        this.busId = busId;
        this.elementId = elementId;
        this.side = side;
    }

    public ConnectableType getType() {
        return type;
    }

    public Optional<SideEnum> getSide() {
        return side;
    }

    public String getBusId() {
        return busId;
    }

    public String getElementId() {
        return elementId;
    }
}
