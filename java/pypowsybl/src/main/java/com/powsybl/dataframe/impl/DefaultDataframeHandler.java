/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.impl;

import com.powsybl.dataframe.DataframeHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * Writes series data to POJOs.
 *
 * @author Sylvain Leclerc {@literal <sylvain.leclerc at rte-france.com>}
 */
public class DefaultDataframeHandler implements DataframeHandler {

    private final Consumer<Series> seriesConsumer;

    public DefaultDataframeHandler(Consumer<Series> seriesConsumer) {
        this.seriesConsumer = Objects.requireNonNull(seriesConsumer);
    }

    @Override
    public void allocate(int seriesCount) {
        //Nothing to do
    }

    @Override
    public StringSeriesWriter newStringIndex(String name, int size) {
        String[] values = new String[size];
        seriesConsumer.accept(Series.index(name, values));
        return (i, s) -> values[i] = s;
    }

    @Override
    public IntSeriesWriter newIntIndex(String name, int size) {
        int[] values = new int[size];
        seriesConsumer.accept(Series.index(name, values));
        return (i, s) -> values[i] = s;
    }

    @Override
    public StringSeriesWriter newStringSeries(String name, int size) {
        String[] values = new String[size];
        seriesConsumer.accept(new Series(name, values));
        return (i, s) -> values[i] = s;
    }

    @Override
    public IntSeriesWriter newIntSeries(String name, int size) {
        int[] values = new int[size];
        seriesConsumer.accept(new Series(name, values));
        return (i, s) -> values[i] = s;
    }

    @Override
    public OptionalIntSeriesWriter newOptionalIntSeries(String name, int size) {
        OptionalInt[] values = new OptionalInt[size];
        seriesConsumer.accept(new Series(name, values));
        return (i, s) -> values[i] = s;
    }

    @Override
    public OptionalDoubleSeriesWriter newOptionalDoubleSeries(String name, int size) {
        OptionalDouble[] values = new OptionalDouble[size];
        seriesConsumer.accept(new Series(name, values));
        return (i, s) -> values[i] = s;
    }

    @Override
    public BooleanSeriesWriter newBooleanSeries(String name, int size) {
        boolean[] values = new boolean[size];
        seriesConsumer.accept(new Series(name, values));
        return (i, s) -> values[i] = s;
    }

    @Override
    public DoubleSeriesWriter newDoubleSeries(String name, int size) {
        double[] values = new double[size];
        seriesConsumer.accept(new Series(name, values));
        return (i, s) -> values[i] = s;
    }

    @Override
    public DictionaryStringSeriesWriter newDictionaryStringIndex(String name, int size) {
        return newDictionaryStringSeries(true, name, size);
    }

    @Override
    public DictionaryStringSeriesWriter newDictionaryStringSeries(String name, int size) {
        return newDictionaryStringSeries(false, name, size);
    }

    private DictionaryStringSeriesWriter newDictionaryStringSeries(boolean index, String name, int size) {
        int[] codes = new int[size];
        List<String> dictionary = new ArrayList<>();
        seriesConsumer.accept(Series.dictionary(index, name, codes, dictionary));
        return new DictionaryStringSeriesWriter() {
            @Override
            public void set(int i, int code) {
                codes[i] = code;
            }

            @Override
            public void setDictionary(List<String> values) {
                dictionary.clear();
                dictionary.addAll(values);
            }
        };
    }
}
