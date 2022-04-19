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
import com.powsybl.iidm.network.extensions.GeneratorEntsoeCategory;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class GeneratorEntsoeCategoryDataframeProvider implements NetworkExtensionDataframeProvider {

    @Override
    public String getExtensionName() {
        return GeneratorEntsoeCategory.NAME;
    }

    private Stream<GeneratorEntsoeCategory> itemsStream(Network network) {
        return network.getGeneratorStream()
                .map(g -> (GeneratorEntsoeCategory) g.getExtension(GeneratorEntsoeCategory.class))
                .filter(Objects::nonNull);
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream)
                .stringsIndex("id", ext -> ext.getExtendable().getId())
                .ints("code", GeneratorEntsoeCategory::getCode)
                .build();
    }
}
