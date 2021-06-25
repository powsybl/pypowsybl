/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.commons.config.ModuleConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
class MapModuleConfigRepositoryTest {

    @Test
    void test() {
        Map<String, String> fakeEnvMap = new HashMap<>();
        fakeEnvMap.put("mod__s", "hello");
        fakeEnvMap.put("mod__i", "3");
        fakeEnvMap.put("mod2__s", "hi");
        MapModuleConfigRepository sut = new MapModuleConfigRepository(fakeEnvMap);
        Optional<ModuleConfig> modOpt = sut.getModuleConfig("mod");
        assertTrue(modOpt.isPresent());
        ModuleConfig moduleConfig = modOpt.get();
        assertEquals("hello", moduleConfig.getStringProperty("s"));
    }
}
