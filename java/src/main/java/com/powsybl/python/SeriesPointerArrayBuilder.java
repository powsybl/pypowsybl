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

    private static final int STRING_SERIES_TYPE = 0;
    private static final int DOUBLE_SERIES_TYPE = 1;
    private static final int INT_SERIES_TYPE = 2;
    private static final int BOOLEAN_SERIES_TYPE = 3;
    private static final int INT_UNDEFINED_VALUE = -99999;

    interface Series<T> {

        String getName();

        int getType();

        PointerBase createDataPtr(List<T> elements);
    }

    abstract static class AbstractSeries<T> implements Series<T> {

        private final String name;

        AbstractSeries(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    class DoubleSeries extends AbstractSeries<T> {

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
    }

    class StringSeries extends AbstractSeries<T> {

        private final Function<T, String> stringGetter;

        StringSeries(String name, Function<T, String> stringGetter) {
            super(name);
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
    }

    class IntSeries extends AbstractSeries<T> {

        private final ToIntFunction<T> intGetter;

        IntSeries(String name, ToIntFunction<T> intGetter) {
            super(name);
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
    }

    class BooleanSeries extends AbstractSeries<T> {

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
    }

    private final List<T> elements;

    private final List<Series<T>> seriesList = new ArrayList<>();

    SeriesPointerArrayBuilder(List<T> elements) {
        this.elements = Objects.requireNonNull(elements);
    }

    List<T> getElements() {
        return elements;
    }

    SeriesPointerArrayBuilder<T> addStringSeries(String seriesName, Function<T, String> stringGetter) {
        Objects.requireNonNull(seriesName);
        Objects.requireNonNull(stringGetter);
        seriesList.add(new StringSeries(seriesName, stringGetter));
        return this;
    }

    SeriesPointerArrayBuilder<T> addEnumSeries(String seriesName, Function<T, Enum<?>> enumGetter) {
        Objects.requireNonNull(enumGetter);
        return addStringSeries(seriesName, element -> enumGetter.apply(element).name());
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

    private SeriesPointerArrayBuilder<T> addIntSeries(String seriesName, ToIntFunction<T> intGetter) {
        Objects.requireNonNull(seriesName);
        Objects.requireNonNull(intGetter);
        seriesList.add(new IntSeries(seriesName, intGetter));
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
            Series<T> series = seriesList.get(seriesIndex);
            PyPowsyblApiHeader.SeriesPointer seriesPtrI = seriesPtr.addressOf(seriesIndex);
            seriesPtrI.setName(CTypeUtil.toCharPtr(series.getName()));
            seriesPtrI.setType(series.getType());
            seriesPtrI.data().setLength(elements.size());
            PointerBase dataPtr = series.createDataPtr(elements);
            seriesPtrI.data().setPtr(dataPtr);
        }

        return PyPowsyblApiHeader.allocArrayPointer(seriesPtr, seriesList.size());
    }
}
