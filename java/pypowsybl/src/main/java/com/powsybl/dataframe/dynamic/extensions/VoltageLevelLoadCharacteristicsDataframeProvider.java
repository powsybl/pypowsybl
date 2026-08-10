/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.network.ExtensionInformation;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframeMapperBuilder;
import com.powsybl.dataframe.network.adders.NetworkElementAdder;
import com.powsybl.dataframe.network.adders.NetworkUtils;
import com.powsybl.dataframe.network.extensions.AbstractSingleDataframeNetworkExtension;
import com.powsybl.dataframe.network.extensions.NetworkExtensionDataframeProvider;
import com.powsybl.dynawo.extensions.api.voltage.VoltageLevelLoadCharacteristics;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class VoltageLevelLoadCharacteristicsDataframeProvider extends AbstractSingleDataframeNetworkExtension {

    @Override
    public String getExtensionName() {
        return VoltageLevelLoadCharacteristics.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(VoltageLevelLoadCharacteristics.NAME,
                "Provides information, for dynamic simulation only, about the type of load connected to a voltage level",
                "index : id (str), " +
                        "characteristic (str)");
    }

    private Stream<VoltageLevelLoadCharacteristics> itemsStream(Network network) {
        return network.getVoltageLevelStream()
                .map(vl -> (VoltageLevelLoadCharacteristics) vl.getExtension(VoltageLevelLoadCharacteristics.class))
                .filter(Objects::nonNull);
    }

    private VoltageLevelLoadCharacteristics getOrThrow(Network network, String id) {
        VoltageLevel voltageLevel = NetworkUtils.getVoltageLevelOrThrow(network, id);
        VoltageLevelLoadCharacteristics vllc = voltageLevel.getExtension(VoltageLevelLoadCharacteristics.class);
        if (vllc == null) {
            throw new PowsyblException("Voltage level '" + id + "' has no VoltageLevelLoadCharacteristics extension");
        }
        return vllc;
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", ext -> ext.getExtendable().getId())
                .enums("characteristic", VoltageLevelLoadCharacteristics.Type.class,
                        VoltageLevelLoadCharacteristics::getCharacteristic,
                        VoltageLevelLoadCharacteristics::setCharacteristic)
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(network::getVoltageLevel)
                .filter(Objects::nonNull)
                .forEach(vl -> vl.removeExtension(VoltageLevelLoadCharacteristics.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new VoltageLevelLoadCharacteristicsDataframeAdder();
    }

}
