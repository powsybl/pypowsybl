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
