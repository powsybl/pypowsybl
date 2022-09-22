package com.powsybl.python.network;

import com.google.auto.service.AutoService;
import com.powsybl.cgmes.conformity.CgmesConformity1Catalog;
import com.powsybl.cgmes.model.test.TestGridModelResources;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.impl.NetworkFactoryImpl;
import com.powsybl.iidm.network.test.*;

import java.util.List;

import static com.powsybl.python.network.Networks.factory;

/**
 * Provides pypowsybl named networks.
 *
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
@AutoService(Networks.NetworksProvider.class)
public class PyPowsyblNetworksProvider implements Networks.NetworksProvider {

    private static final List<Networks.NamedNetworkFactory> FACTORIES = List.of(
            factory("empty", id -> Network.create(id, "")),
            factory("ieee9", () -> IeeeCdfNetworkFactory.create9()),
            factory("ieee14", () -> IeeeCdfNetworkFactory.create14()),
            factory("ieee30", () -> IeeeCdfNetworkFactory.create30()),
            factory("ieee57", () -> IeeeCdfNetworkFactory.create57()),
            factory("ieee118", () -> IeeeCdfNetworkFactory.create118()),
            factory("ieee300", () -> IeeeCdfNetworkFactory.create300()),
            factory("micro_grid_be", () -> importCgmes(CgmesConformity1Catalog.microGridBaseCaseBE())),
            factory("micro_grid_nl", () -> importCgmes(CgmesConformity1Catalog.microGridBaseCaseNL())),
            factory("four_substations_node_breaker", () -> FourSubstationsNodeBreakerFactory.create()),
            factory("eurostag_tutorial_example1", () -> Networks.createEurostagTutorialExample1WithFixedCurrentLimits()),
            factory("eurostag_tutorial_example1_with_power_limits", () -> Networks.createEurostagTutorialExample1WithFixedPowerLimits()),
            factory("eurostag_tutorial_example1_with_apc_extension", () -> Networks.createEurostagTutorialExample1WithApcExtension()),
            factory("eurostag_tutorial_example1_with_entsoe_category", () -> Networks.eurostagWithEntsoeCategory()),
            factory("batteries", () -> BatteryNetworkFactory.create()),
            factory("dangling_lines", () -> DanglingLineNetworkFactory.create()),
            factory("three_windings_transformer", () -> ThreeWindingsTransformerNetworkFactory.create()),
            factory("three_windings_transformer_with_current_limits", () -> ThreeWindingsTransformerNetworkFactory.createWithCurrentLimits()),
            factory("shunt", () -> ShuntTestCaseFactory.create()),
            factory("non_linear_shunt", () -> ShuntTestCaseFactory.createNonLinear())
    );

    @Override
    public List<Networks.NamedNetworkFactory> getNetworkFactories() {
        return FACTORIES;
    }

    private static Network importCgmes(TestGridModelResources modelResources) {
        return Importer.find("CGMES")
                .importData(modelResources.dataSource(), new NetworkFactoryImpl(), null);
    }
}
