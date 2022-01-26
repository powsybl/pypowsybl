/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.CreateEquipmentHelper;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;

import java.util.Collections;
import java.util.List;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public class SwitchDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.strings("voltage_level_id"),
            SeriesMetadata.strings("bus1_id"),
            SeriesMetadata.strings("bus2_id"),
            SeriesMetadata.ints("node1"),
            SeriesMetadata.ints("node2"),
            SeriesMetadata.strings("id"),
            SeriesMetadata.strings("name"),
            SeriesMetadata.strings("kind"),
            SeriesMetadata.booleans("open"),
            SeriesMetadata.booleans("retained"),
            SeriesMetadata.booleans("fictitious")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public void addElement(Network network, UpdatingDataframe dataframe, int indexElement) {
        VoltageLevel vl = network.getVoltageLevel(dataframe.getStringValue("voltage_level_id", indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing")));
        TopologyKind kind = vl.getTopologyKind();
        if (kind == TopologyKind.NODE_BREAKER) {
            VoltageLevel.NodeBreakerView.SwitchAdder adder = vl.getNodeBreakerView().newSwitch();
            CreateEquipmentHelper.createIdentifiable(adder, dataframe, indexElement);
            dataframe.getStringValue("kind", indexElement).ifPresent(adder::setKind);
            dataframe.getIntValue("node1", indexElement).ifPresent(adder::setNode1);
            dataframe.getIntValue("node2", indexElement).ifPresent(adder::setNode2);
            dataframe.getIntValue("open", indexElement).ifPresent(open -> adder.setOpen(open == 1));
            dataframe.getIntValue("retained", indexElement).ifPresent(retained -> adder.setRetained(retained == 1));
            dataframe.getIntValue("fictitious", indexElement).ifPresent(fictitious -> adder.setFictitious(fictitious == 1));
            adder.add();
        } else if (kind == TopologyKind.BUS_BREAKER) {
            VoltageLevel.BusBreakerView.SwitchAdder adder = vl.getBusBreakerView().newSwitch();
            CreateEquipmentHelper.createIdentifiable(adder, dataframe, indexElement);
            dataframe.getStringValue("bus1_id", indexElement).ifPresent(adder::setBus1);
            dataframe.getStringValue("bus2_id", indexElement).ifPresent(adder::setBus2);
            dataframe.getIntValue("open", indexElement).ifPresent(open -> adder.setOpen(open == 1));
            dataframe.getIntValue("fictitious", indexElement).ifPresent(fictitious -> adder.setFictitious(fictitious == 1));
            adder.add();
        }
    }
}
