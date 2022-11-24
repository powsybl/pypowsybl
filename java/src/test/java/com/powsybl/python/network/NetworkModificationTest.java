/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.powsybl.python.network.NetworkModificationsCFunctions.getFeedersByConnectable;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Coline Piloquet <coline.piloquet at rte-france.com>
 */
public class NetworkModificationTest {

    @Test
    void testGetFeedersByConnectable() {
        Network network = Network.read("testNetworkNodeBreaker.xiidm", getClass().getResourceAsStream("/testNetworkNodeBreaker.xiidm"));
        Map<String, List<ConnectablePosition.Feeder>> feeders = getFeedersByConnectable(network.getVoltageLevel("vl1"));
        assertFalse(feeders.isEmpty());
        assertEquals(13, feeders.size());
        assertTrue(feeders.containsKey("trf5"));
        List<ConnectablePosition.Feeder> feeder = feeders.get("trf6");
        assertEquals(1, feeder.size());
        assertEquals("trf61", feeder.get(0).getName());
        assertEquals(50, feeder.get(0).getOrder().orElse(0));
    }

    @Test
    void testGetFeedersByConnectableWithInternalLine() {
        Network network = Network.read("network-node-breaker-with-new-internal-line.xml", getClass().getResourceAsStream("/network-node-breaker-with-new-internal-line.xml"));
        Map<String, List<ConnectablePosition.Feeder>> feeders = getFeedersByConnectable(network.getVoltageLevel("vl1"));
        assertEquals(2, feeders.get("lineTest").size());
        assertEquals(14, feeders.size());
        List<ConnectablePosition.Feeder> feedersLineTest = feeders.get("lineTest");
        List<Integer> ordersLineTest = new ArrayList<>();
        feedersLineTest.forEach(feeder -> ordersLineTest.add(feeder.getOrder().orElse(0)));
        Collections.sort(ordersLineTest);
        assertEquals(List.of(14, 105), ordersLineTest);
    }

}
