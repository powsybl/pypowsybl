/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe;

import com.powsybl.dataframe.network.DataframeContext;

import java.util.List;
import java.util.function.ToDoubleBiFunction;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class DoubleSeriesMapper<T> implements SeriesMapper<T> {

    private final SeriesMetadata metadata;
    private final DoubleUpdater<T> updater;
    private final DoubleSimpleUpdater<T> simpleUpdater;
    private final ToDoubleBiFunction<T, DataframeContext> value;

    @FunctionalInterface
    public interface DoubleUpdater<U> {
        void update(U object, double value, DataframeContext context);
    }

    @FunctionalInterface
    public interface DoubleSimpleUpdater<U> {
        void update(U object, double value);
    }

    public DoubleSeriesMapper(String name, ToDoubleBiFunction<T, DataframeContext> value) {
        this(name, value, null, null, true);
    }

    public DoubleSeriesMapper(String name, ToDoubleBiFunction<T, DataframeContext> value, DoubleSimpleUpdater<T> simpleUpdater, boolean defaultAttribute) {
        this(name, value, null, simpleUpdater, defaultAttribute);
    }

    public DoubleSeriesMapper(String name, ToDoubleBiFunction<T, DataframeContext> value, DoubleUpdater<T> updater, boolean defaultAttribute) {
        this(name, value, updater, null, defaultAttribute);
    }

    public DoubleSeriesMapper(String name, ToDoubleBiFunction<T, DataframeContext> value, DoubleUpdater<T> updater, DoubleSimpleUpdater<T> simpleUpdater, boolean defaultAttribute) {
        this.metadata = new SeriesMetadata(false, name, updater != null, SeriesDataType.DOUBLE, defaultAttribute);
        this.updater = updater;
        this.simpleUpdater = simpleUpdater;
        this.value = value;
    }

    @Override
    public SeriesMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void createSeries(List<T> items, DataframeHandler factory, DataframeContext dataframeContext) {

        DataframeHandler.DoubleSeriesWriter writer = factory.newDoubleSeries(metadata.getName(), items.size());
        for (int i = 0; i < items.size(); i++) {
            writer.set(i, value.applyAsDouble(items.get(i), dataframeContext));
        }
    }

    @Override
    public void updateDouble(T object, double value, DataframeContext context) {
        if (updater == null && simpleUpdater == null) {
            throw new UnsupportedOperationException("Series '" + getMetadata().getName() + "' is not modifiable.");
        }
        if (updater == null) {
            simpleUpdater.update(object, value);
        }
        if (simpleUpdater == null) {
            updater.update(object, value, context);
        }
    }
}
