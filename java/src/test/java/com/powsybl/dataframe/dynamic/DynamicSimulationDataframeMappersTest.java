/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic;

import com.powsybl.dataframe.impl.Series;
import com.powsybl.dynamicsimulation.TimelineEvent;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.powsybl.dataframe.dynamic.DynamicSimulationDataframeMappersUtils.fsvDataFrameMapper;
import static com.powsybl.dataframe.dynamic.DynamicSimulationDataframeMappersUtils.timelineEventDataFrameMapper;
import static com.powsybl.python.network.Dataframes.createSeries;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class DynamicSimulationDataframeMappersTest {

    @Test
    void testFsvDataframesMapper() {
        Map<String, Double> fsv = new LinkedHashMap<>();
        fsv.put("GEN_Upu_value", 45.8);
        fsv.put("LOAD_load_PPu", 22.1);
        List<Series> series = createSeries(fsvDataFrameMapper(), fsv);
        assertThat(series)
                .extracting(Series::getName)
                .containsExactly("variables", "values");
        assertThat(series).satisfiesExactly(
                col1 -> assertThat(col1.getStrings()).containsExactly("GEN_Upu_value", "LOAD_load_PPu"),
                col2 -> assertThat(col2.getDoubles()).containsExactly(45.8, 22.1));
    }

    @Test
    void testTimelineDataframesMapper() {
        List<TimelineEvent> timelineEvents = List.of(
                new TimelineEvent(0.0, "BBM_GEN6", "PMIN : activation"),
                new TimelineEvent(0.0, "BBM_GEN8", "PMIN : activation"),
                new TimelineEvent(2.2, "BBM_GEN6", "PMIN : deactivation"));
        List<Series> series = createSeries(timelineEventDataFrameMapper(), timelineEvents);
        assertThat(series)
                .extracting(Series::getName)
                .containsExactly("index", "time", "model", "message");
        assertThat(series).satisfiesExactly(
                index -> assertThat(index.getInts()).containsExactly(0, 1, 2),
                col1 -> assertThat(col1.getDoubles()).containsExactly(0.0, 0.0, 2.2),
                col2 -> assertThat(col2.getStrings()).containsExactly("BBM_GEN6", "BBM_GEN8", "BBM_GEN6"),
                col3 -> assertThat(col3.getStrings()).containsExactly("PMIN : activation", "PMIN : activation", "PMIN : deactivation"));
    }
}
