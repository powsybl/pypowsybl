package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.CreateEquipmentHelper;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.BusAdder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;

import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class BusDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.strings("voltage_level_id"),
            SeriesMetadata.strings("id"),
            SeriesMetadata.strings("name")
    );

    @Override
    public List<SeriesMetadata> getSeriesMetadata() {
        return METADATA;
    }

    @Override
    public void addElement(Network network, UpdatingDataframe dataframe, int index) {
        VoltageLevel vl = network.getVoltageLevel(dataframe.getStringValue("voltage_level_id", index)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing")));
        BusAdder adder = vl.getBusBreakerView().newBus();
        CreateEquipmentHelper.createIdentifiable(adder, dataframe, index);
        adder.add();
    }
}
