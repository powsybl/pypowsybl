package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.CreateEquipmentHelper;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.BusbarSectionAdder;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class BusBarDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.strings("voltage_level_id"),
            SeriesMetadata.ints("node"),
            SeriesMetadata.strings("id"),
            SeriesMetadata.strings("name")
    );

    @Override
    public List<SeriesMetadata> getSeriesMetadata() {
        return METADATA;
    }

    @Override
    public void addElement(Network network, UpdatingDataframe dataframe, int indexElement) {
        BusbarSectionAdder adder = network.getVoltageLevel(dataframe.getStringValue("voltage_level_id", indexElement)
                        .orElseThrow(() -> new PowsyblException("voltage_level_id is missing")))
                .getNodeBreakerView()
                .newBusbarSection();
        CreateEquipmentHelper.createIdentifiable(adder, dataframe, indexElement);
        dataframe.getIntValue("node", indexElement).ifPresent(adder::setNode);
        adder.add();
    }
}
