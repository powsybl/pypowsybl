package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public interface NetworkElementAdder {

    /**
     * Get the list of metadata: one list of columns metadata for each input dataframe.
     */
    List<List<SeriesMetadata>> getMetadata();

    /**
     * Adds an element to the network, based on a list of dataframes.
     * The first dataframe is considered the "primary" dataframe, other dataframes
     * can provide additional data (think steps for the tap changers).
     */
    void addElement(Network network, List<UpdatingDataframe> dataframes, int index);
}
