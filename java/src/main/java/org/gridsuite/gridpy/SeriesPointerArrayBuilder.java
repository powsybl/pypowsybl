/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.gridpy;

import com.powsybl.commons.PowsyblException;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;

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

    private final List<T> elements;

    private final GridPyApiHeader.SeriesPointer seriesPtr;

    private final int seriesCount;

    private int seriesIndex = 0;

    SeriesPointerArrayBuilder(List<T> elements, int seriesCount) {
        this.elements = Objects.requireNonNull(elements);
        if (seriesCount < 1) {
            throw new IllegalArgumentException("Bad series count: " + seriesCount);
        }
        this.seriesCount = seriesCount;
        seriesPtr = UnmanagedMemory.calloc(seriesCount * SizeOf.get(GridPyApiHeader.SeriesPointer.class));
    }

    private void checkColumnIndex() {
        if (seriesIndex >= seriesCount) {
            throw new PowsyblException("Not enough series:" + seriesCount);
        }
    }

    SeriesPointerArrayBuilder<T> addStringSeries(String seriesName, Function<T, String> stringGetter) {
        Objects.requireNonNull(seriesName);
        Objects.requireNonNull(stringGetter);
        checkColumnIndex();
        GridPyApiHeader.SeriesPointer seriesPtrI = seriesPtr.addressOf(seriesIndex);
        seriesPtrI.setName(CTypeConversion.toCString(seriesName).get());
        seriesPtrI.setType(STRING_SERIES_TYPE);
        seriesPtrI.data().setLength(elements.size());
        CCharPointerPointer dataPtr = UnmanagedMemory.calloc(elements.size() * SizeOf.get(CCharPointerPointer.class));
        for (int i = 0; i < elements.size(); i++) {
            T element = elements.get(i);
            dataPtr.addressOf(i).write(CTypeConversion.toCString(stringGetter.apply(element)).get());
        }
        seriesPtrI.data().setPtr(dataPtr);
        seriesIndex++;
        return this;
    }

    SeriesPointerArrayBuilder<T> addEnumSeries(String seriesName, Function<T, Enum> enumGetter) {
        Objects.requireNonNull(enumGetter);
        return addStringSeries(seriesName, element -> enumGetter.apply(element).name());
    }

    SeriesPointerArrayBuilder<T> addDoubleSeries(String seriesName, ToDoubleFunction<T> doubleGetter) {
        Objects.requireNonNull(seriesName);
        Objects.requireNonNull(doubleGetter);
        checkColumnIndex();
        GridPyApiHeader.SeriesPointer seriesPtrI = seriesPtr.addressOf(seriesIndex);
        seriesPtrI.setName(CTypeConversion.toCString(seriesName).get());
        seriesPtrI.setType(DOUBLE_SERIES_TYPE);
        seriesPtrI.data().setLength(elements.size());
        CDoublePointer dataPtr = UnmanagedMemory.calloc(elements.size() * SizeOf.get(CDoublePointer.class));
        for (int i = 0; i < elements.size(); i++) {
            T element = elements.get(i);
            dataPtr.addressOf(i).write(doubleGetter.applyAsDouble(element));
        }
        seriesPtrI.data().setPtr(dataPtr);
        seriesIndex++;
        return this;
    }

    SeriesPointerArrayBuilder<T> addIntSeries(String seriesName, ToIntFunction<T> intGetter) {
        return addIntSeries(seriesName, intGetter, INT_SERIES_TYPE);
    }

    private SeriesPointerArrayBuilder<T> addIntSeries(String seriesName, ToIntFunction<T> intGetter, int type) {
        Objects.requireNonNull(seriesName);
        Objects.requireNonNull(intGetter);
        checkColumnIndex();
        GridPyApiHeader.SeriesPointer seriesPtrI = seriesPtr.addressOf(seriesIndex);
        seriesPtrI.setName(CTypeConversion.toCString(seriesName).get());
        seriesPtrI.setType(type);
        seriesPtrI.data().setLength(elements.size());
        CIntPointer dataPtr = UnmanagedMemory.calloc(elements.size() * SizeOf.get(CIntPointer.class));
        for (int i = 0; i < elements.size(); i++) {
            T element = elements.get(i);
            dataPtr.addressOf(i).write(intGetter.applyAsInt(element));
        }
        seriesPtrI.data().setPtr(dataPtr);
        seriesIndex++;
        return this;
    }

    SeriesPointerArrayBuilder<T> addBooleanSeries(String seriesName, Predicate<T> booleanGetter) {
        Objects.requireNonNull(booleanGetter);
        return addIntSeries(seriesName, element -> booleanGetter.test(element) ? 1 : 0, BOOLEAN_SERIES_TYPE);
    }

    GridPyApiHeader.ArrayPointer<GridPyApiHeader.SeriesPointer> build() {
        return GridPyApi.allocArrayPointer(seriesPtr, seriesCount);
    }
}
