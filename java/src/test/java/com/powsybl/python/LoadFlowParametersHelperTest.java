/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.iidm.network.Country;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystem;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
class LoadFlowParametersHelperTest {

    InMemoryPlatformConfig platformConfig;
    FileSystem fileSystem;

    @BeforeEach
    void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        platformConfig = new InMemoryPlatformConfig(fileSystem);
    }

    @Test
    void test() {
        PyPowsyblApiHeader.LoadFlowParametersPointer ptr = mock(PyPowsyblApiHeader.LoadFlowParametersPointer.class);

        MapModuleConfig moduleConfig = platformConfig.createModuleConfig("load-flow-default-parameters");
        var expectedEnumFromPtr = LoadFlowParameters.VoltageInitMode.DC_VALUES;
        moduleConfig.setStringProperty("voltageInitMode", "PREVIOUS_VALUES");
        // non default in pointer
        when(ptr.getVoltageInitMode()).thenReturn(expectedEnumFromPtr.ordinal());

        // non default in config and default in pointer
        var expectedEnumFromFile = LoadFlowParameters.BalanceType.PROPORTIONAL_TO_LOAD;
        moduleConfig.setStringProperty("balanceType", expectedEnumFromFile.name());
        when(ptr.getBalanceType()).thenReturn(LoadFlowParameters.DEFAULT_BALANCE_TYPE.ordinal());

        moduleConfig.setStringProperty("transformerVoltageControlOn", Boolean.toString(true));
        moduleConfig.setStringProperty("noGeneratorReactiveLimits", Boolean.toString(true));
        MapModuleConfig olf = platformConfig.createModuleConfig("open-loadflow-default-parameters");
        olf.setStringProperty("slackBusId", "bus");

        when(ptr.isSimulShunt()).thenReturn(true);
        HashMap<String, String> map = new HashMap<>();
        map.put("open-loadflow-default-parameters__slackBusId", "newBus");
        List<String> countries = List.of(Country.FR.toString(), Country.GE.toString());
        LoadFlowParameters loadFlowParameters = LoadFlowParametersHelper.createLoadFlowParameters(false, map, countries, ptr, platformConfig);
        assertTrue(loadFlowParameters.isNoGeneratorReactiveLimits());
        assertTrue(loadFlowParameters.isSimulShunt());
        assertEquals(expectedEnumFromPtr, loadFlowParameters.getVoltageInitMode());
        assertEquals(expectedEnumFromFile, loadFlowParameters.getBalanceType());
        assertEquals(Set.of(Country.FR, Country.GE), loadFlowParameters.getCountriesToBalance());

        Collection<Extension<LoadFlowParameters>> extensions = loadFlowParameters.getExtensions();
        List<Extension<LoadFlowParameters>> list = new ArrayList<>(extensions);
        Extension<LoadFlowParameters> olfParams = list.get(1);
        assertTrue(olfParams instanceof OpenLoadFlowParameters);
        assertEquals("newBus", ((OpenLoadFlowParameters) olfParams).getSlackBusId());
    }
}
