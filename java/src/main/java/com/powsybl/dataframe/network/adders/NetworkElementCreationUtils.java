/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.BranchAdder;
import com.powsybl.iidm.network.HvdcConverterStationAdder;
import com.powsybl.iidm.network.IdentifiableAdder;
import com.powsybl.iidm.network.InjectionAdder;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
public final class NetworkElementCreationUtils {

    public static void createIdentifiable(IdentifiableAdder adder, UpdatingDataframe dataframe, int indexElement) {
        dataframe.getStringValue("id", indexElement).ifPresent(adder::setId);
        dataframe.getStringValue("name", indexElement).ifPresent(adder::setName);
    }

    public static void createInjection(InjectionAdder adder, UpdatingDataframe dataframe, int indexElement) {
        createIdentifiable(adder, dataframe, indexElement);
        dataframe.getStringValue("connectable_bus_id", indexElement).ifPresent(adder::setConnectableBus);
        dataframe.getStringValue("bus_id", indexElement).ifPresent(adder::setBus);
        dataframe.getIntValue("node", indexElement).ifPresent(adder::setNode);
    }

    public static void createHvdc(HvdcConverterStationAdder adder, UpdatingDataframe dataframe, int indexElement) {
        createInjection(adder, dataframe, indexElement);
        dataframe.getDoubleValue("loss_factor", indexElement).ifPresent(lf -> adder.setLossFactor((float) lf));
    }

    public static void createBranch(BranchAdder adder, UpdatingDataframe dataframe, int indexElement) {
        createIdentifiable(adder, dataframe, indexElement);
        dataframe.getStringValue("bus1_id", indexElement).ifPresent(adder::setBus1);
        dataframe.getStringValue("bus2_id", indexElement).ifPresent(adder::setBus2);
        dataframe.getStringValue("voltage_level1_id", indexElement).ifPresent(adder::setVoltageLevel1);
        dataframe.getStringValue("voltage_level2_id", indexElement).ifPresent(adder::setVoltageLevel2);
        dataframe.getStringValue("connectable_bus1_id", indexElement).ifPresent(adder::setConnectableBus1);
        dataframe.getStringValue("connectable_bus2_id", indexElement).ifPresent(adder::setConnectableBus2);
        dataframe.getIntValue("node1", indexElement).ifPresent(adder::setNode1);
        dataframe.getIntValue("node2", indexElement).ifPresent(adder::setNode2);
    }

    private NetworkElementCreationUtils() {
    }
}
