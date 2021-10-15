/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe;

import com.google.common.base.Functions;
import com.powsybl.dataframe.impl.DefaultDataframeHandler;
import com.powsybl.python.update.Series;
import com.powsybl.dataframe.update.UpdatingDataframe;
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
        private final int id2;
        private String strValue;
        private double doubleValue;
        private int intValue;
        private Color colorValue;

        Element(String id, String strValue, double doubleValue, int intValue, Color colorValue) {
            this.id2 = 0;
            this.id = id;
            this.strValue = strValue;
            this.doubleValue = doubleValue;
            this.intValue = intValue;
            this.colorValue = colorValue;
        }

        Element(String id, int id2, String strValue, double doubleValue, int intValue, Color colorValue) {
            this.id2 = id2;
            this.id = id;
            this.strValue = strValue;
            this.doubleValue = doubleValue;
            this.intValue = intValue;
            this.colorValue = colorValue;
        }

        public String getId() {
            return id;
        }

        public int getId2() {
            return id2;
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

    private static class MultiIndexContainer {
        private final List<Element> elements = new ArrayList<>();

        MultiIndexContainer(Element... elements) {
            this(Arrays.asList(elements));
        }

        MultiIndexContainer(Collection<Element> elements) {
            this.elements.addAll(elements);
        }

        List<Element> getElements() {
            return elements;
        }

        Element getElement(UpdatingDataframe dataframe, int index) {
            List<Element> result = elements.stream().filter(element -> dataframe.getStringValue("id", index).equals(element.getId())
                    && dataframe.getIntValue("id2", index) == element.getId2()).collect(Collectors.toList());
            return result.get(0);
        }

        Element getElement(String id, int id2) {
            List<Element> result = elements.stream().filter(element -> id.equals(element.getId())
                    && id2 == element.getId2()).collect(Collectors.toList());
            return result.get(0);
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

        List<com.powsybl.dataframe.impl.Series> series = new ArrayList<>();
        mapper.createDataframe(container, new DefaultDataframeHandler(series::add));

        assertThat(series)
                .extracting(com.powsybl.dataframe.impl.Series::getName)
                .containsExactly("id", "str", "int", "double", "color");
    }

    class TestIntSeries implements Series<List<Integer>> {

        private final List<Integer> values = new ArrayList<>();
        private final String name;

        public TestIntSeries(List<Integer> values, String name) {
            this.values.addAll(values);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getSize() {
            return values.size();
        }

        @Override
        public List<Integer> getValues() {
            return values;
        }
    }

    class TestDoubleSeries implements Series<List<Double>> {

        private final List<Double> values = new ArrayList<>();
        private final String name;

        public TestDoubleSeries(List<Double> values, String name) {
            this.values.addAll(values);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getSize() {
            return values.size();
        }

        @Override
        public List<Double> getValues() {
            return values;
        }
    }

    class TestStringSeries implements Series<List<String>> {

        private final List<String> values = new ArrayList<>();
        private final String name;

        public TestStringSeries(List<String> values, String name) {
            this.values.addAll(values);
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getSize() {
            return values.size();
        }

        @Override
        public List<String> getValues() {
            return values;
        }
    }

    class TestDataframe implements UpdatingDataframe {

        private final int size;
        private final Map<String, SeriesMetadata> columns = new LinkedHashMap<>();
        private final Map<String, Series> series = new HashMap<>();

        public TestDataframe(int size) {
            this.size = size;
        }

        public void addSeries(Series series) {
            this.series.put(series.getName(), series);
        }

        public void addColumnName(String name, SeriesDataType type, boolean index) {
            columns.put(name, new SeriesMetadata(index, name, false, type));
        }

        @Override
        public List<SeriesMetadata> getSeriesMetadata() {
            return new ArrayList<>(columns.values());
        }

        @Override
        public double getDoubleValue(String column, int index) {
            return ((TestDoubleSeries) series.get(column)).getValues().get(index);
        }

        @Override
        public double getDoubleValue(int column, int index) {
            return ((TestDoubleSeries) series.get(getSeriesMetadata().get(column).getName())).getValues().get(index);
        }

        @Override
        public double getDoubleValue(String columnName, int column, int index) {
            if (containsColumnName(columnName, SeriesDataType.DOUBLE)) {
                return getDoubleValue(columnName, index);
            } else {
                return getDoubleValue(column, index);
            }
        }

        @Override
        public String getStringValue(String column, int index) {
            return ((TestStringSeries) series.get(column)).getValues().get(index);
        }

        @Override
        public String getStringValue(int column, int index) {
            return ((TestStringSeries) series.get(getSeriesMetadata().get(column).getName())).getValues().get(index);
        }

        @Override
        public String getStringValue(String columnName, int column, int index) {
            if (containsColumnName(columnName, SeriesDataType.STRING)) {
                return getStringValue(columnName, index);
            } else {
                return getStringValue(column, index);
            }
        }

        @Override
        public int getIntValue(String column, int index) {
            return ((TestIntSeries) series.get(column)).getValues().get(index);
        }

        @Override
        public int getIntValue(int column, int index) {
            return ((TestIntSeries) series.get(getSeriesMetadata().get(column).getName())).getValues().get(index);
        }

        @Override
        public int getIntValue(String columnName, int column, int index) {
            if (containsColumnName(columnName, SeriesDataType.INT)) {
                return getIntValue(columnName, index);
            } else {
                return getIntValue(column, index);
            }
        }

        @Override
        public int getLineCount() {
            return size;
        }

        private boolean containsColumnName(String columnName, SeriesDataType type) {
            return columns.containsKey(columnName) && columns.get(columnName).getType().equals(type);
        }
    }

    UpdatingDataframe createDataframe(int size) {
        TestDataframe dataframe = new TestDataframe(size);
        dataframe.addColumnName("id", SeriesDataType.STRING, true);
        dataframe.addColumnName("double", SeriesDataType.DOUBLE, false);
        dataframe.addSeries(new TestStringSeries(List.of("el1", "el2"), "id"));
        dataframe.addSeries(new TestDoubleSeries(List.of(1.2, 2.2), "double"));
        return dataframe;

    }

    @Test
    void updateMonoIndex() {

        Container container = new Container(
                new Element("el1", "val1", 1.0, 10, Color.RED),
                new Element("el2", "val2", 2.0, 20, Color.BLUE)
        );
        mapper = new DataframeMapperBuilder<Container, Element>()
                .itemsProvider(Container::getElements)
                .itemGetter(Container::getElement)
                .stringsIndex("id", Element::getId)
                .strings("str", Element::getStrValue, Element::setStrValue)
                .ints("int", Element::getIntValue, Element::setIntValue)
                .doubles("double", Element::getDoubleValue, Element::setDoubleValue)
                .enums("color", Color.class, Element::getColorValue, Element::setColorValue)
                .build();
        mapper.updateSeries(container, createDataframe(2));
        assertEquals(1.2, container.elements.get("el1").getDoubleValue());
        assertEquals(2.2, container.elements.get("el2").getDoubleValue());
    }

    UpdatingDataframe createDataframeMultiIndex(int size) {
        TestDataframe dataframe = new TestDataframe(size);
        dataframe.addColumnName("id", SeriesDataType.STRING, true);
        dataframe.addColumnName("id2", SeriesDataType.INT, true);
        dataframe.addColumnName("double", SeriesDataType.DOUBLE, false);
        dataframe.addColumnName("str", SeriesDataType.STRING, false);
        dataframe.addSeries(new TestStringSeries(List.of("el1", "el2"), "id"));
        dataframe.addSeries(new TestIntSeries(List.of(1, 0), "id2"));
        dataframe.addSeries(new TestDoubleSeries(List.of(1.2, 2.2), "double"));
        dataframe.addSeries(new TestStringSeries(List.of("val3", "val4"), "str"));
        return dataframe;

    }

    @Test
    void updateMultiIndex() {
        MultiIndexContainer container = new MultiIndexContainer(
                new Element("el1", 0, "val1", 1.0, 10, Color.RED),
                new Element("el1", 1, "val2", 2.0, 20, Color.BLUE),
                new Element("el2", 0, "val2", 2.0, 20, Color.BLUE)
        );
        DataframeMapper<MultiIndexContainer> multiIndexMapper = new DataframeMapperBuilder<MultiIndexContainer, Element>()
                .itemsProvider(MultiIndexContainer::getElements)
                .itemMultiIndexGetter(MultiIndexContainer::getElement)
                .stringsIndex("id", Element::getId)
                .intsIndex("id2", Element::getId2)
                .strings("str", Element::getStrValue, Element::setStrValue)
                .ints("int", Element::getIntValue, Element::setIntValue)
                .doubles("double", Element::getDoubleValue, Element::setDoubleValue)
                .enums("color", Color.class, Element::getColorValue, Element::setColorValue)
                .build();
        multiIndexMapper.updateSeries(container, createDataframeMultiIndex(2));
        assertEquals(1.0, container.getElement("el1", 0).getDoubleValue());
        assertEquals(1.2, container.getElement("el1", 1).getDoubleValue());
        assertEquals(2.2, container.getElement("el2", 0).getDoubleValue());
        assertEquals("val1", container.getElement("el1", 0).getStrValue());
        assertEquals("val3", container.getElement("el1", 1).getStrValue());
        assertEquals("val4", container.getElement("el2", 0).getStrValue());
    }
}
