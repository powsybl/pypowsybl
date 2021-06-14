/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.word.PointerBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SeriesPointerArrayBuilder<T> {

    static final int STRING_SERIES_TYPE = 0;
    static final int DOUBLE_SERIES_TYPE = 1;
    static final int INT_SERIES_TYPE = 2;
    static final int BOOLEAN_SERIES_TYPE = 3;
    static final int INT_UNDEFINED_VALUE = -99999;

    interface Series<T, E> {

        String getName();

        boolean isIndex();

        int getType();

        PointerBase createDataPtr(List<T> elements);

        // used for unittest
        List<E> toJavaList(List<T> elements);
    }

    abstract static class AbstractSeries<T, E> implements Series<T, E> {

        private final String name;

        private final boolean index;

        AbstractSeries(String name) {
            this(name, false);
        }

        AbstractSeries(String name, boolean index) {
            this.name = name;
            this.index = index;
        }

        @Override
        public boolean isIndex() {
            return index;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    class DoubleSeries extends AbstractSeries<T, Double> {

        private final ToDoubleFunction<T> doubleGetter;

        DoubleSeries(String name, ToDoubleFunction<T> doubleGetter) {
            super(name);
            this.doubleGetter = doubleGetter;
        }

        @Override
        public int getType() {
            return DOUBLE_SERIES_TYPE;
        }

        public PointerBase createDataPtr(List<T> elements) {
            CDoublePointer dataPtr = UnmanagedMemory.calloc(elements.size() * SizeOf.get(CDoublePointer.class));
            for (int i = 0; i < elements.size(); i++) {
                T element = elements.get(i);
                dataPtr.addressOf(i).write(doubleGetter.applyAsDouble(element));
            }
            return dataPtr;
        }

        @Override
        public List<Double> toJavaList(List<T> elements) {
            List<Double> list = new ArrayList<>();
            for (int i = 0; i < elements.size(); i++) {
                list.add(doubleGetter.applyAsDouble(elements.get(i)));
            }
            return list;
        }
    }

    class StringSeries extends AbstractSeries<T, String> {

        final Function<T, String> stringGetter;

        StringSeries(String name, boolean index, Function<T, String> stringGetter) {
            super(name, index);
            this.stringGetter = stringGetter;
        }

        @Override
        public int getType() {
            return STRING_SERIES_TYPE;
        }

        @Override
        public PointerBase createDataPtr(List<T> elements) {
            CCharPointerPointer dataPtr = UnmanagedMemory.calloc(elements.size() * SizeOf.get(CCharPointerPointer.class));
            for (int i = 0; i < elements.size(); i++) {
                T element = elements.get(i);
                dataPtr.addressOf(i).write(CTypeUtil.toCharPtr(stringGetter.apply(element)));
            }
            return dataPtr;
        }

        @Override
        public List<String> toJavaList(List<T> elements) {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < elements.size(); i++) {
                list.add(stringGetter.apply(elements.get(i)));
            }
            return list;
        }
    }

    class IntSeries extends AbstractSeries<T, Integer> {

        private final ToIntFunction<T> intGetter;

        IntSeries(String name, boolean index, ToIntFunction<T> intGetter) {
            super(name, index);
            this.intGetter = intGetter;
        }

        @Override
        public int getType() {
            return INT_SERIES_TYPE;
        }

        @Override
        public PointerBase createDataPtr(List<T> elements) {
            CIntPointer dataPtr = UnmanagedMemory.calloc(elements.size() * SizeOf.get(CIntPointer.class));
            for (int i = 0; i < elements.size(); i++) {
                T element = elements.get(i);
                dataPtr.addressOf(i).write(intGetter.applyAsInt(element));
            }
            return dataPtr;
        }

        @Override
        public List<Integer> toJavaList(List<T> elements) {
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < elements.size(); i++) {
                list.add(intGetter.applyAsInt(elements.get(i)));
            }
            return list;
        }
    }

    class BooleanSeries extends AbstractSeries<T, Boolean> {

        private final Predicate<T> boolGetter;

        BooleanSeries(String name, Predicate<T> boolGetter) {
            super(name);
            this.boolGetter = boolGetter;
        }

        @Override
        public int getType() {
            return BOOLEAN_SERIES_TYPE;
        }

        @Override
        public PointerBase createDataPtr(List<T> elements) {
            CCharPointer dataPtr = UnmanagedMemory.calloc(elements.size() * SizeOf.get(CCharPointer.class));
            for (int i = 0; i < elements.size(); i++) {
                T element = elements.get(i);
                dataPtr.addressOf(i).write(boolGetter.test(element) ? (byte) 1 : 0);
            }
            return dataPtr;
        }

        @Override
        public List<Boolean> toJavaList(List<T> elements) {
            List<Boolean> list = new ArrayList<>();
            for (int i = 0; i < elements.size(); i++) {
                list.add(boolGetter.test(elements.get(i)));
            }
            return list;
        }
    }

    private final List<T> elements;

    final List<Series<T, ?>> seriesList = new ArrayList<>();

    SeriesPointerArrayBuilder(List<T> elements) {
        this.elements = Objects.requireNonNull(elements);
    }

    List<T> getElements() {
        return elements;
    }

    SeriesPointerArrayBuilder<T> addStringSeries(String seriesName, Function<T, String> stringGetter) {
        return addStringSeries(seriesName, false, stringGetter);
    }

    SeriesPointerArrayBuilder<T> addStringSeries(String seriesName, boolean index, Function<T, String> stringGetter) {
        Objects.requireNonNull(seriesName);
        Objects.requireNonNull(stringGetter);
        seriesList.add(new StringSeries(seriesName, index, stringGetter));
        return this;
    }

    SeriesPointerArrayBuilder<T> addEnumSeries(String seriesName, Function<T, Enum<?>> enumGetter) {
        Objects.requireNonNull(enumGetter);
        return addStringSeries(seriesName, false, element -> enumGetter.apply(element).name());
    }

    <U> SeriesPointerArrayBuilder<T> addDoubleSeries(String seriesName, Function<T, U> objectGetter, ToDoubleFunction<U> doubleGetter) {
        return addDoubleSeries(seriesName, objectGetter, doubleGetter, Double.NaN);
    }

    <U> SeriesPointerArrayBuilder<T> addDoubleSeries(String seriesName, Function<T, U> objectGetter, ToDoubleFunction<U> doubleGetter, double undefinedValue) {
        return addDoubleSeries(seriesName, value -> {
            U object = objectGetter.apply(value);
            return object != null ? doubleGetter.applyAsDouble(object) : undefinedValue;
        });
    }

    SeriesPointerArrayBuilder<T> addDoubleSeries(String seriesName, ToDoubleFunction<T> doubleGetter) {
        Objects.requireNonNull(seriesName);
        Objects.requireNonNull(doubleGetter);
        seriesList.add(new DoubleSeries(seriesName, doubleGetter));
        return this;
    }

    <U> SeriesPointerArrayBuilder<T> addIntSeries(String seriesName, Function<T, U> objectGetter, ToIntFunction<U> intGetter) {
        return addIntSeries(seriesName, objectGetter, intGetter, INT_UNDEFINED_VALUE);
    }

    <U> SeriesPointerArrayBuilder<T> addIntSeries(String seriesName, Function<T, U> objectGetter, ToIntFunction<U> intGetter, int undefinedValue) {
        Objects.requireNonNull(objectGetter);
        Objects.requireNonNull(intGetter);
        return addIntSeries(seriesName, value -> {
            U object = objectGetter.apply(value);
            return object != null ? intGetter.applyAsInt(object) : undefinedValue;
        });
    }

    public SeriesPointerArrayBuilder<T> addIntSeries(String seriesName, ToIntFunction<T> intGetter) {
        return addIntSeries(seriesName, false, intGetter);
    }

    public SeriesPointerArrayBuilder<T> addIntSeries(String seriesName, boolean index, ToIntFunction<T> intGetter) {
        Objects.requireNonNull(seriesName);
        Objects.requireNonNull(intGetter);
        seriesList.add(new IntSeries(seriesName, index, intGetter));
        return this;
    }

    SeriesPointerArrayBuilder<T> addBooleanSeries(String seriesName, Predicate<T> booleanGetter) {
        Objects.requireNonNull(booleanGetter);
        seriesList.add(new BooleanSeries(seriesName, booleanGetter));
        return this;
    }

    PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> build() {
        PyPowsyblApiHeader.SeriesPointer seriesPtr = UnmanagedMemory.calloc(seriesList.size() * SizeOf.get(PyPowsyblApiHeader.SeriesPointer.class));

        for (int seriesIndex = 0; seriesIndex < seriesList.size(); seriesIndex++) {
            Series<T, ?> series = seriesList.get(seriesIndex);
            PyPowsyblApiHeader.SeriesPointer seriesPtrI = seriesPtr.addressOf(seriesIndex);
            seriesPtrI.setName(CTypeUtil.toCharPtr(series.getName()));
            seriesPtrI.setIndex(series.isIndex());
            seriesPtrI.setType(series.getType());
            seriesPtrI.data().setLength(elements.size());
            PointerBase dataPtr = series.createDataPtr(elements);
            seriesPtrI.data().setPtr(dataPtr);
        }

        return PyPowsyblApiHeader.allocArrayPointer(seriesPtr, seriesList.size());
    }

    // used for unittest
    List<List> buildJavaSeries() {
        List<List> list = new ArrayList<>();
        for (int seriesIndex = 0; seriesIndex < seriesList.size(); seriesIndex++) {
            Series<T, ?> series = seriesList.get(seriesIndex);
            List<?> objects = series.toJavaList(elements);
            list.add(objects);
        }
        return list;
    }
}
