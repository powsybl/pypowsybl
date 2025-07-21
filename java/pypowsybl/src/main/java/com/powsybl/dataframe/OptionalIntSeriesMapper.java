/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Function;

/**
 * @author Hugo Kulesza {@literal <hugo.kulesza at rte-france.com>}
 */
public class OptionalIntSeriesMapper<T, C> implements SeriesMapper<T, C> {

    private final SeriesMetadata metadata;
    private final Function<T, OptionalInt> value;

    public OptionalIntSeriesMapper(String name, Function<T, OptionalInt> value) {
        this(name, value, true);
    }

    public OptionalIntSeriesMapper(String name, Function<T, OptionalInt> value, boolean defaultAttribute) {
        this.metadata = new SeriesMetadata(false, name, false, SeriesDataType.INT, defaultAttribute);
        this.value = value;
    }

    @Override
    public SeriesMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void createSeries(List<T> items, DataframeHandler handler, C context) {
        String name = metadata.getName();
        DataframeHandler.OptionalIntSeriesWriter writer = handler.newOptionalIntSeries(name, items.size());
        for (int i = 0; i < items.size(); i++) {
            writer.set(i, value.apply(items.get(i)));
        }
    }
}
