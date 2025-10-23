/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic;

import com.powsybl.dataframe.*;
import com.powsybl.dataframe.impl.DefaultDataframeHandler;
import com.powsybl.dataframe.impl.Series;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.dataframe.CDataframeHandler;
import com.powsybl.timeseries.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public final class TimeSeriesConverter {

    private static final String INDEX_NAME = "timestamp";

    private TimeSeriesConverter() {
    }

    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> createCDataframe(List<DoubleTimeSeries> timeSeriesList) {
        CDataframeHandler handler = new CDataframeHandler();
        handler.allocate(timeSeriesList.size() + 1);
        convertDoubleTimeSeries(timeSeriesList, handler);
        return handler.getDataframePtr();
    }

    public static List<Series> createSeries(List<DoubleTimeSeries> timeSeriesList) {
        List<Series> series = new ArrayList<>(timeSeriesList.size() + 1);
        convertDoubleTimeSeries(timeSeriesList, new DefaultDataframeHandler(series::add));
        return List.copyOf(series);
    }

    private static void convertDoubleTimeSeries(List<DoubleTimeSeries> timeSeriesList, DataframeHandler dataframeHandler) {
        Objects.requireNonNull(timeSeriesList);
        if (timeSeriesList.isEmpty()) {
            return;
        }
        // check index unicity
        long indexCount = timeSeriesList.stream().map(DoubleTimeSeries::getMetadata)
                .map(TimeSeriesMetadata::getIndex)
                .distinct()
                .count();
        if (indexCount > 1) {
            throw new TimeSeriesException("Time series must have the same index");
        }

        int size = timeSeriesList.getFirst().getMetadata().getIndex().getPointCount();
        convertSingleDoubleTimeSeries(timeSeriesList.getFirst(), dataframeHandler, size);
        for (int i = 1; i < timeSeriesList.size(); i++) {
            DoubleTimeSeries timeSeries = timeSeriesList.get(i);
            DataframeHandler.DoubleSeriesWriter writer = dataframeHandler.newDoubleSeries(timeSeries.getMetadata().getName(), size);
            int j = 0;
            for (DoublePoint point : timeSeries) {
                writer.set(j, point.getValue());
                j++;
            }
        }
    }

    private static void convertSingleDoubleTimeSeries(DoubleTimeSeries timeSeries, DataframeHandler dataframeHandler, int size) {
        DataframeHandler.StringSeriesWriter indexWriter = dataframeHandler.newStringIndex(INDEX_NAME, size);
        DataframeHandler.DoubleSeriesWriter writer = dataframeHandler.newDoubleSeries(timeSeries.getMetadata().getName(), size);
        int i = 0;
        for (DoublePoint point : timeSeries) {
            indexWriter.set(i, point.getInstant().toString());
            writer.set(i, point.getValue());
            i++;
        }
    }
}
