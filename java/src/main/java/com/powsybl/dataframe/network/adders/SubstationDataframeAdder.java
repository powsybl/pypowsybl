/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.SubstationAdder;

import java.util.Collections;
import java.util.List;

import static com.powsybl.dataframe.update.UpdatingDataframe.applyIfPresent;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class SubstationDataframeAdder implements NetworkElementAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.strings("country"),
            SeriesMetadata.strings("tso"),
            SeriesMetadata.strings("TSO")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElements(Network network, List<UpdatingDataframe> dataframes) {
        UpdatingDataframe primaryDf = getPrimaryDataframe(dataframes);
        StringSeries countries = primaryDf.getStrings("country");
        StringSeries tsos1 = primaryDf.getStrings("tso");
        StringSeries tsos2 = primaryDf.getStrings("TSO");
        for (int i = 0; i < primaryDf.getRowCount(); i++) {
            addElement(network, primaryDf, countries, tsos1, tsos2, i);
        }
    }

    protected UpdatingDataframe getPrimaryDataframe(List<UpdatingDataframe> dataframes) {
        if (dataframes.size() != 1) {
            throw new IllegalArgumentException("Expected only one input dataframe");
        }
        return dataframes.get(0);
    }

    private void addElement(Network network, UpdatingDataframe dataframe,
                            StringSeries countries, StringSeries tsos1, StringSeries tsos2, int row) {
        SubstationAdder adder = network.newSubstation();
        NetworkElementCreationUtils.createIdentifiable(adder, dataframe, row);
        applyIfPresent(countries, row, country -> adder.setCountry(Country.valueOf(country)));
        applyIfPresent(tsos1, row, adder::setTso);
        applyIfPresent(tsos2, row, adder::setTso);
        adder.add();
    }
}
