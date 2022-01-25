package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.CreateEquipmentHelper;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.SubstationAdder;

import java.util.Collections;
import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class SubstationDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.strings("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.strings("country"),
            SeriesMetadata.strings("tso")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElement(Network network, UpdatingDataframe dataframe, int indexElement) {
        SubstationAdder adder = network.newSubstation();
        CreateEquipmentHelper.createIdentifiable(adder, dataframe, indexElement);
        dataframe.getStringValue("country", indexElement).map(Country::valueOf).ifPresent(adder::setCountry);
        dataframe.getStringValue("tso", indexElement).ifPresent(adder::setTso);
        adder.add();
    }
}
