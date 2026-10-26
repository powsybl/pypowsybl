/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerFactory;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    void testSwitchFlowResultsParallelSwitch() {
        Network reference = FourSubstationsNodeBreakerFactory.create();
        SwitchFlowContext referenceCoupler = NetworkUtil.getSwitchFlowResults(reference, List.of("S1VL2_COUPLER")).get(0);
        assertTrue(referenceCoupler.firstParallelSwitchId().isEmpty());

        // Add a second closed breaker between the same nodes as S1VL2_COUPLER (22 and 23)
        Network network = FourSubstationsNodeBreakerFactory.create();
        network.getVoltageLevel("S1VL2").getNodeBreakerView().newSwitch()
                .setId("S1VL2_COUPLER_PAR").setKind(SwitchKind.BREAKER).setOpen(false).setRetained(true)
                .setNode1(22).setNode2(23).add();
        List<SwitchFlowContext> results = NetworkUtil.getSwitchFlowResults(network,
                List.of("S1VL2_COUPLER", "S1VL2_COUPLER_PAR", "S1VL2_SHUNT_BREAKER"));

        SwitchFlowContext coupler = results.get(0);
        SwitchFlowContext parallel = results.get(1);
        SwitchFlowContext shuntBreaker = results.get(2);
        assertEquals(Optional.of("S1VL2_COUPLER_PAR"), coupler.firstParallelSwitchId());
        assertEquals(Optional.of("S1VL2_COUPLER"), parallel.firstParallelSwitchId());
        assertTrue(shuntBreaker.firstParallelSwitchId().isEmpty());
        // The cut flow is allocated to one of the parallel switches, the other one gets zero
        assertEquals(referenceCoupler.p(), coupler.p() + parallel.p(), 1e-9);
        assertEquals(referenceCoupler.q(), coupler.q() + parallel.q(), 1e-9);
        assertEquals(0.0, parallel.p());
        assertEquals(0.0, parallel.q());
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
