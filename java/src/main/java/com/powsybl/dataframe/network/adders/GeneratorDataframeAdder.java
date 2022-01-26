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
public class GeneratorDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.strings("voltage_level_id"),
            SeriesMetadata.strings("bus_id"),
            SeriesMetadata.strings("connectable_bus_id"),
            SeriesMetadata.ints("node"),
            SeriesMetadata.strings("id"),
            SeriesMetadata.strings("energy_source"),
            SeriesMetadata.doubles("max_p"),
            SeriesMetadata.doubles("min_p"),
            SeriesMetadata.doubles("target_p"),
            SeriesMetadata.doubles("target_q"),
            SeriesMetadata.doubles("rated_s"),
            SeriesMetadata.doubles("target_v"),
            SeriesMetadata.booleans("voltage_regulator_on")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElement(Network network, UpdatingDataframe dataframe, int indexElement) {
        GeneratorAdder generatorAdder = network.getVoltageLevel(dataframe.getStringValue("voltage_level_id", indexElement)
                        .orElseThrow(() -> new PowsyblException("voltage_level_id is missing")))
                .newGenerator();
        CreateEquipmentHelper.createInjection(generatorAdder, dataframe, indexElement);
        dataframe.getDoubleValue("max_p", indexElement).ifPresent(generatorAdder::setMaxP);
        dataframe.getDoubleValue("min_p", indexElement).ifPresent(generatorAdder::setMinP);
        dataframe.getDoubleValue("target_p", indexElement).ifPresent(generatorAdder::setTargetP);
        dataframe.getDoubleValue("target_q", indexElement).ifPresent(generatorAdder::setTargetQ);
        dataframe.getDoubleValue("target_v", indexElement).ifPresent(generatorAdder::setTargetV);
        dataframe.getDoubleValue("rated_s", indexElement).ifPresent(generatorAdder::setRatedS);
        generatorAdder.setVoltageRegulatorOn(dataframe.getIntValue("voltage_regulator_on", indexElement).orElse(0) == 1);
        dataframe.getStringValue("energy_source", indexElement)
                .ifPresent(v -> generatorAdder.setEnergySource(EnergySource.valueOf(v)));
        generatorAdder.add();
    }
}
