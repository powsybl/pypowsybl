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
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.extensions.ReferenceTerminals;

import java.util.List;
import java.util.stream.Stream;

/**
 * This extension is attached to the network and holds a set of terminals, so the dataframe has one row per
 * reference terminal: the id of the element the terminal belongs to, and the side of that element.
 *
 * <p>Creating replaces the whole set rather than adding to it. The underlying adder only exposes
 * {@code withTerminals(Set)}, so the rows given to create describe the complete extension. There is nothing to
 * update on an individual row, a reference terminal being only an element and a side.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class ReferenceTerminalsDataframeProvider extends AbstractSingleDataframeNetworkExtension {

    @Override
    public String getExtensionName() {
        return ReferenceTerminals.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(ReferenceTerminals.NAME,
                "Defines the terminals used as angle references by a power flow calculation, for the whole network",
                "index : element_id (str), " +
                        "side (str)");
    }

    private Stream<Terminal> itemsStream(Network network) {
        ReferenceTerminals extension = network.getExtension(ReferenceTerminals.class);
        if (extension == null) {
            return Stream.empty();
        }
        return extension.getReferenceTerminals().stream();
    }

    private Terminal getOrThrow(Network network, String elementId) {
        throw new PowsyblException("Reference terminals of network '" + network.getId()
                + "' cannot be updated row by row, use create_extensions to set the whole set at once");
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("element_id", terminal -> terminal.getConnectable().getId())
                // an injection terminal has no side, only branches and three windings transformers do
                .strings("side", terminal -> Terminal.getConnectableSide(terminal).map(Enum::toString).orElse(""))
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        // the extension holds the reference terminals of the whole network, there is nothing to remove per element
        network.removeExtension(ReferenceTerminals.class);
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new ReferenceTerminalsDataframeAdder();
    }

}
