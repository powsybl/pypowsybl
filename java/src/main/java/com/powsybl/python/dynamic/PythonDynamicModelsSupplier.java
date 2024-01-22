/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.reporter.Reporter;
import com.powsybl.dynamicsimulation.DynamicModel;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynawaltz.models.automatons.currentlimits.CurrentLimitAutomatonBuilder;
import com.powsybl.dynawaltz.models.generators.SynchronizedGeneratorBuilder;
import com.powsybl.dynawaltz.models.loads.BaseLoadBuilder;
import com.powsybl.dynawaltz.models.loads.LoadOneTransformerBuilder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 */
public class PythonDynamicModelsSupplier implements DynamicModelsSupplier {

    private final List<Function<Network, DynamicModel>> dynamicModelList = new ArrayList<>();

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public List<DynamicModel> get(Network network, Reporter reporter) {
        return get(network);
    }

    @Override
    public List<DynamicModel> get(Network network) {
        return dynamicModelList.stream().map(f -> f.apply(network)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * maps static element to a dynamic alpha_beta load
     *
     * @param staticId also determines the dynamic id of the element
     */
    public void addAlphaBetaLoad(String staticId, String parameterSetId) {
        dynamicModelList.add(network -> BaseLoadBuilder.of(network, "LoadAlphaBeta")
                .staticId(staticId)
                .parameterSetId(parameterSetId)
                .build());
    }

    /**
     * maps static element to a dynamic one transformer
     *
     * @param staticId also determines the dynamic id of the element
     */
    public void addOneTransformerLoad(String staticId, String parameterSetId) {
        dynamicModelList.add(network -> LoadOneTransformerBuilder.of(network, "LoadOneTransformer")
                .staticId(staticId)
                .parameterSetId(parameterSetId)
                .build());
    }

    public void addGeneratorSynchronous(String staticId, String parameterSetId, String generatorLib) {
        dynamicModelList.add(network -> SynchronizedGeneratorBuilder.of(network, generatorLib)
                .staticId(staticId)
                .parameterSetId(parameterSetId)
                .build());
    }

    public void addGeneratorSynchronousThreeWindings(String staticId, String parameterSetId) {
        addGeneratorSynchronous(staticId, parameterSetId, "GeneratorSynchronousThreeWindings");
    }

    public void addGeneratorSynchronousThreeWindingsProportionalRegulations(String staticId, String parameterSetId) {
        addGeneratorSynchronous(staticId, parameterSetId, "GeneratorSynchronousThreeWindingsProportionalRegulations");
    }

    public void addGeneratorSynchronousFourWindings(String staticId, String parameterSetId) {
        addGeneratorSynchronous(staticId, parameterSetId, "GeneratorSynchronousFourWindings");
    }

    public void addGeneratorSynchronousFourWindingsProportionalRegulations(String staticId, String parameterSetId) {
        addGeneratorSynchronous(staticId, parameterSetId, "GeneratorSynchronousFourWindingsProportionalRegulations");
    }

    public void addCurrentLimitAutomaton(String staticId, String parameterSetId, TwoSides side) {
        dynamicModelList.add(network -> CurrentLimitAutomatonBuilder.of(network, "CurrentLimitAutomaton")
                .parameterSetId(parameterSetId)
                .iMeasurement(staticId)
                .iMeasurementSide(side)
                .build());
    }
}
