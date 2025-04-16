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
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerFactory;
import com.powsybl.sld.SldParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SingleLineDiagramUtilTest {

    @Test
    void test() throws IOException {
        Network network = FourSubstationsNodeBreakerFactory.create();
        try (StringWriter writer = new StringWriter()) {
            SingleLineDiagramUtil.writeSvg(network, "S1VL1", writer);
            assertEquals(TestUtil.normalizeLineSeparator(new String(ByteStreams.toByteArray(Objects.requireNonNull(SingleLineDiagramUtil.class.getResourceAsStream("/sld.svg"))), StandardCharsets.UTF_8)),
                         TestUtil.normalizeLineSeparator(writer.toString()));
        }
    }

    @Test
    void testSvgAndMetadata() throws IOException {
        Network network = FourSubstationsNodeBreakerFactory.create();
        SldParameters sldParameters = SingleLineDiagramUtil.createSldParameters();
        List<String> svgAndMeta = SingleLineDiagramUtil.getSvgAndMetadata(network, "S1VL1", sldParameters);

        assertEquals(TestUtil.normalizeLineSeparator(new String(ByteStreams.toByteArray(Objects.requireNonNull(SingleLineDiagramUtil.class.getResourceAsStream("/sld.svg"))), StandardCharsets.UTF_8)),
                     TestUtil.normalizeLineSeparator(svgAndMeta.get(0)));
        assertFalse(svgAndMeta.get(1).isEmpty());
    }

    @Test
    void testWriteMatrixMultiSubstationSvg() throws IOException {
        Network network = FourSubstationsNodeBreakerFactory.create();
        SldParameters sldParameters = SingleLineDiagramUtil.createSldParameters();
        String[][] matrixIds = {{"S1", "" }, {"", "S2"}};
        try (StringWriter writer = new StringWriter(); StringWriter writerMeta = new StringWriter()) {
            SingleLineDiagramUtil.writeMatrixMultiSubstationSvg(network, matrixIds, writer, writerMeta, sldParameters);
            assertFalse(writer.toString().isEmpty());
            assertFalse(writerMeta.toString().isEmpty());
        }
    }
}
