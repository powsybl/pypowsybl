/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.update.UpdatingDataframe;
import org.apache.commons.lang3.function.TriFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for builders of mappers. It uses recursive generic pattern to return the actual
 * builder class.
 *
 * @author Sylvain Leclerc {@literal <sylvain.leclerc at rte-france.com>}
 */
public class BaseDataframeMapperBuilder<T, U, C, B extends BaseDataframeMapperBuilder<T, U, C, B>> {

    @FunctionalInterface
    public interface ItemGetter<T, I> {
        I getItem(T network, UpdatingDataframe updatingDataframe, int lineNumber);
    }

    @FunctionalInterface
    public interface ItemGetterWithContext<T, C, I> {
        I getItem(T network, UpdatingDataframe updatingDataframe, int lineNumber, C context);
    }

    protected BiFunction<T, C, List<U>> itemsProvider;
    protected ItemGetterWithContext<T, C, U> itemMultiIndexGetter;

    protected final List<SeriesMapper<U, C>> series = new ArrayList<>();

    public B itemsProvider(Function<T, List<U>> itemsProvider) {
        this.itemsProvider = (t, c) -> itemsProvider.apply(t);
        return (B) this;
    }

    public B itemsProvider(BiFunction<T, C, List<U>> itemsProvider) {
        this.itemsProvider = itemsProvider;
        return (B) this;
    }

    public B itemsStreamProvider(Function<T, Stream<U>> itemsProvider) {
        return this.itemsProvider((object, context) -> itemsProvider.apply(object).collect(Collectors.toList()));
    }

    public B itemsStreamProvider(BiFunction<T, C, Stream<U>> itemsProvider) {
        return this.itemsProvider((object, context) -> itemsProvider.apply(object, context).collect(Collectors.toList()));
    }

    public B itemGetter(BiFunction<T, String, U> itemGetter) {
        return this.itemGetter((object, context, id) -> itemGetter.apply(object, id));
    }

    public B itemGetter(TriFunction<T, C, String, U> itemGetter) {
        this.itemMultiIndexGetter = (network, updatingDataframe, lineNumber, context) -> {
            String id = updatingDataframe.getStringValue("id", lineNumber)
                    .orElseThrow(() -> new PowsyblException("id is missing"));
            return itemGetter.apply(network, context, id);
        };
        return (B) this;
    }

    public B itemMultiIndexGetter(ItemGetterWithContext<T, C, U> itemMultiIndexGetter) {
        this.itemMultiIndexGetter = itemMultiIndexGetter;
        return (B) this;
    }

    public B itemMultiIndexGetter(ItemGetter<T, U> itemMultiIndexGetter) {
        return itemMultiIndexGetter((network, updatingDataframe, lineNumber, context) -> itemMultiIndexGetter.getItem(network, updatingDataframe, lineNumber));
    }

    public B doubles(String name, ToDoubleFunction<U> value, DoubleSeriesMapper.DoubleUpdater<U, C> updater) {
        return doubles(name, value, updater, true);
    }

    public B doubles(String name, ToDoubleBiFunction<U, C> value, DoubleSeriesMapper.DoubleUpdater<U, C> updater, boolean defaultAttribute) {
        series.add(new DoubleSeriesMapper<>(name, value, updater, defaultAttribute));
        return (B) this;
    }

    public B doubles(String name, ToDoubleBiFunction<U, C> value, DoubleSeriesMapper.DoubleUpdater<U, C> updater) {
        series.add(new DoubleSeriesMapper<>(name, value, updater, true));
        return (B) this;
    }

    public B doubles(String name, ToDoubleFunction<U> value, DoubleSeriesMapper.DoubleUpdater<U, C> updater, boolean defaultAttribute) {
        return doubles(name, (u, pu) -> value.applyAsDouble(u), updater, defaultAttribute);
    }

    public B doubles(Map<String, ToDoubleFunction<U>> nameValuesMap) {
        nameValuesMap.forEach(this::doubles);
        return (B) this;
    }

