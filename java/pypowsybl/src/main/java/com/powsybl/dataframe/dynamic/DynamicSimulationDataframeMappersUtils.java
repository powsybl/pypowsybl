/**
 * Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic;

import com.powsybl.dataframe.*;
import com.powsybl.dynamicsimulation.TimelineEvent;

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
}
