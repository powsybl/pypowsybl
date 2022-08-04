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
import com.powsybl.entsoe.util.MergedXnode;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class MergedXnodeDataframeProvider implements NetworkExtensionDataframeProvider {

    @Override
    public String getExtensionName() {
        return MergedXnode.NAME;
    }

    private Stream<MergedXnode> itemsStream(Network network) {
        return network.getLineStream()
                .map(s -> (MergedXnode) s.getExtension(MergedXnode.class))
                .filter(Objects::nonNull);
    }

    private MergedXnode getOrThrow(Network network, String id) {
        Line line = network.getLine(id);
        if (line == null) {
            throw new PowsyblException("Line '" + id + "' not found");
        }
        MergedXnode mxn = line.getExtension(MergedXnode.class);
        if (mxn == null) {
            throw new PowsyblException("Line '" + id + "' has no MergedXnode extension");
        }
        return mxn;
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", ext -> ext.getExtendable().getId())
                .strings("code", MergedXnode::getCode, MergedXnode::setCode)
                .strings("line1", MergedXnode::getLine1Name, MergedXnode::setLine1Name)
                .strings("line2", MergedXnode::getLine2Name, MergedXnode::setLine2Name)
                .doubles("r_dp", MergedXnode::getRdp, MergedXnode::setRdp)
                .doubles("x_dp", MergedXnode::getXdp, MergedXnode::setXdp)
                .doubles("g1_dp", MergedXnode::getG1dp, MergedXnode::setG1dp)
                .doubles("b1_dp", MergedXnode::getB1dp, MergedXnode::setB1dp)
                .doubles("g2_dp", MergedXnode::getG2dp, MergedXnode::setG2dp)
                .doubles("b2_dp", MergedXnode::getB2dp, MergedXnode::setB2dp)
                .doubles("p1", MergedXnode::getXnodeP1, MergedXnode::setXnodeP1)
                .doubles("q1", MergedXnode::getXnodeQ1, MergedXnode::setXnodeQ1)
                .doubles("p2", MergedXnode::getXnodeP2, MergedXnode::setXnodeP2)
                .doubles("q2", MergedXnode::getXnodeQ2, MergedXnode::setXnodeQ2)
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(id -> network.getLine(id))
                .filter(Objects::nonNull)
                .forEach(g -> g.removeExtension(MergedXnode.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new MergedXnodeDataframeAdder();
    }
}
