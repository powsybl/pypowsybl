/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.BranchAdder;

/**
 * Common series for all branches.
 *
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
class BranchSeries extends IdentifiableSeries {

    protected final StringSeries voltageLevels1;
    protected final StringSeries connectableBuses1;
    protected final StringSeries buses1;
    protected final IntSeries nodes1;
    protected final StringSeries voltageLevels2;
    protected final StringSeries connectableBuses2;
    protected final StringSeries buses2;
    protected final IntSeries nodes2;

    BranchSeries(UpdatingDataframe dataframe) {
        super(dataframe);
        this.voltageLevels1 = dataframe.getStrings("voltage_level1_id");
        this.connectableBuses1 = dataframe.getStrings("connectable_bus1_id");
        this.buses1 = dataframe.getStrings("bus1_id");
        this.nodes1 = dataframe.getInts("node1");
        this.voltageLevels2 = dataframe.getStrings("voltage_level2_id");
        this.connectableBuses2 = dataframe.getStrings("connectable_bus2_id");
        this.buses2 = dataframe.getStrings("bus2_id");
        this.nodes2 = dataframe.getInts("node2");
    }

    protected void setBranchAttributes(BranchAdder<?> adder, int row) {
        setIdentifiableAttributes(adder, row);
        NetworkElementCreationUtils.applyIfPresent(voltageLevels1, row, adder::setVoltageLevel1);
        NetworkElementCreationUtils.applyIfPresent(connectableBuses1, row, adder::setConnectableBus1);
        NetworkElementCreationUtils.applyIfPresent(buses1, row, adder::setBus1);
        NetworkElementCreationUtils.applyIfPresent(nodes1, row, adder::setNode1);
        NetworkElementCreationUtils.applyIfPresent(voltageLevels2, row, adder::setVoltageLevel2);
        NetworkElementCreationUtils.applyIfPresent(connectableBuses2, row, adder::setConnectableBus2);
        NetworkElementCreationUtils.applyIfPresent(buses2, row, adder::setBus2);
        NetworkElementCreationUtils.applyIfPresent(nodes2, row, adder::setNode2);
    }

}
