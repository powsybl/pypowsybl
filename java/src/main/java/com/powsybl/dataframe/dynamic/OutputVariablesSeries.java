/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic;

import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.timeseries.DoublePoint;
import com.powsybl.timeseries.DoubleTimeSeries;
import com.powsybl.timeseries.TimeSeries;

import java.util.Map;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public final class OutputVariablesSeries {

    private OutputVariablesSeries() {
    }

    public static DataframeMapper<DoubleTimeSeries, Void> curvesDataFrameMapper(String colName) {
        return new DataframeMapperBuilder<DoubleTimeSeries, DoublePoint, Void>()
                .itemsStreamProvider(TimeSeries::stream)
                .intsIndex("timestamp", pt -> (int) (pt.getTime() % Integer.MAX_VALUE))
                .doubles(colName, DoublePoint::getValue)
                .build();
    }

    public static DataframeMapper<Map<String, Double>, Void> fsvDataFrameMapper() {
        return new DataframeMapperBuilder<Map<String, Double>, Map.Entry<String, Double>, Void>()
                .itemsStreamProvider(m -> m.entrySet().stream())
                .stringsIndex("variables", Map.Entry::getKey)
                .doubles("values", Map.Entry::getValue)
                .build();
    }
}
