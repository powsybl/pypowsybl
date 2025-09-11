/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawo.builders.ModelInfo;
import com.powsybl.python.dynamic.PythonDynamicModelsSupplier;

/**
 * @author Nicolas Pierre {@literal <nicolas.pierre@artelys.com>}
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public final class DynamicMappingHandler {

    private static final Map<String, DynamicMappingAdder> ADDERS = loadModelAdders();

    private static Map<String, DynamicMappingAdder> loadModelAdders() {
        return ServiceLoader.load(ModelAdderLoader.class).stream()
                .flatMap(ma -> ma.get().getModelAdders().stream())
                .collect(Collectors.toMap(DynamicMappingAdder::getCategory, Function.identity()));
    }

    public static void addElements(String category, PythonDynamicModelsSupplier modelMapping, List<UpdatingDataframe> dataframes) {
        ADDERS.get(category).addElements(modelMapping, dataframes);
    }

    public static List<List<SeriesMetadata>> getMetadata(String category) {
        return ADDERS.get(category).getMetadata();
    }

    public static Collection<String> getCategories() {
        return ADDERS.keySet().stream().sorted().toList();
    }

    public static Collection<String> getSupportedModels(String category) {
        return ADDERS.get(category).getSupportedModels().stream().map(ModelInfo::name).toList();
    }

    private DynamicMappingHandler() {
    }
}
