/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public abstract class AbstractDataframeMapper<T> implements DataframeMapper {

    protected final Map<String, SeriesMapper<T>> seriesMappers;
    private final boolean addProperties;

    public AbstractDataframeMapper(List<SeriesMapper<T>> seriesMappers, boolean addProperties) {
        this.seriesMappers = seriesMappers.stream()
            .collect(toImmutableMap(mapper -> mapper.getMetadata().getName(), Function.identity()));
        this.addProperties = addProperties;
    }

    @Override
    public List<SeriesMetadata> getSeriesMetadata() {
        return seriesMappers.values().stream().map(SeriesMapper::getMetadata).collect(Collectors.toList());
    }

    @Override
    public SeriesMetadata getSeriesMetadata(String seriesName) {
        SeriesMapper<T> mapper = seriesMappers.get(seriesName);
        if (mapper == null) {
            throw new PowsyblException("No series named " + seriesName);
        }
        return mapper.getMetadata();
    }

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

    @Override
    public void updateDoubleSeries(Network network, String seriesName, DoubleIndexedSeries values) {
        SeriesMapper<T> series = seriesMappers.get(seriesName);
        for (int i = 0; i < values.getSize(); i++) {
            series.updateDouble(getItem(network, values.getId(i)), values.getValue(i));
        }
    }

    @Override
    public void updateIntSeries(Network network, String seriesName, IntIndexedSeries values) {
        SeriesMapper<T> series = seriesMappers.get(seriesName);
        for (int i = 0; i < values.getSize(); i++) {
            series.updateInt(getItem(network, values.getId(i)), values.getValue(i));
        }
    }

    @Override
    public void updateStringSeries(Network network, String seriesName, IndexedSeries<String> values) {
        SeriesMapper<T> series = seriesMappers.get(seriesName);
        for (int i = 0; i < values.getSize(); i++) {
            series.updateString(getItem(network, values.getId(i)), values.getValue(i));
        }
    }

    protected abstract List<T> getItems(Network network);

    protected abstract T getItem(Network network, String id);

}
