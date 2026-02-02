/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic;

import com.powsybl.dataframe.dynamic.adders.*;
import com.powsybl.dataframe.impl.Series;
import com.powsybl.dynamicsimulation.TimelineEvent;
import com.powsybl.dynawo.builders.ModelInfo;
import com.powsybl.timeseries.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.powsybl.dataframe.dynamic.DynamicSimulationDataframeMappersUtils.*;
import static com.powsybl.python.network.Dataframes.createSeries;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
class DynamicSimulationDataframeMappersTest {

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
    void testCurveDataframesMapper() {
        Instant t0 = Instant.ofEpochSecond(0);
        Instant t1 = t0.plusNanos(10);
        Instant t2 = t1.plusMillis(20);
        Instant t3 = t2.plusSeconds(4);
        TimeSeriesIndex index = new IrregularTimeSeriesIndex(new Instant[]{t0, t1, t2, t3});
        Map<String, DoubleTimeSeries> curves = new LinkedHashMap<>();
        curves.put("curve1", TimeSeries.createDouble("curve1", index, 1d, 2d, 2d, 3d));
        curves.put("curve2", TimeSeries.createDouble("curve2", index, 4d, 5d, 6d, 6d));
        List<Series> series = TimeSeriesConverter.createSeries(curves.values().stream().toList());
        assertThat(series)
                .extracting(Series::getName)
                .containsExactly("timestamp", "curve1", "curve2");
        assertThat(series).satisfiesExactly(
                col1 -> assertThat(col1.getStrings()).containsExactly("1970-01-01T00:00:00Z",
                        "1970-01-01T00:00:00.000000010Z",
                        "1970-01-01T00:00:00.020000010Z",
                        "1970-01-01T00:00:04.020000010Z"),
                col2 -> assertThat(col2.getDoubles()).containsExactly(1d, 2d, 2d, 3d),
                col2 -> assertThat(col2.getDoubles()).containsExactly(4d, 5d, 6d, 6d));
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

    @Test
    void testCategoryDataframe() {
        List<Series> series = createSeries(DynamicSimulationDataframeMappersUtils.categoriesDataFrameMapper(),
                List.of(new BaseLoadAdder(), new TapChangerBlockingAutomationSystemAdder(), new PhaseShifterBlockingIAdder()));
        assertThat(series).satisfiesExactly(
                names -> assertThat(names.getStrings()).containsExactly("Load", "PhaseShifterBlockingI", "TapChangerBlocking"),
                desc -> assertThat(desc.getStrings()).containsExactly("Standard load", "Phase shifter blocking I", "Tap changer blocking automation system"),
                attr -> assertThat(attr.getStrings()).containsExactly(
                        "index : static_id (str), parameter_set_id (str), model_name (str)",
                        "index : dynamic_model_id (str), parameter_set_id (str), model_name (str), phase_shifter_id (str)",
                        "[dataframe \"Tcb\"] index : dynamic_model_id (str), parameter_set_id (str), model_name (str) / [dataframe \"Transformers\"] index : dynamic_model_id (str), transformer_id (str) / [dataframe \"U measurement 1\"] index : dynamic_model_id (str), measurement_point_id (str) / [dataframe \"U measurement 2\"] index : dynamic_model_id (str), measurement_point_id (str) / [dataframe \"U measurement 3\"] index : dynamic_model_id (str), measurement_point_id (str) / [dataframe \"U measurement 4\"] index : dynamic_model_id (str), measurement_point_id (str) / [dataframe \"U measurement 5\"] index : dynamic_model_id (str), measurement_point_id (str)"));
    }

    @Test
    void testSupportedModelsDataframe() {
        Collection<ModelInfo> infos = DynamicMappingHandler.getSupportedModelsInformation("SimplifiedGenerator");
        List<Series> series = createSeries(DynamicSimulationDataframeMappersUtils.supportedModelsDataFrameMapper(), infos);
        assertThat(series).satisfiesExactly(
                names -> assertThat(names.getStrings()).containsExactly("GeneratorFictitious", "GeneratorPVFixed"),
                desc -> assertThat(desc.getStrings()).containsExactly("Fictitious generator (behaves in a similar way as an alpha-beta load)", ""));
    }
}
