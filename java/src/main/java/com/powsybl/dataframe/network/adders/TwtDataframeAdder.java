package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.CreateEquipmentHelper;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.VoltageLevel;

import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class TwtDataframeAdder extends AbstractSimpleAdder {

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
            SeriesMetadata.doubles("rated_u1"),
            SeriesMetadata.doubles("rated_u2"),
            SeriesMetadata.doubles("rated_s"),
            SeriesMetadata.doubles("b"),
            SeriesMetadata.doubles("g"),
            SeriesMetadata.doubles("r"),
            SeriesMetadata.doubles("x")
    );

    @Override
    public List<SeriesMetadata> getSeriesMetadata() {
        return METADATA;
    }

    @Override
    public void addElement(Network network, UpdatingDataframe dataframe, int indexElement) {
        String id = dataframe.getStringValue("id", indexElement)
                .orElseThrow(() -> new PowsyblException("id is missing"));
        String vlId1 = dataframe.getStringValue("voltage_level1_id", indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level1_id is missing"));
        String vlId2 = dataframe.getStringValue("voltage_level2_id", indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level2_id is missing"));
        VoltageLevel vl1 = network.getVoltageLevel(vlId1);
        if (vl1 == null) {
            throw new PowsyblException("Invalid voltage_level1_id : coud not find " + vlId1);
        }
        VoltageLevel vl2 = network.getVoltageLevel(vlId2);
        if (vl2 == null) {
            throw new PowsyblException("Invalid voltage_level1_id : coud not find " + vlId2);
        }
        Substation s1 = vl1.getSubstation().orElseThrow(() -> new PowsyblException("Could not create transformer " + id + ": no substation."));
        Substation s2 = vl2.getSubstation().orElseThrow(() -> new PowsyblException("Could not create transformer " + id + ": no substation."));
        if (s1 != s2) {
            throw new PowsyblException("Could not create transformer " + id + ": both voltage ids must be on the same substation");
        }
        var adder = s1.newTwoWindingsTransformer();

        CreateEquipmentHelper.createBranch(adder, dataframe, indexElement);
        dataframe.getDoubleValue("rated_u1", indexElement).ifPresent(adder::setRatedU1);
        dataframe.getDoubleValue("rated_u2", indexElement).ifPresent(adder::setRatedU2);
        dataframe.getDoubleValue("rated_s", indexElement).ifPresent(adder::setRatedS);
        dataframe.getDoubleValue("b", indexElement).ifPresent(adder::setB);
        dataframe.getDoubleValue("g", indexElement).ifPresent(adder::setG);
        dataframe.getDoubleValue("r", indexElement).ifPresent(adder::setR);
        dataframe.getDoubleValue("x", indexElement).ifPresent(adder::setX);
        adder.add();
    }
}
