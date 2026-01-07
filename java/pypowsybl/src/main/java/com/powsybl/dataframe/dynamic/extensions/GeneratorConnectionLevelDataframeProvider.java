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
import com.powsybl.dynawo.extensions.api.generator.connection.GeneratorConnectionLevel;
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
public class GeneratorConnectionLevelDataframeProvider extends AbstractSingleDataframeNetworkExtension {

    @Override
    public String getExtensionName() {
        return GeneratorConnectionLevel.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(GeneratorConnectionLevel.NAME,
                "Provides information, for dynamic simulation only, about the characteristics of a Synchronized generator",
                "index : id (str), " +
                        "level (str)");
    }

    private Stream<GeneratorConnectionLevel> itemsStream(Network network) {
        return network.getGeneratorStream()
                .map(g -> (GeneratorConnectionLevel) g.getExtension(GeneratorConnectionLevel.class))
                .filter(Objects::nonNull);
    }

    private GeneratorConnectionLevel getOrThrow(Network network, String id) {
        Generator gen = network.getGenerator(id);
        if (gen == null) {
            throw new PowsyblException("Generator '" + id + "' not found");
        }
        GeneratorConnectionLevel sgp = gen.getExtension(GeneratorConnectionLevel.class);
        if (sgp == null) {
            throw new PowsyblException("Generator '" + id + "' has no GeneratorConnectionLevel extension");
        }
        return sgp;
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", ext -> ext.getExtendable().getId())
                .enums("level", GeneratorConnectionLevel.GeneratorConnectionLevelType.class, GeneratorConnectionLevel::getLevel, GeneratorConnectionLevel::setLevel)
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(network::getGenerator)
                .filter(Objects::nonNull)
                .forEach(g -> g.removeExtension(GeneratorConnectionLevel.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new GeneratorConnectionLevelDataframeAdder();
    }

}
