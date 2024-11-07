/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.ActivePowerControlAdder;
import com.powsybl.iidm.network.extensions.GeneratorEntsoeCategoryAdder;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Networks factories, for "classic" networks or test network.
 *
 * @author Sylvain Leclerc {@literal <sylvain.leclerc at rte-france.com>}
 */
public final class Networks {

    private Networks() {
    }

    /**
     * Service provider interface for third party builds
     * to provide additional named networks.
     */
    public interface NetworksProvider {
        List<NamedNetworkFactory> getNetworkFactories();
    }

    @FunctionalInterface
    public interface NetworkFactory {
        Network createNetwork(String id);
    }

    public interface NamedNetworkFactory extends NetworkFactory {
        String getName();
    }

    private static final Map<String, NetworkFactory> FACTORIES = new ServiceLoaderCache<>(NetworksProvider.class)
            .getServices().stream()
            .flatMap(provider -> provider.getNetworkFactories().stream())
            .collect(Collectors.toMap(NamedNetworkFactory::getName, Function.identity()));

    /**
     * Creates an instance of network corresponding to the specified factory name.
     * A network ID may be provided but will not be honoured by all factories.
     *
     * @param networkFactoryName Name of the network factory (for ex. "empty" or "ieee9")
     * @param networkId          Id of the network. It may not be used by all factories.
     * @return                   A new network.
     */
    public static Network create(String networkFactoryName, String networkId) {
        NetworkFactory factory = FACTORIES.get(networkFactoryName);
        if (factory == null) {
            throw new PowsyblException("No network factory for ID " + networkFactoryName);
        }
        return factory.createNetwork(networkId);
    }

    /**
     * Helper method to create a named network factory, which will ignore network ID.
     */
    public static NamedNetworkFactory factory(String name, Supplier<Network> supplier) {
        return factory(name, id -> supplier.get());
    }

    /**
     * Helper method to create a named network factory.
     */
    public static NamedNetworkFactory factory(String name, NetworkFactory factory) {
        return new NamedNetworkFactory() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Network createNetwork(String id) {
                return factory.createNetwork(id);
            }
        };
    }

    public static Network createEurostagTutorialExample1() {
        Network network = EurostagTutorialExample1Factory.create();
        return fix(network);
    }

    public static Network createEurostagTutorialExample1WithFixedCurrentLimits() {
        Network network = EurostagTutorialExample1Factory.createWithFixedCurrentLimits();
        return fix(network);
    }

    public static Network createEurostagTutorialExample1WithFixedPowerLimits() {
        Network network = EurostagTutorialExample1Factory.createWithFixedLimits();
        return fix(network);
    }

    private static Network fix(Network network) {
        Generator gen = network.getGenerator("GEN");
        if (gen != null) {
            gen.setMaxP(4999);
        }
        Generator gen2 = network.getGenerator("GEN2");
        if (gen2 != null) {
            gen2.setMaxP(4999);
        }
        return network;
    }

    public static Network createEurostagTutorialExample1WithApcExtension() {
        Network network = createEurostagTutorialExample1();
        network.getGenerator("GEN")
                .newExtension(ActivePowerControlAdder.class)
                .withParticipate(true)
                .withDroop(1.1)
                .add();
        return network;
    }

    public static Network eurostagWithEntsoeCategory() {
        Network network = createEurostagTutorialExample1();
        network.getGenerator("GEN")
                .newExtension(GeneratorEntsoeCategoryAdder.class)
                .withCode(5)
                .add();
        return network;
    }

    public static Network eurostagWithTieLine() {
        Network network = EurostagTutorialExample1Factory.createWithTieLine();
        return fix(network);
    }

    public static Network eurostagWithTieLinesAndAreas() {
        Network network = EurostagTutorialExample1Factory.createWithTieLinesAndAreas();
        return fix(network);
    }
}
