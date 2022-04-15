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
import com.powsybl.entsoe.util.MergedXnode;
import com.powsybl.iidm.network.Network;

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

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream)
                .stringsIndex("id", ext -> ext.getExtendable().getId())
                .strings("code", MergedXnode::getCode)
                .strings("line1", MergedXnode::getLine1Name)
                .strings("line2", MergedXnode::getLine2Name)
                .doubles("r_dp", MergedXnode::getRdp)
                .doubles("x_dp", MergedXnode::getXdp)
                .doubles("g1_dp", MergedXnode::getG1dp)
                .doubles("b1_dp", MergedXnode::getB1dp)
                .doubles("g2_dp", MergedXnode::getG2dp)
                .doubles("b2_dp", MergedXnode::getB2dp)
                .doubles("p1", MergedXnode::getXnodeP1)
                .doubles("q1", MergedXnode::getXnodeQ1)
                .doubles("p2", MergedXnode::getXnodeP2)
                .doubles("q2", MergedXnode::getXnodeQ2)
                .build();
    }
}
