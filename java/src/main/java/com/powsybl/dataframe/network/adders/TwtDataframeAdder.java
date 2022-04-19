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
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;

import java.util.Collections;
import java.util.List;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class TwtDataframeAdder extends AbstractSimpleAdder {

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
            SeriesMetadata.doubles("rated_u1"),
            SeriesMetadata.doubles("rated_u2"),
            SeriesMetadata.doubles("rated_s"),
            SeriesMetadata.doubles("b"),
            SeriesMetadata.doubles("g"),
            SeriesMetadata.doubles("r"),
            SeriesMetadata.doubles("x")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class TwoWindingsTransformerSeries extends BranchSeries {

        private final DoubleSeries ratedU1;
        private final DoubleSeries ratedU2;
        private final DoubleSeries ratedS;
        private final DoubleSeries b;
        private final DoubleSeries g;
        private final DoubleSeries r;
        private final DoubleSeries x;

        TwoWindingsTransformerSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.ratedU1 = dataframe.getDoubles("rated_u1");
            this.ratedU2 = dataframe.getDoubles("rated_u2");
            this.ratedS = dataframe.getDoubles("rated_s");
            this.b = dataframe.getDoubles("b");
            this.g = dataframe.getDoubles("g");
            this.r = dataframe.getDoubles("r");
            this.x = dataframe.getDoubles("x");
        }

        void create(Network network, int row) {
            String id = ids.get(row);
            String vlId1 = voltageLevels1.get(row);
            String vlId2 = voltageLevels2.get(row);
            VoltageLevel vl1 = network.getVoltageLevel(vlId1);
            if (vl1 == null) {
                throw new PowsyblException("Invalid voltage_level1_id : coud not find " + vlId1);
            }
            VoltageLevel vl2 = network.getVoltageLevel(vlId2);
            if (vl2 == null) {
                throw new PowsyblException("Invalid voltage_level1_id : coud not find " + vlId2);
            }
            Substation s1 = vl1.getSubstation().orElseThrow(() -> new PowsyblException("Could not create transformer " + id + ": no substation."));
            Substation s2 = vl2.getSubstation().orElseThrow(() -> new PowsyblException("Could not create transformer " + id + ": no substation."));
            if (s1 != s2) {
                throw new PowsyblException("Could not create transformer " + id + ": both voltage ids must be on the same substation");
            }
            var adder = s1.newTwoWindingsTransformer();
            setBranchAttributes(adder, row);
            applyIfPresent(ratedU1, row, adder::setRatedU1);
            applyIfPresent(ratedU2, row, adder::setRatedU2);
            applyIfPresent(ratedS, row, adder::setRatedS);
            applyIfPresent(b, row, adder::setB);
            applyIfPresent(g, row, adder::setG);
            applyIfPresent(r, row, adder::setR);
            applyIfPresent(x, row, adder::setX);
            adder.add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        TwoWindingsTransformerSeries series = new TwoWindingsTransformerSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
