/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.network;

import com.google.common.io.ByteStreams;
import com.powsybl.commons.test.TestUtil;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class NetworkAreaDiagramUtilTest {

    @Test
    void test() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();
        String svg = NetworkAreaDiagramUtil.getSvg(network, Collections.emptyList(), 0);
        assertEquals(TestUtil.normalizeLineSeparator(new String(ByteStreams.toByteArray(Objects.requireNonNull(NetworkAreaDiagramUtil.class.getResourceAsStream("/nad.svg"))), StandardCharsets.UTF_8)),
                     TestUtil.normalizeLineSeparator(svg));
    }
}
