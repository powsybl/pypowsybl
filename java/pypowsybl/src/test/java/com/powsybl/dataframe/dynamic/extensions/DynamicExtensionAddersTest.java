/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.extensions;

import com.powsybl.dataframe.network.adders.NetworkElementAdders;
import com.powsybl.dataframe.update.DefaultUpdatingDataframe;
import com.powsybl.dataframe.update.TestDoubleSeries;
import com.powsybl.dataframe.update.TestIntSeries;
import com.powsybl.dataframe.update.TestStringSeries;
import com.powsybl.dynawo.extensions.api.generator.RpclType;
import com.powsybl.dynawo.extensions.api.generator.SynchronizedGeneratorProperties;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.extensions.api.generator.connection.GeneratorConnectionLevel;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
class DynamicExtensionAddersTest {

    @Test
    void synchronousGeneratorPropertiesExtension() {
        var network = EurostagTutorialExample1Factory.create();
        String genId = "GEN";
        SynchronousGeneratorProperties extension = network.getGenerator(genId).getExtension(SynchronousGeneratorProperties.class);
        assertNull(extension);
        SynchronousGeneratorProperties.Windings numberOfWindings = SynchronousGeneratorProperties.Windings.FOUR_WINDINGS;
        String governor = "Proportional";
        String voltageRegulator = "Proportional";
        String pss = "";
        RpclType rpcl = RpclType.RPCL1;
        SynchronousGeneratorProperties.Uva uva = SynchronousGeneratorProperties.Uva.DISTANT;

        DefaultUpdatingDataframe dataframe = new DefaultUpdatingDataframe(1);
        addStringColumn(dataframe, "id", genId);
        addStringColumn(dataframe, "numberOfWindings", String.valueOf(numberOfWindings));
        addStringColumn(dataframe, "governor", governor);
        addStringColumn(dataframe, "voltageRegulator", voltageRegulator);
        addStringColumn(dataframe, "pss", pss);
        addIntColumn(dataframe, "auxiliaries", 1);
        addIntColumn(dataframe, "internalTransformer", 0);
        addStringColumn(dataframe, "rpcl", String.valueOf(rpcl));
        addStringColumn(dataframe, "uva", String.valueOf(uva));
        addIntColumn(dataframe, "fictitious", 0);
        addIntColumn(dataframe, "qlim", 0);
        NetworkElementAdders.addExtensions("synchronousGeneratorProperties", network, singletonList(dataframe));

        extension = network.getGenerator(genId).getExtension(SynchronousGeneratorProperties.class);
        assertNotNull(extension);
        assertEquals(numberOfWindings, extension.getNumberOfWindings());
        assertEquals(governor, extension.getGovernor());
        assertEquals(voltageRegulator, extension.getVoltageRegulator());
        assertEquals(pss, extension.getPss());
        assertTrue(extension.isAuxiliaries());
        assertFalse(extension.isInternalTransformer());
        assertEquals(rpcl, extension.getRpcl());
        assertEquals(uva, extension.getUva());
        assertFalse(extension.isAggregated());
        assertFalse(extension.isQlim());
    }

    @Test
    void synchronizedGeneratorPropertiesExtension() {
        var network = EurostagTutorialExample1Factory.create();
        String genId = "GEN";
        SynchronizedGeneratorProperties extension = network.getGenerator(genId).getExtension(SynchronizedGeneratorProperties.class);
        assertNull(extension);
        String type = "PV";

        DefaultUpdatingDataframe dataframe = new DefaultUpdatingDataframe(1);
        addStringColumn(dataframe, "id", genId);
        addStringColumn(dataframe, "type", type);
        addIntColumn(dataframe, "rpcl2", 0);
        NetworkElementAdders.addExtensions("synchronizedGeneratorProperties", network, singletonList(dataframe));

        extension = network.getGenerator(genId).getExtension(SynchronizedGeneratorProperties.class);
        assertNotNull(extension);
        assertEquals(type, extension.getType());
        assertFalse(extension.isRpcl2());
    }

    @Test
    void generatorConnectionLevelExtension() {
        var network = EurostagTutorialExample1Factory.create();
        String genId = "GEN";
        GeneratorConnectionLevel extension = network.getGenerator(genId).getExtension(GeneratorConnectionLevel.class);
        assertNull(extension);
        String level = "TSO";

        DefaultUpdatingDataframe dataframe = new DefaultUpdatingDataframe(1);
        addStringColumn(dataframe, "id", genId);
        addStringColumn(dataframe, "level", level);
        NetworkElementAdders.addExtensions("generatorConnectionLevel", network, singletonList(dataframe));

        extension = network.getGenerator(genId).getExtension(GeneratorConnectionLevel.class);
        assertNotNull(extension);
        assertEquals(GeneratorConnectionLevel.GeneratorConnectionLevelType.valueOf(level), extension.getLevel());
    }

    private void addStringColumn(DefaultUpdatingDataframe dataframe, String column, String... value) {
        dataframe.addSeries(column, false, new TestStringSeries(value));
    }

    private void addIntColumn(DefaultUpdatingDataframe dataframe, String column, int... value) {
        dataframe.addSeries(column, false, new TestIntSeries(value));
    }
}
