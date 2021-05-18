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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
class SeriesArrayHelperTest {

    @Test
    void testGetter() {
        Network network = EurostagTutorialExample1Factory.create();

        final SeriesPointerArrayBuilder builder = SeriesArrayHelper.prepareData(network, PyPowsyblApiHeader.ElementType.BUS);

        assertEquals("VLGEN_0", ((SeriesPointerArrayBuilder.StringSeries) builder.seriesList.get(0)).stringGetter.apply(builder.getElements().get(0)));
    }

    @Test
    void testUpdate() {
        Network network = EurostagTutorialExample1Factory.create();

        SeriesArrayHelper.updateNetworkElementsWithDoubleSeries(network, PyPowsyblApiHeader.ElementType.GENERATOR, 1, "target_p", i -> "GEN", i -> 33.0d);
        assertEquals(33.0d, network.getGenerator("GEN").getTargetP());
    }

}
