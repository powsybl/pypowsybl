/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynamicsimulation.EventModel;
import com.powsybl.dynamicsimulation.EventModelsSupplier;
import com.powsybl.dynawo.models.events.EventDisconnectionBuilder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class EventSupplier implements EventModelsSupplier {

    private final List<Function<Network, EventModel>> eventSupplierList = new ArrayList<>();

    /**
     * According to Dynawaltz staticId must refer to an injection, branch or hvdc line
     * <p>
     * The event represent the disconnection the given equipment
     */
    public void addEventDisconnection(String staticId, double eventTime, TwoSides disconnectOnly) {
        eventSupplierList.add(network -> {
            EventDisconnectionBuilder builder = EventDisconnectionBuilder.of(network)
                    .staticId(staticId)
                    .startTime(eventTime);
            if (disconnectOnly != null) {
                builder.disconnectOnly(disconnectOnly);
            }
            return builder.build();
        });
    }

    @Override
    public List<EventModel> get(Network network, ReportNode reportNode) {
        return get(network);
    }

    @Override
    public List<EventModel> get(Network network) {
        return eventSupplierList.stream().map(f -> f.apply(network)).filter(Objects::nonNull).toList();
    }
}
