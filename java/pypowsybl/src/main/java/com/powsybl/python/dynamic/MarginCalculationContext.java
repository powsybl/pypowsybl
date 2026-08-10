/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynawo.margincalculation.MarginCalculation;
import com.powsybl.dynawo.margincalculation.MarginCalculationParameters;
import com.powsybl.dynawo.margincalculation.MarginCalculationRunParameters;
import com.powsybl.dynawo.margincalculation.loadsvariation.supplier.LoadsVariationSupplier;
import com.powsybl.dynawo.margincalculation.results.MarginCalculationResult;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.CommonObjects;
import com.powsybl.python.contingency.ContingencyContainerImpl;

/**
 * Holds the contingencies of a margin calculation (inherited from
 * {@link ContingencyContainerImpl}) and runs it through the {@link MarginCalculation} API,
 * reusing the dynamic simulation model supplier and a {@link LoadsVariationSupplier}.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class MarginCalculationContext extends ContingencyContainerImpl {

    MarginCalculationResult run(Network network,
                                DynamicModelsSupplier dynamicModelsSupplier,
                                LoadsVariationSupplier loadsVariationSupplier,
                                MarginCalculationParameters marginCalculationParameters,
                                ReportNode reportNode) {
        ContingenciesProvider contingencies = this::createContingencies;
        MarginCalculationRunParameters runParameters = new MarginCalculationRunParameters()
                .setMarginCalculationParameters(marginCalculationParameters)
                .setComputationManager(CommonObjects.getComputationManager())
                .setReportNode(reportNode == null ? ReportNode.NO_OP : reportNode);
        return MarginCalculation.run(network,
                network.getVariantManager().getWorkingVariantId(),
                dynamicModelsSupplier,
                contingencies,
                loadsVariationSupplier,
                runParameters);
    }
}
