package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.CreateEquipmentHelper;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.LineAdder;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class LineDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.strings("voltage_level1_id"),
            SeriesMetadata.strings("bus1_id"),
            SeriesMetadata.strings("connectable_bus1_id"),
            SeriesMetadata.ints("node1"),
            SeriesMetadata.strings("voltage_level2_id"),
            SeriesMetadata.strings("bus2_id"),
            SeriesMetadata.strings("connectable_bus2_id"),
            SeriesMetadata.ints("node2"),
            SeriesMetadata.strings("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.doubles("b1"),
            SeriesMetadata.doubles("b2"),
            SeriesMetadata.doubles("g1"),
            SeriesMetadata.doubles("g2"),
            SeriesMetadata.doubles("r"),
            SeriesMetadata.doubles("x")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElement(Network network, UpdatingDataframe dataframe, int indexElement) {
        LineAdder lineAdder = network.newLine();
        CreateEquipmentHelper.createBranch(lineAdder, dataframe, indexElement);
        dataframe.getDoubleValue("b1", indexElement).ifPresent(lineAdder::setB1);
        dataframe.getDoubleValue("b2", indexElement).ifPresent(lineAdder::setB2);
        dataframe.getDoubleValue("g1", indexElement).ifPresent(lineAdder::setG1);
        dataframe.getDoubleValue("g2", indexElement).ifPresent(lineAdder::setG2);
        dataframe.getDoubleValue("r", indexElement).ifPresent(lineAdder::setR);
        dataframe.getDoubleValue("x", indexElement).ifPresent(lineAdder::setX);
        lineAdder.add();
    }
}
