package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;

import java.util.List;

/**
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public abstract class AbstractSimpleAdder implements NetworkElementAdder {
    @Override
    public void addElement(Network network, List<UpdatingDataframe> dataframes, int index) {
        if (dataframes.size() != 1) {
            throw new IllegalArgumentException("Expected only one input dataframe");
        }
        UpdatingDataframe dataframe = dataframes.get(0);
        addElement(network, dataframe, index);
    }

    protected abstract void addElement(Network network, UpdatingDataframe dataframe, int index);
}
