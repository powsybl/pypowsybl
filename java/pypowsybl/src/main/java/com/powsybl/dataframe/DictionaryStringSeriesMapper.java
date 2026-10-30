/**
 * Copyright (c) 2026, Elia Group (http://www.eliagroup.eu)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * String series transferred dictionary-encoded (codes and distinct values), for columns and indexes with many
 * repeated values; it is read as a plain string column or index.
 *
 * @param <T> the type of the mapped objects
 * @param <C> the type of the context passed to the series creation
 * @author Nico Westerbeck {@literal <nico.westerbeck at 50hertz.com>}
 */
public class DictionaryStringSeriesMapper<T, C> implements SeriesMapper<T, C> {

    private final SeriesMetadata metadata;
    private final BiConsumer<T, String> updater;
    private final Function<T, String> value;

    /**
     * @param name             the name of the series
     * @param index            whether the series is an index level rather than a column
     * @param value            the value of the series for an object
     * @param updater          the update of an object from a value, or null when the series is not modifiable
     * @param defaultAttribute whether the series belongs to the default attributes of the dataframe
     */
    public DictionaryStringSeriesMapper(String name, boolean index, Function<T, String> value,
                                        BiConsumer<T, String> updater, boolean defaultAttribute) {
        this.metadata = new SeriesMetadata(index, name, updater != null, SeriesDataType.STRING, defaultAttribute);
        this.updater = updater;
        this.value = value;
    }

    @Override
    public SeriesMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void createSeries(List<T> items, DataframeHandler handler, C context) {
        String name = metadata.getName();
        DataframeHandler.DictionaryStringSeriesWriter writer = metadata.isIndex()
                ? handler.newDictionaryStringIndex(name, items.size()) : handler.newDictionaryStringSeries(name, items.size());
        StringDictionary dictionary = new StringDictionary();
        for (int i = 0; i < items.size(); i++) {
            writer.set(i, dictionary.code(value.apply(items.get(i))));
        }
        writer.setDictionary(dictionary.getValues());
    }

    @Override
    public void updateString(T object, String value) {
        if (updater == null) {
            throw new UnsupportedOperationException("Series '" + getMetadata().getName() + "' is not modifiable.");
        }
        updater.accept(object, value);
    }
}
