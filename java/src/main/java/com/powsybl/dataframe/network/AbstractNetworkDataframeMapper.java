/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network;

import com.powsybl.dataframe.*;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Triple;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public abstract class AbstractNetworkDataframeMapper<T> extends AbstractDataframeMapper<Network, T> implements NetworkDataframeMapper {

    private final boolean addProperties;

    public AbstractNetworkDataframeMapper(List<SeriesMapper<T>> seriesMappers, boolean addProperties) {
        super(seriesMappers);
        this.addProperties = addProperties;
    }

    @Override
    public void createDataframe(Network network, DataframeHandler dataframeHandler, DataframeFilter dataframeFilter) {
        List<T> items = getFilteredItems(network, dataframeFilter);
        List<SeriesMapper<T>> mappers = new ArrayList<>(getSeriesMappers(dataframeFilter));
        if (addProperties) {
            mappers.addAll(getPropertiesSeries(items, dataframeFilter));
        }
        dataframeHandler.allocate(mappers.size());
        mappers.stream().forEach(mapper -> mapper.createSeries(items, dataframeHandler));
    }

    protected List<T> getFilteredItems(Network network, DataframeFilter dataframeFilter) {
        if (dataframeFilter.getRowsIds().size() == 0) {
            return getItems(network);
        }
        return getItems(network).stream().filter(item -> filterItem(item, dataframeFilter)).collect(Collectors.toList());
    }

    protected boolean filterItem(T item, DataframeFilter dataframeFilter) {
        if (item instanceof Triple) {
            return filterItem((String) ((Triple) item).getLeft(), (int) ((Triple) item).getRight(), dataframeFilter);
        }
        return dataframeFilter.getRowsIds().contains(((Identifiable) item).getId());
    }

    protected boolean filterItem(String id, int subId, DataframeFilter dataframeFilter) {
        int[] idIndexes = IntStream.range(0, dataframeFilter.getRowsIds().size())
                                   .filter(i -> dataframeFilter.getRowsIds().get(i).equals(id))
                                   .toArray();
        if (idIndexes.length == 0) {
            return false;
        }
        return Arrays.stream(idIndexes)
                     .mapToObj(i -> dataframeFilter.getRowsSubIds().get(i))
                     .collect(Collectors.toList())
                     .contains(subId);
    }

    private List<SeriesMapper<T>> getPropertiesSeries(List<T> items, DataframeFilter dataframeFilter) {
        Stream<String> propertyNames = items.stream()
            .map(Identifiable.class::cast)
            .filter(Identifiable::hasProperty)
            .flatMap(e -> e.getPropertyNames().stream())
            .distinct();
        return propertyNames
            .map(property -> new StringSeriesMapper<T>(property, t -> ((Identifiable) t).getProperty(property), false))
            .filter(mapper -> filterMapper(mapper, dataframeFilter))
            .collect(Collectors.toList());
    }
}
