/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.update;

import com.powsybl.dataframe.SeriesDataType;
import com.powsybl.dataframe.SeriesMetadata;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * @author Etienne Lesot {@literal <etienne.lesot at rte-france.com>}
 */
public interface UpdatingDataframe {

    int getIndex(String column, String value);

    List<SeriesMetadata> getSeriesMetadata();

    OptionalDouble getDoubleValue(String column, int index);

    OptionalDouble getDoubleValue(int column, int index);

    OptionalDouble getDoubleValue(String columnName, int column, int index);

    Optional<String> getStringValue(String column, int index);

    Optional<String> getStringValue(int column, int index);

    /**
     * get a string value in the dataframe according to the column name and the index.
     * If the column name is not present in the map of the string series it gets the column indice in the ordered column names
     * example:
     * string_series1   int_series1   string_series2   double_series1
     * 0               a              3            d               1.1
     * 1               b              4            e               1.2
     * 2               c              8            f               1.3
     * <p>
     * getStringValue("string_series1", 0, 1) gives "b"
     * getStringValue("random", 0, 1) gives also "b"
     * getStringValue("random", 1, 2) gives "f"
     *
     * @param columnName
     * @param column
     * @param index
     * @return
     */
    Optional<String> getStringValue(String columnName, int column, int index);

    OptionalInt getIntValue(String column, int index);

    OptionalInt getIntValue(int column, int index);

    OptionalInt getIntValue(String columnName, int column, int index);

    int getLineCount();

    boolean containsColumnName(String columnName, SeriesDataType type);
}
