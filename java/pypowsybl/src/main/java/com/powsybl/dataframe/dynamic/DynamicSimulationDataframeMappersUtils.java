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
import com.powsybl.dynamicsimulation.TimelineEvent;
import com.powsybl.dynawo.builders.ModelInfo;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author Nicolas Pierre {@literal <nicolas.pierre@artelys.com>}
 */
public final class DynamicSimulationDataframeMappersUtils {

    private static final String DESCRIPTION_NAME = "description";

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
                        .map(DynamicMappingAdder::getCategoryInformation))
                .stringsIndex("name", CategoryInformation::name)
                .strings(DESCRIPTION_NAME, CategoryInformation::description)
                .strings("attribute", CategoryInformation::attribute)
                .build();
    }

    public static DataframeMapper<Collection<ModelInfo>, Void> supportedModelsDataFrameMapper() {
        return new DataframeMapperBuilder<Collection<ModelInfo>, ModelInfo, Void>()
                .itemsStreamProvider(Collection::stream)
                .stringsIndex("name", ModelInfo::name)
                .strings(DESCRIPTION_NAME, mi -> mi.doc() != null ? mi.doc() : "")
                .build();
    }

    public static DataframeMapper<Collection<DynamicMappingAdder>, Void> allSupportedModelsDataFrameMapper() {
        return new DataframeMapperBuilder<Collection<DynamicMappingAdder>, Pair<String, ModelInfo>, Void>()
                .itemsStreamProvider(a -> a.stream()
                        .flatMap(adder -> {
                            String cat = adder.getCategory();
                            return adder.getSupportedModels().stream().map(m -> Pair.of(cat, m));
                        }))
                .stringsIndex("name", p -> p.getValue().name())
                .strings(DESCRIPTION_NAME, p -> {
                    String doc = p.getValue().doc();
                    return doc != null ? doc : "";
                })
                .strings("category", Pair::getKey)
                .build();
    }
}
