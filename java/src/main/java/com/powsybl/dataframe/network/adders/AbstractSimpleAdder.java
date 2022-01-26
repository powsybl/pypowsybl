/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
