/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * String series whose empty values are reported as missing values (None in python) rather than empty strings.
 *
 * @author Nico Westerbeck {@literal <nico.westerbeck at 50hertz.com>}
 */
public class OptionalStringSeriesMapper<T, C> implements SeriesMapper<T, C> {

    private final SeriesMetadata metadata;
    private final Function<T, Optional<String>> value;

    public OptionalStringSeriesMapper(String name, Function<T, Optional<String>> value) {
        this(name, value, true);
    }

    public OptionalStringSeriesMapper(String name, Function<T, Optional<String>> value, boolean defaultAttribute) {
        this.metadata = new SeriesMetadata(false, name, false, SeriesDataType.STRING, defaultAttribute);
        this.value = value;
    }

    @Override
    public SeriesMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void createSeries(List<T> items, DataframeHandler handler, C context) {
        String name = metadata.getName();
        DataframeHandler.OptionalStringSeriesWriter writer = handler.newOptionalStringSeries(name, items.size());
        for (int i = 0; i < items.size(); i++) {
            writer.set(i, value.apply(items.get(i)));
        }
    }
}
