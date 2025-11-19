/**
 * Copyright (c) 2025, SuperGrid Institute (https://www.supergrid-institute.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.DcLineAdder;
import com.powsybl.iidm.network.Network;

import java.util.Optional;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Denis BONNAND {@literal <denis.bonnand at supergrid-institute.com>}
 */
public class DcLineSeries extends IdentifiableSeries {

    protected final StringSeries dcNodes1;
    protected final StringSeries dcNodes2;
    private final DoubleSeries r;

    DcLineSeries(UpdatingDataframe dataframe) {
        super(dataframe);
        this.dcNodes1 = dataframe.getStrings("dc_node1_id");
        this.dcNodes2 = dataframe.getStrings("dc_node2_id");
        this.r = dataframe.getDoubles("r");
    }

    protected void setDcLineAttributes(DcLineAdder adder, int row) {
        setIdentifiableAttributes(adder, row);
        applyIfPresent(dcNodes1, row, dcNode1 -> {
            if (!dcNode1.isEmpty()) {
                adder.setDcNode1(dcNode1);
                adder.setConnected1(true);
            }
        });
        applyIfPresent(dcNodes2, row, dcNode2 -> {
            if (!dcNode2.isEmpty()) {
                adder.setDcNode2(dcNode2);
                adder.setConnected2(true);
            }
        });
    }

    DcLineAdder create(Network network, int row) {
        return create(network, row, true).orElseThrow();
    }

    Optional<DcLineAdder> create(Network network, int row, boolean throwException) {
        DcLineAdder adder = network.newDcLine();
        setDcLineAttributes(adder, row);
        applyIfPresent(r, row, adder::setR);
        return Optional.of(adder);
    }
}
