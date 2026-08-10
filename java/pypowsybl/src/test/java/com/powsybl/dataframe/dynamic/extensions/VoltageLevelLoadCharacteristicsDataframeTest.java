/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.extensions;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.DataframeFilter;
import com.powsybl.dataframe.impl.DefaultDataframeHandler;
import com.powsybl.dataframe.impl.Series;
import com.powsybl.dataframe.network.NetworkDataframeContext;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframes;
import com.powsybl.dataframe.network.adders.NetworkElementAdders;
import com.powsybl.dataframe.network.extensions.NetworkExtensions;
import com.powsybl.dataframe.update.DefaultUpdatingDataframe;
import com.powsybl.dataframe.update.TestStringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawo.extensions.api.voltage.VoltageLevelLoadCharacteristics;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class VoltageLevelLoadCharacteristicsDataframeTest {

    private static final String EXTENSION_NAME = "voltageLevelLoadCharacteristics";

    @Test
    void createReadUpdateAndRemove() {
        Network network = EurostagTutorialExample1Factory.create();
        assertNull(network.getVoltageLevel("VLLOAD").getExtension(VoltageLevelLoadCharacteristics.class));

        DefaultUpdatingDataframe dataframe = new DefaultUpdatingDataframe(1);
        dataframe.addSeries("id", true, new TestStringSeries("VLLOAD"));
        dataframe.addSeries("characteristic", false, new TestStringSeries("INDUSTRIAL"));
        NetworkElementAdders.addExtensions(EXTENSION_NAME, network, singletonList(dataframe));

        VoltageLevelLoadCharacteristics extension =
                network.getVoltageLevel("VLLOAD").getExtension(VoltageLevelLoadCharacteristics.class);
        assertNotNull(extension);
        assertEquals(VoltageLevelLoadCharacteristics.Type.INDUSTRIAL, extension.getCharacteristic());

        List<Series> series = createExtensionDataFrame(network);
        assertThat(series)
                .extracting(Series::getName)
                .containsExactly("id", "characteristic");
        assertThat(series.get(0).getStrings()).containsExactly("VLLOAD");
        assertThat(series.get(1).getStrings()).containsExactly("INDUSTRIAL");

        DefaultUpdatingDataframe update = new DefaultUpdatingDataframe(1);
        update.addSeries("id", true, new TestStringSeries("VLLOAD"));
        update.addSeries("characteristic", false, new TestStringSeries("CONSTANT"));
        updateExtension(network, update);
        assertEquals(VoltageLevelLoadCharacteristics.Type.CONSTANT,
                network.getVoltageLevel("VLLOAD").getExtension(VoltageLevelLoadCharacteristics.class).getCharacteristic());
        assertThat(createExtensionDataFrame(network).get(1).getStrings()).containsExactly("CONSTANT");

        NetworkExtensions.removeExtensions(network, EXTENSION_NAME, List.of("VLLOAD"));
        assertNull(network.getVoltageLevel("VLLOAD").getExtension(VoltageLevelLoadCharacteristics.class));
        assertEquals(0, createExtensionDataFrame(network).get(0).getStrings().length);
    }

    @Test
    void createOnSeveralVoltageLevels() {
        Network network = EurostagTutorialExample1Factory.create();
        DefaultUpdatingDataframe dataframe = new DefaultUpdatingDataframe(2);
        dataframe.addSeries("id", true, new TestStringSeries("VLGEN", "VLLOAD"));
        dataframe.addSeries("characteristic", false, new TestStringSeries("CONSTANT", "INDUSTRIAL"));
        NetworkElementAdders.addExtensions(EXTENSION_NAME, network, singletonList(dataframe));

        List<Series> series = createExtensionDataFrame(network);
        assertThat(series.get(0).getStrings()).containsExactly("VLGEN", "VLLOAD");
        assertThat(series.get(1).getStrings()).containsExactly("CONSTANT", "INDUSTRIAL");
    }

    @Test
    void createOnAnUnknownVoltageLevelThrows() {
        Network network = EurostagTutorialExample1Factory.create();
        DefaultUpdatingDataframe dataframe = new DefaultUpdatingDataframe(1);
        dataframe.addSeries("id", true, new TestStringSeries("UNKNOWN"));
        dataframe.addSeries("characteristic", false, new TestStringSeries("CONSTANT"));
        List<UpdatingDataframe> dataframes = singletonList(dataframe);

        PowsyblException e = assertThrows(PowsyblException.class,
                () -> NetworkElementAdders.addExtensions(EXTENSION_NAME, network, dataframes));
        assertEquals("Voltage level 'UNKNOWN' does not exist.", e.getMessage());
    }

    @Test
    void createWithoutCharacteristicThrows() {
        Network network = EurostagTutorialExample1Factory.create();
        DefaultUpdatingDataframe dataframe = new DefaultUpdatingDataframe(1);
        dataframe.addSeries("id", true, new TestStringSeries("VLLOAD"));
        List<UpdatingDataframe> dataframes = singletonList(dataframe);

        // the characteristic has no default value, the extension cannot be created without it
        PowsyblException e = assertThrows(PowsyblException.class,
                () -> NetworkElementAdders.addExtensions(EXTENSION_NAME, network, dataframes));
        assertEquals("Required column characteristic is missing.", e.getMessage());
    }

    private static List<Series> createExtensionDataFrame(Network network) {
        List<Series> series = new ArrayList<>();
        NetworkDataframeMapper mapper = NetworkDataframes.getExtensionDataframeMapper(EXTENSION_NAME, null);
        assertNotNull(mapper);
        mapper.createDataframe(network, new DefaultDataframeHandler(series::add), new DataframeFilter(),
                NetworkDataframeContext.DEFAULT);
        return series;
    }

    private static void updateExtension(Network network, UpdatingDataframe updatingDataframe) {
        NetworkDataframeMapper mapper = NetworkDataframes.getExtensionDataframeMapper(EXTENSION_NAME, null);
        assertNotNull(mapper);
        mapper.updateSeries(network, updatingDataframe, NetworkDataframeContext.DEFAULT);
    }
}
