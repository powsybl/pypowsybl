/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 */
public class DynamicModelMapper implements DynamicModelsSupplier {

    private LinkedList<Function<Network, DynamicModel>> dynamicModelList;

    public DynamicModelMapper() {
        dynamicModelList = new LinkedList<>();
    }

    public List<DynamicModel> get(Network network) {
        return dynamicModelList.stream().map(f -> f.apply(network)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * maps static element to a dynamic alpha_beta load
     *
     * @param staticId also determines the dynamic id of the element
     */
    public void addAlphaBetaLoad(String staticId, String parametersIds) {
        dynamicModelList.add(n -> {
            Load load = n.getLoad(staticId);
            return load != null ? new LoadAlphaBeta(staticId, load, parametersIds) : null;
        });
    }

    /**
     * maps static element to a dynamic one transformer
     *
     * @param staticId also determines the dynamic id of the element
     */
    public void addOneTransformerLoad(String staticId, String parametersIds) {
        dynamicModelList.add(n -> {
            Load load = n.getLoad(staticId);
            return load != null ? new LoadOneTransformer(staticId, load, parametersIds) : null;
        });
    }

    public void addGeneratorSynchronous(String staticId, String parametersIds, String generatorLib) {
        dynamicModelList.add(n -> {
            Generator gen = n.getGenerator(staticId);
            return gen != null ? new GeneratorSynchronous(staticId, gen, parametersIds, generatorLib) : null;
        });
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
        dynamicModelList.add(n -> new CurrentLimitAutomaton(staticId, staticId, parametersIds, SideConverter.convert(side)));
    }

}
