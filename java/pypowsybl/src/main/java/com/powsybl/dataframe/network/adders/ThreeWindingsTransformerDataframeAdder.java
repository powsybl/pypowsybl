/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
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
 * @author Coline Piloquet {@literal <coline.piloquet at rte-france.com>}
 */
public class ThreeWindingsTransformerDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
        SeriesMetadata.stringIndex("id"),
        SeriesMetadata.strings("name"),
        SeriesMetadata.doubles("rated_u0"),
        SeriesMetadata.doubles("r1"),
        SeriesMetadata.doubles("x1"),
        SeriesMetadata.doubles("g1"),
        SeriesMetadata.doubles("b1"),
        SeriesMetadata.doubles("rated_u1"),
        SeriesMetadata.doubles("rated_s1"),
        SeriesMetadata.strings("voltage_level1_id"),
        SeriesMetadata.ints("node1"),
        SeriesMetadata.strings("bus1_id"),
        SeriesMetadata.strings("connectable_bus1_id"),
        SeriesMetadata.doubles("r2"),
        SeriesMetadata.doubles("x2"),
        SeriesMetadata.doubles("g2"),
        SeriesMetadata.doubles("b2"),
        SeriesMetadata.doubles("rated_u2"),
        SeriesMetadata.doubles("rated_s2"),
        SeriesMetadata.strings("voltage_level2_id"),
        SeriesMetadata.ints("node2"),
        SeriesMetadata.strings("bus2_id"),
        SeriesMetadata.strings("connectable_bus2_id"),
        SeriesMetadata.doubles("r3"),
        SeriesMetadata.doubles("x3"),
        SeriesMetadata.doubles("g3"),
        SeriesMetadata.doubles("b3"),
        SeriesMetadata.doubles("rated_u3"),
        SeriesMetadata.doubles("rated_s3"),
        SeriesMetadata.strings("voltage_level3_id"),
        SeriesMetadata.ints("node3"),
        SeriesMetadata.strings("bus3_id"),
        SeriesMetadata.strings("connectable_bus3_id")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        ThreeWindingsTransformerSeries series = new ThreeWindingsTransformerSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row).add();
        }
    }

}
