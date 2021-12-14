/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.update.UpdatingDataframe;

import java.util.Collection;
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
    public void createDataframe(T object, DataframeHandler dataframeHandler, DataframeFilter dataframeFilter) {
        Collection<SeriesMapper<U>> mappers = getSeriesMappers(dataframeFilter);
        dataframeHandler.allocate(mappers.size());
        List<U> items = getItems(object);
        mappers.stream().forEach(mapper -> mapper.createSeries(items, dataframeHandler));
    }

    @Override
    public void updateSeries(T object, UpdatingDataframe updatingDataframe) {
        for (int i = 0; i < updatingDataframe.getLineCount(); i++) {
            for (SeriesMetadata column : updatingDataframe.getSeriesMetadata()) {
                if (!column.isIndex()) {
                    String seriesName = column.getName();
                    SeriesMapper<U> series = seriesMappers.get(seriesName);
                    switch (column.getType()) {
                        case STRING:
                            series.updateString(getItem(object, updatingDataframe, i),
                                    updatingDataframe.getStringValue(seriesName, i));
                            break;
                        case DOUBLE:
                            series.updateDouble(getItem(object, updatingDataframe, i),
                                    updatingDataframe.getDoubleValue(seriesName, i));
                            break;
                        case INT:
                            series.updateInt(getItem(object, updatingDataframe, i),
                                    updatingDataframe.getIntValue(seriesName, i));
                            break;
                        default:
                            throw new IllegalStateException("Unexpected series type for update: " + column.getType());
                    }
                }
            }
        }
    }

    public Collection<SeriesMapper<U>> getSeriesMappers(DataframeFilter dataframeFilter) {
        Collection<SeriesMapper<U>> mappers = seriesMappers.values();
        return mappers.stream()
                      .filter(mapper -> filterMapper(mapper, dataframeFilter))
                      .collect(Collectors.toList());
    }

    protected boolean filterMapper(SeriesMapper<U> mapper, DataframeFilter dataframeFilter) {
        switch (dataframeFilter.getAttributeFilterType()) {
            case DEFAULT_ATTRIBUTES:
                return mapper.getMetadata().isDefaultAttribute() || mapper.getMetadata().isIndex();
            case INPUT_ATTRIBUTES:
                return dataframeFilter.getInputAttributes().contains(mapper.getMetadata().getName()) || mapper.getMetadata().isIndex();
            case ALL_ATTRIBUTES:
                return true;
            default:
                throw new IllegalStateException("Unexpected attribute filter type: " + dataframeFilter.getAttributeFilterType());
        }
    }

    protected abstract List<U> getItems(T object);

    protected abstract U getItem(T object, UpdatingDataframe dataframe, int index);
}
