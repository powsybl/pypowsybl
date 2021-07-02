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

import java.util.List;
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
    public void createDataframe(Network network, DataframeHandler dataframeHandler) {
        dataframeHandler.allocate(seriesMappers.size());
        List<T> items = getItems(network);
        seriesMappers.values().stream().forEach(mapper -> mapper.createSeries(items, dataframeHandler));
        if (addProperties) {
            addPropertiesSeries(items);
        }
    }

    private void addPropertiesSeries(List<T> items) {
        Stream<String> propertyNames = items.stream()
            .map(Identifiable.class::cast)
            .filter(Identifiable::hasProperty)
            .flatMap(e -> e.getPropertyNames().stream())
            .distinct();
        propertyNames.forEach(property -> {
            new StringSeriesMapper<T>(property, t -> ((Identifiable) t).getProperty(property));
        });
    }
}
