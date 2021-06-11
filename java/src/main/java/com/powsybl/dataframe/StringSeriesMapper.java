package com.powsybl.dataframe;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class StringSeriesMapper<T> implements SeriesMapper<T> {

    private final SeriesMetadata metadata;
    private final BiConsumer<T, String> updater;
    private final Function<T, String> value;

    public StringSeriesMapper(String name, Function<T, String> value) {
        this(name, false, value);
    }

    public StringSeriesMapper(String name, boolean index, Function<T, String> value) {
        this(name, index, value, null);
    }

    public StringSeriesMapper(String name, Function<T, String> value, BiConsumer<T, String> updater) {
        this(name, false, value, updater);
    }

    public StringSeriesMapper(String name, boolean index, Function<T, String> value, BiConsumer<T, String> updater) {
        this.metadata = new SeriesMetadata(index, name, updater != null, SeriesDataType.STRING);
        this.updater = updater;
        this.value = value;
    }

    @Override
    public SeriesMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void createSeries(List<T> items, DataframeHandler handler) {
        boolean index = getMetadata().isIndex();
        String name = getMetadata().getName();
        DataframeHandler.StringSeriesWriter writer = index ? handler.newStringIndex(name, items.size()) : handler.newStringSeries(name, items.size());
        for (int i = 0; i < items.size(); i++) {
            writer.set(i, value.apply(items.get(i)));
        }
    }

    @Override
    public void updateString(T object, String value) {
        updater.accept(object, value);
    }
}
