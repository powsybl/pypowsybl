/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.dynamicsimulation.DynamicModel;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 */
public class DynamicModelMapper implements DynamicModelsSupplier {

    private final List<Supplier<DynamicModel>> dynamicModelList = new ArrayList<>();

    public List<DynamicModel> get(Network network) {
        return dynamicModelList.stream().map(Supplier::get).collect(Collectors.toList());
    }

    /**
     * maps static element to a dynamic alpha_beta load
     *
     * @param staticId also determines the dynamic id of the element
     */
    public void addAlphaBetaLoad(String staticId, String parametersIds) {
//        dynamicModelList.add(() -> new LoadAlphaBeta(staticId, staticId, parametersIds));
    }

    /**
     * maps static element to a dynamic one transformer
     *
     * @param staticId also determines the dynamic id of the element
     */
    public void addOneTransformerLoad(String staticId, String parametersIds) {
//        dynamicModelList.add(() -> new LoadOneTransformer(staticId, staticId, parametersIds));
    }

    public void addGeneratorSynchronous(String staticId, String parametersIds, String generatorLib) {
//        dynamicModelList.add(() -> new GeneratorSynchronous(staticId, staticId, parametersIds, generatorLib));
    }

    public void addGeneratorSynchronousThreeWindings(String staticId, String parametersIds) {
        addGeneratorSynchronous(staticId, parametersIds, "ThreeWindings");
    }

    public void addGeneratorSynchronousThreeWindingsProportionalRegulations(String staticId, String parametersIds) {
        addGeneratorSynchronous(staticId, parametersIds, "ThreeWindingsProportionalRegulations");
    }

    public void addGeneratorSynchronousFourWindings(String staticId, String parametersIds) {
        addGeneratorSynchronous(staticId, parametersIds, "FourWindings");
    }

    public void addGeneratorSynchronousFourWindingsProportionalRegulations(String staticId, String parametersIds) {
        addGeneratorSynchronous(staticId, parametersIds, "FourWindingsProportionalRegulations");
    }

    public void addCurrentLimitAutomaton(String staticId, String parametersIds, Branch.Side side) {
//        dynamicModelList.add(() -> new CurrentLimitAutomaton(staticId, staticId, parametersIds, SideConverter.convert(side)));
    }

}
