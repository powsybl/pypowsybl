package com.powsybl.dataframe;

/**
 * Receives series data, is in charge of doing something with it,
 * typically writing to a data structure.
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public interface DataframeHandler {

    @FunctionalInterface
    interface IntSeriesWriter {
        void set(int index, int value);
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

    void allocate(int seriesCount);

    StringSeriesWriter newStringIndex(String name, int size);

    IntSeriesWriter newIntIndex(String name, int size);

    StringSeriesWriter newStringSeries(String name, int size);

    IntSeriesWriter newIntSeries(String name, int size);

    BooleanSeriesWriter newBooleanSeries(String name, int size);

    DoubleSeriesWriter newDoubleSeries(String name, int size);

}
