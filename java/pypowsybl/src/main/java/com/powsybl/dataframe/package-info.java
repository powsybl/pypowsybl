/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

/**
 * Provides mapping between java classes and dataframe (=table) based views.
 *
 * Mapping is defined by instances of {@link com.powsybl.dataframe.DataframeMapper}.
 *
 * {@link com.powsybl.dataframe.DataframeHandler} interface allows to define what to do with the data,
 * and {@link com.powsybl.dataframe.IndexedSeries} and its primitive variants allow to provide
 * some input series to update the data.
 *
 * <h2>Dictionary-encoded string series</h2>
 *
 * <p>A string series normally crosses the native boundary as one string per row: each value is copied to native
 * memory, then to a C++ string, then to a Python string, and index levels are factorized once more by pandas.
 * For a series whose values repeat a lot (a contingency id repeated for every monitored element, an element id
 * repeated for every contingency), this copies a lot of duplicated strings.</p>
 *
 * <p>Such a series can instead be transferred <em>dictionary-encoded</em>: an int code per row, plus the list of
 * the distinct values (the <em>dictionary</em>) that the codes refer to, so that only ints are copied repeatedly.
 * The encoding is a transport optimization only: the dataframe read on the Python side is the same as for a plain
 * string series. The code {@code -1} encodes a missing value. The dictionary is transferred in the order in which
 * the producer registered the values.</p>
 *
 * <p>It is opted into per column by the code writing a table:</p>
 * <ul>
 *   <li>through the mapper builders, with
 *       {@link com.powsybl.dataframe.BaseDataframeMapperBuilder#dictionaryStrings(String, java.util.function.Function)}
 *       and
 *       {@link com.powsybl.dataframe.BaseDataframeMapperBuilder#dictionaryStringsIndex(String, java.util.function.Function)}
 *       in place of {@code strings} and {@code stringsIndex}; the values are encoded by a
 *       {@link com.powsybl.dataframe.StringDictionary} (a null value is encoded like an empty string, which is also
 *       what a null value of a plain string series is read as);</li>
 *   <li>directly on the handler, with {@link com.powsybl.dataframe.DataframeHandler#newDictionaryStringIndex(String, int)}
 *       and {@link com.powsybl.dataframe.DataframeHandler#newDictionaryStringSeries(String, int)}: the returned
 *       {@link com.powsybl.dataframe.DataframeHandler.DictionaryStringSeriesWriter} takes one code per row and,
 *       once all codes are written, the dictionary. This lets a producer build the dictionary while it writes, with
 *       its own encoding strategy.</li>
 * </ul>
 *
 * @author Sylvain Leclerc {@literal <sylvain.leclerc at rte-france.com>}
 */
package com.powsybl.dataframe;
