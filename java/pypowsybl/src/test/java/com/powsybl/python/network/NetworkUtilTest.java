/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElementFactory;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerFactory;
import com.powsybl.openloadflow.sa.OpenSecurityAnalysisParameters;
import com.powsybl.python.commons.CommonObjects;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.security.SecurityAnalysis;
import com.powsybl.security.SecurityAnalysisParameters;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.SecurityAnalysisRunParameters;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class NetworkUtilTest {

    @Test
    void test() {
        Network network = EurostagTutorialExample1Factory.create();
        List<String> elementsIds = NetworkUtil.getElementsIds(network, PyPowsyblApiHeader.ElementType.TWO_WINDINGS_TRANSFORMER, Collections.singleton(24.0), Collections.singleton("FR"), true, true, false);
        assertEquals(Collections.singletonList("NGEN_NHV1"), elementsIds);
    }

    @Test
    void testBusFromBusBreakerViewBus() {
        Network network = createTopologyTestNetwork();
        var expected = Map.of(
            "B1", "VL1_0",
            "B2", "VL1_0",
            "B3", "",
            "VL2_0", "VL2_0",
            "VL2_1", "VL2_0",
            "VL2_2", "");
        expected.forEach((busBreakerBusId, busIdExpected) -> {
            Optional<Bus> bus = NetworkUtil.getBusViewBus(network.getBusBreakerView().getBus(busBreakerBusId));
            if (!busIdExpected.isEmpty()) {
                assertTrue(bus.isPresent());
                assertEquals(busIdExpected, bus.orElseThrow().getId());
            } else {
                assertTrue(bus.isEmpty());
            }
        });
    }

    @Test
    void testOutageGroupsMatchSecurityAnalysisConnectivityResults() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        List<Contingency> contingencies = network.getIdentifiables().stream()
            .map(NetworkUtilTest::createContingency)
            .flatMap(Optional::stream)
            .toList();
        ContingenciesProvider contingenciesProvider = ignored -> contingencies;

        SecurityAnalysisParameters parameters = new SecurityAnalysisParameters();
        parameters.addExtension(OpenSecurityAnalysisParameters.class,
                new OpenSecurityAnalysisParameters()
                        .setContingencyPropagation(true)
                        .setCreateResultExtension(true));
        SecurityAnalysisRunParameters runParameters = new SecurityAnalysisRunParameters()
            .setSecurityAnalysisParameters(parameters)
            .setComputationManager(CommonObjects.getComputationManager())
            .setReportNode(ReportNode.NO_OP);

        SecurityAnalysisResult securityAnalysisResult = SecurityAnalysis.find("OpenLoadFlow")
            .run(network, network.getVariantManager().getWorkingVariantId(), contingenciesProvider, runParameters)
            .getResult();
        Map<String, List<String>> expectedOutageGroups = securityAnalysisResult.getPostContingencyResults().stream()
            .collect(Collectors.toMap(postContingencyResult -> postContingencyResult.getContingency().getId(),
                postContingencyResult -> postContingencyResult.getConnectivityResult().getDisconnectedElements().stream().sorted().toList(),
                (left, right) -> left,
                LinkedHashMap::new));

        List<String> elementIds = contingencies.stream()
            .map(Contingency::getId)
            .toList();
        Map<String, List<String>> outageGroups = NetworkUtil.getOutageGroups(network, elementIds);

        assertThat(outageGroups.values()).allSatisfy(outageGroup -> assertThat(outageGroup).isSorted());
        assertThat(outageGroups).containsOnlyKeys(elementIds);
        elementIds.forEach(elementId -> assertThat(outageGroups.get(elementId)).isEqualTo(expectedOutageGroups.getOrDefault(elementId, List.of())));
    }

    private static Optional<Contingency> createContingency(Identifiable<?> identifiable) {
        try {
            return Optional.of(new Contingency(identifiable.getId(), List.of(ContingencyElementFactory.create(identifiable))));
        } catch (PowsyblException e) {
            return Optional.empty();
        }
    }

    public static Network createTopologyTestNetwork() {
        Network network = NetworkFactory.findDefault().createNetwork("test", "code");

        var vl1 = network.newVoltageLevel().setTopologyKind(TopologyKind.BUS_BREAKER).setId("VL1").setNominalV(400.).add();
        vl1.getBusBreakerView().newBus().setId("B1").add();
        vl1.getBusBreakerView().newBus().setId("B2").add();
        vl1.getBusBreakerView().newBus().setId("B3").add();
        vl1.getBusBreakerView().newSwitch().setId("CB1.1").setOpen(false).setBus1("B1").setBus2("B2").add();
        vl1.getBusBreakerView().newSwitch().setId("CB1.2").setOpen(true).setBus1("B1").setBus2("B2").add();
        vl1.newLoad().setId("L1").setP0(10.).setQ0(3.).setConnectableBus("B1").setBus("B1").add();

        var vl2 = network.newVoltageLevel().setTopologyKind(TopologyKind.NODE_BREAKER).setId("VL2").setNominalV(400.).add();
        vl2.getNodeBreakerView().newBusbarSection().setId("BBS1").setNode(0).add();
        vl2.getNodeBreakerView()
                .newSwitch()
                .setId("CB2.1").setOpen(false).setRetained(true).setKind(SwitchKind.BREAKER).setNode1(0).setNode2(1).add();
        vl2.getNodeBreakerView()
                .newSwitch()
                .setId("CB2.2").setOpen(true).setRetained(true).setKind(SwitchKind.BREAKER).setNode1(1).setNode2(2).add();
        vl2.newLoad().setId("L2").setP0(10.).setQ0(3.).setNode(3).add();
        vl2.getNodeBreakerView().newInternalConnection().setNode1(0).setNode2(3).add();

        return network;
    }
}
