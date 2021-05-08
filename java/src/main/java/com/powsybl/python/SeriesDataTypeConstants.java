/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
final class SeriesDataTypeConstants {

    static final Map<String, Integer> SWITCH_MAP = ImmutableMap.<String, Integer>builder()
            .put("open", SeriesPointerArrayBuilder.INT_SERIES_TYPE)
            .put("retained", SeriesPointerArrayBuilder.INT_SERIES_TYPE)
            .build();

    static final Map<String, Integer> GENERATOR_MAP = ImmutableMap.<String, Integer>builder()
            .put("voltage_regulator_on", SeriesPointerArrayBuilder.INT_SERIES_TYPE)
            .put("target_p", SeriesPointerArrayBuilder.DOUBLE_SERIES_TYPE)
            .put("target_q", SeriesPointerArrayBuilder.DOUBLE_SERIES_TYPE)
            .put("target_v", SeriesPointerArrayBuilder.DOUBLE_SERIES_TYPE)
            .build();

    static final Map<String, Integer> LOAD_MAP = ImmutableMap.<String, Integer>builder()
            .put("p0", SeriesPointerArrayBuilder.DOUBLE_SERIES_TYPE)
            .put("q0", SeriesPointerArrayBuilder.DOUBLE_SERIES_TYPE)
            .build();

    static final Map<String, Integer> DANGLING_LINE_MAP = ImmutableMap.<String, Integer>builder()
            .put("p0", SeriesPointerArrayBuilder.DOUBLE_SERIES_TYPE)
            .put("q0", SeriesPointerArrayBuilder.DOUBLE_SERIES_TYPE)
            .build();

    static final Map<String, Integer> VSC_CONVERTER_STATION_MAP = ImmutableMap.<String, Integer>builder()
            .put("voltage_regulator_on", SeriesPointerArrayBuilder.INT_SERIES_TYPE)
            .put("voltage_setpoint", SeriesPointerArrayBuilder.DOUBLE_SERIES_TYPE)
            .put("reactive_power_setpoint", SeriesPointerArrayBuilder.DOUBLE_SERIES_TYPE)
            .build();

    static final Map<String, Integer> STATIC_VAR_COMPENSATOR_MAP = ImmutableMap.<String, Integer>builder()
            .put("regulation_mode", SeriesPointerArrayBuilder.STRING_SERIES_TYPE)
            .put("voltage_setpoint", SeriesPointerArrayBuilder.DOUBLE_SERIES_TYPE)
            .put("reactive_power_setpoint", SeriesPointerArrayBuilder.DOUBLE_SERIES_TYPE)
            .build();

    static final Map<String, Integer> HVDC_LINE_MAP = ImmutableMap.<String, Integer>builder()
            .put("converters_mode", SeriesPointerArrayBuilder.STRING_SERIES_TYPE)
            .put("active_power_setpoint", SeriesPointerArrayBuilder.DOUBLE_SERIES_TYPE)
            .build();

    static final Map<String, Integer> TWO_WINDINGS_TRANSFORMER_MAP = ImmutableMap.<String, Integer>builder()
            .put("ratio_tap_position", SeriesPointerArrayBuilder.INT_SERIES_TYPE)
            .put("phase_tap_position", SeriesPointerArrayBuilder.INT_SERIES_TYPE)
            .build();

    private SeriesDataTypeConstants() {
    }
}
