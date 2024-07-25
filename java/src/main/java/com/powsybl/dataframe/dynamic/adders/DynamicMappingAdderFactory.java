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
            // Equipments
            Map.entry(DynamicMappingType.BASE_LOAD, new BaseLoadAdder()),
            Map.entry(DynamicMappingType.LOAD_ONE_TRANSFORMER, new LoadOneTransformerAdder()),
            Map.entry(DynamicMappingType.LOAD_ONE_TRANSFORMER_TAP_CHANGER, new LoadOneTransformerTapChangerAdder()),
            Map.entry(DynamicMappingType.LOAD_TWO_TRANSFORMERS, new LoadTwoTransformersAdder()),
            Map.entry(DynamicMappingType.LOAD_TWO_TRANSFORMERS_TAP_CHANGERS, new LoadTwoTransformersTapChangersAdder()),
            Map.entry(DynamicMappingType.BASE_GENERATOR, new BaseGeneratorAdder()),
            Map.entry(DynamicMappingType.SYNCHRONIZED_GENERATOR, new SynchronizedGeneratorAdder()),
            Map.entry(DynamicMappingType.SYNCHRONOUS_GENERATOR, new SynchronousGeneratorAdder()),
            Map.entry(DynamicMappingType.WECC, new WeccAdder()),
            Map.entry(DynamicMappingType.GRID_FORMING_CONVERTER, new GridFormingConverterAdder()),
            Map.entry(DynamicMappingType.HVDC_P, new HvdcPAdder()),
            Map.entry(DynamicMappingType.HVDC_VSC, new HvdcVscAdder()),
            Map.entry(DynamicMappingType.TRANSFORMER, new TransformerAdder()),
            Map.entry(DynamicMappingType.BASE_STATIC_VAR_COMPENSATOR, new StaticVarCompensatorAdder()),
            Map.entry(DynamicMappingType.BASE_LINE, new LineAdder()),
            Map.entry(DynamicMappingType.BASE_BUS, new BaseBusAdder()),
            Map.entry(DynamicMappingType.INFINITE_BUS, new InfiniteBusAdder()),
            // Automation systems
            Map.entry(DynamicMappingType.CURRENT_LIMIT_AUTOMATON, new CurrentLimitAutomatonAdder()));

    public static DynamicMappingAdder getAdder(DynamicMappingType type) {
        return ADDERS.get(type);
    }

    private DynamicMappingAdderFactory() {
    }
}
