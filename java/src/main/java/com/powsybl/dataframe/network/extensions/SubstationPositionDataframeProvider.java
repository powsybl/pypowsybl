/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.network.ExtensionInformation;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframeMapperBuilder;
import com.powsybl.dataframe.network.adders.NetworkElementAdder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.extensions.ActivePowerControl;
import com.powsybl.iidm.network.extensions.SubstationPosition;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class SubstationPositionDataframeProvider extends AbstractSingleDataframeNetworkExtension {

    @Override
    public String getExtensionName() {
        return SubstationPosition.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(ActivePowerControl.NAME,
                "Provides information about the susbtation geographical coordinate",
                "index : id (str), latitude (float), longitude (float)");
    }

    private Stream<SubstationPosition> itemsStream(Network network) {
        return network.getSubstationStream()
                .map(g -> (SubstationPosition) g.getExtension(SubstationPosition.class))
                .filter(Objects::nonNull);
    }

    private SubstationPosition getOrThrow(Network network, String id) {
        Substation s = network.getSubstation(id);
        if (s == null) {
            throw new PowsyblException("Substation '" + id + "' not found");
        }
        SubstationPosition sp = s.getExtension(SubstationPosition.class);
        if (sp == null) {
            throw new PowsyblException("Substation '" + id + "' has no SubstationPosition extension");
        }
        return sp;
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", ext -> ext.getExtendable().getId())
                .doubles("latitude", s -> s.getCoordinate().getLatitude())
                .doubles("longitude", s -> s.getCoordinate().getLongitude())
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(network::getSubstation)
                .filter(Objects::nonNull)
                .forEach(g -> g.removeExtension(SubstationPosition.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new SubstationPositionDataframeAdder();
    }

}
