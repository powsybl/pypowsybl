package com.powsybl.python.dynamic;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.powsybl.dynamicsimulation.EventModel;
import com.powsybl.dynamicsimulation.EventModelsSupplier;
import com.powsybl.dynawaltz.events.EventQuadripoleDisconnection;
import com.powsybl.dynawaltz.events.EventSetPointBoolean;
import com.powsybl.iidm.network.Network;

public class EventSupplier implements EventModelsSupplier {
    private List<Supplier<EventModel>> eventSupplierList;

    public EventSupplier() {
        eventSupplierList = new LinkedList<>();
    }

    /**
     * According to Dynawaltz staticId must refer to a line or a two winding
     * transformer
     * <p>
     * The event represent the disconnection the given line/transformer
     */
    public void addEventQuadripoleDisconnection(String eventModelId, String staticId, String parameterSetId) {
        this.eventSupplierList.add(() -> new EventQuadripoleDisconnection(eventModelId, staticId, parameterSetId));
    }

    /**
     * According to Dynawaltz staticId must refer to a generator
     * <p>
     * The event represent the disconnection of the given generator
     */
    public void addEventSetPointBoolean(String eventModelId, String staticId, String parameterSetId) {
        this.eventSupplierList.add(() -> new EventSetPointBoolean(eventModelId, staticId, parameterSetId));
    }

    @Override
    public List<EventModel> get(Network network) {
        return eventSupplierList.stream().map(supplier -> supplier.get()).collect(Collectors.toList());
    }
}
