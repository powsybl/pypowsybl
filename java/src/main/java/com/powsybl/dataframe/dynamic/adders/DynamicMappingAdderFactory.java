/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import java.util.Map;

import com.powsybl.python.commons.PyPowsyblApiHeader.DynamicMappingType;

/**
 * @author Nicolas Pierre {@literal <nicolas.pierre@artelys.com>}
 */
public final class DynamicMappingAdderFactory {

    private static final Map<DynamicMappingType, DynamicMappingAdder> ADDERS = Map.ofEntries(
            // Equipments
            Map.entry(DynamicMappingType.ALPHA_BETA_LOAD, new AlphaBetaLoadAdder()),
            Map.entry(DynamicMappingType.ONE_TRANSFORMER_LOAD, new OneTransformerLoadAdder()),
            Map.entry(DynamicMappingType.FICTITIOUS_GENERATOR, new FictitiousGeneratorAdder()),
            Map.entry(DynamicMappingType.SYNCHRONIZED_GENERATOR, new SynchronizedGeneratorAdder()),
            Map.entry(DynamicMappingType.SYNCHRONOUS_GENERATOR, new SynchronousGeneratorAdder()),
            Map.entry(DynamicMappingType.WECC, new WeccAdder()),
            Map.entry(DynamicMappingType.GRID_FORMING_CONVERTER, new GridFormingConverterAdder()),
            // Automation systems
            Map.entry(DynamicMappingType.CURRENT_LIMIT_AUTOMATON, new CurrentLimitAutomatonAdder()));

    public static DynamicMappingAdder getAdder(DynamicMappingType type) {
        return ADDERS.get(type);
    }

    private DynamicMappingAdderFactory() {
    }
}
