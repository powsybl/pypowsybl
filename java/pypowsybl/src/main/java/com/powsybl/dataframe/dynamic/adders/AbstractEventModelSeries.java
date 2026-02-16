/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.dynamic.PersistentDoubleSeries;
import com.powsybl.dataframe.dynamic.PersistentStringSeries;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynamicsimulation.EventModel;
import com.powsybl.dynawo.builders.ModelBuilder;
import com.powsybl.iidm.network.Network;

import java.util.function.BiFunction;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.START_TIME;
import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.STATIC_ID;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
abstract class AbstractEventModelSeries<T extends ModelBuilder<EventModel>> implements EventModelSeries {

    protected final StringSeries staticIds;
    protected final DoubleSeries startTimes;

    protected AbstractEventModelSeries(UpdatingDataframe dataframe) {
        this.staticIds = PersistentStringSeries.copyOf(dataframe, STATIC_ID);
        this.startTimes = PersistentDoubleSeries.copyOf(dataframe, START_TIME);
    }

    @Override
    public BiFunction<Network, ReportNode, EventModel> getModelSupplier(int row) {
        return (network, reportNode) -> {
            T builder = createBuilder(network, reportNode);
            if (builder == null) {
                return null;
            }
            applyOnBuilder(row, builder);
            return builder.build();
        };
    }

    protected abstract void applyOnBuilder(int row, T builder);

    protected abstract T createBuilder(Network network, ReportNode reportNode);
}
