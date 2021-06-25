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
    void testExtentions() {
        PyPowsyblApiHeader.LoadFlowParametersPointer ptr = mock(PyPowsyblApiHeader.LoadFlowParametersPointer.class);
        when(ptr.isReadConfig()).thenReturn(true);

        MapModuleConfig olf = platformConfig.createModuleConfig("open-loadflow-default-parameters");
        olf.setStringProperty("slackBusId", "bus");
        Map<String, String> map = new HashMap<>();
        var emptyPythonReadConfig = LoadFlowParametersHelper.createLoadFlowParameters(false, map, Collections.emptyList(), ptr, platformConfig);
        assertSlackBus("bus", emptyPythonReadConfig.getExtensions());
        map.put("open-loadflow-default-parameters__slackBusId", "newBus");
        var overridedPython = LoadFlowParametersHelper.createLoadFlowParameters(false, map, Collections.emptyList(), ptr, platformConfig);
        assertSlackBus("newBus", overridedPython.getExtensions());
    }

    private void assertSlackBus(String expectedValue, Collection<Extension<LoadFlowParameters>> extensions) {
        List<Extension<LoadFlowParameters>> list = new ArrayList<>(extensions);
        Extension<LoadFlowParameters> olfParams = list.get(1);
        assertTrue(olfParams instanceof OpenLoadFlowParameters);
        assertEquals(expectedValue, ((OpenLoadFlowParameters) olfParams).getSlackBusId());
    }
}
