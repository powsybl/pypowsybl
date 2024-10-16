package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;

import java.util.List;

public class SelectedOperationalLimitsDataframeAdder implements NetworkElementAdder {
    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return List.of();
    }

    @Override
    public void addElements(Network network, List<UpdatingDataframe> dataframes) {

    }
}
