/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynamicsimulation.EventModel;
import com.powsybl.dynamicsimulation.EventModelsSupplier;
import com.powsybl.dynawaltz.models.events.EventInjectionDisconnection;
import com.powsybl.dynawaltz.models.events.EventQuadripoleDisconnection;
import com.powsybl.iidm.network.*;

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
        eventSupplierList.add(network -> {
            Branch<?> branch = network.getBranch(staticId);
            if (branch == null) {
                throw new PowsyblException("Branch '" + staticId + "' not found");
            }
            return new EventQuadripoleDisconnection(branch, eventTime, disconnectOrigin, disconnectExtremity);
        });
    }

    /**
     * According to Dynawaltz staticId must refer to a generator
     * <p>
     * The event represent the disconnection of the given generator, load, static var compensator or shunt compensator
     */
    public void addEventInjectionDisconnection(String staticId, double eventTime, boolean stateEvent) {
        eventSupplierList.add(network -> {
            Generator generator = network.getGenerator(staticId);
            if (generator != null) {
                return new EventInjectionDisconnection(generator, eventTime, stateEvent);
            } else {
                Load load = network.getLoad(staticId);
                if (load != null) {
                    return new EventInjectionDisconnection(load, eventTime, stateEvent);
                } else {
                    StaticVarCompensator svc = network.getStaticVarCompensator(staticId);
                    if (svc != null) {
                        return new EventInjectionDisconnection(svc, eventTime, stateEvent);
                    } else {
                        ShuntCompensator sc = network.getShuntCompensator(staticId);
                        if (sc != null) {
                            return new EventInjectionDisconnection(sc, eventTime, stateEvent);
                        } else {
                            throw new PowsyblException("Generator, load, static var compensator or shunt compensator '" + staticId + "' not found");
                        }
                    }
                }
            }
        });
    }

    @Override
    public List<EventModel> get(Network network) {
        return eventSupplierList.stream().map(f -> f.apply(network)).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
