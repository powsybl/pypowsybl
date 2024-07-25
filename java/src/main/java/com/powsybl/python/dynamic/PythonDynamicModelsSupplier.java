/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynamicsimulation.DynamicModel;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynawaltz.models.automationsystems.overloadmanagments.DynamicOverloadManagementSystemBuilder;
import com.powsybl.dynawaltz.models.generators.SynchronousGeneratorBuilder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class PythonDynamicModelsSupplier implements DynamicModelsSupplier {

    private final List<BiFunction<Network, ReportNode, DynamicModel>> dynamicModelList = new ArrayList<>();

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public List<DynamicModel> get(Network network, ReportNode reportNode) {
        return dynamicModelList.stream().map(f -> f.apply(network, reportNode)).filter(Objects::nonNull).toList();
    }

    public void addModel(BiFunction<Network, ReportNode, DynamicModel> modelFunction) {
        dynamicModelList.add(modelFunction);
    }

    //TODO remove
    public void addSynchronousGenerator(String staticId, String parameterSetId, String generatorLib) {
        dynamicModelList.add((network, reportNode) -> SynchronousGeneratorBuilder.of(network, generatorLib)
                .staticId(staticId)
                .parameterSetId(parameterSetId)
                .build());
    }

    //TODO remove
    public void addCurrentLimitAutomaton(String staticId, String parameterSetId, TwoSides side) {
        dynamicModelList.add((network, reportNode) -> DynamicOverloadManagementSystemBuilder.of(network, "CurrentLimitAutomaton")
                .parameterSetId(parameterSetId)
                .iMeasurement(staticId)
                .iMeasurementSide(side)
                .build());
    }
}
