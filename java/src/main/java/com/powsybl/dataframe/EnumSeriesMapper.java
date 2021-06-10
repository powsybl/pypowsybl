package com.powsybl.dataframe;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class EnumSeriesMapper<T, E extends Enum<E>> extends SeriesMapper<T> {

    private final Class<E> enumClass;
    private final BiConsumer<T, E> updater;
    private final Function<T, E> value;

    public EnumSeriesMapper(String name, Class<E> enumClass, Function<T, E> value) {
        this(name, enumClass, value, null);
    }

    public EnumSeriesMapper(String name, Class<E> enumClass, Function<T, E> value, BiConsumer<T, E> updater) {
        super(name, false);
        this.enumClass = enumClass;
        this.updater = updater;
        this.value = value;
    }

    @Override
    public void createSeries(List<T> items, DataframeHandler factory) {
        DataframeHandler.StringSeriesWriter writer = factory.newStringSeries(name, items.size());
        for (int i = 0; i < items.size(); i++) {
            writer.set(i, value.apply(items.get(i)).toString());
        }
    }

    @Override
    public void updateString(T object, String stringValue) {
        E enumValue = Enum.valueOf(enumClass, stringValue);
        updater.accept(object, enumValue);
    }
}
