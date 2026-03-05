/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.update.DefaultUpdatingDataframe;
import com.powsybl.dataframe.update.TestDoubleSeries;
import com.powsybl.dataframe.update.TestStringSeries;
import com.powsybl.dynawo.models.events.AbstractEvent;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.python.dynamic.PythonEventModelsSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
class EventModelsAdderTest {

    private DefaultUpdatingDataframe dataframe;
    private PythonEventModelsSupplier eventModelsSupplier;

    @BeforeEach
    void setup() {
        dataframe = new DefaultUpdatingDataframe(1);
        eventModelsSupplier = new PythonEventModelsSupplier();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("eventProvider")
    void testEventAdders(String name, Consumer<DefaultUpdatingDataframe> updateDataframe) {
        Network network = EurostagTutorialExample1Factory.createWithLFResults();
        dataframe.addSeries(START_TIME, false, new TestDoubleSeries(10));
        updateDataframe.accept(dataframe);
        EventMappingHandler.addElements(name, eventModelsSupplier, dataframe);

        assertThat(eventModelsSupplier.get(network)).satisfiesExactly(
                model1 -> assertThat(model1).isInstanceOf(AbstractEvent.class));
    }

    @Test
    void testUnknownEventName() {
        String eventName = "WRONG_NAME";
        assertThatThrownBy(() -> EventMappingHandler.getMetadata(eventName))
                .isInstanceOf(PowsyblException.class)
                .hasMessage("No event named WRONG_NAME");
        assertThatThrownBy(() -> EventMappingHandler.addElements(eventName, null, null))
                .isInstanceOf(PowsyblException.class)
                .hasMessage("No event named WRONG_NAME");
    }

    static Stream<Arguments> eventProvider() {
        return Stream.of(
                Arguments.of("Disconnect",
                        (Consumer<DefaultUpdatingDataframe>) df -> {
                            df.addSeries(STATIC_ID, false, new TestStringSeries("NHV1_NHV2_1"));
                            df.addSeries(DISCONNECT_ONLY, false, new TestStringSeries(TwoSides.ONE.toString()));
                        }),
                Arguments.of("Disconnect",
                        (Consumer<DefaultUpdatingDataframe>) df -> df.addSeries(STATIC_ID, false, new TestStringSeries("GEN"))),
                Arguments.of("ActivePowerVariation",
                        (Consumer<DefaultUpdatingDataframe>) df -> {
                            df.addSeries(STATIC_ID, false, new TestStringSeries("GEN"));
                            df.addSeries(DELTA_P, false, new TestDoubleSeries(1.3));
                        }),
                Arguments.of("ReactivePowerVariation",
                        (Consumer<DefaultUpdatingDataframe>) df -> {
                            df.addSeries(STATIC_ID, false, new TestStringSeries("GEN"));
                            df.addSeries(DELTA_Q, false, new TestDoubleSeries(1.4));
                        }),
                Arguments.of("ReferenceVoltageVariation",
                        (Consumer<DefaultUpdatingDataframe>) df -> {
                            df.addSeries(STATIC_ID, false, new TestStringSeries("GEN"));
                            df.addSeries(DELTA_U, false, new TestDoubleSeries(1.5));
                        }),
                Arguments.of("NodeFault",
                        (Consumer<DefaultUpdatingDataframe>) df -> {
                            df.addSeries(STATIC_ID, false, new TestStringSeries("NLOAD"));
                            df.addSeries(FAULT_TIME, false, new TestDoubleSeries(0.1));
                            df.addSeries(R_PU, false, new TestDoubleSeries(0));
                            df.addSeries(X_PU, false, new TestDoubleSeries(0.01));
                        })
        );
    }
}
