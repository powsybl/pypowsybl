/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe;

import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Receives series data, is in charge of doing something with it,
 * typically writing to a data structure.
 *
 * @author Sylvain Leclerc {@literal <sylvain.leclerc at rte-france.com>}
 */
public interface DataframeHandler {

    @FunctionalInterface
    interface IntSeriesWriter {
        void set(int index, int value);
    }

    @FunctionalInterface
    interface OptionalIntSeriesWriter {
        void set(int index, OptionalInt value);
    }

    @FunctionalInterface
    interface OptionalDoubleSeriesWriter {
        void set(int index, OptionalDouble value);
    }

    @FunctionalInterface
    interface DoubleSeriesWriter {
        void set(int index, double value);
    }

    @FunctionalInterface
    interface BooleanSeriesWriter {
        void set(int index, boolean value);
    }

    @FunctionalInterface
    interface StringSeriesWriter {
        void set(int index, String value);
    }

    /**
     * Writer of a dictionary-encoded string series: one int code per row, referring to the list of distinct values
     * (the dictionary) which is set once, after all codes are written. The code -1 encodes a missing value.
     */
    interface DictionaryStringSeriesWriter {
        /**
         * Writes the code of one row.
         *
         * @param index the row
         * @param code  the index of the row's value in the dictionary, or -1 for a missing value
         */
        void set(int index, int code);

        /**
         * Sets the distinct values the codes refer to; to be called once, after all codes are written.
         *
         * @param dictionary the distinct values, indexed by their code
         */
        void setDictionary(List<String> dictionary);
    }

    void allocate(int seriesCount);

    StringSeriesWriter newStringIndex(String name, int size);

    IntSeriesWriter newIntIndex(String name, int size);

    StringSeriesWriter newStringSeries(String name, int size);

    IntSeriesWriter newIntSeries(String name, int size);

    OptionalIntSeriesWriter newOptionalIntSeries(String name, int size);

    OptionalDoubleSeriesWriter newOptionalDoubleSeries(String name, int size);

    BooleanSeriesWriter newBooleanSeries(String name, int size);

    DoubleSeriesWriter newDoubleSeries(String name, int size);

    /**
     * Creates an index level transferred as codes and a dictionary; it is read as the same index as a string index.
     *
     * @param name the name of the index level
     * @param size the number of rows
     * @return the writer of the codes and dictionary
     */
    DictionaryStringSeriesWriter newDictionaryStringIndex(String name, int size);

    /**
     * Creates a string series transferred as codes and a dictionary; it is read as the same column as a string series.
     *
     * @param name the name of the series
     * @param size the number of rows
     * @return the writer of the codes and dictionary
     */
    DictionaryStringSeriesWriter newDictionaryStringSeries(String name, int size);

}
