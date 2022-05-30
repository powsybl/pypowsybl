/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.LineAdder;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class LineDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("voltage_level1_id"),
            SeriesMetadata.strings("bus1_id"),
            SeriesMetadata.strings("connectable_bus1_id"),
            SeriesMetadata.ints("node1"),
            SeriesMetadata.strings("voltage_level2_id"),
            SeriesMetadata.strings("bus2_id"),
            SeriesMetadata.strings("connectable_bus2_id"),
            SeriesMetadata.ints("node2"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.doubles("b1"),
            SeriesMetadata.doubles("b2"),
            SeriesMetadata.doubles("g1"),
            SeriesMetadata.doubles("g2"),
            SeriesMetadata.doubles("r"),
            SeriesMetadata.doubles("x")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class LineSeries extends BranchSeries {

        private final DoubleSeries b1;
        private final DoubleSeries b2;
        private final DoubleSeries g1;
        private final DoubleSeries g2;
        private final DoubleSeries r;
        private final DoubleSeries x;

        LineSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.b1 = dataframe.getDoubles("b1");
            this.b2 = dataframe.getDoubles("b2");
            this.g1 = dataframe.getDoubles("g1");
            this.g2 = dataframe.getDoubles("g2");
            this.r = dataframe.getDoubles("r");
            this.x = dataframe.getDoubles("x");
        }

        void create(Network network, int row) {
            LineAdder adder = network.newLine();
            setBranchAttributes(adder, row);
            applyIfPresent(b1, row, adder::setB1);
            applyIfPresent(b2, row, adder::setB2);
            applyIfPresent(g1, row, adder::setG1);
            applyIfPresent(g2, row, adder::setG2);
            applyIfPresent(r, row, adder::setR);
            applyIfPresent(x, row, adder::setX);
            adder.add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        LineSeries series = new LineSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
