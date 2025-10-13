/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.extensions;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.AbstractSimpleAdder;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Battery;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.VoltageRegulationAdder;

import java.util.Collections;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class VoltageRegulationDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("battery_id"),
            SeriesMetadata.booleans("voltage_regulator_on"),
            SeriesMetadata.doubles("target_v")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class VoltageRegulationSerie {
        private final StringSeries batteryId;
        private final IntSeries voltageRegulatorOn;
        private final DoubleSeries targetV;

        VoltageRegulationSerie(UpdatingDataframe dataframe) {
            this.batteryId = dataframe.getStrings("battery_id");
            this.voltageRegulatorOn = dataframe.getInts("voltage_regulator_on");
            this.targetV = dataframe.getDoubles("target_v");
        }

        void create(Network network, int row) {
            String id = this.batteryId.get(row);
            Battery battery = network.getBattery(id);
            if (battery == null) {
                throw new PowsyblException("Invalid battery id : could not find " + id);
            }
            VoltageRegulationAdder adder = battery.newExtension(VoltageRegulationAdder.class);
            SeriesUtils.applyIfPresent(voltageRegulatorOn, row, value -> adder.withVoltageRegulatorOn(value != 0));
            SeriesUtils.applyIfPresent(targetV, row, adder::withTargetV);
            adder.add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        VoltageRegulationSerie series = new VoltageRegulationSerie(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
