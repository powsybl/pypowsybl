/**
 * Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic;

import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.dataframe.dynamic.adders.DynamicMappingAdder;
import com.powsybl.dataframe.dynamic.adders.EventMappingAdder;
import com.powsybl.dynamicsimulation.TimelineEvent;
import com.powsybl.dynawo.builders.ModelInfo;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author Nicolas Pierre {@literal <nicolas.pierre@artelys.com>}
 */
public final class DynamicSimulationDataframeMappersUtils {

    private DynamicSimulationDataframeMappersUtils() {
    }

    public static DataframeMapper<Map<String, Double>, Void> fsvDataFrameMapper() {
        return new DataframeMapperBuilder<Map<String, Double>, Map.Entry<String, Double>, Void>()
                .itemsStreamProvider(m -> m.entrySet().stream())
                .stringsIndex("variables", Map.Entry::getKey)
                .doubles("values", Map.Entry::getValue)
                .build();
    }

    public static DataframeMapper<List<TimelineEvent>, Void> timelineEventDataFrameMapper() {
        AtomicInteger index = new AtomicInteger();
        return new DataframeMapperBuilder<List<TimelineEvent>, TimelineEvent, Void>()
                .itemsProvider(Function.identity())
                .intsIndex("index", e -> index.getAndIncrement())
                .doubles("time", TimelineEvent::time)
                .strings("model", TimelineEvent::modelName)
                .strings("message", TimelineEvent::message)
                .build();
    }

    public static DataframeMapper<Collection<DynamicMappingAdder>, Void> categoriesDataFrameMapper() {
        return new DataframeMapperBuilder<Collection<DynamicMappingAdder>, CategoryInformation, Void>()
                .itemsStreamProvider(a -> a.stream()
                        .map(DynamicMappingAdder::getCategoryInformation)
                        .sorted(Comparator.comparing(CategoryInformation::name)))
                .stringsIndex("name", CategoryInformation::name)
                .strings("description", CategoryInformation::description)
                .strings("attribute", CategoryInformation::attribute)
                .build();
    }

    public static DataframeMapper<Collection<EventMappingAdder>, Void> eventInformationDataFrameMapper() {
        return new DataframeMapperBuilder<Collection<EventMappingAdder>, ModelInfo, Void>()
                .itemsStreamProvider(a -> a.stream()
                        .map(EventMappingAdder::getEventInformation))
                .stringsIndex("name", ModelInfo::name)
                .strings("description", ModelInfo::doc)
                //TODO return attribute
                .build();
    }
}
