/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.network.ExtensionInformation;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframeMapperBuilder;
import com.powsybl.dataframe.network.adders.NetworkElementAdder;
import com.powsybl.dataframe.network.extensions.AbstractSingleDataframeNetworkExtension;
import com.powsybl.dataframe.network.extensions.NetworkExtensionDataframeProvider;
import com.powsybl.dynawo.extensions.api.generator.RpclType;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
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
                "Provides information, for dynamic simulation only, about the characteristics of a synchronous generator",
                "index : id (str), " +
                        "numberOfWindings (str), " +
                        "governor (str), " +
                        "voltageRegulator (str), " +
                        "pss (str), " +
                        "auxiliaries (bool), " +
                        "internalTransformer (bool), " +
                        "rpcl (str), " +
                        "uva (str), " +
                        "aggregated (bool), " +
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
                .stringsIndex("id", ext -> ext.getExtendable().getId())
                .enums("numberOfWindings", SynchronousGeneratorProperties.Windings.class, SynchronousGeneratorProperties::getNumberOfWindings, SynchronousGeneratorProperties::setNumberOfWindings)
                .strings("governor", sgp -> String.valueOf(sgp.getGovernor()), SynchronousGeneratorProperties::setGovernor)
                .strings("voltageRegulator", sgp -> String.valueOf(sgp.getVoltageRegulator()), SynchronousGeneratorProperties::setVoltageRegulator)
                .strings("pss", sgp -> String.valueOf(sgp.getPss()), SynchronousGeneratorProperties::setPss)
                .booleans("auxiliaries", SynchronousGeneratorProperties::isAuxiliaries, SynchronousGeneratorProperties::setAuxiliaries)
                .booleans("internalTransformer", SynchronousGeneratorProperties::isInternalTransformer, SynchronousGeneratorProperties::setInternalTransformer)
                .enums("rpcl", RpclType.class, SynchronousGeneratorProperties::getRpcl, SynchronousGeneratorProperties::setRpcl)
                .enums("uva", SynchronousGeneratorProperties.Uva.class, SynchronousGeneratorProperties::getUva, SynchronousGeneratorProperties::setUva)
                .booleans("aggregated", SynchronousGeneratorProperties::isAggregated, SynchronousGeneratorProperties::setAggregated)
                .booleans("qlim", SynchronousGeneratorProperties::isQlim, SynchronousGeneratorProperties::setQlim)
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
