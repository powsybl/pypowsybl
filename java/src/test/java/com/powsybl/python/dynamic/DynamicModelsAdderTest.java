/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.dataframe.dynamic.adders.DynamicMappingAdderFactory;
import com.powsybl.dataframe.update.DefaultUpdatingDataframe;
import com.powsybl.dataframe.update.TestDoubleSeries;
import com.powsybl.dataframe.update.TestIntSeries;
import com.powsybl.dataframe.update.TestStringSeries;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import org.junit.jupiter.api.Test;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class DynamicModelsAdderTest {

    @Test
    void testSynchronousGeneratorAdder() {
        Network network = EurostagTutorialExample1Factory.createWithMoreGenerators();
        DefaultUpdatingDataframe dataframe = new DefaultUpdatingDataframe(2);
        dataframe.addSeries(STATIC_ID, true, new TestStringSeries("GEN", "GEN2"));
        dataframe.addSeries(PARAMETER_SET_ID, true, new TestStringSeries("gen_par", "gen_par"));
        dataframe.addSeries(MODEL_NAME, true, new TestStringSeries("GeneratorSynchronousThreeWindings", ""));
        PythonDynamicModelsSupplier dynamicModelsSupplier = new PythonDynamicModelsSupplier();
        DynamicMappingAdderFactory.getAdder(PyPowsyblApiHeader.DynamicMappingType.GENERATOR_SYNCHRONOUS).addElements(dynamicModelsSupplier, dataframe);

        assertThat(dynamicModelsSupplier.get(network)).satisfiesExactly(
                model1 -> assertThat(model1).hasFieldOrPropertyWithValue("dynamicModelId", "GEN").hasFieldOrPropertyWithValue("lib", "GeneratorSynchronousThreeWindings"),
                model2 -> assertThat(model2).hasFieldOrPropertyWithValue("dynamicModelId", "GEN2").hasFieldOrPropertyWithValue("lib", "GeneratorSynchronousFourWindings"));
    }
}
