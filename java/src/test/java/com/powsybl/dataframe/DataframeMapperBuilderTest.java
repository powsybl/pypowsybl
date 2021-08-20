/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe;

import com.google.common.base.Functions;
import com.powsybl.dataframe.impl.DefaultDataframeHandler;
import com.powsybl.dataframe.impl.Series;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
class DataframeMapperBuilderTest {

    private static enum Color {
        RED,
        BLUE
    }

    private static class Element {
        private final String id;
        private String strValue;
        private double doubleValue;
        private int intValue;
        private Color colorValue;

        Element(String id, String strValue, double doubleValue, int intValue, Color colorValue) {
            this.id = id;
            this.strValue = strValue;
            this.doubleValue = doubleValue;
            this.intValue = intValue;
            this.colorValue = colorValue;
        }

        public String getId() {
            return id;
        }

        public String getStrValue() {
            return strValue;
        }

        public void setStrValue(String strValue) {
            this.strValue = strValue;
        }

        public double getDoubleValue() {
            return doubleValue;
        }

        public void setDoubleValue(double doubleValue) {
            this.doubleValue = doubleValue;
        }

        public int getIntValue() {
            return intValue;
        }

        public void setIntValue(int intValue) {
            this.intValue = intValue;
        }

        public Color getColorValue() {
            return colorValue;
        }

        public void setColorValue(Color colorValue) {
            this.colorValue = colorValue;
        }
    }

    private static class Container {
        private final Map<String, Element> elements;

        Container(Element... elements) {
            this(Arrays.asList(elements));
        }

        Container(Collection<Element> elements) {
            this.elements = elements.stream().collect(Collectors.toUnmodifiableMap(Element::getId, Functions.identity()));
        }

        List<Element> getElements() {
            return new ArrayList<>(elements.values());
        }

        Element getElement(String id) {
            return elements.get(id);
        }
    }

    private DataframeMapper<Container> mapper;

    @BeforeEach
    void setUp() {
        mapper = new DataframeMapperBuilder<Container, Element>()
            .itemsProvider(Container::getElements)
            .itemGetter(Container::getElement)
            .stringsIndex("id", Element::getId)
            .strings("str", Element::getStrValue, Element::setStrValue)
            .ints("int", Element::getIntValue, Element::setIntValue)
            .doubles("double", Element::getDoubleValue, Element::setDoubleValue)
            .enums("color", Color.class, Element::getColorValue, Element::setColorValue)
            .build();
    }

    @Test
    void test() {
        DataframeMapper<Container> mapper = new DataframeMapperBuilder<Container, Element>()
            .itemsProvider(Container::getElements)
            .stringsIndex("id", Element::getId)
            .strings("str", Element::getStrValue)
            .ints("int", Element::getIntValue)
            .doubles("double", Element::getDoubleValue)
            .enums("color", Color.class, Element::getColorValue)
            .build();

        Container container = new Container(
            new Element("el1", "val1", 1, 10, Color.RED),
            new Element("el2", "val2", 2, 20, Color.BLUE)
        );

        List<Series> series = new ArrayList<>();
        mapper.createDataframe(container, new DefaultDataframeHandler(series::add));

        assertThat(series)
            .extracting(Series::getName)
            .containsExactly("id", "str", "int", "double", "color");
    }

    private static IndexedSeries<String> createSeries(List<String> ids, List<String> values) {
        return new IndexedSeries<>() {
            @Override
            public int getSize() {
                return ids.size();
            }

            @Override
            public String getId(int index) {
                return ids.get(index);
            }

            @Override
            public String getValue(int index) {
                return values.get(index);
            }
        };
    }

    private static IntIndexedSeries createSeries(List<String> ids, int[] values) {
        return new IntIndexedSeries() {
            @Override
            public int getSize() {
                return ids.size();
            }

            @Override
            public String getId(int index) {
                return ids.get(index);
            }

            @Override
            public int getValue(int index) {
                return values[index];
            }
        };
    }

    private static DoubleIndexedSeries createSeries(List<String> ids, double[] values) {
        return new DoubleIndexedSeries() {
            @Override
            public int getSize() {
                return ids.size();
            }

            @Override
            public String getId(int index) {
                return ids.get(index);
            }

            @Override
            public double getValue(int index) {
                return values[index];
            }
        };
    }

    @Test
    void update() {
        Container container = new Container(
            new Element("el1", "val1", 1, 10, Color.RED),
            new Element("el2", "val2", 2, 20, Color.BLUE)
        );

        mapper.updateIntSeries(container, "int", createSeries(List.of("el1"), new int[]{112}));
        assertEquals(112, container.getElement("el1").getIntValue());

        mapper.updateDoubleSeries(container, "double", createSeries(List.of("el2"), new double[]{0.1}));
        assertEquals(0.1, container.getElement("el2").getDoubleValue());

        mapper.updateStringSeries(container, "str", createSeries(List.of("el2"), List.of("hello")));
        assertEquals("hello", container.getElement("el2").getStrValue());

        mapper.updateStringSeries(container, "color", createSeries(List.of("el2"), List.of("RED")));
        assertEquals(Color.RED, container.getElement("el2").getColorValue());
    }

}
