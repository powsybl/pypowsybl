/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.reporter.Reporter;
import com.powsybl.dynamicsimulation.DynamicModel;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynawaltz.models.automatons.CurrentLimitAutomaton;
import com.powsybl.dynawaltz.models.generators.SynchronousGenerator;
import com.powsybl.dynawaltz.models.loads.BaseLoad;
import com.powsybl.dynawaltz.models.loads.LoadOneTransformer;
import com.powsybl.iidm.network.*;
import org.jetbrains.annotations.NotNull;

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

    @NotNull
    private static Load getLoad(String staticId, Network network) {
        Load load = network.getLoad(staticId);
        if (load == null) {
            throw new PowsyblException("Load '" + staticId + "' not found");
        }
        return load;
    }

    /**
     * maps static element to a dynamic alpha_beta load
     *
     * @param staticId also determines the dynamic id of the element
     */
    public void addAlphaBetaLoad(String staticId, String parameterSetId) {
        dynamicModelList.add(network -> {
            Load load = getLoad(staticId, network);
            return new BaseLoad(staticId, load, parameterSetId, "LoadAlphaBeta");
        });
    }

    /**
     * maps static element to a dynamic one transformer
     *
     * @param staticId also determines the dynamic id of the element
     */
    public void addOneTransformerLoad(String staticId, String parameterSetId) {
        dynamicModelList.add(network -> {
            Load load = getLoad(staticId, network);
            return new LoadOneTransformer(staticId, load, parameterSetId);
        });
    }

    public void addGeneratorSynchronous(String staticId, String parameterSetId, String generatorLib) {
        dynamicModelList.add(network -> {
            Generator gen = network.getGenerator(staticId);
            if (gen == null) {
                throw new PowsyblException("Generator '" + staticId + "' not found");
            }
            return new SynchronousGenerator(staticId, gen, parameterSetId, generatorLib);
        });
    }

    public void addGeneratorSynchronousThreeWindings(String staticId, String parameterSetId) {
        addGeneratorSynchronous(staticId, parameterSetId, "ThreeWindings");
    }

    public void addGeneratorSynchronousThreeWindingsProportionalRegulations(String staticId, String parameterSetId) {
        addGeneratorSynchronous(staticId, parameterSetId, "ThreeWindingsProportionalRegulations");
    }

    public void addGeneratorSynchronousFourWindings(String staticId, String parameterSetId) {
        addGeneratorSynchronous(staticId, parameterSetId, "FourWindings");
    }

    public void addGeneratorSynchronousFourWindingsProportionalRegulations(String staticId, String parameterSetId) {
        addGeneratorSynchronous(staticId, parameterSetId, "FourWindingsProportionalRegulations");
    }

    public void addCurrentLimitAutomaton(String staticId, String parameterSetId, TwoSides side) {
        dynamicModelList.add(network -> {
            Branch<?> branch = network.getBranch(staticId);
            if (branch == null) {
                throw new PowsyblException("Branch '" + staticId + "' not found");
            }
            return new CurrentLimitAutomaton(staticId, parameterSetId, branch, side, "CurrentLimitAutomaton");
        });
    }
}
