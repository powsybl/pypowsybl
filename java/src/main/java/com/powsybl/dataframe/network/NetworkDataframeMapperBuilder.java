/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network;

import com.powsybl.dataframe.BaseDataframeMapperBuilder;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Specific build for network mappers :
 * it provides network-specific features, in particular the {@link #addProperties()} method.
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class NetworkDataframeMapperBuilder<T> extends BaseDataframeMapperBuilder<Network, T, DataframeContext, NetworkDataframeMapperBuilder<T>> {

    private boolean addProperties;

    public NetworkDataframeMapperBuilder() {
        this.addProperties = false;
    }

    public static <U> NetworkDataframeMapperBuilder<U> ofStream(Function<Network, Stream<U>> itemProvider, ItemGetter<Network, U> itemGetter) {
        return new NetworkDataframeMapperBuilder<U>()
                .itemsStreamProvider(itemProvider)
                .itemMultiIndexGetter(itemGetter);
    }

    public static <U> NetworkDataframeMapperBuilder<U> ofStream(Function<Network, Stream<U>> itemProvider, BiFunction<Network, String, U> itemGetter) {
        return new NetworkDataframeMapperBuilder<U>()
                .itemsStreamProvider(itemProvider)
                .itemGetter(itemGetter);
    }

    public static <U> NetworkDataframeMapperBuilder<U> ofStream(BiFunction<Network, DataframeContext, Stream<U>> itemProvider, ItemGetterWithContext<Network, DataframeContext, U> itemGetter) {
        return new NetworkDataframeMapperBuilder<U>()
                .itemsStreamProvider(itemProvider)
                .itemMultiIndexGetter(itemGetter);
    }

    public static <U> NetworkDataframeMapperBuilder<U> ofStream(BiFunction<Network, DataframeContext, Stream<U>> itemProvider, BiFunction<Network, String, U> itemGetter) {
        return new NetworkDataframeMapperBuilder<U>()
                .itemsStreamProvider(itemProvider)
                .itemGetter(itemGetter);
    }

    public static <U> NetworkDataframeMapperBuilder<U> ofStream(Function<Network, Stream<U>> streamProvider) {
        BiFunction<Network, String, U> noUpdate = (n, s) -> {
            throw new UnsupportedOperationException("Update is not supported");
        };
        return NetworkDataframeMapperBuilder.ofStream(streamProvider, noUpdate);
    }

    public static <U> NetworkDataframeMapperBuilder<U> ofStream(BiFunction<Network, DataframeContext, Stream<U>> streamProvider) {
        BiFunction<Network, String, U> noUpdate = (n, s) -> {
            throw new UnsupportedOperationException("Update is not supported");
        };
        return NetworkDataframeMapperBuilder.ofStream(streamProvider, noUpdate);
    }

    public NetworkDataframeMapperBuilder<T> addProperties() {
        addProperties = true;
        return this;
    }

    @Override
    public NetworkDataframeMapper build() {
        return new AbstractNetworkDataframeMapper<T>(series, addProperties) {
            @Override
            protected List<T> getItems(Network network, DataframeContext dataframeContext) {
                return itemsProvider.apply(network, dataframeContext);
            }

            @Override
            protected T getItem(Network network, UpdatingDataframe updatingDataframe, int index, DataframeContext dataframeContext) {
                return itemMultiIndexGetter.getItem(network, updatingDataframe, index, dataframeContext);
            }
        };
    }
}
