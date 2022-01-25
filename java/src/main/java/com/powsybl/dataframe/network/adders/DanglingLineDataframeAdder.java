package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.CreateEquipmentHelper;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.DanglingLineAdder;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class DanglingLineDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.strings("voltage_level_id"),
            SeriesMetadata.strings("bus_id"),
            SeriesMetadata.strings("connectable_bus_id"),
            SeriesMetadata.ints("node"),
            SeriesMetadata.strings("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.doubles("p0"),
            SeriesMetadata.doubles("q0"),
            SeriesMetadata.doubles("r"),
            SeriesMetadata.doubles("x"),
            SeriesMetadata.doubles("g"),
            SeriesMetadata.doubles("b")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElement(Network network, UpdatingDataframe dataframe, int indexElement) {
        DanglingLineAdder adder = network.getVoltageLevel(dataframe.getStringValue("voltage_level_id", indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing"))).newDanglingLine();
        CreateEquipmentHelper.createInjection(adder, dataframe, indexElement);
        dataframe.getDoubleValue("p0", indexElement).ifPresent(adder::setP0);
        dataframe.getDoubleValue("q0", indexElement).ifPresent(adder::setQ0);
        dataframe.getDoubleValue("r", indexElement).ifPresent(adder::setR);
        dataframe.getDoubleValue("x", indexElement).ifPresent(adder::setX);
        dataframe.getDoubleValue("g", indexElement).ifPresent(adder::setG);
        dataframe.getDoubleValue("b", indexElement).ifPresent(adder::setB);
        adder.add();
    }
}
