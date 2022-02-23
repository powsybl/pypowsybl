package com.powsybl.dataframe;

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
    public int getIndex(String column, String value) {
        return ((TestStringSeries) series.get(column)).getValues().indexOf(value);
    }

    @Override
    public List<SeriesMetadata> getSeriesMetadata() {
        return new ArrayList<>(columns.values());
    }

    @Override
    public OptionalDouble getDoubleValue(String column, int index) {
        if (series.get(column) == null) {
            return OptionalDouble.empty();
        } else {
            return OptionalDouble.of(((TestDoubleSeries) series.get(column)).getValues().get(index));
        }
    }

    @Override
    public OptionalDouble getDoubleValue(int column, int index) {
        if (series.get(getSeriesMetadata().get(column).getName()) == null) {
            return  OptionalDouble.empty();
        } else {
            return OptionalDouble.of(((TestDoubleSeries) series.get(getSeriesMetadata().get(column).getName())).getValues().get(index));
        }
    }

    @Override
    public OptionalDouble getDoubleValue(String columnName, int column, int index) {
        if (containsColumnName(columnName, SeriesDataType.DOUBLE)) {
            return getDoubleValue(columnName, index);
        } else {
            return getDoubleValue(column, index);
        }
    }

    @Override
    public Optional<String> getStringValue(String column, int index) {
        if (series.get(column) == null) {
            return Optional.empty();
        } else {
            return Optional.of(((TestStringSeries) series.get(column)).getValues().get(index));
        }
    }

    @Override
    public Optional<String> getStringValue(int column, int index) {
        if (series.get(column) == null) {
            return Optional.empty();
        } else {
            return Optional.of(((TestStringSeries) series.get(getSeriesMetadata().get(column).getName())).getValues().get(index));
        }
    }

    @Override
    public Optional<String> getStringValue(String columnName, int column, int index) {
        if (containsColumnName(columnName, SeriesDataType.STRING)) {
            return getStringValue(columnName, index);
        } else {
            return getStringValue(column, index);
        }
    }

    @Override
    public OptionalInt getIntValue(String column, int index) {
        if (series.get(column) == null) {
            return OptionalInt.empty();
        } else {
            return OptionalInt.of(((TestIntSeries) series.get(column)).getValues().get(index));
        }
    }

    @Override
    public OptionalInt getIntValue(int column, int index) {
        if (series.get(getSeriesMetadata().get(column).getName()) == null) {
            return OptionalInt.empty();
        } else {
            return OptionalInt.of(((TestIntSeries) series.get(getSeriesMetadata().get(column).getName())).getValues().get(index));
        }
    }

    @Override
    public OptionalInt getIntValue(String columnName, int column, int index) {
        if (containsColumnName(columnName, SeriesDataType.INT)) {
            return getIntValue(columnName, index);
        } else {
            return getIntValue(column, index);
        }
    }

    @Override
    public int getLineCount() {
        return size;
    }

    @Override
    public boolean containsColumnName(String columnName, SeriesDataType type) {
        return columns.containsKey(columnName) && columns.get(columnName).getType().equals(type);
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
