package com.powsybl.dataframe;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Function;

public class OptionalIntSeriesMapper<T, C> implements SeriesMapper<T, C> {

    private final SeriesMetadata metadata;
    private Function<T, OptionalInt> value;

    public OptionalIntSeriesMapper(String name, Function<T, OptionalInt> value) {
        this(name, value, true);
    }

    public OptionalIntSeriesMapper(String name, Function<T, OptionalInt> value, boolean defaultAttribute) {
        this.metadata = new SeriesMetadata(false, name, false, SeriesDataType.INT, defaultAttribute);
        this.value = value;
    }

    @Override
    public SeriesMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void createSeries(List<T> items, DataframeHandler handler, C context) {
        String name = metadata.getName();
        DataframeHandler.OptionalIntSeriesWriter writer = handler.newOptionalIntSeries(name, items.size());
        for (int i = 0; i < items.size(); i++) {
            writer.set(i, value.apply(items.get(i)));
        }
    }
}
