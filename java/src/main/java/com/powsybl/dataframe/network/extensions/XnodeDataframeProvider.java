/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframeMapperBuilder;
import com.powsybl.dataframe.network.adders.NetworkElementAdder;
import com.powsybl.dataframe.network.adders.XnodeDataframeAdder;
import com.powsybl.entsoe.util.Xnode;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class XnodeDataframeProvider implements NetworkExtensionDataframeProvider {

    @Override
    public String getExtensionName() {
        return Xnode.NAME;
    }

    private Stream<Xnode> itemsStream(Network network) {
        return network.getDanglingLineStream()
                .map(s -> (Xnode) s.getExtension(Xnode.class))
                .filter(Objects::nonNull);
    }

    private Xnode getOrThrow(Network network, String id) {
        DanglingLine line = network.getDanglingLine(id);
        if (line == null) {
            throw new PowsyblException("DanglingLine '" + id + "' not found");
        }
        Xnode xn = line.getExtension(Xnode.class);
        if (xn == null) {
            throw new PowsyblException("DanglingLine '" + id + "' has no Xnode extension");
        }
        return xn;
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", ext -> ext.getExtendable().getId())
                .strings("code", Xnode::getCode, Xnode::setCode)
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(id -> network.getDanglingLine(id))
                .filter(Objects::nonNull)
                .forEach(g -> g.removeExtension(Xnode.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new XnodeDataframeAdder();
    }
}
