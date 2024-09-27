/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
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
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.ActivePowerControl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class ActivePowerControlDataframeProvider extends AbstractSingleDataframeNetworkExtension {

    @Override
    public String getExtensionName() {
        return ActivePowerControl.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(ActivePowerControl.NAME,
                "Provides information about the participation of generators to balancing",
                "index : id (str), " +
                        "participate (bool), " +
                        "droop (float), " +
                        "participation_factor (float), " +
                        "max_target_p (float), " +
                        "min_target_p (float)");
    }

    private Stream<ActivePowerControl> itemsStream(Network network) {
        return network.getGeneratorStream()
                .map(g -> (ActivePowerControl) g.getExtension(ActivePowerControl.class))
                .filter(Objects::nonNull);
    }

    private ActivePowerControl getOrThrow(Network network, String id) {
        Generator gen = network.getGenerator(id);
        if (gen == null) {
            throw new PowsyblException("Generator '" + id + "' not found");
        }
        ActivePowerControl apc = gen.getExtension(ActivePowerControl.class);
        if (apc == null) {
            throw new PowsyblException("Generator '" + id + "' has no ActivePowerControl extension");
        }
        return apc;
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", ext -> ((Identifiable<?>) ext.getExtendable()).getId())
                .doubles("droop", (apc, context) -> apc.getDroop(), (apc, droop, context) -> apc.setDroop(droop))
                .booleans("participate", ActivePowerControl::isParticipate, ActivePowerControl::setParticipate)
                .doubles("participation_factor", (apc, context) -> apc.getParticipationFactor(), (apc, participationFactor, context) -> apc.setParticipationFactor(participationFactor))
                .doubles("max_target_p", (apc, context) -> apc.getMaxTargetP().orElse(Double.NaN), (apc, maxTargetP, context) -> apc.setMaxTargetP(maxTargetP))
                .doubles("min_target_p", (apc, context) -> apc.getMinTargetP().orElse(Double.NaN), (apc, minTargetP, context) -> apc.setMinTargetP(minTargetP))
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(network::getGenerator)
                .filter(Objects::nonNull)
                .forEach(g -> g.removeExtension(ActivePowerControl.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new ActivePowerControlDataframeAdder();
    }

}
