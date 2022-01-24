package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.CreateEquipmentHelper;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.BatteryAdder;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class BatteryDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.strings("voltage_level_id"),
            SeriesMetadata.strings("bus_id"),
            SeriesMetadata.strings("connectable_bus_id"),
            SeriesMetadata.ints("node"),
            SeriesMetadata.strings("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.strings("max_p"),
            SeriesMetadata.doubles("min_p"),
            SeriesMetadata.doubles("p0"),
            SeriesMetadata.doubles("q0")
    );

    @Override
    public List<SeriesMetadata> getSeriesMetadata() {
        return METADATA;
    }

    @Override
    public void addElement(Network network, UpdatingDataframe dataframe, int indexElement) {
        BatteryAdder batteryAdder = network.getVoltageLevel(dataframe.getStringValue("voltage_level_id", indexElement)
                        .orElseThrow(() -> new PowsyblException("voltage_level_id is missing")))
                .newBattery();
        CreateEquipmentHelper.createInjection(batteryAdder, dataframe, indexElement);
        dataframe.getDoubleValue("max_p", indexElement).ifPresent(batteryAdder::setMaxP);
        dataframe.getDoubleValue("min_p", indexElement).ifPresent(batteryAdder::setMinP);
        dataframe.getDoubleValue("p0", indexElement).ifPresent(batteryAdder::setP0);
        dataframe.getDoubleValue("q0", indexElement).ifPresent(batteryAdder::setQ0);
        batteryAdder.add();
    }
}
