/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VscConverterStationAdder;

import java.util.Collections;
import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class VscDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.strings("voltage_level_id"),
            SeriesMetadata.strings("bus_id"),
            SeriesMetadata.strings("connectable_bus_id"),
            SeriesMetadata.ints("node"),
            SeriesMetadata.strings("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.doubles("target_v"),
            SeriesMetadata.doubles("target_q"),
            SeriesMetadata.doubles("loss_factor"),
            SeriesMetadata.booleans("voltage_regulator_on")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElement(Network network, UpdatingDataframe dataframe, int indexElement) {
        VscConverterStationAdder adder = network.getVoltageLevel(dataframe.getStringValue("voltage_level_id", indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing"))).newVscConverterStation();
        NetworkElementCreationUtils.createHvdc(adder, dataframe, indexElement);
        dataframe.getDoubleValue("target_v", indexElement).ifPresent(adder::setVoltageSetpoint);
        dataframe.getDoubleValue("target_q", indexElement).ifPresent(adder::setReactivePowerSetpoint);
        adder.setVoltageRegulatorOn(dataframe.getIntValue("voltage_regulator_on", indexElement).orElse(0) == 1);
        adder.add();
    }
}
