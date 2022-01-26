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
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevelAdder;

import java.util.Collections;
import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class VoltageLevelDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.strings("substation_id"),
            SeriesMetadata.strings("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.doubles("high_voltage_limit"),
            SeriesMetadata.doubles("low_voltage_limit"),
            SeriesMetadata.doubles("nominal_v"),
            SeriesMetadata.strings("topology_kind")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElement(Network network, UpdatingDataframe dataframe, int indexElement) {
        VoltageLevelAdder adder = network.getSubstation(dataframe.getStringValue("substation_id", indexElement)
                .orElseThrow(() -> new PowsyblException("substation_id is missing"))).newVoltageLevel();
        CreateEquipmentHelper.createIdentifiable(adder, dataframe, indexElement);
        dataframe.getDoubleValue("high_voltage_limit", indexElement).ifPresent(adder::setHighVoltageLimit);
        dataframe.getDoubleValue("low_voltage_limit", indexElement).ifPresent(adder::setLowVoltageLimit);
        dataframe.getDoubleValue("nominal_v", indexElement).ifPresent(adder::setNominalV);
        dataframe.getStringValue("topology_kind", indexElement).ifPresent(adder::setTopologyKind);
        adder.add();
    }
}
