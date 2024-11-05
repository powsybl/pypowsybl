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
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.HvdcTestNetwork;
import com.powsybl.iidm.network.test.SvcTestCaseFactory;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.dynamic.PythonDynamicModelsSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;
import static com.powsybl.python.commons.PyPowsyblApiHeader.DynamicMappingType.*;
import static com.powsybl.python.commons.PyPowsyblApiHeader.DynamicMappingType.BASE_TRANSFORMER;
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
    void testEquipmentsAdder(PyPowsyblApiHeader.DynamicMappingType mappingType, Network network, String staticId) {
        String expectedModelName = DynamicMappingHandler.getSupportedModels(mappingType).stream().findFirst().orElse("");
        dataframe.addSeries(STATIC_ID, true, createTwoRowsSeries(staticId));
        dataframe.addSeries(DYNAMIC_MODEL_ID, false, createTwoRowsSeries("BBM" + staticId));
        dataframe.addSeries(PARAMETER_SET_ID, false, createTwoRowsSeries("eq_par"));
        dataframe.addSeries(MODEL_NAME, false, new TestStringSeries(expectedModelName, ""));
        DynamicMappingHandler.addElements(mappingType, dynamicModelsSupplier, List.of(dataframe));

        assertThat(dynamicModelsSupplier.get(network)).satisfiesExactly(
                model1 -> assertThat(model1).hasFieldOrPropertyWithValue("dynamicModelId", "BBM" + staticId)
                        .hasFieldOrPropertyWithValue("lib", expectedModelName),
                model2 -> assertThat(model2).hasFieldOrPropertyWithValue("dynamicModelId", "BBM" + staticId));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("automationSystemProvider")
    void testAutomationSystemAdders(PyPowsyblApiHeader.DynamicMappingType mappingType, Consumer<DefaultUpdatingDataframe> updateDataframe) {
        String expectedModelName = DynamicMappingHandler.getSupportedModels(mappingType).stream().findFirst().orElse("");
        Network network = EurostagTutorialExample1Factory.createWithLFResults();
        String dynamicModelId = "BBM_automation_system";
        setupDataFrame(dataframe, dynamicModelId, expectedModelName);
        updateDataframe.accept(dataframe);
        DynamicMappingHandler.addElements(mappingType, dynamicModelsSupplier, List.of(dataframe));

        assertThat(dynamicModelsSupplier.get(network)).satisfiesExactly(
                model1 -> assertThat(model1).hasFieldOrPropertyWithValue("dynamicModelId", dynamicModelId)
                        .isInstanceOf(AbstractPureDynamicBlackBoxModel.class),
                model2 -> assertThat(model2).hasFieldOrPropertyWithValue("dynamicModelId", dynamicModelId + DEFAULT_SUFFIX));
    }

    @Test
    void testTapChangerBlockingAdders() {
        String expectedModelName = DynamicMappingHandler.getSupportedModels(TAP_CHANGER_BLOCKING).stream().findFirst().orElse("");
        Network network = EurostagTutorialExample1Factory.createWithLFResults();
        String dynamicModelId = "BBM_TCB";
        String defaultDynamicModelId = dynamicModelId + DEFAULT_SUFFIX;
        // Setup Tcb df
        setupDataFrame(dataframe, dynamicModelId, expectedModelName);
        dataframe.addSeries(U_MEASUREMENTS, false, createTwoRowsSeries("NHV1"));
        // Setup Tfo df
        DefaultUpdatingDataframe tfoDataFrame = new DefaultUpdatingDataframe(3);
        tfoDataFrame.addSeries(DYNAMIC_MODEL_ID, true, new TestStringSeries(dynamicModelId, dynamicModelId, defaultDynamicModelId));
        tfoDataFrame.addSeries(TRANSFORMER_ID, false, new TestStringSeries("NGEN_NHV1", "NHV2_NLOAD", "NHV2_NLOAD"));
        DynamicMappingHandler.addElements(TAP_CHANGER_BLOCKING, dynamicModelsSupplier, List.of(dataframe, tfoDataFrame));

        assertThat(dynamicModelsSupplier.get(network)).satisfiesExactly(
                model1 -> assertThat(model1).hasFieldOrPropertyWithValue("dynamicModelId", dynamicModelId)
                        .isInstanceOf(AbstractPureDynamicBlackBoxModel.class),
                model2 -> assertThat(model2).hasFieldOrPropertyWithValue("dynamicModelId", defaultDynamicModelId));
    }

    @Test
    void testIncompleteDataFrame() {
        Network network = EurostagTutorialExample1Factory.create();
        DefaultUpdatingDataframe missingStaticDF = new DefaultUpdatingDataframe(1);
        missingStaticDF.addSeries(PARAMETER_SET_ID, false, new TestStringSeries("eq_par"));
        DynamicMappingHandler.addElements(BASE_LOAD, dynamicModelsSupplier, List.of(missingStaticDF));
        DefaultUpdatingDataframe missingParamDF = new DefaultUpdatingDataframe(1);
        missingParamDF.addSeries(STATIC_ID, false, new TestStringSeries("LOAD"));
        DynamicMappingHandler.addElements(BASE_LOAD, dynamicModelsSupplier, List.of(missingParamDF));
        assertThat(dynamicModelsSupplier.get(network)).isEmpty();
    }

    @Test
    void testWrongModelName() {
        Network network = EurostagTutorialExample1Factory.create();
        DefaultUpdatingDataframe wrongModelNameDF = new DefaultUpdatingDataframe(1);
        wrongModelNameDF.addSeries(STATIC_ID, false, new TestStringSeries("LOAD"));
        wrongModelNameDF.addSeries(PARAMETER_SET_ID, false, new TestStringSeries("eq_par"));
        wrongModelNameDF.addSeries(MODEL_NAME, false, new TestStringSeries("wrongModelName"));
        DynamicMappingHandler.addElements(BASE_LOAD, dynamicModelsSupplier, List.of(wrongModelNameDF));
        assertThat(dynamicModelsSupplier.get(network)).isEmpty();
    }

    static Stream<Arguments> equipmentDataProvider() {
        return Stream.of(
                Arguments.of(BASE_LOAD, EurostagTutorialExample1Factory.create(), "LOAD"),
                Arguments.of(LOAD_ONE_TRANSFORMER, EurostagTutorialExample1Factory.create(), "LOAD"),
                Arguments.of(LOAD_ONE_TRANSFORMER_TAP_CHANGER, EurostagTutorialExample1Factory.create(), "LOAD"),
                Arguments.of(LOAD_TWO_TRANSFORMERS, EurostagTutorialExample1Factory.create(), "LOAD"),
                Arguments.of(LOAD_TWO_TRANSFORMERS_TAP_CHANGERS, EurostagTutorialExample1Factory.create(), "LOAD"),
                Arguments.of(BASE_GENERATOR, EurostagTutorialExample1Factory.create(), "GEN"),
                Arguments.of(SYNCHRONIZED_GENERATOR, EurostagTutorialExample1Factory.create(), "GEN"),
                Arguments.of(SYNCHRONOUS_GENERATOR, EurostagTutorialExample1Factory.create(), "GEN"),
                Arguments.of(WECC, EurostagTutorialExample1Factory.create(), "GEN"),
                Arguments.of(GRID_FORMING_CONVERTER, EurostagTutorialExample1Factory.create(), "GEN"),
                Arguments.of(SIGNAL_N_GENERATOR, EurostagTutorialExample1Factory.create(), "GEN"),
                Arguments.of(BASE_TRANSFORMER, EurostagTutorialExample1Factory.create(), "NGEN_NHV1"),
                Arguments.of(BASE_STATIC_VAR_COMPENSATOR, SvcTestCaseFactory.create(), "SVC2"),
                Arguments.of(BASE_LINE, EurostagTutorialExample1Factory.create(), "NHV1_NHV2_1"),
                Arguments.of(BASE_BUS, EurostagTutorialExample1Factory.create(), "NHV1"),
                Arguments.of(INFINITE_BUS, EurostagTutorialExample1Factory.create(), "NHV1"),
                Arguments.of(HVDC_P, HvdcTestNetwork.createVsc(), "L"),
                Arguments.of(HVDC_VSC, HvdcTestNetwork.createVsc(), "L")
                );
    }

    static Stream<Arguments> automationSystemProvider() {
        return Stream.of(
                Arguments.of(OVERLOAD_MANAGEMENT_SYSTEM,
                        (Consumer<DefaultUpdatingDataframe>) df -> {
                            String lineId = "NGEN_NHV1";
                            df.addSeries(CONTROLLED_BRANCH, false, createTwoRowsSeries(lineId));
                            df.addSeries(I_MEASUREMENT, false, createTwoRowsSeries(lineId));
                            df.addSeries(I_MEASUREMENT_SIDE, false, createTwoRowsSeries(TwoSides.ONE.toString()));
                        }),
                Arguments.of(TWO_LEVELS_OVERLOAD_MANAGEMENT_SYSTEM,
                        (Consumer<DefaultUpdatingDataframe>) df -> {
                            String lineId = "NGEN_NHV1";
                            df.addSeries(CONTROLLED_BRANCH, false, createTwoRowsSeries(lineId));
                            df.addSeries(I_MEASUREMENT_1, false, createTwoRowsSeries("NHV1_NHV2_1"));
                            df.addSeries(I_MEASUREMENT_1_SIDE, false, createTwoRowsSeries(TwoSides.ONE.toString()));
                            df.addSeries(I_MEASUREMENT_2, false, createTwoRowsSeries("NHV1_NHV2_2"));
                            df.addSeries(I_MEASUREMENT_2_SIDE, false, createTwoRowsSeries(TwoSides.ONE.toString()));
                        }),
                Arguments.of(PHASE_SHIFTER_I,
                        (Consumer<DefaultUpdatingDataframe>) df -> df.addSeries(DynamicModelDataframeConstants.TRANSFORMER, false, createTwoRowsSeries("NGEN_NHV1"))),
                Arguments.of(PHASE_SHIFTER_P,
                        (Consumer<DefaultUpdatingDataframe>) df -> df.addSeries(DynamicModelDataframeConstants.TRANSFORMER, false, createTwoRowsSeries("NGEN_NHV1"))),
                Arguments.of(PHASE_SHIFTER_BLOCKING_I,
                        (Consumer<DefaultUpdatingDataframe>) df -> df.addSeries(PHASE_SHIFTER_ID, false, createTwoRowsSeries("PSI"))),
                Arguments.of(TAP_CHANGER,
                        (Consumer<DefaultUpdatingDataframe>) df -> {
                            df.addSeries(STATIC_ID, false, createTwoRowsSeries("LOAD"));
                            df.addSeries(SIDE, false, createTwoRowsSeries(TransformerSide.LOW_VOLTAGE.toString()));
                        }),
                Arguments.of(TAP_CHANGER,
                        (Consumer<DefaultUpdatingDataframe>) df -> df.addSeries(STATIC_ID, false, createTwoRowsSeries("LOAD"))),
                Arguments.of(UNDER_VOLTAGE,
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
