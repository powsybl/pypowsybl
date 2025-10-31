/**
 * Copyright (c) 2025, SuperGrid Institute (https://www.supergrid-institute.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.DcNodeAdder;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Denis BONNAND {@literal <denis.bonnand at supergrid-institute.com>}
 */
public class DcNodeDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.doubles("nominal_v")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        DcNodeSeries series = new DcNodeSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }

    private static class DcNodeSeries extends IdentifiableSeries {

        private final DoubleSeries nominalV;

        DcNodeSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.nominalV = dataframe.getDoubles("nominal_v");
        }

        void create(Network network, int row) {
            DcNodeAdder adder = network.newDcNode();
            setIdentifiableAttributes(adder, row);
            applyIfPresent(nominalV, row, adder::setNominalV);
            adder.add();
        }
    }
}
