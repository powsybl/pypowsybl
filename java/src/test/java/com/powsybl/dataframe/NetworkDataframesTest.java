/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.dataframe.DataframeHandlerImpl.Series;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
class NetworkDataframesTest {

    @Test
    void buses() {
        Network network = EurostagTutorialExample1Factory.create();

        List<Series> series = new ArrayList<>();
        NetworkDataframes.buses().createDataframe(network, new DataframeHandlerImpl(series::add));

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "v_mag", "v_angle");
        assertThat(series.get(1).getDoubles())
            .containsExactly(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
    }

    private DoubleIndexedSeries createInput(List<String> names, double... values) {
        return new DoubleIndexedSeries() {
            @Override
            public int getSize() {
                return names.size();
            }

            @Override
            public String getId(int index) {
                return names.get(index);
            }

            @Override
            public double getValue(int index) {
                return values[index];
            }
        };
    }

    @Test
    void generators() {
        Network network = EurostagTutorialExample1Factory.create();

        List<Series> series = new ArrayList<>();
        NetworkDataframes.generators().createDataframe(network, new DataframeHandlerImpl(series::add));

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "energy_source", "target_p", "max_p", "min_p", "target_v",
                             "target_q", "voltage_regulator_on", "p", "q", "voltage_level_id", "bus_id");

        assertThat(series.get(2).getDoubles())
            .containsExactly(607);

        NetworkDataframes.generators().updateDoubleSeries(network, "target_p", createInput(List.of("GEN"), 500));

        assertEquals(500, network.getGenerator("GEN").getTargetP());
    }
}
