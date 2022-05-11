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
import com.powsybl.entsoe.util.MergedXnodeAdder;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
public class MergedXnodeDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.stringIndex("code"),
            SeriesMetadata.stringIndex("line1"),
            SeriesMetadata.stringIndex("line2"),
            SeriesMetadata.doubles("r_dp"),
            SeriesMetadata.doubles("x_dp"),
            SeriesMetadata.doubles("g1_dp"),
            SeriesMetadata.doubles("b1_dp"),
            SeriesMetadata.doubles("g2_dp"),
            SeriesMetadata.doubles("b2_dp"),
            SeriesMetadata.doubles("p1"),
            SeriesMetadata.doubles("q1"),
            SeriesMetadata.doubles("p2"),
            SeriesMetadata.doubles("q2")
            );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class MergedXnodeSeries {

        private final StringSeries id;
        private final StringSeries code;
        private final StringSeries line1;
        private final StringSeries line2;
        private final DoubleSeries rDp;
        private final DoubleSeries xDp;
        private final DoubleSeries g1Dp;
        private final DoubleSeries b1Dp;
        private final DoubleSeries g2Dp;
        private final DoubleSeries b2Dp;
        private final DoubleSeries p1;
        private final DoubleSeries q1;
        private final DoubleSeries p2;
        private final DoubleSeries q2;

        MergedXnodeSeries(UpdatingDataframe dataframe) {
            this.id = dataframe.getStrings("id");
            this.code = dataframe.getStrings("code");
            this.line1 = dataframe.getStrings("line1");
            this.line2 = dataframe.getStrings("line2");
            this.rDp = dataframe.getDoubles("r_dp");
            this.xDp = dataframe.getDoubles("x_dp");
            this.g1Dp = dataframe.getDoubles("g1_dp");
            this.b1Dp = dataframe.getDoubles("b1_dp");
            this.g2Dp = dataframe.getDoubles("g2_dp");
            this.b2Dp = dataframe.getDoubles("b2_dp");
            this.p1 = dataframe.getDoubles("p1");
            this.q1 = dataframe.getDoubles("q1");
            this.p2 = dataframe.getDoubles("p2");
            this.q2 = dataframe.getDoubles("q2");
        }

        void create(Network network, int row) {
            String id = this.id.get(row);
            Line l = network.getLine(id);
            if (l == null) {
                throw new PowsyblException("Invalid line id : could not find " + id);
            }
            var adder = l.newExtension(MergedXnodeAdder.class);
            applyIfPresent(code, row, adder::withCode);
            applyIfPresent(line1, row, adder::withLine1Name);
            applyIfPresent(line2, row, adder::withLine2Name);
            applyIfPresent(rDp, row, adder::withRdp);
            applyIfPresent(xDp, row, adder::withXdp);
            applyIfPresent(g1Dp, row, adder::withG1dp);
            applyIfPresent(b1Dp, row, adder::withB1dp);
            applyIfPresent(g2Dp, row, adder::withG2dp);
            applyIfPresent(b2Dp, row, adder::withB2dp);
            applyIfPresent(p1, row, adder::withXnodeP1);
            applyIfPresent(q1, row, adder::withXnodeQ1);
            applyIfPresent(p2, row, adder::withXnodeP2);
            applyIfPresent(q2, row, adder::withXnodeQ2);
            adder.add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        MergedXnodeSeries series = new MergedXnodeSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
