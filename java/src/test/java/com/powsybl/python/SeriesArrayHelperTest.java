/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
class SeriesArrayHelperTest {

    @Test
    void test() {
        Network network = EurostagTutorialExample1Factory.create();

        SeriesPointerArrayBuilder builder = SeriesArrayHelper.prepareData(network, PyPowsyblApiHeader.ElementType.BUS);

        List list = builder.buildJavaSeries();
        List ids = (List) list.get(0);
        List<String> expectedIds = Arrays.asList("VLGEN_0", "VLHV1_0", "VLHV2_0", "VLLOAD_0");
        assertEquals(expectedIds, ids);
    }

    @Test
    void testUpdate() {
        Network network = EurostagTutorialExample1Factory.create();

        SeriesArrayHelper.updateNetworkElementsWithDoubleSeries(network, PyPowsyblApiHeader.ElementType.GENERATOR, 1, "target_p", i -> "GEN", i -> 33.0d);
        assertEquals(33.0d, network.getGenerator("GEN").getTargetP());
    }

}
