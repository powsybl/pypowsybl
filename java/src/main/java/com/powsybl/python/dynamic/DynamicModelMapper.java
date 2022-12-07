/**
 * Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.powsybl.dataframe.dynamic.DynamicMappingsDataframeHandle;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynamicsimulation.DynamicModel;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynawaltz.automatons.CurrentLimitAutomaton;
import com.powsybl.dynawaltz.dynamicmodels.GeneratorSynchronousFourWindings;
import com.powsybl.dynawaltz.dynamicmodels.GeneratorSynchronousFourWindingsProportionalRegulations;
import com.powsybl.dynawaltz.dynamicmodels.GeneratorSynchronousThreeWindings;
import com.powsybl.dynawaltz.dynamicmodels.GeneratorSynchronousThreeWindingsProportionalRegulations;
import com.powsybl.dynawaltz.dynamicmodels.LoadAlphaBeta;
import com.powsybl.dynawaltz.dynamicmodels.LoadOneTransformer;
import com.powsybl.dynawaltz.dynamicmodels.OmegaRef;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.PyPowsyblApiHeader.DynamicMappingType;

public class DynamicModelMapper implements DynamicModelsSupplier {

    private LinkedList<Supplier<DynamicModel>> dynamicModelList;

    public DynamicModelMapper() {
        dynamicModelList = new LinkedList<>();
    }

    public List<DynamicModel> get(Network network) {
        return dynamicModelList.stream().map(supplier -> supplier.get()).collect(Collectors.toList());
    }

    /**
     * maps static element to a dynamic alpha_beta load
     *
     * @param staticId also determines the dynamic id of the element
     */
    public void addAlphaBetaLoad(String staticId, String parametersIds) {
        dynamicModelList.add(() -> new LoadAlphaBeta(staticId, staticId, parametersIds));
    }

    /**
     * maps static element to a dynamic one transformer
     *
     * @param staticId also determines the dynamic id of the element
     */
    public void addOneTransformerLoad(String staticId, String parametersIds) {
        dynamicModelList.add(() -> new LoadOneTransformer(staticId, staticId, parametersIds));
    }

    public void addOmegaRef(String generatorId) {
        dynamicModelList.add(() -> new OmegaRef(generatorId));
    }

    public void addGeneratorSynchronousThreeWindings(String staticId, String parametersIds) {
        dynamicModelList.add(() -> new GeneratorSynchronousThreeWindings(staticId, staticId, parametersIds));
    }

    public void addGeneratorSynchronousThreeWindingsProportionalRegulations(String staticId, String parametersIds) {
        dynamicModelList.add(
            () -> new GeneratorSynchronousThreeWindingsProportionalRegulations(staticId, staticId, parametersIds));
    }

    public void addGeneratorSynchronousFourWindings(String staticId, String parametersIds) {
        dynamicModelList.add(() -> new GeneratorSynchronousFourWindings(staticId, staticId, parametersIds));
    }

    public void addGeneratorSynchronousFourWindingsProportionalRegulations(String staticId, String parametersIds) {
        dynamicModelList.add(
            () -> new GeneratorSynchronousFourWindingsProportionalRegulations(staticId, staticId, parametersIds));
    }

    public void addCurrentLimitAutomaton(String staticId, String parametersIds, Branch.Side side) {
        dynamicModelList.add(() -> new CurrentLimitAutomaton(staticId, staticId, parametersIds, side));
    }

    /**
     * Will add all the dynamic mapping contained in the dataframe.
     *
     * Handles default values for missing arguments like CURRENT_LIMIT_AUTOMATON.
     * Ignores parameter_set_id for OMEGA_REF
     *
     * @param mappingDf dataframe that should respect
     *                  {@link DynamicMappingsDataframeHandle#dynamicMappingsDataFrameMapper}
     *                  format
     */
    public void addAllMappings(UpdatingDataframe mappingDf) {
        StringSeries staticIdSeries = mappingDf.getStrings("static_id");
        StringSeries parameterSetIdSeries = mappingDf.getStrings("parameter_set_id");
        IntSeries dynamicModelSeries = mappingDf.getInts("mapping_type");
        for (int row = 0; row < mappingDf.getRowCount(); ++row) {
            DynamicMappingType type = DynamicMappingType.fromCValue(dynamicModelSeries.get(row));
            String staticId = staticIdSeries.get(row);
            String parameterSetId = parameterSetIdSeries.get(row);
            switch (type) {
                case ALPHA_BETA_LOAD:
                    this.addAlphaBetaLoad(staticId, parameterSetId);
                    break;
                case CURRENT_LIMIT_AUTOMATON:
                    this.addCurrentLimitAutomaton(staticId, parameterSetId, Branch.Side.ONE);
                    break;
                case GENERATOR_SYNCHRONOUS_FOUR_WINDINGS:
                    this.addGeneratorSynchronousFourWindings(staticId, parameterSetId);
                    break;
                case GENERATOR_SYNCHRONOUS_FOUR_WINDINGS_PROPORTIONAL_REGULATIONS:
                    this.addGeneratorSynchronousFourWindingsProportionalRegulations(staticId, parameterSetId);
                    break;
                case GENERATOR_SYNCHRONOUS_THREE_WINDINGS:
                    this.addGeneratorSynchronousThreeWindings(staticId, parameterSetId);
                    break;
                case GENERATOR_SYNCHRONOUS_THREE_WINDINGS_PROPORTIONAL_REGULATIONS:
                    this.addGeneratorSynchronousThreeWindingsProportionalRegulations(staticId, parameterSetId);
                    break;
                case OMEGA_REF:
                    this.addOmegaRef(staticId);
                    break;
                case ONE_TRANSFORMER_LOAD:
                    this.addOneTransformerLoad(staticId, parameterSetId);
                    break;
            }
        }
    }

}
