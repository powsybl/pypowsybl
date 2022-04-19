/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.BatteryAdder;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class BatteryDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("voltage_level_id"),
            SeriesMetadata.strings("bus_id"),
            SeriesMetadata.strings("connectable_bus_id"),
            SeriesMetadata.ints("node"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.doubles("max_p"),
            SeriesMetadata.doubles("min_p"),
            SeriesMetadata.doubles("p0"),
            SeriesMetadata.doubles("q0")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class BatterySeries extends InjectionSeries {

        private final StringSeries voltageLevels;
        private final DoubleSeries maxP;
        private final DoubleSeries minP;
        private final DoubleSeries p0;
        private final DoubleSeries q0;

        BatterySeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.voltageLevels = dataframe.getStrings("voltage_level_id");
            if (voltageLevels == null) {
                throw new PowsyblException("voltage_level_id is missing");
            }
            this.maxP = dataframe.getDoubles("max_p");
            this.minP = dataframe.getDoubles("min_p");
            this.p0 = dataframe.getDoubles("p0");
            this.q0 = dataframe.getDoubles("q0");
        }

        void createBattery(Network network, int row) {
            BatteryAdder batteryAdder = network.getVoltageLevel(voltageLevels.get(row))
                    .newBattery();
            setInjectionAttributes(batteryAdder, row);
            NetworkElementCreationUtils.applyIfPresent(maxP, row, batteryAdder::setMaxP);
            NetworkElementCreationUtils.applyIfPresent(minP, row, batteryAdder::setMinP);
            NetworkElementCreationUtils.applyIfPresent(p0, row, batteryAdder::setP0);
            NetworkElementCreationUtils.applyIfPresent(q0, row, batteryAdder::setQ0);
            batteryAdder.add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        BatterySeries series = new BatterySeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.createBattery(network, row);
        }
    }
}
