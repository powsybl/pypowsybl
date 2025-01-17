/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;

import java.util.List;
import java.util.Optional;

import static com.powsybl.dataframe.network.adders.NetworkUtils.getVoltageLevelOrThrowWithBusOrBusbarSectionId;

/**
 * @author Hugo Kulesza {@literal <hugo.kulesza@rte-france.com>}
 */
public class GroundDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("voltage_level_id"),
            SeriesMetadata.strings("bus_id"),
            SeriesMetadata.strings("connectable_bus_id"),
            SeriesMetadata.ints("node"),
            SeriesMetadata.strings("name")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return List.of(METADATA);
    }

    private static class GroundSeries extends InjectionSeries {

        private final StringSeries voltageLevels;
        private final StringSeries busOrBusbarSections;

        GroundSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.voltageLevels = dataframe.getStrings("voltage_level_id");
            this.busOrBusbarSections = dataframe.getStrings("bus_id");
        }

        Optional<GroundAdder> createAdder(Network network, int row, boolean throwException) {
            Optional<VoltageLevel> vl = getVoltageLevelOrThrowWithBusOrBusbarSectionId(network, row, voltageLevels, busOrBusbarSections, throwException);
            if (vl.isPresent()) {
                GroundAdder adder = vl.get().newGround();
                setInjectionAttributes(adder, row);
                return Optional.of(adder);
            }
            return Optional.empty();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe, AdditionStrategy addition, boolean throwException, ReportNode reportNode) {
        GroundSeries series = new GroundSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            Optional<GroundAdder> adder = series.createAdder(network, row, throwException);
            if (adder.isPresent()) {
                addition.add(network, dataframe, adder.get(), row, throwException, reportNode);
            }
        }
    }
}
