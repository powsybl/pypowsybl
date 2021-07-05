/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe;

import com.powsybl.commons.PowsyblException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public abstract class AbstractDataframeMapper<T, U> implements DataframeMapper<T> {

    protected final Map<String, SeriesMapper<U>> seriesMappers;

    public AbstractDataframeMapper(List<SeriesMapper<U>> seriesMappers) {
        this.seriesMappers = seriesMappers.stream()
            .collect(toImmutableMap(mapper -> mapper.getMetadata().getName(), Function.identity()));
    }

    @Override
    public List<SeriesMetadata> getSeriesMetadata() {
        return seriesMappers.values().stream().map(SeriesMapper::getMetadata).collect(Collectors.toList());
    }

    @Override
    public SeriesMetadata getSeriesMetadata(String seriesName) {
        SeriesMapper<U> mapper = seriesMappers.get(seriesName);
        if (mapper == null) {
            throw new PowsyblException("No series named " + seriesName);
        }
        return mapper.getMetadata();
    }

    @Override
    public boolean isSeriesMetaDataExists(String seriesName) {
        return seriesMappers.get(seriesName) != null;
    }

    public void createDataframe(T object, DataframeHandler dataframeHandler) {
        dataframeHandler.allocate(seriesMappers.size());
        List<U> items = getItems(object);
        seriesMappers.values().stream().forEach(mapper -> mapper.createSeries(items, dataframeHandler));
    }

    @Override
    public void updateDoubleSeries(T object, String seriesName, DoubleIndexedSeries values) {
        SeriesMapper<U> series = seriesMappers.get(seriesName);
        for (int i = 0; i < values.getSize(); i++) {
            series.updateDouble(getItem(object, values.getId(i)), values.getValue(i));
        }
    }

    @Override
    public void updateIntSeries(T object, String seriesName, IntIndexedSeries values) {
        SeriesMapper<U> series = seriesMappers.get(seriesName);
        for (int i = 0; i < values.getSize(); i++) {
            series.updateInt(getItem(object, values.getId(i)), values.getValue(i));
        }
    }

    @Override
    public void updateStringSeries(T object, String seriesName, IndexedSeries<String> values) {
        SeriesMapper<U> series = seriesMappers.get(seriesName);
        for (int i = 0; i < values.getSize(); i++) {
            series.updateString(getItem(object, values.getId(i)), values.getValue(i));
        }
    }

    protected abstract List<U> getItems(T object);

    protected abstract U getItem(T object, String id);

}
