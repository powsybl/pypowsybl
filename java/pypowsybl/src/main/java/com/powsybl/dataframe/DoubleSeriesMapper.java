/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe;

import java.util.List;
import java.util.function.ToDoubleBiFunction;

/**
 * @author Sylvain Leclerc {@literal <sylvain.leclerc at rte-france.com>}
 */
public class DoubleSeriesMapper<T, C> implements SeriesMapper<T, C> {

    private final SeriesMetadata metadata;
    private final DoubleUpdater<T, C> updater;
    private final ToDoubleBiFunction<T, C> value;

    @FunctionalInterface
    public interface DoubleUpdater<U, C> {
        void update(U object, double value, C context);
    }

    public DoubleSeriesMapper(String name, ToDoubleBiFunction<T, C> value) {
        this(name, value, null, true);
    }

    public DoubleSeriesMapper(String name, ToDoubleBiFunction<T, C> value, DoubleUpdater<T, C> updater, boolean defaultAttribute) {
        this.metadata = new SeriesMetadata(false, name, updater != null, SeriesDataType.DOUBLE, defaultAttribute);
        this.updater = updater;
        this.value = value;
    }

    @Override
    public SeriesMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void createSeries(List<T> items, DataframeHandler factory, C context) {

        DataframeHandler.DoubleSeriesWriter writer = factory.newDoubleSeries(metadata.getName(), items.size());
        for (int i = 0; i < items.size(); i++) {
            writer.set(i, value.applyAsDouble(items.get(i), context));
        }
    }

    @Override
    public void updateDouble(T object, double value, C context) {
        if (updater == null) {
            throw new UnsupportedOperationException("Series '" + getMetadata().getName() + "' is not modifiable.");
        }
        updater.update(object, value, context);
    }
}
