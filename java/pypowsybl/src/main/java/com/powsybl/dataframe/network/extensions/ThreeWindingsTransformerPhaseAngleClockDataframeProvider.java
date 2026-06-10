/**
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
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.extensions.ThreeWindingsTransformerPhaseAngleClock;

/**
 * @author Nico Westerbeck {@literal <nico.westerbeck@50hertz.com>}
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class ThreeWindingsTransformerPhaseAngleClockDataframeProvider extends AbstractSingleDataframeNetworkExtension {

    @Override
    public String getExtensionName() {
        return ThreeWindingsTransformerPhaseAngleClock.NAME;
    }
qwf
    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(ThreeWindingsTransformerPhaseAngleClock.NAME,
                "Provides phase angle clock information for three windings transformers",
                "index : id (str), phase_angle_clock_leg2 (int), phase_angle_clock_leg3 (int)");
    }

    private Stream<ThreeWindingsTransformerPhaseAngleClock> itemsStream(Network network) {
        return network.getThreeWindingsTransformerStream()
                .map(transformer -> (ThreeWindingsTransformerPhaseAngleClock) transformer.getExtension(ThreeWindingsTransformerPhaseAngleClock.class))
                .filter(Objects::nonNull);
    }

    private ThreeWindingsTransformerPhaseAngleClock getOrThrow(Network network, String id) {
        ThreeWindingsTransformer transformer = network.getThreeWindingsTransformer(id);
        if (transformer == null) {
            throw new PowsyblException("Three windings transformer '" + id + "' does not exist.");
        }
        ThreeWindingsTransformerPhaseAngleClock extension = transformer.getExtension(ThreeWindingsTransformerPhaseAngleClock.class);
        if (extension == null) {
            throw new PowsyblException("Three windings transformer '" + id + "' has no phase angle clock extension");
        }
        return extension;
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", extension -> extension.getExtendable().getId())
                .ints("phase_angle_clock_leg2",
                        ThreeWindingsTransformerPhaseAngleClock::getPhaseAngleClockLeg2,
                        ThreeWindingsTransformerPhaseAngleClock::setPhaseAngleClockLeg2)
                .ints("phase_angle_clock_leg3",
                        ThreeWindingsTransformerPhaseAngleClock::getPhaseAngleClockLeg3,
                        ThreeWindingsTransformerPhaseAngleClock::setPhaseAngleClockLeg3)
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream()
                .filter(Objects::nonNull)
                .map(network::getThreeWindingsTransformer)
                .filter(Objects::nonNull)
                .forEach(transformer -> transformer.removeExtension(ThreeWindingsTransformerPhaseAngleClock.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new ThreeWindingsTransformerPhaseAngleClockDataframeAdder();
    }
}