    public B doubles(String name, ToDoubleFunction<U> value) {
        return doubles(name, value, null, true);
    }

    public B doubles(String name, ToDoubleBiFunction<U, C> value) {
        return doubles(name, value, null, true);
    }

    public B doubles(String name, ToDoubleBiFunction<U, C> value, boolean defaultAttribute) {
        return doubles(name, value, null, defaultAttribute);
    }

    public B doubles(String name, ToDoubleFunction<U> value, boolean defaultAttribute) {
        return doubles(name, value, null, defaultAttribute);
    }

    public B ints(String name, ToIntFunction<U> value, IntSeriesMapper.IntUpdater<U> updater) {
        return ints(name, value, updater, true);
    }

    public B ints(String name, ToIntFunction<U> value, IntSeriesMapper.IntUpdater<U> updater, boolean defaultAttribute) {
        series.add(new IntSeriesMapper<>(name, value, updater, defaultAttribute));
        return (B) this;
    }

    public B ints(String name, ToIntFunction<U> value) {
        return ints(name, value, null, true);
    }

    public B ints(String name, ToIntFunction<U> value, boolean defaultAttribute) {
        return ints(name, value, null, defaultAttribute);
    }

    public B intsIndex(String name, ToIntFunction<U> value) {
        series.add(new IntSeriesMapper<>(name, true, value));
        return (B) this;
    }

    public B booleans(String name, Predicate<U> value, BooleanSeriesMapper.BooleanUpdater<U> updater) {
        return booleans(name, value, updater, true);
    }

    public B booleans(String name, Predicate<U> value, BooleanSeriesMapper.BooleanUpdater<U> updater, boolean defaultAttribute) {
        series.add(new BooleanSeriesMapper<>(name, value, updater, defaultAttribute));
        return (B) this;
    }

    public B booleans(String name, Predicate<U> value) {
        return booleans(name, value, null, true);
    }

    public B booleans(String name, Predicate<U> value, boolean defaultAttribute) {
        return booleans(name, value, null, defaultAttribute);
    }

    public B strings(String name, Function<U, String> value, BiConsumer<U, String> updater) {
        return strings(name, value, updater, true);
    }

    public B strings(String name, Function<U, String> value, BiConsumer<U, String> updater, boolean defaultAttribute) {
        series.add(new StringSeriesMapper<>(name, value, updater, defaultAttribute));
        return (B) this;
    }

    public B strings(String name, Function<U, String> value) {
        return strings(name, value, null, true);
    }

    public B strings(String name, Function<U, String> value, boolean defaultAttribute) {
        return strings(name, value, null, defaultAttribute);
    }

    public B stringsIndex(String name, Function<U, String> value) {
        series.add(new StringSeriesMapper<>(name, true, value));
        return (B) this;
    }

    public <E extends Enum<E>> B enums(String name, Class<E> enumClass, Function<U, E> value, BiConsumer<U, E> updater) {
        return enums(name, enumClass, value, updater, true);
    }

    public <E extends Enum<E>> B enums(String name, Class<E> enumClass, Function<U, E> value, BiConsumer<U, E> updater, boolean defaultAttribute) {
        series.add(new EnumSeriesMapper<>(name, enumClass, value, updater, defaultAttribute));
        return (B) this;
    }

    public <E extends Enum<E>> B enums(String name, Class<E> enumClass, Function<U, E> value) {
        return enums(name, enumClass, value, null, true);
    }

    public <E extends Enum<E>> B enums(String name, Class<E> enumClass, Function<U, E> value, boolean defaultAttribute) {
        return enums(name, enumClass, value, null, defaultAttribute);
    }

    public DataframeMapper<T, C> build() {
        return new AbstractDataframeMapper<>(series) {
            @Override
            protected List<U> getItems(T object, C context) {
                return itemsProvider.apply(object, context);
            }

            @Override
            protected U getItem(T object, UpdatingDataframe dataframe, int index, C context) {
                return itemMultiIndexGetter.getItem(object, dataframe, index, context);
            }
        };
    }
}
