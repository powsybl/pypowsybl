/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynawo.margincalculation.loadsvariation.LoadsVariation;
import com.powsybl.dynawo.margincalculation.loadsvariation.LoadsVariationBuilder;
import com.powsybl.dynawo.margincalculation.loadsvariation.supplier.LoadsVariationSupplier;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates the loads variations declared from Python and builds the corresponding
 * {@link LoadsVariation} objects at margin calculation run time.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class PythonLoadsVariationSupplier implements LoadsVariationSupplier {

    private record LoadsVariationData(List<String> loadIds, double variationValue) {
    }

    private final List<LoadsVariationData> loadsVariations = new ArrayList<>();

    public void addLoadsVariation(List<String> loadIds, double variationValue) {
        loadsVariations.add(new LoadsVariationData(List.copyOf(loadIds), variationValue));
    }

    @Override
    public List<LoadsVariation> getLoadsVariations(Network network, ReportNode reportNode) {
        List<LoadsVariation> result = new ArrayList<>();
        for (LoadsVariationData data : loadsVariations) {
            result.add(new LoadsVariationBuilder(network, reportNode)
                    .loads(data.loadIds())
                    .variationValue(data.variationValue())
                    .build());
        }
        return result;
    }
}
