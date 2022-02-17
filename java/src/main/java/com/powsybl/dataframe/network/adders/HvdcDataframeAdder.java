/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.HvdcLineAdder;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class HvdcDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.strings("converter_station1_id"),
            SeriesMetadata.strings("converter_station2_id"),
            SeriesMetadata.doubles("max_p"),
            SeriesMetadata.strings("converters_mode"),
            SeriesMetadata.doubles("target_p"),
            SeriesMetadata.doubles("r"),
            SeriesMetadata.doubles("nominal_v")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElement(Network network, UpdatingDataframe dataframe, int indexElement) {
        HvdcLineAdder adder = network.newHvdcLine();
        NetworkElementCreationUtils.createIdentifiable(adder, dataframe, indexElement);
        dataframe.getStringValue("converter_station1_id", indexElement).ifPresent(adder::setConverterStationId1);
        dataframe.getStringValue("converter_station2_id", indexElement).ifPresent(adder::setConverterStationId2);
        dataframe.getDoubleValue("max_p", indexElement).ifPresent(adder::setMaxP);
        dataframe.getStringValue("converters_mode", indexElement).map(HvdcLine.ConvertersMode::valueOf).ifPresent(adder::setConvertersMode);
        dataframe.getDoubleValue("target_p", indexElement).ifPresent(adder::setActivePowerSetpoint);
        dataframe.getDoubleValue("r", indexElement).ifPresent(adder::setR);
        dataframe.getDoubleValue("nominal_v", indexElement).ifPresent(adder::setNominalV);
        adder.add();
    }
}
