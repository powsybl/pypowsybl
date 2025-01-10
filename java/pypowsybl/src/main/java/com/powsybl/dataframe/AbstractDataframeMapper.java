/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableMap.toImmutableMap;

/**
 * @author Sylvain Leclerc {@literal <sylvain.leclerc at rte-france.com>}
 */
public abstract class AbstractDataframeMapper<T, U, C> implements DataframeMapper<T, C> {

    protected final Map<String, SeriesMapper<U, C>> seriesMappers;

    protected AbstractDataframeMapper(List<SeriesMapper<U, C>> seriesMappers) {
        this.seriesMappers = seriesMappers.stream()
            .collect(toImmutableMap(mapper -> mapper.getMetadata().getName(), Function.identity()));
    }

    @Override
    public List<SeriesMetadata> getSeriesMetadata() {
        return seriesMappers.values().stream().map(SeriesMapper::getMetadata).collect(Collectors.toList());
    }

    @Override
    public SeriesMetadata getSeriesMetadata(String seriesName) {
        SeriesMapper<U, C> mapper = seriesMappers.get(seriesName);
        if (mapper == null) {
            throw new PowsyblException("No series named " + seriesName);
        }
        return mapper.getMetadata();
    }

    @Override
    public void createDataframe(T object, DataframeHandler dataframeHandler, DataframeFilter dataframeFilter, C context) {
        Collection<SeriesMapper<U, C>> mappers = getSeriesMappers(dataframeFilter);
        dataframeHandler.allocate(mappers.size());
        List<U> items = getItems(object, context);
        mappers.forEach(mapper -> mapper.createSeries(items, dataframeHandler, context));
    }

    @Override
    public void createDataframe(T object, DataframeHandler dataframeHandler, DataframeFilter dataframeFilter) {
        createDataframe(object, dataframeHandler, dataframeFilter, null);
    }

    interface ColumnUpdater<U, C> {

        void update(int index, U object, C context);
    }

    private static final class IntColumnUpdater<U, C> implements ColumnUpdater<U, C> {
        private final IntSeries values;
        private final SeriesMapper<U, C> mapper;

        private IntColumnUpdater(IntSeries values, SeriesMapper<U, C> mapper) {
            this.values = values;
            this.mapper = mapper;
        }

        @Override
        public void update(int index, U object, C context) {
            mapper.updateInt(object, values.get(index));
        }
    }

    private static final class DoubleColumnUpdater<U, C> implements ColumnUpdater<U, C> {
        private final DoubleSeries values;
        private final SeriesMapper<U, C> mapper;

        private DoubleColumnUpdater(DoubleSeries values, SeriesMapper<U, C> mapper) {
            this.values = values;
            this.mapper = mapper;
        }

        @Override
        public void update(int index, U object, C context) {
            mapper.updateDouble(object, values.get(index), context);
        }
    }

    private static final class StringColumnUpdater<U, C> implements ColumnUpdater<U, C> {
        private final StringSeries values;
        private final SeriesMapper<U, C> mapper;

        private StringColumnUpdater(StringSeries values, SeriesMapper<U, C> mapper) {
            this.values = values;
            this.mapper = mapper;
        }

        @Override
        public void update(int index, U object, C context) {
            mapper.updateString(object, values.get(index));
        }
    }

    @Override
    public void updateSeries(T object, UpdatingDataframe updatingDataframe, C context) {

        //Setup links to minimize searches on column names
        List<ColumnUpdater<U, C>> updaters = new ArrayList<>();
        for (SeriesMetadata column : updatingDataframe.getSeriesMetadata()) {
            if (column.isIndex()) {
                continue;
            }
            String seriesName = column.getName();
            SeriesMapper<U, C> mapper = seriesMappers.get(seriesName);
            ColumnUpdater<U, C> updater = switch (column.getType()) {
                case STRING -> new StringColumnUpdater<>(updatingDataframe.getStrings(seriesName), mapper);
                case DOUBLE -> new DoubleColumnUpdater<>(updatingDataframe.getDoubles(seriesName), mapper);
                case INT -> new IntColumnUpdater<>(updatingDataframe.getInts(seriesName), mapper);
                default -> throw new IllegalStateException("Unexpected series type for update: " + column.getType());
            };
            updaters.add(updater);
        }

        for (int i = 0; i < updatingDataframe.getRowCount(); i++) {
            U item = getItem(object, updatingDataframe, i, context);
            int itemIndex = i;
            updaters.forEach(updater -> updater.update(itemIndex, item, context));
        }
    }

    @Override
    public void updateSeries(T object, UpdatingDataframe updatingDataframe) {
        updateSeries(object, updatingDataframe, null);
    }

    @Override
    public boolean isSeriesMetaDataExists(String seriesName) {
        return seriesMappers.containsKey(seriesName);
    }

    public Collection<SeriesMapper<U, C>> getSeriesMappers(DataframeFilter dataframeFilter) {
        Collection<SeriesMapper<U, C>> mappers = seriesMappers.values();
        return mappers.stream()
            .filter(mapper -> filterMapper(mapper, dataframeFilter))
            .collect(Collectors.toList());
    }

    protected boolean filterMapper(SeriesMapper<U, C> mapper, DataframeFilter dataframeFilter) {
        return switch (dataframeFilter.getAttributeFilterType()) {
            case DEFAULT_ATTRIBUTES -> mapper.getMetadata().isDefaultAttribute() || mapper.getMetadata().isIndex();
            case INPUT_ATTRIBUTES ->
                dataframeFilter.getInputAttributes().contains(mapper.getMetadata().getName()) || mapper.getMetadata().isIndex();
            case ALL_ATTRIBUTES -> true;
        };
    }

    protected abstract List<U> getItems(T object, C context);

    protected abstract U getItem(T object, UpdatingDataframe dataframe, int index, C context);
}
