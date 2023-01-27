/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.extensions;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.AbstractSimpleAdder;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.SecondaryVoltageControlAdder;

import java.util.List;

/**
 * @author Hugo Kulesza <hugo.kulesza@rte-france.com>
 */
public class SecondaryVoltageControlDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> ZONES_METADATA = List.of(
            SeriesMetadata.stringIndex("name"),
            SeriesMetadata.doubles("target_v"),
            SeriesMetadata.strings("bus_id")
        );

    private static final List<SeriesMetadata> UNITS_METADATA = List.of(
            SeriesMetadata.stringIndex("unit_id"),
            SeriesMetadata.strings("zone_name"),
            SeriesMetadata.booleans("participate")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return List.of(ZONES_METADATA, UNITS_METADATA);
    }

    private static class SecondaryVoltageControlSeries {

        private final StringSeries zoneName;
        private final DoubleSeries targetV;
        private final StringSeries busId;
        private final StringSeries unitId;
        private final IntSeries participate;
        private final StringSeries unitZoneName;

        SecondaryVoltageControlSeries(UpdatingDataframe zonesDf, UpdatingDataframe unitsDf) {
            this.zoneName = zonesDf.getStrings("name");
            this.targetV = zonesDf.getDoubles("target_v");
            this.busId = zonesDf.getStrings("bus_id");

            this.unitId = unitsDf.getStrings("unit_id");
            this.participate = unitsDf.getInts("participate");
            this.unitZoneName = unitsDf.getStrings("zone_name");
        }

        void create(Network network) {
            var adder = network.newExtension(SecondaryVoltageControlAdder.class);
        }
    }

    @Override
    public void addElements(Network network, List<UpdatingDataframe> dataframes) {
        if (dataframes.size() != 2) {
            throw new PowsyblException("Two dataframes are expected to describe the secondary voltage control, found : " + dataframes.size());
        }
        UpdatingDataframe zonesDf = dataframes.get(0);
        UpdatingDataframe unitsDf = dataframes.get(1);
        SecondaryVoltageControlSeries series = new SecondaryVoltageControlSeries(zonesDf, unitsDf);
        series.create(network);
    }
}
