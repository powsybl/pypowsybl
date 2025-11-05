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
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.VoltageRegulation;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.powsybl.dataframe.network.extensions.VoltageRegulationDataframeAdder.getTerminal;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class VoltageRegulationDataframeProvider extends AbstractSingleDataframeNetworkExtension {
    @Override
    public String getExtensionName() {
        return VoltageRegulation.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(VoltageRegulation.NAME, "it allows to specify the voltage regulation mode for batteries",
                "index : id (str), voltage_regulator_on (bool), target_v (float), regulated_element_id (str)");
    }

    private Stream<VoltageRegulation> itemsStream(Network network) {
        return network.getBatteryStream().filter(Objects::nonNull)
                .map(battery -> (VoltageRegulation) battery.getExtension(VoltageRegulation.class))
                .filter(Objects::nonNull);
    }

    private VoltageRegulation getOrThrow(Network network, String id) {
        Battery battery = network.getBattery(id);
        if (battery == null) {
            throw new PowsyblException("Battery '" + id + "' not found");
        }
        VoltageRegulation extension = battery.getExtension(VoltageRegulation.class);
        if (extension == null) {
            throw new PowsyblException("Voltage regulation extension for battery '" + id + "' not found");
        }
        return extension;
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", vr -> vr.getExtendable().getId())
                .booleans("voltage_regulator_on", VoltageRegulation::isVoltageRegulatorOn, VoltageRegulation::setVoltageRegulatorOn)
                .doubles("target_v", (vr, context) -> vr.getTargetV(), (vr, targetV, context) -> vr.setTargetV(targetV))
                .strings("regulated_element_id", this::getRegulatedBusId,
                        (regulation, id) -> this.setRegulatedTerminal(id, regulation))
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(network::getBattery)
                .filter(Objects::nonNull)
                .forEach(battery -> battery.removeExtension(VoltageRegulation.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new VoltageRegulationDataframeAdder();
    }

    private String getRegulatedBusId(VoltageRegulation regulation) {
        Terminal terminal = regulation.getRegulatingTerminal();
        return terminal.getConnectable() != null ? terminal.getConnectable().getId() : null;
    }

    private void setRegulatedTerminal(String id, VoltageRegulation regulation) {
        Identifiable<Battery> injection = regulation.getExtendable();
        Network network = injection.getNetwork();
        Terminal terminal = getTerminal(network, id, injection.getId());
        regulation.setRegulatingTerminal(terminal);
    }
}
