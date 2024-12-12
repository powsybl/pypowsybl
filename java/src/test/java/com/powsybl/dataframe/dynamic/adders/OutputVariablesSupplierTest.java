/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.dataframe.impl.Series;
import com.powsybl.dynamicsimulation.OutputVariable;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.python.dynamic.PythonOutputVariablesSupplier;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.powsybl.dataframe.dynamic.OutputVariablesSeries.fsvDataFrameMapper;
import static com.powsybl.dynamicsimulation.OutputVariable.OutputType.*;
import static com.powsybl.python.network.Dataframes.createSeries;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class OutputVariablesSupplierTest {

    @Test
    void testPythonSupplier() {
        PythonOutputVariablesSupplier supplier = new PythonOutputVariablesSupplier();
        supplier.addOutputVariables("LOAD", List.of("load_PPu", "load_QPu"), true, CURVE);
        supplier.addOutputVariables("NGEN", List.of("Upu_value"), false, FINAL_STATE);
        List<OutputVariable> variables = supplier.get(EurostagTutorialExample1Factory.create());
        assertThat(variables).satisfiesExactly(
                var1 -> assertThat(var1).hasFieldOrPropertyWithValue("dynamicModelId", "LOAD")
                        .hasFieldOrPropertyWithValue("variable", "load_PPu")
                        .hasFieldOrPropertyWithValue("outputType", CURVE),
                var2 -> assertThat(var2).hasFieldOrPropertyWithValue("dynamicModelId", "LOAD")
                        .hasFieldOrPropertyWithValue("variable", "load_QPu")
                        .hasFieldOrPropertyWithValue("outputType", CURVE),
                var3 -> assertThat(var3).hasFieldOrPropertyWithValue("dynamicModelId", "NETWORK")
                        .hasFieldOrPropertyWithValue("variable", "NGEN_Upu_value")
                        .hasFieldOrPropertyWithValue("outputType", FINAL_STATE)
        );
    }

    @Test
    void testFsvDataframesMapper() {
        Map<String, Double> fsv = new LinkedHashMap<>(Map.of("LOAD_load_PPu", 22.1, "GEN_Upu_value", 45.8));
        List<Series> series = createSeries(fsvDataFrameMapper(), fsv);
        assertThat(series)
                .extracting(Series::getName)
                .containsExactly("variables", "values");
        assertThat(series).satisfiesExactly(
                col1 -> assertThat(col1.getStrings()).containsExactly("GEN_Upu_value", "LOAD_load_PPu"),
                col2 -> assertThat(col2.getDoubles()).containsExactly(45.8, 22.1));
    }
}
