package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.CreateEquipmentHelper;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.LccConverterStationAdder;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class LccDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.strings("voltage_level_id"),
            SeriesMetadata.strings("bus_id"),
            SeriesMetadata.strings("connectable_bus_id"),
            SeriesMetadata.ints("node"),
            SeriesMetadata.strings("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.doubles("power_factor"),
            SeriesMetadata.doubles("loss_factor")
    );

    @Override
    public List<SeriesMetadata> getSeriesMetadata() {
        return METADATA;
    }

    @Override
    public void addElement(Network network, UpdatingDataframe dataframe, int indexElement) {
        LccConverterStationAdder adder = network.getVoltageLevel(dataframe.getStringValue("voltage_level_id", indexElement)
                        .orElseThrow(() -> new PowsyblException("voltage_level_id is missing")))
                .newLccConverterStation();
        CreateEquipmentHelper.createHvdc(adder, dataframe, indexElement);
        dataframe.getDoubleValue("power_factor", indexElement).ifPresent(pf -> adder.setPowerFactor((float) pf));
        adder.add();
    }
}
