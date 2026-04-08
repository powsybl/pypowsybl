package com.powsybl.dataframe;

import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Function;

public class OptionalDoubleSeriesMapper <T, C> implements SeriesMapper<T, C> {

    private final SeriesMetadata metadata;
    private final Function<T, OptionalDouble> value;

    public OptionalDoubleSeriesMapper(String name, Function<T, OptionalDouble> value) {
        this(name, value, true);
    }

    public OptionalDoubleSeriesMapper(String name, Function<T, OptionalDouble> value, boolean defaultAttribute) {
        this.metadata = new SeriesMetadata(false, name, false, SeriesDataType.DOUBLE, defaultAttribute);
        this.value = value;
    }

    @Override
    public SeriesMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void createSeries(List<T> items, DataframeHandler handler, C context) {
        String name = metadata.getName();
        DataframeHandler.OptionalDoubleSeriesWriter writer = handler.newOptionalDoubleSeries(name, items.size());
        for (int i = 0; i < items.size(); i++) {
            writer.set(i, value.apply(items.get(i)));
        }
    }
}
