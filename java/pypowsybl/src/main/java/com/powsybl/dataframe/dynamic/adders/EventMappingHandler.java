/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.python.dynamic.PythonEventModelsSupplier;

import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public final class EventMappingHandler {

    private static final SortedMap<String, EventMappingAdder> ADDERS = loadModelAdders();

    private static SortedMap<String, EventMappingAdder> loadModelAdders() {
        return ServiceLoader.load(EventModelAdderLoader.class).stream()
                .flatMap(ma -> ma.get().getEventModelAdders().stream())
                .collect(Collectors.toMap(a -> a.getEventInformation().name(),
                        Function.identity(),
                        (existing, replacement) -> existing,
                        TreeMap::new));
    }

    public static void addElements(String name, PythonEventModelsSupplier modelMapping, UpdatingDataframe dataframe) {
        EventMappingAdder adder = ADDERS.get(name);
        if (adder == null) {
            throw new PowsyblException("No event named " + name);
        }
        adder.addElements(modelMapping, dataframe);
    }

    public static List<SeriesMetadata> getMetadata(String name) {
        EventMappingAdder adder = ADDERS.get(name);
        if (adder == null) {
            throw new PowsyblException("No event named " + name);
        }
        return adder.getMetadata();
    }

    public static Collection<EventMappingAdder> getEventMappingAdders() {
        return ADDERS.values();
    }

    private EventMappingHandler() {
    }
}
