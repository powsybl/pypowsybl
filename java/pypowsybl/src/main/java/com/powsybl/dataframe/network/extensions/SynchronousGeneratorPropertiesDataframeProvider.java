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
import com.powsybl.iidm.network.extensions.SynchronousGeneratorProperties;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class SynchronousGeneratorPropertiesDataframeProvider extends AbstractSingleDataframeNetworkExtension {

    @Override
    public String getExtensionName() {
        return SynchronousGeneratorProperties.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(SynchronousGeneratorProperties.NAME,
                "Provides information about the characteristics of a synchronous generator",
                "index : id (str), " +
                        "numberOfWindings (int), " +
                        "governor (str), " +
                        "voltageRegulator (str), " +
                        "pss (str), " +
                        "auxiliaries (bool), " +
                        "internalTransformer (bool), " +
                        "rpcl (bool), " +
                        "rpcl2 (bool), " +
                        "uva (str), " +
                        "fictitious (bool), " +
                        "qlim (bool)");
    }

    private Stream<SynchronousGeneratorProperties> itemsStream(Network network) {
        return network.getGeneratorStream()
                .map(g -> (SynchronousGeneratorProperties) g.getExtension(SynchronousGeneratorProperties.class))
                .filter(Objects::nonNull);
    }

    private SynchronousGeneratorProperties getOrThrow(Network network, String id) {
        Generator gen = network.getGenerator(id);
        if (gen == null) {
            throw new PowsyblException("Generator '" + id + "' not found");
        }
        SynchronousGeneratorProperties sgp = gen.getExtension(SynchronousGeneratorProperties.class);
        if (sgp == null) {
            throw new PowsyblException("Generator '" + id + "' has no SynchronousGeneratorProperties extension");
        }
        return sgp;
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", ext -> ((Identifiable<?>) ext.getExtendable()).getId())
                .ints("numberOfWindings", SynchronousGeneratorProperties::getNumberOfWindings, SynchronousGeneratorProperties::setNumberOfWindings)
                .strings("governor", sgp -> String.valueOf(sgp.getGovernor()), (sgp, governor) -> sgp.setGovernor(governor))
                .strings("voltageRegulator", sgp -> String.valueOf(sgp.getVoltageRegulator()), (sgp, voltageRegulator) -> sgp.setVoltageRegulator(voltageRegulator))
                .strings("pss", sgp -> String.valueOf(sgp.getPss()), (sgp, pss) -> sgp.setPss(pss))
                .booleans("auxiliaries", SynchronousGeneratorProperties::getAuxiliaries, SynchronousGeneratorProperties::setAuxiliaries)
                .booleans("internalTransformer", SynchronousGeneratorProperties::getInternalTransformer, SynchronousGeneratorProperties::setInternalTransformer)
                .booleans("rpcl", SynchronousGeneratorProperties::getRpcl, SynchronousGeneratorProperties::setRpcl)
                .booleans("rpcl2", SynchronousGeneratorProperties::getRpcl2, SynchronousGeneratorProperties::setRpcl2)
                .strings("uva", sgp -> String.valueOf(sgp.getUva()), (sgp, uva) -> sgp.setUva(uva))
                .booleans("fictitious", SynchronousGeneratorProperties::getFictitious, SynchronousGeneratorProperties::setFictitious)
                .booleans("qlim", SynchronousGeneratorProperties::getQlim, SynchronousGeneratorProperties::setQlim)
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(network::getGenerator)
                .filter(Objects::nonNull)
                .forEach(g -> g.removeExtension(SynchronousGeneratorProperties.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new SynchronousGeneratorPropertiesDataframeAdder();
    }

}
