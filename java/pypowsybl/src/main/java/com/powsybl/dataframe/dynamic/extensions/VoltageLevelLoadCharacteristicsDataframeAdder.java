/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.extensions;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.AbstractSimpleAdder;
import com.powsybl.dataframe.network.adders.NetworkUtils;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawo.extensions.api.voltage.VoltageLevelLoadCharacteristics;
import com.powsybl.dynawo.extensions.api.voltage.VoltageLevelLoadCharacteristicsAdder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;

import java.util.Collections;
import java.util.List;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class VoltageLevelLoadCharacteristicsDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("characteristic")
            );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class VoltageLevelLoadCharacteristicsSeries {

        private final StringSeries id;
        private final StringSeries characteristic;

        VoltageLevelLoadCharacteristicsSeries(UpdatingDataframe dataframe) {
            this.id = dataframe.getStrings("id");
            // the characteristic has no default value, the extension cannot be created without it
            this.characteristic = SeriesUtils.getRequiredStrings(dataframe, "characteristic");
        }

        void create(Network network, int row) {
            VoltageLevel voltageLevel = NetworkUtils.getVoltageLevelOrThrow(network, this.id.get(row));
            var adder = voltageLevel.newExtension(VoltageLevelLoadCharacteristicsAdder.class);
            SeriesUtils.applyIfPresent(characteristic, row, VoltageLevelLoadCharacteristics.Type.class,
                    adder::withCharacteristic);
            adder.add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        VoltageLevelLoadCharacteristicsSeries series = new VoltageLevelLoadCharacteristicsSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
