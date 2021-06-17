/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe;

import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * Provides methods to map an object's data to/from dataframes.
 *
 * The dataframe data can be read by a {@link DataframeHandler},
 * and provided by variants of "indexed series".
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public interface DataframeMapper {

    /**
     * Provides dataframe data to the handler, which is responsible to
     * format it as needed.
     */
    void createDataframe(Network network, DataframeHandler dataframeHandler);

    List<SeriesMetadata> getSeriesMetadata();

    SeriesMetadata getSeriesMetadata(String seriesName);

    /**
     * Updates network data with the provided series.
     */
    void updateDoubleSeries(Network network, String seriesName, DoubleIndexedSeries values);

    /**
     * Updates network data with the provided series.
     */
    void updateIntSeries(Network network, String seriesName, IntIndexedSeries values);

    /**
     * Updates network data with the provided series.
     */
    void updateStringSeries(Network network, String seriesName, IndexedSeries<String> values);
}
