/**
 * Copyright (c) 2025, SuperGrid Institute (https://www.supergrid-institute.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;

/**
 * @author Denis BONNAND {@literal <denis.bonnand at supergrid-institute.com>}
 */
public class DcLineDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("dc_node1_id"),
            SeriesMetadata.strings("dc_node2_id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.doubles("r")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        DcLineSeries series = new DcLineSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row).add();
        }
    }
}
