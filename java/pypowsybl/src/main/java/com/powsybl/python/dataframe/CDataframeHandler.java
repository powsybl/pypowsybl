/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dataframe;

import com.powsybl.dataframe.DataframeHandler;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.SeriesPointer;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.word.PointerBase;

import java.util.OptionalInt;

import static org.graalvm.word.WordFactory.nullPointer;

/**
 * Writes dataframe to C structures.
 *
 * @author Sylvain Leclerc {@literal <sylvain.leclerc at rte-france.com>}
 */
@SuppressWarnings({"java:S1602", "java:S1604", "Convert2Lambda"})
public class CDataframeHandler implements DataframeHandler {

    public static final int STRING_SERIES_TYPE = 0;
    public static final int DOUBLE_SERIES_TYPE = 1;
    public static final int INT_SERIES_TYPE = 2;
    public static final int BOOLEAN_SERIES_TYPE = 3;

    private ArrayPointer<SeriesPointer> dataframePtr;
    private int currentIndex;

    public CDataframeHandler() {
        this.dataframePtr = nullPointer();
        this.currentIndex = 0;
    }

    public ArrayPointer<SeriesPointer> getDataframePtr() {
        return dataframePtr;
    }

    @Override
    public void allocate(int seriesCount) {
        SeriesPointer seriesPtr = UnmanagedMemory.calloc(seriesCount * SizeOf.get(SeriesPointer.class));
        dataframePtr = PyPowsyblApiHeader.allocArrayPointer(seriesPtr, seriesCount);
    }

    @Override
    public StringSeriesWriter newStringIndex(String name, int size) {
        CCharPointerPointer dataPtr = UnmanagedMemory.calloc(size * SizeOf.get(CCharPointerPointer.class));
        addIndex(name, size, dataPtr, STRING_SERIES_TYPE);
        return new StringSeriesWriter() {
            @Override
            public void set(int i, String v) {
                dataPtr.addressOf(i).write(CTypeUtil.toCharPtr(v));
            }
        };
    }

    @Override
    public IntSeriesWriter newIntIndex(String name, int size) {
        CIntPointer dataPtr = UnmanagedMemory.calloc(size * SizeOf.get(CIntPointer.class));
        addIndex(name, size, dataPtr, INT_SERIES_TYPE);
        return new IntSeriesWriter() {
            @Override
            public void set(int i, int v) {
                dataPtr.addressOf(i).write(v);
            }
        };
    }

    @Override
    public StringSeriesWriter newStringSeries(String name, int size) {
        CCharPointerPointer dataPtr = UnmanagedMemory.calloc(size * SizeOf.get(CCharPointerPointer.class));
        addSeries(name, size, dataPtr, STRING_SERIES_TYPE);
        return new StringSeriesWriter() {
            @Override
            public void set(int i, String v) {
                dataPtr.addressOf(i).write(CTypeUtil.toCharPtr(v));
            }
        };
    }

    @Override
    public IntSeriesWriter newIntSeries(String name, int size) {
        CIntPointer dataPtr = UnmanagedMemory.calloc(size * SizeOf.get(CIntPointer.class));
        addSeries(name, size, dataPtr, INT_SERIES_TYPE);
        return new IntSeriesWriter() {
            @Override
            public void set(int i, int v) {
                dataPtr.addressOf(i).write(v);
            }
        };
    }

    @Override
    public OptionalIntSeriesWriter newOptionalIntSeries(String name, int size) {
        CIntPointer dataPtr = UnmanagedMemory.calloc(size * SizeOf.get(CIntPointer.class));
        CIntPointer maskPtr = UnmanagedMemory.calloc(size * SizeOf.get(CIntPointer.class));
        addOptionalSeries(name, size, dataPtr, maskPtr, INT_SERIES_TYPE);
        return new OptionalIntSeriesWriter() {
            @Override
            public void set(int index, OptionalInt value) {
                if (value.isEmpty()) {
                    dataPtr.addressOf(index).write(0);
                    maskPtr.addressOf(index).write(1);
                } else {
                    dataPtr.addressOf(index).write(value.getAsInt());
                }
            }
        };
    }

    @Override
    public BooleanSeriesWriter newBooleanSeries(String name, int size) {
        CCharPointer dataPtr = UnmanagedMemory.calloc(size * SizeOf.get(CCharPointer.class));
        addSeries(name, size, dataPtr, BOOLEAN_SERIES_TYPE);
        return new BooleanSeriesWriter() {
            @Override
            public void set(int i, boolean v) {
                dataPtr.addressOf(i).write(v ? (byte) 1 : 0);
            }
        };
    }

    @Override
    public DoubleSeriesWriter newDoubleSeries(String name, int size) {
        CDoublePointer dataPtr = UnmanagedMemory.calloc(size * SizeOf.get(CDoublePointer.class));
        addSeries(name, size, dataPtr, DOUBLE_SERIES_TYPE);
        return new DoubleSeriesWriter() {
            @Override
            public void set(int i, double v) {
                dataPtr.addressOf(i).write(v);
            }
        };
    }

    private void addSeries(String name, int count, PointerBase dataPtr, int type) {
        addSeries(false, name, count, dataPtr, nullPointer(), type);
    }

    private void addOptionalSeries(String name, int count, PointerBase dataPtr, CIntPointer maskPtr, int type) {
        addSeries(false, name, count, dataPtr, maskPtr, type);
    }

    private void addIndex(String name, int count, PointerBase dataPtr, int type) {
        addSeries(true, name, count, dataPtr, nullPointer(), type);
    }

    private void addSeries(boolean index, String name, int count, PointerBase dataPtr, CIntPointer maskPtr, int type) {
        SeriesPointer seriesPtrI = dataframePtr.getPtr().addressOf(currentIndex);
        seriesPtrI.setName(CTypeUtil.toCharPtr(name));
        seriesPtrI.setIndex(index);
        seriesPtrI.setType(type);
        seriesPtrI.data().setLength(count);
        seriesPtrI.data().setPtr(dataPtr);
        seriesPtrI.setMask(maskPtr);
        currentIndex++;
    }
}
