/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.dataframe.network.ExtensionInformation;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframeMapperBuilder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.SecondaryVoltageControl;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Hugo Kulesza <hugo.kulesza@rte-france.com>
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class SecondaryVoltageControlDataframeProvider implements NetworkExtensionDataframeProvider {

    @Override
    public String getExtensionName() {
        return SecondaryVoltageControl.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(SecondaryVoltageControl.NAME,
                "Provides information about the secondary voltage control zones and units",
                "index : ");
    }

    private Stream<SecondaryVoltageControl> itemStream(Network network) {
        return network.getExtensions().stream()
                .filter(ext -> {
                    return ext.getClass() == SecondaryVoltageControl.class;
                })
                .map(ext -> (SecondaryVoltageControl) ext);
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemStream)
                .stringsIndex("zone_name", svc -> svc.getControlZones().get(0).getName())
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        network.removeExtension(SecondaryVoltageControl.class);
    }
}
