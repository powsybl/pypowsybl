package com.powsybl.dataframe;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class StringSeriesMapper<T> extends SeriesMapper<T> {

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
        super(name, index);
        this.updater = updater;
        this.value = value;
    }

    @Override
    public void createSeries(List<T> items, DataframeHandler handler) {
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
