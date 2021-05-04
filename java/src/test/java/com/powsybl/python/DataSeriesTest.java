/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
class DataSeriesTest {

    @Test
    void test() {
        Network network = EurostagTutorialExample1Factory.create();

        List<Bus> buses = network.getBusView().getBusStream().collect(Collectors.toList());
        final BusSeriesPointerArrayBuilder busSeriesPointerArrayBuilder = new BusSeriesPointerArrayBuilder(buses);
        busSeriesPointerArrayBuilder.convert();
        assertEquals("VLGEN_0", ((SeriesPointerArrayBuilder.StringSeries) busSeriesPointerArrayBuilder.seriesList.get(0)).stringGetter.apply(busSeriesPointerArrayBuilder.getElements().get(0)));
    }

}
