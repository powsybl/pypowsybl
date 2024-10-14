/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.DanglingLineAdder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.powsybl.dataframe.network.adders.NetworkUtils.getVoltageLevelOrThrowWithBusOrBusbarSectionId;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyBooleanIfPresent;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class DanglingLineDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
        SeriesMetadata.stringIndex("id"),
        SeriesMetadata.strings("voltage_level_id"),
        SeriesMetadata.strings("bus_id"),
        SeriesMetadata.strings("connectable_bus_id"),
        SeriesMetadata.ints("node"),
        SeriesMetadata.strings("name"),
        SeriesMetadata.doubles("p0"),
        SeriesMetadata.doubles("q0"),
        SeriesMetadata.doubles("r"),
        SeriesMetadata.doubles("x"),
        SeriesMetadata.doubles("g"),
        SeriesMetadata.doubles("b"),
        SeriesMetadata.strings("pairing_key"),
        SeriesMetadata.doubles("min_p"),
        SeriesMetadata.doubles("max_p"),
        SeriesMetadata.doubles("target_p"),
        SeriesMetadata.doubles("target_q"),
        SeriesMetadata.doubles("target_v"),
        SeriesMetadata.booleans("voltage_regulator_on")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class DanglingLineSeries extends InjectionSeries {

        private final StringSeries voltageLevels;
        private final DoubleSeries p0;
        private final DoubleSeries q0;
        private final DoubleSeries r;
        private final DoubleSeries x;
        private final DoubleSeries g;
        private final DoubleSeries b;
        private final StringSeries busOrBusbarSections;
        private final StringSeries pairingKey;
        private final DoubleSeries minP;
        private final DoubleSeries maxP;
        private final DoubleSeries targetP;
        private final DoubleSeries targetQ;
        private final DoubleSeries targetV;
        private final IntSeries voltageRegulatorOn;

        DanglingLineSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.voltageLevels = dataframe.getStrings("voltage_level_id");
            this.p0 = dataframe.getDoubles("p0");
            this.q0 = dataframe.getDoubles("q0");
            this.r = dataframe.getDoubles("r");
            this.x = dataframe.getDoubles("x");
            this.g = dataframe.getDoubles("g");
            this.b = dataframe.getDoubles("b");
            this.busOrBusbarSections = dataframe.getStrings("bus_or_busbar_section_id");
            this.pairingKey = dataframe.getStrings("pairing_key");
            this.minP = dataframe.getDoubles("min_p");
            this.maxP = dataframe.getDoubles("max_p");
            this.targetP = dataframe.getDoubles("target_p");
            this.targetQ = dataframe.getDoubles("target_q");
            this.targetV = dataframe.getDoubles("target_v");
            this.voltageRegulatorOn = dataframe.getInts("voltage_regulator_on");
        }

        Optional<DanglingLineAdder> createAdder(Network network, int row, boolean throwException) {
            Optional<VoltageLevel> vl = getVoltageLevelOrThrowWithBusOrBusbarSectionId(network, row, voltageLevels,
                busOrBusbarSections, throwException);
            if (vl.isPresent()) {
                DanglingLineAdder adder = vl.get().newDanglingLine();
                setInjectionAttributes(adder, row);
                applyIfPresent(p0, row, adder::setP0);
                applyIfPresent(q0, row, adder::setQ0);
                applyIfPresent(r, row, adder::setR);
                applyIfPresent(x, row, adder::setX);
                applyIfPresent(g, row, adder::setG);
                applyIfPresent(b, row, adder::setB);
                applyIfPresent(pairingKey, row, adder::setPairingKey);
                return Optional.of(adder);
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe, AdditionStrategy additionStrategy, boolean throwException, ReportNode reportNode) {
        DanglingLineSeries series = new DanglingLineSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            Optional<DanglingLineAdder> adder = series.createAdder(network, row, throwException);
            if (adder.isPresent()) {
                additionStrategy.add(network, dataframe, adder.get(), row, throwException, reportNode);
            }
        }
    }
}
