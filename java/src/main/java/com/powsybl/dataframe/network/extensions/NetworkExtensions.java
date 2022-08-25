/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.extensions;

import com.google.common.base.Functions;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSortedMap;
import com.powsybl.dataframe.DataframeFilter;
import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.adders.NetworkElementAdder;
import com.powsybl.entsoe.util.EntsoeArea;
import com.powsybl.entsoe.util.MergedXnode;
import com.powsybl.entsoe.util.Xnode;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.*;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.dataframe.CDataframeHandler;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */

public final class NetworkExtensions {

    private static final Map<String, NetworkExtensionDataframeProvider> EXTENSIONS_PROVIDERS = createExtensionsProviders();

    private NetworkExtensions() {
    }

    private static Map<String, NetworkExtensionDataframeProvider> createExtensionsProviders() {
        return Suppliers
                .memoize(() -> ServiceLoader.load(NetworkExtensionDataframeProvider.class)
                        .stream().map(ServiceLoader.Provider::get).collect(Collectors.toList()))
                .get().stream()
                .collect(ImmutableSortedMap.toImmutableSortedMap(String::compareTo,
                        NetworkExtensionDataframeProvider::getExtensionName, Function.identity()));
    }

    public static List<String> getExtensionsNames() {
        return new ArrayList<>(EXTENSIONS_PROVIDERS.keySet());
    }

    public static Map<String, NetworkDataframeMapper> createExtensionsMappers() {
        return EXTENSIONS_PROVIDERS.values().stream()
                .collect(Collectors.toMap(NetworkExtensionDataframeProvider::getExtensionName,
                        NetworkExtensionDataframeProvider::createMapper));
    }

    public static Map<String, NetworkElementAdder> createExtensionsAdders() {
        return EXTENSIONS_PROVIDERS.values().stream()
                .collect(Collectors.toMap(NetworkExtensionDataframeProvider::getExtensionName,
                        NetworkExtensionDataframeProvider::createAdder));
    }

    public static void removeExtensions(Network network, String name, List<String> ids) {
        NetworkExtensionDataframeProvider provider = EXTENSIONS_PROVIDERS.get(name);
        if (provider != null) {
            provider.removeExtensions(network, ids);
        }
    }

    public static class ExtensionInformation {
        private final String id;
        private final String description;
        private final String attributes;

        public ExtensionInformation(String id, String description, String attributes) {
            this.id = id;
            this.description = description;
            this.attributes = attributes;
        }

        public String getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public String getAttributes() {
            return attributes;
        }
    }

    private static class Container {
        private final Map<String, ExtensionInformation> elements;

        Container(ExtensionInformation... elements) {
            this(Arrays.asList(elements));
        }

        Container(Collection<ExtensionInformation> elements) {
            this.elements = elements.stream().collect(Collectors.toUnmodifiableMap(ExtensionInformation::getId, Functions.identity()));
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
                new ExtensionInformation(ActivePowerControl.NAME,
                        "Provides information about the participation of generators to balancing",
                        "index : id (str), participate(bool), droop (float)"),
                new ExtensionInformation(BranchObservability.NAME,
                        "Provides information about the observability of a branch",
                        "index : id (str), observable (bool), p1_standard_deviation (float), p1_redundant (bool), p2_standard_deviation (float), p2_redundant (bool), " +
                                "q1_standard_deviation (float), q1_redundant (bool), q2_standard_deviation (float), q2_redundant (bool)"),
                new ExtensionInformation(EntsoeArea.NAME, "Provides Entsoe geographical code for a substation", "index : id (str), code (str)"),
                new ExtensionInformation(GeneratorEntsoeCategory.NAME, "Provides Entsoe category code for a generator", "index : id (str), code (int)"),
                new ExtensionInformation(HvdcAngleDroopActivePowerControl.NAME, "Active power control mode based on an offset in MW and a droop in MW/degree",
                        "index : id (str), droop (float), p0 (float), enabled (bool)"),
                new ExtensionInformation(HvdcOperatorActivePowerRange.NAME, "",
                        "index : id (str), opr_from_cs1_to_cs2 (float), opr_from_cs2_to_cs1 (float)"),
                new ExtensionInformation(InjectionObservability.NAME, "Provides information about the observability of a injection",
                        "index : id (str), observable (bool), p_standard_deviation (float), p_redundant (bool), q_standard_deviation (float), q_redundant (bool), v_standard_deviation (float), v_redundant (bool)"),
                new ExtensionInformation(LoadDetail.NAME, "Provides active power setpoint and reactive power setpoint for a load",
                        "index : id (str), fixed_p (float), variable_p (float), fixed_q (float), variable_q (float)"),
                new ExtensionInformation(Measurements.NAME, "Provides measurement about a specific equipment",
                        "index : element_id (str),id (str), type (str), standard_deviation (float), value (float), valid (bool)"),
                new ExtensionInformation(MergedXnode.NAME, "Provides information about the border point between 2 TSOs on a merged line",
                        "index : id (str), code (str), line1 (str), line2 (str), r_dp (float), x_dp (float), g1_dp (float), b1_dp (float), " +
                        "g2_dp (float), b2_dp (float), p1 (float), q1 (float), p2 (float), q2 (float)"),
                new ExtensionInformation(Xnode.NAME, "Provides information about the border point of a TSO on a dangling line",
                        "index : id (str), code (str)")
        );
        CDataframeHandler handler = new CDataframeHandler();
        mapper.createDataframe(container, handler, new DataframeFilter());
        return handler.getDataframePtr();
    }
}

