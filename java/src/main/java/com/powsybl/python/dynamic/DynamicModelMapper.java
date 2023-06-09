/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynamicsimulation.DynamicModel;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynawaltz.models.automatons.CurrentLimitAutomaton;
import com.powsybl.dynawaltz.models.generators.GeneratorSynchronous;
import com.powsybl.dynawaltz.models.loads.LoadAlphaBeta;
import com.powsybl.dynawaltz.models.loads.LoadOneTransformer;
import com.powsybl.dynawaltz.models.utils.SideConverter;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 */
public class DynamicModelMapper implements DynamicModelsSupplier {

    private final List<Function<Network, DynamicModel>> dynamicModelList = new ArrayList<>();

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
            return new LoadAlphaBeta(staticId, load, parameterSetId, "LoadAlphaBeta");
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
            return new GeneratorSynchronous(staticId, gen, parameterSetId, generatorLib);
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

    public void addCurrentLimitAutomaton(String staticId, String parameterSetId, Branch.Side side) {
        dynamicModelList.add(network -> {
            Branch<?> branch = network.getBranch(staticId);
            if (branch == null) {
                throw new PowsyblException("Branch '" + staticId + "' not found");
            }
            return new CurrentLimitAutomaton(staticId, parameterSetId, branch, SideConverter.convert(side), "CurrentLimitAutomaton");
        });
    }
}
