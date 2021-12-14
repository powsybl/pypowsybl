/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network;

import com.powsybl.dataframe.*;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        List<T> items = getItems(network);
        List<SeriesMapper<T>> mappers = new ArrayList<>(getSeriesMappers(dataframeFilter));
        if (addProperties) {
            mappers.addAll(getPropertiesSeries(items));
        }
        dataframeHandler.allocate(mappers.size());
        mappers.stream().forEach(mapper -> mapper.createSeries(items, dataframeHandler));
    }

    private List<SeriesMapper<T>> getPropertiesSeries(List<T> items) {
        Stream<String> propertyNames = items.stream()
            .map(Identifiable.class::cast)
            .filter(Identifiable::hasProperty)
            .flatMap(e -> e.getPropertyNames().stream())
            .distinct();
        return propertyNames
            .map(property -> new StringSeriesMapper<T>(property, t -> ((Identifiable) t).getProperty(property)))
            .collect(Collectors.toList());
    }
}
