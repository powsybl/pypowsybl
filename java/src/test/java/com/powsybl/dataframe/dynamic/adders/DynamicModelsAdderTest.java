/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.dataframe.update.DefaultUpdatingDataframe;
import com.powsybl.dataframe.update.TestStringSeries;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.dynamic.PythonDynamicModelsSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;
import static com.powsybl.python.commons.PyPowsyblApiHeader.DynamicMappingType.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class DynamicModelsAdderTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("generatorDataProvider")
    void testGeneratorAdders(PyPowsyblApiHeader.DynamicMappingType mappingType, String modelName, String defaultModelName) {
        Network network = EurostagTutorialExample1Factory.createWithMoreGenerators();
        DefaultUpdatingDataframe dataframe = new DefaultUpdatingDataframe(2);
        dataframe.addSeries(STATIC_ID, true, new TestStringSeries("GEN", "GEN2"));
        dataframe.addSeries(PARAMETER_SET_ID, false, new TestStringSeries("gen_par", "gen_par"));
        dataframe.addSeries(MODEL_NAME, false, new TestStringSeries(modelName, ""));
        PythonDynamicModelsSupplier dynamicModelsSupplier = new PythonDynamicModelsSupplier();
        DynamicMappingAdderFactory.getAdder(mappingType).addElements(dynamicModelsSupplier, dataframe);

        assertThat(dynamicModelsSupplier.get(network)).satisfiesExactly(
                model1 -> assertThat(model1).hasFieldOrPropertyWithValue("dynamicModelId", "GEN").hasFieldOrPropertyWithValue("lib", modelName),
                model2 -> assertThat(model2).hasFieldOrPropertyWithValue("dynamicModelId", "GEN2").hasFieldOrPropertyWithValue("lib", defaultModelName));
    }

    static Stream<Arguments> generatorDataProvider() {
        return Stream.of(
                Arguments.of(FICTITIOUS_GENERATOR, "GeneratorFictitious", "GeneratorFictitious"),
                Arguments.of(SYNCHRONIZED_GENERATOR, "GeneratorPVFixed", "GeneratorPQ"),
                Arguments.of(SYNCHRONOUS_GENERATOR, "GeneratorSynchronousThreeWindings", "GeneratorSynchronousFourWindings"),
                Arguments.of(WECC, "WT4BWeccCurrentSource", "WTG4AWeccCurrentSource"),
                Arguments.of(GRID_FORMING_CONVERTER, "GridFormingConverterMatchingControl", "GridFormingConverterDroopControl")
        );
    }
}
