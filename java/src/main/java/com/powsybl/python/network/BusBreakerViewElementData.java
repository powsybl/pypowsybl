package com.powsybl.python.network;

import com.powsybl.dataframe.SideEnum;
import com.powsybl.iidm.network.IdentifiableType;

import java.util.Optional;

public class BusBreakerViewElementData {
    private final String elementId;
    private final String busId;
    private final IdentifiableType type;
    private final SideEnum side;

    public BusBreakerViewElementData(IdentifiableType type, String busId, String elementId) {
        this(type, busId, elementId, null);
    }

    public BusBreakerViewElementData(IdentifiableType type, String busId, String elementId, SideEnum side) {
        this.type = type;
        this.busId = busId;
        this.elementId = elementId;
        this.side = side;
    }

    public IdentifiableType getType() {
        return type;
    }

    public Optional<SideEnum> getSide() {
        return Optional.ofNullable(side);
    }

    public String getBusId() {
        return busId;
    }

    public String getElementId() {
        return elementId;
    }
}
