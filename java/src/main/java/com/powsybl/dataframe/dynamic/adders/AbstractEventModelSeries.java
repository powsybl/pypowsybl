/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynamicsimulation.EventModel;
import com.powsybl.dynawaltz.builders.ModelBuilder;
import com.powsybl.iidm.network.Network;

import java.util.function.BiFunction;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
abstract class AbstractEventModelSeries<T extends ModelBuilder<EventModel>> implements EventModelSeries {

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
