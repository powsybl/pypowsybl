/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.dynamicsimulation.*;
import com.powsybl.iidm.network.Network;

/**
 * @author Nicolas Pierre {@literal <nicolas.pierre@artelys.com>}
 */
public class DynamicSimulationContext {

    private static final String DEFAULT_PROVIDER = "Dynawo";

    public DynamicSimulationResult run(Network network,
                                       DynamicModelsSupplier dynamicModelsSupplier,
                                       EventModelsSupplier eventModelsSupplier,
                                       OutputVariablesSupplier curvesSupplier,
                                       DynamicSimulationParameters parameters,
                                       ReportNode reportNode) {
        return DynamicSimulation.find(DEFAULT_PROVIDER).run(network,
                dynamicModelsSupplier,
                eventModelsSupplier,
                curvesSupplier,
                network.getVariantManager().getWorkingVariantId(),
                LocalComputationManager.getDefault(),
                parameters,
                reportNode);
    }
}
