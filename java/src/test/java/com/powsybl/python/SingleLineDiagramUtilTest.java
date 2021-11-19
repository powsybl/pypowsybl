/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.google.common.io.ByteStreams;
import com.powsybl.commons.TestUtil;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SingleLineDiagramUtilTest {

    @Test
    void test() throws IOException {
        Network network = FourSubstationsNodeBreakerFactory.create();
        try (StringWriter writer = new StringWriter()) {
            SingleLineDiagramUtil.writeSvg(network, "S1VL1", writer);
            assertEquals(TestUtil.normalizeLineSeparator(new String(ByteStreams.toByteArray(Objects.requireNonNull(SingleLineDiagramUtil.class.getResourceAsStream("/sld.xml"))), StandardCharsets.UTF_8)),
                         TestUtil.normalizeLineSeparator(writer.toString()));
        }
    }
}
