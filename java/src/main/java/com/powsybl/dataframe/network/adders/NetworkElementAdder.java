package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public interface NetworkElementAdder {

    List<SeriesMetadata> getSeriesMetadata();

    void addElement(Network network, List<UpdatingDataframe> dataframes, int index);
}
