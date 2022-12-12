/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.network;

import com.google.common.io.ByteStreams;
import com.powsybl.commons.TestUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SingleLineDiagramUtilTest {

    private static final Pattern SVG_FIX_PATTERN = Pattern.compile(">\\s*(<\\!\\[CDATA\\[.*?]]>)\\s*</", Pattern.DOTALL);

    /**
     * Between Java 9 and 14 an extra new lines is added before and after CDATA element. To support both Java 11 and 17
     * we need to remove these new lines => To remove when migrating to Java 17.
     *
     * See https://stackoverflow.com/questions/55853220/handling-change-in-newlines-by-xml-transformation-for-cdata-from-java-8-to-java
     */
    private static String fixSvg(String svg) {
        return SVG_FIX_PATTERN.matcher(Objects.requireNonNull(svg)).replaceAll(">$1</");
    }

    @Test
    void test() throws IOException {
        Network network = FourSubstationsNodeBreakerFactory.create();
        try (StringWriter writer = new StringWriter()) {
            SingleLineDiagramUtil.writeSvg(network, "S1VL1", writer);
            assertEquals(TestUtil.normalizeLineSeparator(new String(ByteStreams.toByteArray(Objects.requireNonNull(SingleLineDiagramUtil.class.getResourceAsStream("/sld.xml"))), StandardCharsets.UTF_8)),
                         fixSvg(TestUtil.normalizeLineSeparator(writer.toString())));
        }
    }

    @Test
    void testSvgAndMetadata() throws IOException {
        Network network = FourSubstationsNodeBreakerFactory.create();
        NetworkCFunctions.LayoutParametersExt layoutParametersExt = new NetworkCFunctions.LayoutParametersExt();
        try (StringWriter writer = new StringWriter(); StringWriter metadataWriter = new StringWriter()) {
            List<String> svgAndMeta = SingleLineDiagramUtil.getSvgAndMetadata(network, "S1VL1", layoutParametersExt);
            assertEquals(TestUtil.normalizeLineSeparator(new String(ByteStreams.toByteArray(Objects.requireNonNull(SingleLineDiagramUtil.class.getResourceAsStream("/sld.xml"))), StandardCharsets.UTF_8)),
                    fixSvg(TestUtil.normalizeLineSeparator(svgAndMeta.get(0))));
            assertTrue(svgAndMeta.get(1).length() > 0);
        }
    }

}
