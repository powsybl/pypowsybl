/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe;

import com.powsybl.dataframe.update.UpdatingDataframe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for builders of mappers. It uses recursive generic pattern to return the actual
 * builder class.
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class BaseDataframeMapperBuilder<T, U, B extends BaseDataframeMapperBuilder<T, U, B>> {

    @FunctionalInterface
    public interface ItemGetter<T, I> {
        I getItem(T network, UpdatingDataframe updatingDataframe, int lineNumber);
    }

    protected Function<T, List<U>> itemsProvider;
    protected ItemGetter<T, U> itemMultiIndexGetter;

    protected final List<SeriesMapper<U>> series;

    public BaseDataframeMapperBuilder() {
        this.series = new ArrayList<>();
    }

    public B itemsProvider(Function<T, List<U>> itemsProvider) {
        this.itemsProvider = itemsProvider;
        return (B) this;
    }

    public B itemsStreamProvider(Function<T, Stream<U>> itemsProvider) {
        return this.itemsProvider(object -> itemsProvider.apply(object).collect(Collectors.toList()));
    }

    public B itemGetter(BiFunction<T, String, U> itemGetter) {
        this.itemMultiIndexGetter = (network, updatingDataframe, lineNumber) -> {
            String id = updatingDataframe.getStringValue("id", 0, lineNumber);
            return itemGetter.apply(network, id);
        };
        return (B) this;
    }

    public B itemMultiIndexGetter(ItemGetter<T, U> itemMultiIndexGetter) {
        this.itemMultiIndexGetter = itemMultiIndexGetter;
        return (B) this;
    }

    public B doubles(String name, ToDoubleFunction<U> value, DoubleSeriesMapper.DoubleUpdater<U> updater) {
        series.add(new DoubleSeriesMapper<>(name, value, updater));
        return (B) this;
    }

    public B doubles(String name, ToDoubleFunction<U> value) {
        return doubles(name, value, null);
    }

    public B ints(String name, ToIntFunction<U> value, IntSeriesMapper.IntUpdater<U> updater) {
        series.add(new IntSeriesMapper<>(name, value, updater));
        return (B) this;
    }

    public B ints(String name, ToIntFunction<U> value) {
        return ints(name, value, null);
    }

    public B intsIndex(String name, ToIntFunction<U> value) {
        series.add(new IntSeriesMapper<>(name, true, value));
        return (B) this;
    }

    public B booleans(String name, Predicate<U> value, BooleanSeriesMapper.BooleanUpdater<U> updater) {
        series.add(new BooleanSeriesMapper<>(name, value, updater));
        return (B) this;
    }

    public B booleans(String name, Predicate<U> value) {
        return booleans(name, value, null);
    }

    public B strings(String name, Function<U, String> value, BiConsumer<U, String> updater) {
        series.add(new StringSeriesMapper<>(name, value, updater));
        return (B) this;
    }

    public B strings(String name, Function<U, String> value) {
        return strings(name, value, null);
    }

    public B stringsIndex(String name, Function<U, String> value) {
        series.add(new StringSeriesMapper<>(name, true, value));
        return (B) this;
    }

    public <E extends Enum<E>> B enums(String name, Class<E> enumClass, Function<U, E> value, BiConsumer<U, E> updater) {
        series.add(new EnumSeriesMapper<>(name, enumClass, value, updater));
        return (B) this;
    }

    public <E extends Enum<E>> B enums(String name, Class<E> enumClass, Function<U, E> value) {
        return enums(name, enumClass, value, null);
    }

    public DataframeMapper<T> build() {
        return new AbstractDataframeMapper<>(series) {
            @Override
            protected List<U> getItems(T object) {
                return itemsProvider.apply(object);
            }

            @Override
            protected U getItem(T object, UpdatingDataframe dataframe, int index) {
                return itemMultiIndexGetter.getItem(object, dataframe, index);
            }
        };
    }
}
