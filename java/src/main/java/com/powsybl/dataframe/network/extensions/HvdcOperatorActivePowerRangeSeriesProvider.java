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
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcOperatorActivePowerRange;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class HvdcOperatorActivePowerRangeSeriesProvider implements NetworkExtensionDataframeProvider {

    public static final String EXTENSION_NAME = "hvdcOperatorActivePowerRange";

    @Override
    public String getExtensionName() {
        return EXTENSION_NAME;
    }

    private Stream<HvdcOperatorActivePowerRange> itemsStream(Network network) {
        return network.getHvdcLineStream()
                .map(g -> (HvdcOperatorActivePowerRange) g.getExtension(HvdcOperatorActivePowerRange.class))
                .filter(Objects::nonNull);
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream)
                .stringsIndex("id", ext -> ext.getExtendable().getId())
                .doubles("opr_from_cs1_to_cs2", HvdcOperatorActivePowerRange::getOprFromCS1toCS2)
                .doubles("opr_from_cs2_to_cs1", HvdcOperatorActivePowerRange::getOprFromCS2toCS1)
                .build();
    }
}
