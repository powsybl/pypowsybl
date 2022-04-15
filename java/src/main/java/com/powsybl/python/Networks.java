/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.cgmes.conformity.test.CgmesConformity1Catalog;
import com.powsybl.cgmes.model.test.TestGridModelResources;
import com.powsybl.commons.PowsyblException;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.ActivePowerControlAdder;
import com.powsybl.iidm.network.extensions.GeneratorEntsoeCategoryAdder;
import com.powsybl.iidm.network.impl.NetworkFactoryImpl;
import com.powsybl.iidm.network.test.*;

import java.util.Map;

import static java.util.Map.entry;

/**
 * Networks factories, for "classic" networks or test network.
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public final class Networks {

    private Networks() {
    }

    @FunctionalInterface
    private interface NetworkFactory {
        Network createNetwork(String id);
    }

    private static final Map<String, NetworkFactory> FACTORIES = Map.ofEntries(
        entry("empty", id -> Network.create(id, "")),
        entry("ieee9", id -> IeeeCdfNetworkFactory.create9()),
        entry("ieee14", id -> IeeeCdfNetworkFactory.create14()),
        entry("ieee30", id -> IeeeCdfNetworkFactory.create30()),
        entry("ieee57", id -> IeeeCdfNetworkFactory.create57()),
        entry("ieee118", id -> IeeeCdfNetworkFactory.create118()),
        entry("ieee300", id -> IeeeCdfNetworkFactory.create300()),
        entry("micro_grid_be", id -> importCgmes(CgmesConformity1Catalog.microGridBaseCaseBE())),
        entry("micro_grid_nl", id -> importCgmes(CgmesConformity1Catalog.microGridBaseCaseNL())),
        entry("four_substations_node_breaker", id -> FourSubstationsNodeBreakerFactory.create()),
        entry("eurostag_tutorial_example1", id -> createEurostagTutorialExample1WithFixedCurrentLimits()),
        entry("eurostag_tutorial_example1_with_power_limits", id -> createEurostagTutorialExample1WithFixedPowerLimits()),
        entry("eurostag_tutorial_example1_with_apc_extension", id -> createEurostagTutorialExample1WithApcExtension()),
        entry("eurostag_tutorial_example1_with_entsoe_category", id -> eurostagWithEntsoeCategory()),
        entry("batteries", id -> BatteryNetworkFactory.create()),
        entry("dangling_lines", id -> DanglingLineNetworkFactory.create()),
        entry("three_windings_transformer", id -> ThreeWindingsTransformerNetworkFactory.create()),
        entry("three_windings_transformer_with_current_limits", id -> ThreeWindingsTransformerNetworkFactory.createWithCurrentLimits()),
        entry("shunt", id -> ShuntTestCaseFactory.create()),
        entry("non_linear_shunt", id -> ShuntTestCaseFactory.createNonLinear())
    );

    private static Network importCgmes(TestGridModelResources modelResources) {
        return Importers.getImporter("CGMES")
                .importData(modelResources.dataSource(), new NetworkFactoryImpl(), null);
    }

    public static Network create(String networkFactoryName, String networkId) {
        NetworkFactory factory = FACTORIES.get(networkFactoryName);
        if (factory == null) {
            throw new PowsyblException("No network factory for ID " + networkFactoryName);
        }
        return factory.createNetwork(networkId);
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
                .withDroop(1.1f)
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

}
