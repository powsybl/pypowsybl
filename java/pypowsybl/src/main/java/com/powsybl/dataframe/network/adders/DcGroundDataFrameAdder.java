/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.DcGroundAdder;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Yichen TANG {@literal <yichen.tang at rte-france.com>}
 * @author Etienne Lesot {@literal <etienne.lesot at rte-france.com>}
 * @author Sylvain Leclerc {@literal <sylvain.leclerc@rte-france.com>}
 */
public class DcGroundDataFrameAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.strings("dc_node_id"),
            SeriesMetadata.doubles("r")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        DcGroundSeries series = new DcGroundSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }

    private static class DcGroundSeries extends IdentifiableSeries {

        private final DoubleSeries r;
        private final StringSeries dcNodes;

        DcGroundSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.dcNodes = dataframe.getStrings("dc_node_id");
            this.r = dataframe.getDoubles("r");
        }

        void create(Network network, int row) {
            DcGroundAdder adder = network.newDcGround();
            setIdentifiableAttributes(adder, row);
            applyIfPresent(dcNodes, row, adder::setDcNode);
            applyIfPresent(r, row, adder::setR);
            adder.add();
        }
    }
}
