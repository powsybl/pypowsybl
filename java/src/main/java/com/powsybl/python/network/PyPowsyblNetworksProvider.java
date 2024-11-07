package com.powsybl.python.network;

import com.google.auto.service.AutoService;
import com.powsybl.cgmes.conformity.CgmesConformity1Catalog;
import com.powsybl.cgmes.model.GridModelReferenceResources;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Importer;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerWithExtensionsFactory;
import com.powsybl.iidm.network.impl.NetworkFactoryImpl;
import com.powsybl.iidm.network.test.*;
import com.powsybl.iidm.serde.test.MetrixTutorialSixBusesFactory;

import java.util.List;
import java.util.function.Supplier;

import static com.powsybl.python.network.Networks.factory;

/**
 * Provides pypowsybl named networks.
 *
 * @author Sylvain Leclerc {@literal <sylvain.leclerc@rte-france.com>}
 */
@AutoService(Networks.NetworksProvider.class)
public class PyPowsyblNetworksProvider implements Networks.NetworksProvider {

    private static final List<Networks.NamedNetworkFactory> FACTORIES = List.of(
            factory("empty", id -> Network.create(id, "")),
            factory("ieee9", (Supplier<Network>) IeeeCdfNetworkFactory::create9),
            factory("ieee14", (Supplier<Network>) IeeeCdfNetworkFactory::create14),
            factory("ieee30", (Supplier<Network>) IeeeCdfNetworkFactory::create30),
            factory("ieee57", (Supplier<Network>) IeeeCdfNetworkFactory::create57),
            factory("ieee118", (Supplier<Network>) IeeeCdfNetworkFactory::create118),
            factory("ieee300", (Supplier<Network>) IeeeCdfNetworkFactory::create300),
            factory("micro_grid_be", () -> importCgmes(CgmesConformity1Catalog.microGridBaseCaseBE())),
            factory("micro_grid_nl", () -> importCgmes(CgmesConformity1Catalog.microGridBaseCaseNL())),
            factory("four_substations_node_breaker", (Supplier<Network>) FourSubstationsNodeBreakerFactory::create),
            factory("four_substations_node_breaker_with_extensions", (Supplier<Network>) FourSubstationsNodeBreakerWithExtensionsFactory::create),
            factory("eurostag_tutorial_example1", Networks::createEurostagTutorialExample1WithFixedCurrentLimits),
            factory("eurostag_tutorial_example1_with_power_limits", Networks::createEurostagTutorialExample1WithFixedPowerLimits),
            factory("eurostag_tutorial_example1_with_apc_extension", Networks::createEurostagTutorialExample1WithApcExtension),
            factory("eurostag_tutorial_example1_with_entsoe_category", Networks::eurostagWithEntsoeCategory),
            factory("eurostag_tutorial_example1_with_tie_line", Networks::eurostagWithTieLine),
            factory("eurostag_tutorial_example1_with_tie_lines_and_areas", Networks::eurostagWithTieLinesAndAreas),
            factory("batteries", (Supplier<Network>) BatteryNetworkFactory::create),
            factory("dangling_lines", (Supplier<Network>) DanglingLineNetworkFactory::create),
            factory("three_windings_transformer", (Supplier<Network>) ThreeWindingsTransformerNetworkFactory::create),
            factory("three_windings_transformer_with_current_limits", ThreeWindingsTransformerNetworkFactory::createWithCurrentLimits),
            factory("shunt", (Supplier<Network>) ShuntTestCaseFactory::create),
            factory("non_linear_shunt", (Supplier<Network>) ShuntTestCaseFactory::createNonLinear),
            factory("security_analysis_test", () -> SecurityAnalysisTestNetworkFactory.create()),
            factory("security_analysis_test_with_power_limits", () -> SecurityAnalysisTestNetworkFactory.createWithFixedPowerLimits()),
            factory("security_analysis_test_with_current_limits", () -> SecurityAnalysisTestNetworkFactory.createWithFixedCurrentLimits()),
            factory("metrix_tutorial_six_buses", () -> MetrixTutorialSixBusesFactory.create())
    );

    @Override
    public List<Networks.NamedNetworkFactory> getNetworkFactories() {
        return FACTORIES;
    }

    private static Network importCgmes(GridModelReferenceResources modelResources) {
        return Importer.find("CGMES")
                .importData(modelResources.dataSource(), new NetworkFactoryImpl(), null);
    }
}
