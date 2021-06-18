/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Writes series data to POJOs.
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class DataframeHandlerImpl implements DataframeHandler {

    private final Consumer<Series> seriesConsumer;

    public DataframeHandlerImpl(Consumer<Series> seriesConsumer) {
        this.seriesConsumer = Objects.requireNonNull(seriesConsumer);
    }

    /**
     * POJO representation of a series.
     * Using a "union type" for now (only one type of array is used).
     */
    public static class Series {

        private final boolean index;
        private final String name;
        private final double[] doubles;
        private final int[] ints;
        private final boolean[] booleans;
        private final String[] strings;

        public Series(String name, double[] values) {
            this(false, name, values, null, null, null);
        }

        public Series(String name, String[] values) {
            this(false, name, null, null, null, values);
        }

        public Series(String name, int[] values) {
            this(false, name, null, values, null, null);
        }

        public Series(String name, boolean[] values) {
            this(false, name, null, null, values, null);
        }

        public Series(boolean index, String name, double[] doubles, int[] ints, boolean[] booleans, String[] strings) {
            this.index = index;
            this.name = name;
            this.doubles = doubles;
            this.ints = ints;
            this.booleans = booleans;
            this.strings = strings;
        }

        public static Series index(String name, String[] values) {
            return new Series(true, name, null, null, null, values);
        }

        public static Series index(String name, int[] values) {
            return new Series(true, name, null, values, null, null);
        }

        public boolean isIndex() {
            return index;
        }

        public String getName() {
            return name;
        }

        public double[] getDoubles() {
            return doubles;
        }

        public int[] getInts() {
            return ints;
        }

        public boolean[] getBooleans() {
            return booleans;
        }

        public String[] getStrings() {
            return strings;
        }
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
}
