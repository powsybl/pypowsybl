/**
 * Copyright (c) 2024, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.extensions;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.AbstractSimpleAdder;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.ReferencePriority;

import java.util.Collections;
import java.util.List;

/**
 * @author Damien Jeandemange <damien.jeandemange@artelys.com>
 */
public class ReferencePrioritiesDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.ints("priority")
            );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class ReferencePrioritySeries {

        private final StringSeries id;
        private final IntSeries priority;

        ReferencePrioritySeries(UpdatingDataframe dataframe) {
            this.id = dataframe.getStrings("id");
            this.priority = SeriesUtils.getRequiredInts(dataframe, "priority");
        }

        void create(Network network, int row) {
            String id = this.id.get(row);
            Injection<?> injection = ReferencePrioritiesDataframeProvider.getAndCheckInjection(network, id);
            SeriesUtils.applyIfPresent(this.priority, row, priority -> ReferencePriority.set(injection, priority));
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        ReferencePrioritySeries series = new ReferencePrioritySeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
