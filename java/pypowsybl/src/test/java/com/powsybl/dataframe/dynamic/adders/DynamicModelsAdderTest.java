/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.dataframe.update.DefaultUpdatingDataframe;
import com.powsybl.dataframe.update.TestStringSeries;
import com.powsybl.dynawo.models.AbstractPureDynamicBlackBoxModel;
import com.powsybl.dynawo.models.TransformerSide;
import com.powsybl.dynawo.models.automationsystems.TapChangerBlockingAutomationSystem;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.HvdcTestNetwork;
import com.powsybl.iidm.network.test.ShuntTestCaseFactory;
import com.powsybl.iidm.network.test.SvcTestCaseFactory;
import com.powsybl.python.dynamic.PythonDynamicModelsSupplier;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
class DynamicModelsAdderTest {

    private static final String DEFAULT_SUFFIX = "_DEFAULT";
    private DefaultUpdatingDataframe dataframe;
    private PythonDynamicModelsSupplier dynamicModelsSupplier;

    @BeforeEach
    void setup() {
        dataframe = new DefaultUpdatingDataframe(2);
        dynamicModelsSupplier = new PythonDynamicModelsSupplier();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("equipmentDataProvider")
    void testEquipmentsAdder(String category, Network network, String staticId) {
        String expectedModelName = DynamicMappingHandler.getSupportedModels(category).stream().findFirst().orElse("");
        dataframe.addSeries(STATIC_ID, true, createTwoRowsSeries(staticId));
        dataframe.addSeries(PARAMETER_SET_ID, false, createTwoRowsSeries("eq_par"));
        dataframe.addSeries(MODEL_NAME, false, new TestStringSeries(expectedModelName, ""));
        DynamicMappingHandler.addElements(category, dynamicModelsSupplier, List.of(dataframe));

        assertThat(dynamicModelsSupplier.get(network)).satisfiesExactly(
                model1 -> assertThat(model1).hasFieldOrPropertyWithValue("dynamicModelId", staticId)
                        .hasFieldOrPropertyWithValue("lib", expectedModelName),
                model2 -> assertThat(model2).hasFieldOrPropertyWithValue("dynamicModelId", staticId));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("automationSystemProvider")
    void testAutomationSystemAdders(String category, Consumer<DefaultUpdatingDataframe> updateDataframe) {
        String expectedModelName = DynamicMappingHandler.getSupportedModels(category).stream().findFirst().orElse("");
        Network network = EurostagTutorialExample1Factory.createWithLFResults();
        String dynamicModelId = "BBM_automation_system";
        setupDataFrame(dataframe, dynamicModelId, expectedModelName);
        updateDataframe.accept(dataframe);
        DynamicMappingHandler.addElements(category, dynamicModelsSupplier, List.of(dataframe));

        assertThat(dynamicModelsSupplier.get(network)).satisfiesExactly(
                model1 -> assertThat(model1).hasFieldOrPropertyWithValue("dynamicModelId", dynamicModelId)
                        .isInstanceOf(AbstractPureDynamicBlackBoxModel.class),
                model2 -> assertThat(model2).hasFieldOrPropertyWithValue("dynamicModelId", dynamicModelId + DEFAULT_SUFFIX)
                        .isInstanceOf(AbstractPureDynamicBlackBoxModel.class));
    }

    @Test
    void testTapChangerBlockingAdder() {
        String expectedModelName = DynamicMappingHandler.getSupportedModels("TapChangerBlocking").stream().findFirst().orElse("");
        Network network = EurostagTutorialExample1Factory.createWithLFResults();
        String dynamicModelId = "BBM_TCB";
        String defaultDynamicModelId = dynamicModelId + DEFAULT_SUFFIX;
        // Setup Tcb df
        setupDataFrame(dataframe, dynamicModelId, expectedModelName);
        // Setup Tfo df
        DefaultUpdatingDataframe tfoDataFrame = new DefaultUpdatingDataframe(3);
        tfoDataFrame.addSeries(DYNAMIC_MODEL_ID, true, new TestStringSeries(dynamicModelId, dynamicModelId, defaultDynamicModelId));
        tfoDataFrame.addSeries(TRANSFORMER_ID, false, new TestStringSeries("NGEN_NHV1", "NHV2_NLOAD", "NHV2_NLOAD"));
        // Setup measurement points df
        DefaultUpdatingDataframe m1DataFrame = new DefaultUpdatingDataframe(3);
        m1DataFrame.addSeries(DYNAMIC_MODEL_ID, true, new TestStringSeries(dynamicModelId, dynamicModelId, defaultDynamicModelId));
        m1DataFrame.addSeries(MEASUREMENT_POINT_ID, false, new TestStringSeries("BBS_NGEN", "NGEN", "NHV1"));
        DefaultUpdatingDataframe m2DataFrame = new DefaultUpdatingDataframe(2);
        m2DataFrame.addSeries(DYNAMIC_MODEL_ID, true, new TestStringSeries(dynamicModelId, dynamicModelId));
        m2DataFrame.addSeries(MEASUREMENT_POINT_ID, false, new TestStringSeries("OLD_NLOAD_ID", "NLOAD"));
        DynamicMappingHandler.addElements("TapChangerBlocking", dynamicModelsSupplier, List.of(dataframe, tfoDataFrame, m1DataFrame, m2DataFrame));

        assertThat(dynamicModelsSupplier.get(network)).satisfiesExactly(
                model1 -> assertThat(model1).hasFieldOrPropertyWithValue("dynamicModelId", dynamicModelId)
                        .isInstanceOf(TapChangerBlockingAutomationSystem.class)
                        .extracting("uMeasurements")
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .containsExactly(network.getBusBreakerView().getBus("NGEN"), network.getBusBreakerView().getBus("NLOAD")),
                model2 -> assertThat(model2).hasFieldOrPropertyWithValue("dynamicModelId", defaultDynamicModelId)
                        .isInstanceOf(TapChangerBlockingAutomationSystem.class)
                        .extracting("uMeasurements")
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .containsExactly(network.getBusBreakerView().getBus("NHV1")));
    }

    @Test
    void testIncompleteDataFrame() {
        Network network = EurostagTutorialExample1Factory.create();
        DefaultUpdatingDataframe missingStaticDF = new DefaultUpdatingDataframe(1);
        missingStaticDF.addSeries(PARAMETER_SET_ID, false, new TestStringSeries("eq_par"));
        DynamicMappingHandler.addElements("Load", dynamicModelsSupplier, List.of(missingStaticDF));
        assertThat(dynamicModelsSupplier.get(network)).isEmpty();
    }

    @Test
    void testWrongModelName() {
        Network network = EurostagTutorialExample1Factory.create();
        DefaultUpdatingDataframe wrongModelNameDF = new DefaultUpdatingDataframe(1);
        wrongModelNameDF.addSeries(STATIC_ID, false, new TestStringSeries("LOAD"));
        wrongModelNameDF.addSeries(PARAMETER_SET_ID, false, new TestStringSeries("eq_par"));
        wrongModelNameDF.addSeries(MODEL_NAME, false, new TestStringSeries("wrongModelName"));
        DynamicMappingHandler.addElements("Load", dynamicModelsSupplier, List.of(wrongModelNameDF));
        assertThat(dynamicModelsSupplier.get(network)).isEmpty();
    }

    private static Stream<Arguments> equipmentDataProvider() {
        return Stream.of(
                Arguments.of("Load", EurostagTutorialExample1Factory.create(), "LOAD"),
                Arguments.of("LoadOneTransformer", EurostagTutorialExample1Factory.create(), "LOAD"),
                Arguments.of("LoadOneTransformerTapChanger", EurostagTutorialExample1Factory.create(), "LOAD"),
                Arguments.of("LoadTwoTransformers", EurostagTutorialExample1Factory.create(), "LOAD"),
                Arguments.of("LoadTwoTransformersTapChangers", EurostagTutorialExample1Factory.create(), "LOAD"),
                Arguments.of("SimplifiedGenerator", EurostagTutorialExample1Factory.create(), "GEN"),
                Arguments.of("SynchronizedGenerator", EurostagTutorialExample1Factory.create(), "GEN"),
                Arguments.of("SynchronousGenerator", EurostagTutorialExample1Factory.create(), "GEN"),
                Arguments.of("Wecc", EurostagTutorialExample1Factory.create(), "GEN"),
                Arguments.of("GridFormingConverter", EurostagTutorialExample1Factory.create(), "GEN"),
                Arguments.of("SignalNGenerator", EurostagTutorialExample1Factory.create(), "GEN"),
                Arguments.of("Transformer", EurostagTutorialExample1Factory.create(), "NGEN_NHV1"),
                Arguments.of("StaticVarCompensator", SvcTestCaseFactory.create(), "SVC2"),
                Arguments.of("Line", EurostagTutorialExample1Factory.create(), "NHV1_NHV2_1"),
                Arguments.of("Bus", EurostagTutorialExample1Factory.create(), "NHV1"),
                Arguments.of("InfiniteBus", EurostagTutorialExample1Factory.create(), "NHV1"),
                Arguments.of("HvdcP", HvdcTestNetwork.createVsc(), "L"),
                Arguments.of("HvdcVsc", HvdcTestNetwork.createVsc(), "L"),
                Arguments.of("InertialGrid", EurostagTutorialExample1Factory.create(), "GEN"),
                Arguments.of("Shunt", ShuntTestCaseFactory.create(), "SHUNT")
                );
    }

    private static Stream<Arguments> automationSystemProvider() {
        return Stream.of(
                Arguments.of("OverloadManagementSystem",
                        (Consumer<DefaultUpdatingDataframe>) df -> {
                            String lineId = "NGEN_NHV1";
                            df.addSeries(CONTROLLED_BRANCH, false, createTwoRowsSeries(lineId));
                            df.addSeries(I_MEASUREMENT, false, createTwoRowsSeries(lineId));
                            df.addSeries(I_MEASUREMENT_SIDE, false, createTwoRowsSeries(TwoSides.ONE.toString()));
                        }),
                Arguments.of("TwoLevelOverloadManagementSystem",
                        (Consumer<DefaultUpdatingDataframe>) df -> {
                            String lineId = "NGEN_NHV1";
                            df.addSeries(CONTROLLED_BRANCH, false, createTwoRowsSeries(lineId));
                            df.addSeries(I_MEASUREMENT_1, false, createTwoRowsSeries("NHV1_NHV2_1"));
                            df.addSeries(I_MEASUREMENT_1_SIDE, false, createTwoRowsSeries(TwoSides.ONE.toString()));
                            df.addSeries(I_MEASUREMENT_2, false, createTwoRowsSeries("NHV1_NHV2_2"));
                            df.addSeries(I_MEASUREMENT_2_SIDE, false, createTwoRowsSeries(TwoSides.ONE.toString()));
                        }),
                Arguments.of("PhaseShifterI",
                        (Consumer<DefaultUpdatingDataframe>) df -> df.addSeries(DynamicModelDataframeConstants.TRANSFORMER, false, createTwoRowsSeries("NGEN_NHV1"))),
                Arguments.of("PhaseShifterP",
                        (Consumer<DefaultUpdatingDataframe>) df -> df.addSeries(DynamicModelDataframeConstants.TRANSFORMER, false, createTwoRowsSeries("NGEN_NHV1"))),
                Arguments.of("PhaseShifterBlockingI",
                        (Consumer<DefaultUpdatingDataframe>) df -> df.addSeries(PHASE_SHIFTER_ID, false, createTwoRowsSeries("PSI"))),
                Arguments.of("TapChanger",
                        (Consumer<DefaultUpdatingDataframe>) df -> {
                            df.addSeries(STATIC_ID, false, createTwoRowsSeries("LOAD"));
                            df.addSeries(SIDE, false, createTwoRowsSeries(TransformerSide.LOW_VOLTAGE.toString()));
                        }),
                Arguments.of("TapChanger",
                        (Consumer<DefaultUpdatingDataframe>) df -> df.addSeries(STATIC_ID, false, createTwoRowsSeries("LOAD"))),
                Arguments.of("UnderVoltageAutomationSystem",
                        (Consumer<DefaultUpdatingDataframe>) df -> df.addSeries(GENERATOR, false, createTwoRowsSeries("GEN")))
        );
    }

    private static void setupDataFrame(DefaultUpdatingDataframe dataframe, String dynamicModelId, String modelName) {
        dataframe.addSeries(DYNAMIC_MODEL_ID, true, new TestStringSeries(dynamicModelId, dynamicModelId + DEFAULT_SUFFIX));
        dataframe.addSeries(PARAMETER_SET_ID, false, createTwoRowsSeries("as_par"));
        dataframe.addSeries(MODEL_NAME, false, new TestStringSeries(modelName, ""));
    }

    private static TestStringSeries createTwoRowsSeries(String value) {
        return new TestStringSeries(value, value);
    }
}
