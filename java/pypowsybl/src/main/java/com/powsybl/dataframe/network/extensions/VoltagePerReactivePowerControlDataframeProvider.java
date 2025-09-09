/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.network.ExtensionInformation;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframeMapperBuilder;
import com.powsybl.dataframe.network.adders.NetworkElementAdder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.extensions.VoltagePerReactivePowerControl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Hugo Kulesza {@literal <hugo.kulesza at rte-france.com>}
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class VoltagePerReactivePowerControlDataframeProvider extends AbstractSingleDataframeNetworkExtension {
    @Override
    public String getExtensionName() {
        return VoltagePerReactivePowerControl.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(VoltagePerReactivePowerControl.NAME,
                "Models the voltage control static var compensators",
                "index : id (str), slope (float)");
    }

    private Stream<VoltagePerReactivePowerControl> itemsStream(Network network) {
        return network.getStaticVarCompensatorStream()
                .map(svc -> (VoltagePerReactivePowerControl) svc.getExtension(VoltagePerReactivePowerControl.class))
                .filter(Objects::nonNull);
    }

    private VoltagePerReactivePowerControl getOrThrow(Network network, String id) {
        StaticVarCompensator svc = network.getStaticVarCompensator(id);
        if (svc == null) {
            throw new PowsyblException("Static var compensator '" + id + "' not found");
        }
        VoltagePerReactivePowerControl ext = svc.getExtension(VoltagePerReactivePowerControl.class);
        if (ext == null) {
            throw new PowsyblException("Static var compensator '" + id + "' has no VoltagePerReactivePowerControl extension");
        }
        return ext;
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", ext -> ext.getExtendable().getId())
                .doubles("slope", VoltagePerReactivePowerControl::getSlope, (ext, val, context) -> ext.setSlope(val))
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(network::getStaticVarCompensator)
                .filter(Objects::nonNull)
                .forEach(svc -> svc.removeExtension(VoltagePerReactivePowerControl.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new VoltagePerReactivePowerControlDataframeAdder();
    }

}
