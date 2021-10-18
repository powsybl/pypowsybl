/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.update;
/**
 * @author Etienne Lesot {@literal <etienne.lesot at rte-france.com>}
 */

import com.powsybl.dataframe.SeriesDataType;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.python.CTypeUtil;

import java.util.*;

public class CUpdatingDataframe implements UpdatingDataframe {
    private final int lineCount;
    private final Map<String, SeriesMetadata> seriesMetadata = new LinkedHashMap<>();
    private final Map<String, Series> seriesMap = new HashMap<>();

    public CUpdatingDataframe(int lineCount) {
        this.lineCount = lineCount;
    }

    @Override
    public List<SeriesMetadata> getSeriesMetadata() {
        return new ArrayList<>(seriesMetadata.values());
    }

    public void addSeries(Series series, SeriesMetadata seriesMetadata) {
        this.seriesMetadata.put(seriesMetadata.getName(), seriesMetadata);
        this.seriesMap.put(series.getName(), series);
    }

    private Series getSeries(int indice) {
        return seriesMap.get(getSeriesMetadata().get(indice).getName());
    }

    @Override
    public String getStringValue(String columnName, int columnNumber, int index) {
        if (containsColumnName(columnName, SeriesDataType.STRING)) {
            return getStringValue(columnName, index);
        } else {
            return getStringValue(columnNumber, index);
        }
    }

    @Override
    public String getStringValue(String column, int index) {
        return CTypeUtil.toString(((StringSeries) seriesMap.get(column)).getValues().read(index));
    }

    @Override
    public String getStringValue(int columnNumber, int index) {
        return CTypeUtil.toString(((StringSeries) getSeries(columnNumber)).getValues().read(index));
    }

    @Override
    public int getIntValue(String columnName, int columnNumber, int index) {
        if (containsColumnName(columnName, SeriesDataType.INT)) {
            return getIntValue(columnName, index);
        } else {
            return getIntValue(columnNumber, index);
        }
    }

    @Override
    public int getIntValue(String columnNumber, int index) {
        return ((IntSeries) seriesMap.get(columnNumber)).getValues().read(index);
    }

    @Override
    public int getIntValue(int columnNumber, int index) {
        return ((IntSeries) getSeries(columnNumber)).getValues().read(index);
    }

    @Override
    public double getDoubleValue(String columnName, int columnNumber, int index) {
        if (containsColumnName(columnName, SeriesDataType.DOUBLE)) {
            return getDoubleValue(columnName, index);
        } else {
            return getDoubleValue(columnNumber, index);
        }
    }

    @Override
    public double getDoubleValue(String column, int index) {
        return ((DoubleSeries) seriesMap.get(column)).getValues().read(index);
    }

    @Override
    public double getDoubleValue(int columnNumber, int index) {
        return ((DoubleSeries) getSeries(columnNumber)).getValues().read(index);
    }

    @Override
    public int getLineCount() {
        return lineCount;
    }

    private boolean containsColumnName(String columnName, SeriesDataType type) {
        return seriesMetadata.containsKey(columnName) && seriesMetadata.get(columnName).getType().equals(type);
    }
}
