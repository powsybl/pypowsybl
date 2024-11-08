/**
 * Copyright (c) 2024, Artelys (http://www.artelys.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;

import java.util.Collections;
import java.util.List;

/**
 * @author Damien Jeandemange {@literal <damien.jeandemange at rte-france.com>}
 */
public class InternalConnectionDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("voltage_level_id"),
            SeriesMetadata.ints("node1"),
            SeriesMetadata.ints("node2")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class InternalConnectionSeries {
        private final StringSeries voltageLevelIds;
        private final IntSeries nodes1;
        private final IntSeries nodes2;

        InternalConnectionSeries(UpdatingDataframe dataframe) {
            this.voltageLevelIds = SeriesUtils.getRequiredStrings(dataframe, "voltage_level_id");
            this.nodes1 = SeriesUtils.getRequiredInts(dataframe, "node1");
            this.nodes2 = SeriesUtils.getRequiredInts(dataframe, "node2");
        }

        void create(Network network, int row) {
            VoltageLevel.NodeBreakerView view = NetworkUtils.getVoltageLevelNodeBreakerViewOrThrow(network, voltageLevelIds.get(row));
            view.newInternalConnection().setNode1(nodes1.get(row)).setNode2(nodes2.get(row)).add();
        }

        void delete(Network network, int row) {
            VoltageLevel.NodeBreakerView view = NetworkUtils.getVoltageLevelNodeBreakerViewOrThrow(network, voltageLevelIds.get(row));
            view.removeInternalConnections(nodes1.get(row), nodes2.get(row));
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        InternalConnectionSeries series = new InternalConnectionSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }

    public static void deleteElements(Network network, UpdatingDataframe dataframe) {
        InternalConnectionSeries series = new InternalConnectionSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.delete(network, row);
        }
    }
}
