/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Functions;
import com.powsybl.dataframe.DataframeFilter;
import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.dataframe.network.extensions.NetworkExtensionDataframeProvider;
import com.powsybl.dataframe.network.extensions.NetworkExtensions;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.dataframe.CDataframeHandler;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */

public final class PyNetworkExtensions {

    private PyNetworkExtensions() {
    }

    private static class Container {
        private final Map<String, ExtensionInformation> elements;

        Container(ExtensionInformation... elements) {
            this(Arrays.asList(elements));
        }

        Container(Collection<ExtensionInformation> elements) {
            this.elements = elements.stream()
                    .collect(Collectors.toUnmodifiableMap(ExtensionInformation::getId, Functions.identity()));
        }

        List<ExtensionInformation> getExtensionInformation() {
            return new ArrayList<>(elements.values());
        }
    }

    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getExtensionInformation() {
        DataframeMapper<Container> mapper = new DataframeMapperBuilder<Container, ExtensionInformation>()
                .itemsProvider(Container::getExtensionInformation)
                .stringsIndex("id", ExtensionInformation::getId)
                .strings("detail", ExtensionInformation::getDescription)
                .strings("attributes", ExtensionInformation::getAttributes)
                .build();
        Container container = new Container(
                NetworkExtensions.getExtensionsProviders()
                        .stream()
                        .map(NetworkExtensionDataframeProvider::getExtensionInformation)
                        .collect(Collectors.toList()));
        CDataframeHandler handler = new CDataframeHandler();
        mapper.createDataframe(container, handler, new DataframeFilter());
        return handler.getDataframePtr();
    }
}
