/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class NetworkUtilTest {

    @Test
    void test() {
        Network network = EurostagTutorialExample1Factory.create();
        List<String> elementsIds = NetworkUtil.getElementsIds(network, PyPowsyblApiHeader.ElementType.TWO_WINDINGS_TRANSFORMER, Collections.singleton(24.0), Collections.singleton("FR"), true, true, false);
        assertEquals(Collections.singletonList("NGEN_NHV1"), elementsIds);
    }

    @Test
    void testCurrentLimits() {
        var network = EurostagTutorialExample1Factory.createWithFixedCurrentLimits();
        Stream<TemporaryLimitContext> currentLimits = NetworkUtil.getCurrentLimits(network);
        assertThat(currentLimits).hasSize(9).element(0).satisfies(l -> {
            assertEquals("NHV1_NHV2_1", l.getBranchId());
            assertEquals("permanent_limit", l.getName());
            assertEquals(Branch.Side.ONE, l.getSide());
            assertEquals(500.0, l.getValue());
            assertEquals(-1, l.getAcceptableDuration());
            assertFalse(l.isFictitious());
        });
    }
}
