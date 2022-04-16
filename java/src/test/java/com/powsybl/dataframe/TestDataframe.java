package com.powsybl.dataframe;

import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.python.update.Series;

import java.util.*;

public class TestDataframe implements UpdatingDataframe {

    private final int size;
    private final Map<String, SeriesMetadata> columns = new LinkedHashMap<>();
    private final Map<String, Series> series = new HashMap<>();

    public TestDataframe(int size) {
        this.size = size;
    }

    public void addSeries(Series series) {
        this.series.put(series.getName(), series);
    }

    public void addColumnName(String name, SeriesDataType type, boolean index) {
        columns.put(name, new SeriesMetadata(index, name, false, type, true));
    }

    @Override
    public List<SeriesMetadata> getSeriesMetadata() {
        return new ArrayList<>(columns.values());
    }

    @Override
    public DoubleSeries getDoubles(String column) {
        Series col = series.get(column);
        if (!(col instanceof TestDoubleSeries)) {
            return null;
        }
        TestDoubleSeries asDouble = (TestDoubleSeries) col;
        return index -> asDouble.getValues().get(index);
    }

    @Override
    public IntSeries getInts(String column) {
        Series col = series.get(column);
        if (!(col instanceof TestIntSeries)) {
            return null;
        }
        TestIntSeries asInt = (TestIntSeries) col;
        return index -> asInt.getValues().get(index);
    }

    @Override
    public StringSeries getStrings(String column) {
        Series col = series.get(column);
        if (!(col instanceof TestStringSeries)) {
            return null;
        }
        TestStringSeries asString = (TestStringSeries) col;
        return index -> asString.getValues().get(index);
    }

    @Override
    public int getRowCount() {
        return size;
    }

    public static class TestIntSeries implements Series<List<Integer>> {

        private final List<Integer> values = new ArrayList<>();
        private final String name;

        public TestIntSeries(List<Integer> values, String name) {
            this.values.addAll(values);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getSize() {
            return values.size();
        }

        @Override
        public List<Integer> getValues() {
            return values;
        }
    }

    public static class TestDoubleSeries implements Series<List<Double>> {

        private final List<Double> values = new ArrayList<>();
        private final String name;

        public TestDoubleSeries(List<Double> values, String name) {
            this.values.addAll(values);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getSize() {
            return values.size();
        }

        @Override
        public List<Double> getValues() {
            return values;
        }
    }

    public static class TestStringSeries implements Series<List<String>> {

        private final List<String> values = new ArrayList<>();
        private final String name;

        public TestStringSeries(List<String> values, String name) {
            this.values.addAll(values);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getSize() {
            return values.size();
        }

        @Override
        public List<String> getValues() {
            return values;
        }
    }
}
