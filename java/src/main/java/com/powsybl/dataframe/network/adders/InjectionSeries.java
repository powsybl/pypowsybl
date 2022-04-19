package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.InjectionAdder;

import static com.powsybl.dataframe.network.adders.NetworkElementCreationUtils.applyIfPresent;

/**
 * Common series for all injections.
 *
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
class InjectionSeries extends IdentifiableSeries {

    private final StringSeries connectableBuses;
    private final StringSeries buses;
    private final IntSeries nodes;

    InjectionSeries(UpdatingDataframe dataframe) {
        super(dataframe);
        this.connectableBuses = dataframe.getStrings("connectable_bus_id");
        this.buses = dataframe.getStrings("bus_id");
        this.nodes = dataframe.getInts("node");
    }

    protected void setInjectionAttributes(InjectionAdder<?> adder, int row) {
        setIdentifiableAttributes(adder, row);
        applyIfPresent(connectableBuses, row, adder::setConnectableBus);
        applyIfPresent(buses, row, adder::setBus);
        applyIfPresent(nodes, row, adder::setNode);
    }
}
