/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network;

import com.powsybl.dataframe.*;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Sylvain Leclerc {@literal <sylvain.leclerc at rte-france.com>}
 */
public abstract class AbstractNetworkDataframeMapper<T> extends AbstractDataframeMapper<Network, T, NetworkDataframeContext> implements NetworkDataframeMapper {

    private final boolean addProperties;

    protected AbstractNetworkDataframeMapper(List<SeriesMapper<T, NetworkDataframeContext>> seriesMappers, boolean addProperties) {
        super(seriesMappers);
        this.addProperties = addProperties;
    }

    @Override
    public void createDataframe(Network network, DataframeHandler dataframeHandler, DataframeFilter dataframeFilter, NetworkDataframeContext context) {
        List<T> items = getFilteredItems(network, dataframeFilter, context);
        List<SeriesMapper<T, NetworkDataframeContext>> mappers = new ArrayList<>(getSeriesMappers(dataframeFilter));
        if (addProperties) {
            mappers.addAll(getPropertiesSeries(items, dataframeFilter));
        }
        dataframeHandler.allocate(mappers.size());
        mappers.forEach(mapper -> mapper.createSeries(items, dataframeHandler, context));
    }

    protected List<T> getFilteredItems(Network network, DataframeFilter dataframeFilter, NetworkDataframeContext context) {
        Optional<UpdatingDataframe> optionalUpdatingDataframe = dataframeFilter.getSelectingDataframe();
        if (optionalUpdatingDataframe.isEmpty()) {
            return getItems(network, context);
        } else {
            UpdatingDataframe selectedDataframe = optionalUpdatingDataframe.get();
            return IntStream.range(0, selectedDataframe.getRowCount()).mapToObj(i -> getItem(network, selectedDataframe, i, context)).collect(Collectors.toList());
        }
    }

    private List<SeriesMapper<T, NetworkDataframeContext>> getPropertiesSeries(List<T> items, DataframeFilter dataframeFilter) {
        Stream<String> propertyNames = items.stream()
            .map(Identifiable.class::cast)
            .filter(Identifiable::hasProperty)
            .flatMap(e -> e.getPropertyNames().stream())
            .distinct();
        return propertyNames
            .map(property -> new StringSeriesMapper<T, NetworkDataframeContext>(property, t -> ((Identifiable<?>) t).getProperty(property), false))
            .filter(mapper -> filterMapper(mapper, dataframeFilter))
            .collect(Collectors.toList());
    }
}
