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

    public int getIndex(String column, String value) {
        StringSeries serie = (StringSeries) seriesMap.get(column);
        for (int i = 0; i < serie.getSize(); i++) {
            if (CTypeUtil.toString(serie.getValues().read(i)).equals(value)) {
                return i;
            }
        }
        return -1;
    }

    public void addSeries(Series series, SeriesMetadata seriesMetadata) {
        this.seriesMetadata.put(seriesMetadata.getName(), seriesMetadata);
        this.seriesMap.put(series.getName(), series);
    }

    private Series getSeries(int indice) {
        return seriesMap.get(getSeriesMetadata().get(indice).getName());
    }

    @Override
    public Optional<String> getStringValue(String columnName, int columnNumber, int index) {
        if (containsColumnName(columnName, SeriesDataType.STRING)) {
            return getStringValue(columnName, index);
        } else {
            return getStringValue(columnNumber, index);
        }
    }

    @Override
    public Optional<String> getStringValue(String column, int index) {
        if (seriesMap.get(column) == null) {
            return Optional.empty();
        } else {
            return Optional.of(CTypeUtil.toString(((StringSeries) seriesMap.get(column)).getValues().read(index)));
        }
    }

    @Override
    public Optional<String> getStringValue(int columnNumber, int index) {
        return Optional.of(CTypeUtil.toString(((StringSeries) getSeries(columnNumber)).getValues().read(index)));
    }

    @Override
    public OptionalInt getIntValue(String columnName, int columnNumber, int index) {
        if (containsColumnName(columnName, SeriesDataType.INT)) {
            return getIntValue(columnName, index);
        } else {
            return getIntValue(columnNumber, index);
        }
    }

    @Override
    public OptionalInt getIntValue(String columnNumber, int index) {
        if (seriesMap.get(columnNumber) == null) {
            return OptionalInt.empty();
        } else {
            return OptionalInt.of(((IntSeries) seriesMap.get(columnNumber)).getValues().read(index));
        }
    }

    @Override
    public OptionalInt getIntValue(int columnNumber, int index) {
        if (getSeries(columnNumber) == null) {
            return OptionalInt.empty();
        } else {
            return OptionalInt.of(((IntSeries) getSeries(columnNumber)).getValues().read(index));
        }
    }

    @Override
    public OptionalDouble getDoubleValue(String columnName, int columnNumber, int index) {
        if (containsColumnName(columnName, SeriesDataType.DOUBLE)) {
            return getDoubleValue(columnName, index);
        } else {
            return getDoubleValue(columnNumber, index);
        }
    }

    @Override
    public OptionalDouble getDoubleValue(String column, int index) {
        if (seriesMap.get(column) == null) {
            return OptionalDouble.empty();
        } else {
            return OptionalDouble.of(((DoubleSeries) seriesMap.get(column)).getValues().read(index));
        }
    }

    @Override
    public OptionalDouble getDoubleValue(int columnNumber, int index) {
        if (getSeries(columnNumber) == null) {
            return OptionalDouble.empty();
        } else {
            return OptionalDouble.of(((DoubleSeries) getSeries(columnNumber)).getValues().read(index));
        }
    }

    @Override
    public int getLineCount() {
        return lineCount;
    }

    @Override
    public boolean containsColumnName(String columnName, SeriesDataType type) {
        return seriesMetadata.containsKey(columnName) && seriesMetadata.get(columnName).getType().equals(type);
    }
}
