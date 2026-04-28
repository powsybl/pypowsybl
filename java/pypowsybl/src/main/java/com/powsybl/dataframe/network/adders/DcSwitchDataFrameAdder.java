/**
 * Copyright (c) 2026, SuperGrid Institute (https://www.supergrid-institute.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.dataframe.network.adders;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyBooleanIfPresent;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

import java.util.Collections;
import java.util.List;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.DcSwitchAdder;
import com.powsybl.iidm.network.DcSwitchKind;
import com.powsybl.iidm.network.Network;

/**
 * Converter for DC switches from Pandas dataframe to the PowSyBl data model.
 *
 * @author Landry Huet {@literal <landry.huet at supergrid-institute.com>}
 */
public class DcSwitchDataFrameAdder extends AbstractSimpleAdder {

    // List of columns in the data series
    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.strings("dc_node1_id"),
            SeriesMetadata.strings("dc_node2_id"),
            SeriesMetadata.strings("kind"),
            SeriesMetadata.booleans("open"),
            SeriesMetadata.doubles("r"),
            SeriesMetadata.booleans("fictitious")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    /**
     * Helper class to store the fields and to add a single row of the record to
     * a Pandas dataframe.
     */
    private static class DcSwitchSeries extends IdentifiableSeries {

        private final StringSeries dcNode1Ids;
        private final StringSeries dcNode2Ids;
        private final StringSeries kinds;
        private final IntSeries open;
        private final DoubleSeries r;
        private final IntSeries fictitious;

        DcSwitchSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.dcNode1Ids = dataframe.getStrings("dc_node1_id");
            this.dcNode2Ids = dataframe.getStrings("dc_node2_id");
            this.kinds = dataframe.getStrings("kind");
            this.open = dataframe.getInts("open");
            this.r = dataframe.getDoubles("r");
            this.fictitious = dataframe.getInts("fictitious");
        }

        /**
         * Add a DcSwitch to `network` with the data from the iRow-th row
         * in each series.
         * @param network Network to update.
         * @param iRow id of row to fetch in each series.
         */
        void create(Network network, int iRow) {
            DcSwitchAdder adder = network.newDcSwitch();
            setIdentifiableAttributes(adder, iRow); // id + name
            applyIfPresent(dcNode1Ids, iRow, adder::setDcNode1);
            applyIfPresent(dcNode2Ids, iRow, adder::setDcNode2);
            applyIfPresent(kinds, iRow, DcSwitchKind.class, adder::setKind);
            applyBooleanIfPresent(open, iRow, adder::setOpen);
            applyIfPresent(r, iRow, adder::setR);
            applyBooleanIfPresent(fictitious, iRow, adder::setFictitious);
            adder.add();
        }
    }

    /**
     * Add all DcSwitches from the dataframe of DC switches to the network.
     */
    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        DcSwitchSeries series = new DcSwitchSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }

}
