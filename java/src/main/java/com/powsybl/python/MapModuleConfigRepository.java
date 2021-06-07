/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.google.common.base.Strings;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.ModuleConfigRepository;

import java.util.*;

/**
 * A {@link ModuleConfigRepository} designed to read property values
 * from a map.
 *
 * For a configuration property named "property-name" in module "module-name",
 * the expected key of the map is module-name__property-name.
 *
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
public class MapModuleConfigRepository implements ModuleConfigRepository {

    private static final String SEPARATOR = "__";

    private final Map<String, Properties> normalized = new HashMap<>();

    public MapModuleConfigRepository(Map<String, String> map) {
        Objects.requireNonNull(map);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            String[] split = k.split(SEPARATOR);
            String moduleName = split[0];
            String propertyName = split[1];
            normalized.computeIfAbsent(moduleName, moduleNameKey -> new Properties());
            normalized.get(moduleName).computeIfAbsent(propertyName, propertyNameKey -> v);
        }
    }

    @Override
    public boolean moduleExists(String name) {
        if (Strings.isNullOrEmpty(name)) {
            return false;
        }
        return normalized.containsKey(name);
    }

    @Override
    public Optional<ModuleConfig> getModuleConfig(String name) {
        if (!moduleExists(name)) {
            return Optional.empty();
        }
        MapModuleConfig mapModuleConfig = new MapModuleConfig(normalized.get(name));
        return Optional.of(mapModuleConfig);
    }
}
