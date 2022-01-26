/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.CreateEquipmentHelper;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;

import java.util.Collections;
import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class SvcDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.strings("voltage_level_id"),
            SeriesMetadata.strings("bus_id"),
            SeriesMetadata.strings("connectable_bus_id"),
            SeriesMetadata.ints("node"),
            SeriesMetadata.strings("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.doubles("b_max"),
            SeriesMetadata.doubles("b_min"),
            SeriesMetadata.strings("regulation_mode"),
            SeriesMetadata.doubles("target_v"),
            SeriesMetadata.doubles("target_q")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElement(Network network, UpdatingDataframe dataframe, int indexElement) {
        StaticVarCompensatorAdder adder = network.getVoltageLevel(dataframe.getStringValue("voltage_level_id", indexElement)
                        .orElseThrow(() -> new PowsyblException("voltage_level_id is missing")))
                .newStaticVarCompensator();
        CreateEquipmentHelper.createInjection(adder, dataframe, indexElement);
        dataframe.getDoubleValue("b_max", indexElement).ifPresent(adder::setBmax);
        dataframe.getDoubleValue("b_min", indexElement).ifPresent(adder::setBmin);
        dataframe.getStringValue("regulation_mode", indexElement)
                .ifPresent(rm -> adder.setRegulationMode(StaticVarCompensator.RegulationMode.valueOf(rm)));
        dataframe.getDoubleValue("target_v", indexElement).ifPresent(adder::setVoltageSetpoint);
        dataframe.getDoubleValue("target_q", indexElement).ifPresent(adder::setReactivePowerSetpoint);
        adder.add();
    }
}
