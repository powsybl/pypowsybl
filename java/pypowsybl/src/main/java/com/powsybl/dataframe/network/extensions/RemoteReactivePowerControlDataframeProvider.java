/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
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
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.extensions.RemoteReactivePowerControl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.powsybl.dataframe.network.extensions.RemoteReactivePowerControlDataframeAdder.getTerminalSideStr;

@AutoService(NetworkExtensionDataframeProvider.class)
public class RemoteReactivePowerControlDataframeProvider extends AbstractSingleDataframeNetworkExtension {

    @Override
    public String getExtensionName() {
        return RemoteReactivePowerControl.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(RemoteReactivePowerControl.NAME,
                "it allows to control the reactive power at a remote terminal from a generator",
                "index : id (str), target_q (float), regulated_element_id (str), regulated_side (str), enabled (bool)");
    }

    private Stream<RemoteReactivePowerControl> itemsStream(Network network) {
        return network.getGeneratorStream().filter(Objects::nonNull)
                .map(RemoteReactivePowerControlDataframeProvider::getExtension)
                .filter(Objects::nonNull);
    }

    private static RemoteReactivePowerControl getExtension(Generator generator) {
        return generator.getExtension(RemoteReactivePowerControl.class);
    }

    private RemoteReactivePowerControl getOrThrow(Network network, String id) {
        Generator generator = network.getGenerator(id);
        if (generator == null) {
            throw new PowsyblException("Invalid generator id : could not find " + id);
        }
        RemoteReactivePowerControl extension = generator.getExtension(RemoteReactivePowerControl.class);
        if (extension == null) {
            throw new PowsyblException("Generator '" + id + "' has no RemoteReactivePowerControl extension");
        }
        return extension;
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", rrpc -> rrpc.getExtendable().getId())
                .doubles("target_q", (rrpc, context) -> rrpc.getTargetQ(),
                        (rrpc, targetQ, context) -> rrpc.setTargetQ(targetQ))
                .strings("regulated_element_id", this::getRegulatedElementId, this::setRegulatedTerminal)
                .strings("regulated_side", rrpc -> getTerminalSideStr(rrpc.getRegulatingTerminal()))
                .booleans("enabled", RemoteReactivePowerControl::isEnabled, RemoteReactivePowerControl::setEnabled)
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(network::getGenerator)
                .filter(Objects::nonNull)
                .forEach(generator -> generator.removeExtension(RemoteReactivePowerControl.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new RemoteReactivePowerControlDataframeAdder();
    }

    private String getRegulatedElementId(RemoteReactivePowerControl rrpc) {
        Terminal terminal = rrpc.getRegulatingTerminal();
        return terminal != null && terminal.getConnectable() != null ? terminal.getConnectable().getId() : null;
    }

    private void setRegulatedTerminal(RemoteReactivePowerControl rrpc, String elementId) {
        Generator generator = rrpc.getExtendable();
        Network network = generator.getNetwork();
        Terminal terminal = RemoteReactivePowerControlDataframeAdder.getTerminal(network, elementId,
                getTerminalSideStr(rrpc.getRegulatingTerminal()), generator.getId());
        rrpc.setRegulatingTerminal(terminal);
    }
}
