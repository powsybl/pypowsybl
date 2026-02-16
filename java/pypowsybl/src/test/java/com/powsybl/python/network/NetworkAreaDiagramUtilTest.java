/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.google.common.io.ByteStreams;
import com.powsybl.commons.test.TestUtil;
import com.powsybl.dataframe.DataframeFilter;
import com.powsybl.dataframe.impl.DefaultDataframeHandler;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.*;
import com.powsybl.nad.*;
import com.powsybl.nad.svg.CustomLabelProvider;
import com.powsybl.nad.svg.SvgParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.powsybl.python.network.NetworkAreaDiagramUtil.createNadParameters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class NetworkAreaDiagramUtilTest {

    @Test
    void test() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();
        String svg = NetworkAreaDiagramUtil.getSvg(network, Collections.emptyList(), createNadParameters());
        assertEquals(TestUtil.normalizeLineSeparator(new String(ByteStreams.toByteArray(Objects.requireNonNull(NetworkAreaDiagramUtil.class.getResourceAsStream("/nad.svg"))), StandardCharsets.UTF_8)),
                     TestUtil.normalizeLineSeparator(svg));
    }

    @Test
    void testSvgAndMetadata() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();
        List<String> svgAndMeta = NetworkAreaDiagramUtil.getSvgAndMetadata(network, Collections.emptyList(), createNadParameters());
        assertEquals(TestUtil.normalizeLineSeparator(new String(ByteStreams.toByteArray(Objects.requireNonNull(NetworkAreaDiagramUtil.class.getResourceAsStream("/nad.svg"))), StandardCharsets.UTF_8)),
                TestUtil.normalizeLineSeparator(svgAndMeta.get(0)));
        assertFalse(svgAndMeta.get(1).isEmpty());
    }

    @Test
    void testGetVisibleVoltageLevels() {
        Network network = EurostagTutorialExample1Factory.createWithTieLine();
        List<String> ids = NetworkAreaDiagram.getDisplayedVoltageLevels(network, List.of("VLHV1"), 1);
        assertEquals("VLGEN, VLHV1, VLHV2", String.join(", ", ids));

        ids = NetworkAreaDiagram.getDisplayedVoltageLevels(network, List.of("VLHV1"), 2);
        assertEquals("VLGEN, VLHV1, VLHV2, VLLOAD", String.join(", ", ids));
    }

    @Test
    void testExtractingLabelsFromNadLabelProvider() {
        Network network = IeeeCdfNetworkFactory.create14();
        network.getGeneratorStream().forEach(gen -> gen.getTerminal().setP(200.0));

        assertNotNull(network);

        SvgParameters pars = new SvgParameters()
                .setSvgWidthAndHeightAdded(true);

        List<com.powsybl.dataframe.impl.Series> series = new ArrayList<>();
        Map<String, CustomLabelProvider.BranchLabels> labelMap = NetworkAreaDiagramUtil.getBranchLabelsMap(network, pars);
        NetworkAreaDiagramUtil.BRANCH_LABELS_MAPPER.createDataframe(labelMap, new DefaultDataframeHandler(series::add), new DataframeFilter());
        assertThat(series)
                .extracting(com.powsybl.dataframe.impl.Series::getName)
                .containsExactly("id", "side1", "middle", "side2", "arrow1", "arrow2");

        List<com.powsybl.dataframe.impl.Series> seriesTwt = new ArrayList<>();
        Map<String, CustomLabelProvider.ThreeWtLabels> labelTwtMap = NetworkAreaDiagramUtil.getThreeWtBranchLabelsMap(network, pars);
        NetworkAreaDiagramUtil.TWT_LABELS_MAPPER.createDataframe(labelTwtMap, new DefaultDataframeHandler(seriesTwt::add), new DataframeFilter());
        assertThat(seriesTwt)
                .extracting(com.powsybl.dataframe.impl.Series::getName)
                .containsExactly("id", "side1", "side2", "side3", "arrow1", "arrow2", "arrow3");

        List<com.powsybl.dataframe.impl.Series> seriesBusDescription = new ArrayList<>();
        Map<String, String> busDescriptionMap = NetworkAreaDiagramUtil.getBusDescriptionsMap(network, pars);
        NetworkAreaDiagramUtil.BUS_DESCRIPTIONS_MAPPER.createDataframe(busDescriptionMap, new DefaultDataframeHandler(seriesBusDescription::add), new DataframeFilter());
        assertThat(seriesBusDescription)
                .extracting(com.powsybl.dataframe.impl.Series::getName)
                .containsExactly("id", "description");

        List<com.powsybl.dataframe.impl.Series> seriesVlInfos = new ArrayList<>();
        List<NetworkAreaDiagramUtil.VlInfos> vlInfos = NetworkAreaDiagramUtil.getVlDescriptionsWithType(network, pars);
        NetworkAreaDiagramUtil.VL_DESCRIPTIONS_MAPPER.createDataframe(vlInfos, new DefaultDataframeHandler(seriesVlInfos::add), new DataframeFilter());
        assertThat(seriesVlInfos)
                .extracting(com.powsybl.dataframe.impl.Series::getName)
                .containsExactly("id", "type", "description");

    }
}
