package com.powsybl.dataframe;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class DataframeMapperBuilder<T extends Identifiable<T>> {

    private final Function<Network, List<T>> listProvider;
    private final BiFunction<Network, String, T> itemProvider;

    private final List<SeriesMapper<T>> series;

    public DataframeMapperBuilder(Function<Network, List<T>> listProvider, BiFunction<Network, String, T> itemProvider) {
        this.listProvider = Objects.requireNonNull(listProvider);
        this.itemProvider = Objects.requireNonNull(itemProvider);
        this.series = new ArrayList<>();
    }

    public static <U extends Identifiable<U>> DataframeMapperBuilder<U> ofStream(Function<Network, Stream<U>> streamProvider, BiFunction<Network, String, U> itemProvider) {
        return new DataframeMapperBuilder<>(n -> streamProvider.apply(n).collect(Collectors.toList()), itemProvider);
    }

    public static <U extends Identifiable<U>> DataframeMapperBuilder<U> ofStream(Function<Network, Stream<U>> streamProvider) {
        BiFunction<Network, String, U> noUpdate = (n, s) -> {
            throw new UnsupportedOperationException("Update is not supported");
        };
        return DataframeMapperBuilder.ofStream(streamProvider, noUpdate);
    }

    public DataframeMapperBuilder<T> doubles(String name, ToDoubleFunction<T> value, DoubleSeriesMapper.DoubleUpdater<T> updater) {
        series.add(new DoubleSeriesMapper<>(name, value, updater));
        return this;
    }

    public DataframeMapperBuilder<T> doubles(String name, ToDoubleFunction<T> value) {
        return doubles(name, value, null);
    }

    public DataframeMapperBuilder<T> ints(String name, ToIntFunction<T> value, IntSeriesMapper.IntUpdater<T> updater) {
        series.add(new IntSeriesMapper<>(name, value, updater));
        return this;
    }

    public DataframeMapperBuilder<T> ints(String name, ToIntFunction<T> value) {
        return ints(name, value, null);
    }

    public DataframeMapperBuilder<T> intsIndex(String name, ToIntFunction<T> value) {
        series.add(new IntSeriesMapper<>(name, true, value));
        return this;
    }

    public DataframeMapperBuilder<T> booleans(String name, Predicate<T> value, BooleanSeriesMapper.BooleanUpdater<T> updater) {
        series.add(new BooleanSeriesMapper<>(name, value, updater));
        return this;
    }

    public DataframeMapperBuilder<T> booleans(String name, Predicate<T> value) {
        return booleans(name, value, null);
    }

    public DataframeMapperBuilder<T> strings(String name, Function<T, String> value, BiConsumer<T, String> updater) {
        series.add(new StringSeriesMapper<>(name, value, updater));
        return this;
    }

    public DataframeMapperBuilder<T> strings(String name, Function<T, String> value) {
        return strings(name, value, null);
    }

    public DataframeMapperBuilder<T> stringsIndex(String name, Function<T, String> value) {
        series.add(new StringSeriesMapper<>(name, true, value));
        return this;
    }

    public <E extends Enum<E>> DataframeMapperBuilder<T> enums(String name, Class<E> enumClass, Function<T, E> value, BiConsumer<T, E> updater) {
        series.add(new EnumSeriesMapper<>(name, enumClass, value, updater));
        return this;
    }

    public <E extends Enum<E>> DataframeMapperBuilder<T> enums(String name, Class<E> enumClass, Function<T, E> value) {
        return enums(name, enumClass, value, null);
    }

    DataframeMapper build() {
        return new AbstractDataframeMapper<>(series) {
            @Override
            protected List<T> getObjects(Network network) {
                return listProvider.apply(network);
            }

            @Override
            protected T getObject(Network network, String id) {
                return itemProvider.apply(network, id);
            }
        };
    }
}
