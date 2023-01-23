/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.reporter.Reporter;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.DanglingLineAdder;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;

import static com.powsybl.dataframe.network.adders.NetworkUtils.getVoltageLevelOrThrow;
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
            SeriesMetadata.doubles("b")
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

        DanglingLineSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.voltageLevels = dataframe.getStrings("voltage_level_id");
            if (voltageLevels == null) {
                throw new PowsyblException("voltage_level_id is missing");
            }
            this.p0 = dataframe.getDoubles("p0");
            this.q0 = dataframe.getDoubles("q0");
            this.r = dataframe.getDoubles("r");
            this.x = dataframe.getDoubles("x");
            this.g = dataframe.getDoubles("g");
            this.b = dataframe.getDoubles("b");
        }

        DanglingLineAdder createAdder(Network network, int row) {
            DanglingLineAdder adder = getVoltageLevelOrThrow(network, voltageLevels.get(row))
                    .newDanglingLine();
            setInjectionAttributes(adder, row);
            applyIfPresent(p0, row, adder::setP0);
            applyIfPresent(q0, row, adder::setQ0);
            applyIfPresent(r, row, adder::setR);
            applyIfPresent(x, row, adder::setX);
            applyIfPresent(g, row, adder::setG);
            applyIfPresent(b, row, adder::setB);
            return adder;
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe, AdditionStrategy additionStrategy, boolean throwException, Reporter reporter) {
        DanglingLineSeries series = new DanglingLineSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            DanglingLineAdder adder = series.createAdder(network, row);
            additionStrategy.add(network, dataframe, adder, row, throwException, reporter);
        }
    }
}
