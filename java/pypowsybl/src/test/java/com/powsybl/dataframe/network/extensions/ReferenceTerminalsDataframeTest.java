/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.extensions;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.DataframeFilter;
import com.powsybl.dataframe.impl.DefaultDataframeHandler;
import com.powsybl.dataframe.impl.Series;
import com.powsybl.dataframe.network.NetworkDataframeContext;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframes;
import com.powsybl.dataframe.network.adders.NetworkElementAdders;
import com.powsybl.dataframe.update.DefaultUpdatingDataframe;
import com.powsybl.dataframe.update.TestStringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.ReferenceTerminals;
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
class ReferenceTerminalsDataframeTest {

    @Test
    void referenceTerminalsIsEmptyWhenNotSet() {
        Network network = EurostagTutorialExample1Factory.create();
        assertThat(createExtensionDataFrame(ReferenceTerminals.NAME, network).get(0).getStrings()).isEmpty();
    }

    @Test
    void referenceTerminalsCreateReadAndRemove() {
        Network network = EurostagTutorialExample1Factory.create();

        DefaultUpdatingDataframe dataframe = new DefaultUpdatingDataframe(2);
        dataframe.addSeries("element_id", true, new TestStringSeries("GEN", "NHV1_NHV2_1"));
        dataframe.addSeries("side", false, new TestStringSeries("ONE", "TWO"));
        NetworkElementAdders.addExtensions(ReferenceTerminals.NAME, network, singletonList(dataframe));

        ReferenceTerminals extension = network.getExtension(ReferenceTerminals.class);
        assertNotNull(extension);
        assertEquals(2, extension.getReferenceTerminals().size());

        List<Series> series = createExtensionDataFrame(ReferenceTerminals.NAME, network);
        assertThat(series).extracting(Series::getName).containsExactly("element_id", "side");
        assertThat(series.get(0).getStrings()).containsExactlyInAnyOrder("GEN", "NHV1_NHV2_1");

        // creating again replaces the whole set rather than adding to it
        DefaultUpdatingDataframe replacement = new DefaultUpdatingDataframe(1);
        replacement.addSeries("element_id", true, new TestStringSeries("LOAD"));
        replacement.addSeries("side", false, new TestStringSeries("ONE"));
        NetworkElementAdders.addExtensions(ReferenceTerminals.NAME, network, singletonList(replacement));
        assertThat(createExtensionDataFrame(ReferenceTerminals.NAME, network).get(0).getStrings())
                .containsExactly("LOAD");

        NetworkExtensions.removeExtensions(network, ReferenceTerminals.NAME, List.of());
        assertNull(network.getExtension(ReferenceTerminals.class));
        assertThat(createExtensionDataFrame(ReferenceTerminals.NAME, network).get(0).getStrings()).isEmpty();
    }

    @Test
    void referenceTerminalsOnAnUnknownElementThrows() {
        Network network = EurostagTutorialExample1Factory.create();
        DefaultUpdatingDataframe dataframe = new DefaultUpdatingDataframe(1);
        dataframe.addSeries("element_id", true, new TestStringSeries("UNKNOWN"));
        dataframe.addSeries("side", false, new TestStringSeries("ONE"));
        List<UpdatingDataframe> dataframes = singletonList(dataframe);

        assertThrows(PowsyblException.class,
                () -> NetworkElementAdders.addExtensions(ReferenceTerminals.NAME, network, dataframes));
    }

    private static List<Series> createExtensionDataFrame(String name, Network network) {
        List<Series> series = new ArrayList<>();
        NetworkDataframeMapper mapper = NetworkDataframes.getExtensionDataframeMapper(name, null);
        assertNotNull(mapper);
        mapper.createDataframe(network, new DefaultDataframeHandler(series::add), new DataframeFilter(),
                NetworkDataframeContext.DEFAULT);
        return series;
    }

    private static void updateExtension(String name, Network network, UpdatingDataframe updatingDataframe) {
        NetworkDataframeMapper mapper = NetworkDataframes.getExtensionDataframeMapper(name, null);
        assertNotNull(mapper);
        mapper.updateSeries(network, updatingDataframe, NetworkDataframeContext.DEFAULT);
    }
}
