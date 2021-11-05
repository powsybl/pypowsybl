package com.powsybl.dataframe.network;

import com.powsybl.dataframe.DataframeElementType;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
public interface NetworkExtensionSeriesProvider {
    String getExtensionName();

    DataframeElementType getElementType();

    void addSeries(NetworkDataframeMapperBuilder builder);
}
