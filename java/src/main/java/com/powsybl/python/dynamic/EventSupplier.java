/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.reporter.Reporter;
import com.powsybl.dynamicsimulation.EventModel;
import com.powsybl.dynamicsimulation.EventModelsSupplier;
import com.powsybl.dynawaltz.models.events.EventDisconnectionBuilder;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 */
public class EventSupplier implements EventModelsSupplier {

    private final List<Function<Network, EventModel>> eventSupplierList = new ArrayList<>();

    /**
     * According to Dynawaltz staticId must refer to a line or a two winding
     * transformer
     * <p>
     * The event represent the disconnection the given line/transformer
     */
    public void addEventBranchDisconnection(String staticId, double eventTime, boolean disconnectOrigin, boolean disconnectExtremity) {
        //TODO handle disconnect side
        //TODO replace one event with one disconnection event
        eventSupplierList.add(network -> EventDisconnectionBuilder.of(network)
                .staticId(staticId)
                .startTime(eventTime)
                .build());
    }

    /**
     * According to Dynawaltz staticId must refer to a generator
     * <p>
     * The event represent the disconnection of the given generator, load, static var compensator or shunt compensator
     */
    public void addEventInjectionDisconnection(String staticId, double eventTime, boolean stateEvent) {
        eventSupplierList.add(network -> EventDisconnectionBuilder.of(network)
                .staticId(staticId)
                .startTime(eventTime)
                .build());
    }

    @Override
    public List<EventModel> get(Network network, Reporter reporter) {
        return get(network);
    }

    @Override
    public List<EventModel> get(Network network) {
        return eventSupplierList.stream().map(f -> f.apply(network)).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
