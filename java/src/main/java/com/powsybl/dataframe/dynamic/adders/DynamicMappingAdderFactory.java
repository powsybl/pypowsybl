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
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 */
public final class DynamicMappingAdderFactory {

    private static final Map<DynamicMappingType, DynamicMappingAdder> ADDERS = Map.ofEntries(
            Map.entry(DynamicMappingType.ALPHA_BETA_LOAD, new AlphaBetaLoadAdder()),
            Map.entry(DynamicMappingType.ONE_TRANSFORMER_LOAD, new OneTransformerLoadAdder()),
            Map.entry(DynamicMappingType.GENERATOR_SYNCHRONOUS_THREE_WINDINGS,
                    new GeneratorSynchronousThreeWindingsAdder()),
            Map.entry(DynamicMappingType.GENERATOR_SYNCHRONOUS_THREE_WINDINGS_PROPORTIONAL_REGULATIONS,
                    new GeneratorSynchronousThreeWindingsProportionalRegulationsAdder()),
            Map.entry(DynamicMappingType.GENERATOR_SYNCHRONOUS_FOUR_WINDINGS,
                    new GeneratorSynchronousFourWindingsAdder()),
            Map.entry(DynamicMappingType.GENERATOR_SYNCHRONOUS_FOUR_WINDINGS_PROPORTIONAL_REGULATIONS,
                    new GeneratorSynchronousFourWindingsProportionalRegulationsAdder()),
            Map.entry(DynamicMappingType.GENERATOR_SYNCHRONOUS, new SynchronousGeneratorAdder()),
            Map.entry(DynamicMappingType.CURRENT_LIMIT_AUTOMATON, new CurrentLimitAutomatonAdder()));

    public static DynamicMappingAdder getAdder(DynamicMappingType type) {
        return ADDERS.get(type);
    }

    private DynamicMappingAdderFactory() {
    }
}
