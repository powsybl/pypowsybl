/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.PhaseShifterTestCaseFactory;
import com.powsybl.iidm.network.test.ThreeWindingsTransformerNetworkFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
class SeriesArrayHelperTest {

    @Test
    void testBusGetter() {
        Network network = EurostagTutorialExample1Factory.create();
        SeriesPointerArrayBuilder builder = SeriesArrayHelper.prepareData(network, PyPowsyblApiHeader.ElementType.BUS);

        List list = builder.buildJavaSeries();
        assertArraysEquals(network.getBusView().getBusStream(), Bus::getId, (List) list.get(0));
        assertArraysEquals(network.getBusView().getBusStream(), Bus::getV, (List) list.get(1));
        assertArraysEquals(network.getBusView().getBusStream(), Bus::getAngle, (List) list.get(2));
    }

    @Test
    void testLineGetter() {
        Network network = EurostagTutorialExample1Factory.create();
        SeriesPointerArrayBuilder builder = SeriesArrayHelper.prepareData(network, PyPowsyblApiHeader.ElementType.LINE);

        List list = builder.buildJavaSeries();
        assertArraysEquals(network.getLineStream(), Line::getId, (List) list.get(0));
        assertArraysEquals(network.getLineStream(), Line::getR, (List) list.get(1));
        assertArraysEquals(network.getLineStream(), Line::getX, (List) list.get(2));
        assertArraysEquals(network.getLineStream(), Line::getG1, (List) list.get(3));
        assertArraysEquals(network.getLineStream(), Line::getB1, (List) list.get(4));
        assertArraysEquals(network.getLineStream(), Line::getG2, (List) list.get(5));
        assertArraysEquals(network.getLineStream(), Line::getB2, (List) list.get(6));
        assertArraysEquals(network.getLineStream(), l -> l.getTerminal1().getP(), (List) list.get(7));
        assertArraysEquals(network.getLineStream(), l -> l.getTerminal1().getQ(), (List) list.get(8));
        assertArraysEquals(network.getLineStream(), l -> l.getTerminal2().getP(), (List) list.get(9));
        assertArraysEquals(network.getLineStream(), l -> l.getTerminal2().getQ(), (List) list.get(10));
        assertArraysEquals(network.getLineStream(), l -> l.getTerminal1().getVoltageLevel().getId(), (List) list.get(11));
        assertArraysEquals(network.getLineStream(), l -> l.getTerminal2().getVoltageLevel().getId(), (List) list.get(12));
    }

    @Test
    void testTransfo2Getter() {
        Network network = PhaseShifterTestCaseFactory.create();
        SeriesPointerArrayBuilder builder = SeriesArrayHelper.prepareData(network, PyPowsyblApiHeader.ElementType.TWO_WINDINGS_TRANSFORMER);
        List list = builder.buildJavaSeries();

        assertArraysEquals(network.getTwoWindingsTransformerStream(), TwoWindingsTransformer::getId, (List) list.get(0));
        assertArraysEquals(network.getTwoWindingsTransformerStream(), TwoWindingsTransformer::getR, (List) list.get(1));
        assertArraysEquals(network.getTwoWindingsTransformerStream(), TwoWindingsTransformer::getX, (List) list.get(2));
        assertArraysEquals(network.getTwoWindingsTransformerStream(), TwoWindingsTransformer::getG, (List) list.get(3));
        assertArraysEquals(network.getTwoWindingsTransformerStream(), TwoWindingsTransformer::getB, (List) list.get(4));
    }

    @Test
    void testTransfo3Getter() {
        Network network = ThreeWindingsTransformerNetworkFactory.create();
        SeriesPointerArrayBuilder builder = SeriesArrayHelper.prepareData(network, PyPowsyblApiHeader.ElementType.THREE_WINDINGS_TRANSFORMER);
        List list = builder.buildJavaSeries();

        assertArraysEquals(network.getThreeWindingsTransformerStream(), ThreeWindingsTransformer::getId, (List) list.get(0));
        assertArraysEquals(network.getThreeWindingsTransformerStream(), ThreeWindingsTransformer::getRatedU0, (List) list.get(1));
    }

    @Test
    void testGeneratorGetter() {
        Network network = EurostagTutorialExample1Factory.create();
        SeriesPointerArrayBuilder builder = SeriesArrayHelper.prepareData(network, PyPowsyblApiHeader.ElementType.GENERATOR);

        List list = builder.buildJavaSeries();
        assertArraysEquals(network.getGeneratorStream(), Generator::getId, (List) list.get(0));
    }

    @Test
    void testLoadGetter() {
        Network network = EurostagTutorialExample1Factory.create();
        SeriesPointerArrayBuilder builder = SeriesArrayHelper.prepareData(network, PyPowsyblApiHeader.ElementType.LOAD);

        List list = builder.buildJavaSeries();
        assertArraysEquals(network.getLoadStream(), Load::getId, (List) list.get(0));
    }

    <E, V> void assertArraysEquals(Stream<E> elements, Function<E, V> getter, List<V> actual) {
        final List<V> expected = elements.map(getter::apply).collect(Collectors.toList());
        assertEquals(expected, actual);
    }

    @Test
    void testUpdate() {
        Network network = EurostagTutorialExample1Factory.create();

        SeriesArrayHelper.updateNetworkElementsWithDoubleSeries(network, PyPowsyblApiHeader.ElementType.GENERATOR, 1, "target_p", i -> "GEN", i -> 33.0d);
        assertEquals(33.0d, network.getGenerator("GEN").getTargetP());
    }

}
