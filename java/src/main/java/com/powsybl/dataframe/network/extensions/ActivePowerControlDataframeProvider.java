/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframeMapperBuilder;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.ActivePowerControl;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class ActivePowerControlDataframeProvider implements NetworkExtensionDataframeProvider {

    @Override
    public String getExtensionName() {
        return ActivePowerControl.NAME;
    }

    private Stream<ActivePowerControl> itemsStream(Network network) {
        return network.getGeneratorStream()
                .map(g -> (ActivePowerControl) g.getExtension(ActivePowerControl.class))
                .filter(Objects::nonNull);
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream)
                .stringsIndex("id", ext -> ((Identifiable) ext.getExtendable()).getId())
                .doubles("droop", ActivePowerControl::getDroop)
                .booleans("participate", ActivePowerControl::isParticipate)
                .build();
    }
}
