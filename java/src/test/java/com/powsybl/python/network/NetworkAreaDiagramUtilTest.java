/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.google.common.io.ByteStreams;
import com.powsybl.commons.test.TestUtil;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.*;
import com.powsybl.nad.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.powsybl.python.network.NetworkAreaDiagramUtil.createNadParameters;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class NetworkAreaDiagramUtilTest {

    @Test
    void test() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();
        String svg = NetworkAreaDiagramUtil.getSvg(network, Collections.emptyList(), createNadParameters());
        assertEquals(TestUtil.normalizeLineSeparator(new String(ByteStreams.toByteArray(Objects.requireNonNull(NetworkAreaDiagramUtil.class.getResourceAsStream("/nad.svg"))), StandardCharsets.UTF_8)),
                     TestUtil.normalizeLineSeparator(svg));
    }

    @Test
    void testSvgAndMetadata() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();
        List<String> svgAndMeta = NetworkAreaDiagramUtil.getSvgAndMetadata(network, Collections.emptyList(), createNadParameters());
        assertEquals(TestUtil.normalizeLineSeparator(new String(ByteStreams.toByteArray(Objects.requireNonNull(NetworkAreaDiagramUtil.class.getResourceAsStream("/nad.svg"))), StandardCharsets.UTF_8)),
                TestUtil.normalizeLineSeparator(svgAndMeta.get(0)));
        assertFalse(svgAndMeta.get(1).isEmpty());
    }

    @Test
    void testGetVisibleVoltageLevels() {
        Network network = EurostagTutorialExample1Factory.createWithTieLine();
        List<String> ids = NetworkAreaDiagram.getDisplayedVoltageLevels(network, List.of("VLHV1"), 1);
        assertEquals("VLGEN, VLHV1, VLHV2", String.join(", ", ids));

        ids = NetworkAreaDiagram.getDisplayedVoltageLevels(network, List.of("VLHV1"), 2);
        assertEquals("VLGEN, VLHV1, VLHV2, VLLOAD", String.join(", ", ids));
    }
}
