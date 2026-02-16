/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.google.auto.service.AutoService;

import java.util.List;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
@AutoService(ModelAdderLoader.class)
public class ModelAdderLoaderImpl implements ModelAdderLoader {

    private static final List<DynamicMappingAdder> BASE_MODEL_ADDERS = List.of(
            // Equipments
            new BaseLoadAdder(),
            new LoadOneTransformerAdder(),
            new LoadOneTransformerTapChangerAdder(),
            new LoadTwoTransformersAdder(),
            new LoadTwoTransformersTapChangersAdder(),
            new BaseGeneratorAdder(),
            new SynchronizedGeneratorAdder(),
            new SynchronousGeneratorAdder(),
            new WeccAdder(),
            new GridFormingConverterAdder(),
            new SignalNGeneratorAdder(),
            new HvdcPAdder(),
            new HvdcVscAdder(),
            new TransformerAdder(),
            new StaticVarCompensatorAdder(),
            new LineAdder(),
            new BaseBusAdder(),
            new InfiniteBusAdder(),
            new InertialGridAdder(),
            // Automation systems
            new DynamicOverloadManagementSystemAdder(),
            new DynamicTwoLevelOverloadManagementSystemAdder(),
            new PhaseShifterIAdder(),
            new PhaseShifterPAdder(),
            new PhaseShifterBlockingIAdder(),
            new UnderVoltageAutomationSystemAdder(),
            new TapChangerAutomationSystemAdder(),
            new TapChangerBlockingAutomationSystemAdder());

    @Override
    public List<DynamicMappingAdder> getModelAdders() {
        return BASE_MODEL_ADDERS;
    }
}
