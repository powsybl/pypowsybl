/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawo.builders.ModelInfo;
import com.powsybl.python.dynamic.PythonDynamicModelsSupplier;

/**
 * @author Nicolas Pierre {@literal <nicolas.pierre@artelys.com>}
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public final class DynamicMappingHandler {

    private static final SortedMap<String, DynamicMappingAdder> ADDERS = loadModelAdders();

    private static SortedMap<String, DynamicMappingAdder> loadModelAdders() {
        return ServiceLoader.load(ModelAdderLoader.class).stream()
                .flatMap(ma -> ma.get().getModelAdders().stream())
                .collect(Collectors.toMap(DynamicMappingAdder::getCategory,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        TreeMap::new));
    }

    public static void addElements(String category, PythonDynamicModelsSupplier modelMapping, List<UpdatingDataframe> dataframes) {
        DynamicMappingAdder adder = ADDERS.get(category);
        if (adder == null) {
            throw new PowsyblException("No category named " + category);
        }
        adder.addElements(modelMapping, dataframes);
    }

    public static List<List<SeriesMetadata>> getMetadata(String category) {
        DynamicMappingAdder adder = ADDERS.get(category);
        if (adder == null) {
            throw new PowsyblException("No category named " + category);
        }
        return adder.getMetadata();
    }

    public static Collection<String> getCategories() {
        return ADDERS.keySet();
    }

    public static Collection<DynamicMappingAdder> getDynamicMappingAdders() {
        return ADDERS.values();
    }

    public static Collection<String> getSupportedModels(String category) {
        DynamicMappingAdder adder = ADDERS.get(category);
        if (adder == null) {
            return Collections.emptyList();
        }
        return adder.getSupportedModels().stream().map(ModelInfo::name).toList();
    }

    public static Collection<String> getAllSupportedModels() {
        return ADDERS.values().stream()
                .flatMap(m -> m.getSupportedModels().stream())
                .map(ModelInfo::name)
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    public static Collection<ModelInfo> getSupportedModelsInformation(String category) {
        DynamicMappingAdder adder = ADDERS.get(category);
        if (adder == null) {
            return Collections.emptyList();
        }
        return adder.getSupportedModels();
    }

    private DynamicMappingHandler() {
    }
}
