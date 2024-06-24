/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.commons;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.python.commons.PyPowsyblApiHeader.DataframeMetadataPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.SeriesMetadataPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.StringMap;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.*;
import org.graalvm.word.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public final class CTypeUtil {

    private CTypeUtil() {
    }

    public static CCharPointer toCharPtr(String str) {
        if (str == null) {
            return WordFactory.nullPointer();
        }
        // pybind11 convert std::string and char* to python utf-8 string
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        CCharPointer charPtr = UnmanagedMemory.calloc((bytes.length + 1) * SizeOf.get(CCharPointer.class));
        for (int i = 0; i < bytes.length; ++i) {
            charPtr.write(i, bytes[i]);
        }
        charPtr.write(bytes.length, (byte) 0);
        return charPtr;
    }

    public static CCharPointer toBytePtr(byte[] bytes) {
        if (bytes == null) {
            return WordFactory.nullPointer();
        }
        CCharPointer charPtr = UnmanagedMemory.calloc(bytes.length * SizeOf.get(CCharPointer.class));
        for (int i = 0; i < bytes.length; ++i) {
            charPtr.write(i, bytes[i]);
        }
        return charPtr;
    }

    /**
     * Creates a string from a UTF-8 encoded char*
     */
    public static String toString(CCharPointer charPtr) {
        // pybind11 convert std::string and char* to python utf-8 string
        return CTypeConversion.utf8ToJavaString(charPtr);
    }

    /**
     * Creates a string from a UTF-8 encoded char*, or {@code null} if it's empty.
     */
    public static String toStringOrNull(CCharPointer charPtr) {
        String str = toString(charPtr);
        return str.isEmpty() ? null : str;
    }

    public static String[][] toString2DArray(CCharPointerPointer charPtrPtr, int length, int rows) {
        int cols = length / rows;
        String[][] string2DArray = new String[rows][length / cols];
        int index = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                CCharPointer charPtr = charPtrPtr.read(index++);
                String str = toString(charPtr);
                string2DArray[i][j] = str;
            }
        }
        return string2DArray;
    }

    public static List<String> toStringList(CCharPointerPointer charPtrPtr, int length) {
        List<String> stringList = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            CCharPointer charPtr = charPtrPtr.read(i);
            String str = toString(charPtr);
            stringList.add(str);
        }
        return stringList;
    }

    public static List<Double> toDoubleList(CDoublePointer doublePtr, int length) {
        List<Double> doubleList = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            double d = doublePtr.read(i);
            doubleList.add(d);
        }
        return doubleList;
    }

    public static List<Integer> toIntegerList(CIntPointer intPointer, int length) {
        List<Integer> ints = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            int j = intPointer.read(i);
            ints.add(j);
        }
        return ints;
    }

    /**
     * Convert an int list to a set of enum using the specified converter
     */
    public static <T> Set<T> toEnumSet(CIntPointer intPointer, int length, IntFunction<T> converter) {
        Set<T> enumSet = new HashSet<>();
        for (int i = 0; i < length; i++) {
            T value = converter.apply(intPointer.read(i));
            enumSet.add(value);
        }
        return enumSet;
    }

    public static Map<String, String> toStringMap(CCharPointerPointer keysPointer, int keysCount,
                                                  CCharPointerPointer valuesPointer, int valuesCount) {
        List<String> keys = toStringList(keysPointer, keysCount);
        List<String> values = toStringList(valuesPointer, valuesCount);
        return IntStream.range(0, keys.size())
                .boxed()
                .collect(Collectors.toMap(keys::get, values::get));
    }

    public static StringMap fromStringMap(Map<String, String> stringMap) {
        StringMap mapPtr = UnmanagedMemory.calloc(SizeOf.get(StringMap.class));
        mapPtr.setLength(stringMap.size());
        List<String> keys = new ArrayList<>(stringMap.size());
        List<String> values = new ArrayList<>(stringMap.size());
        stringMap.forEach((key, value) -> {
            keys.add(key);
            values.add(value);
        });
        mapPtr.setKeys(Util.getStringListAsPtr(keys));
        mapPtr.setValues(Util.getStringListAsPtr(values));
        return mapPtr;
    }

    public static DataframeMetadataPointer createSeriesMetadata(List<SeriesMetadata> metadata) {
        DataframeMetadataPointer res = UnmanagedMemory.calloc(SizeOf.get(DataframeMetadataPointer.class));
        createSeriesMetadata(metadata, res);
        return res;
    }

    public static void createSeriesMetadata(List<SeriesMetadata> metadata, DataframeMetadataPointer cMetadata) {
        SeriesMetadataPointer seriesMetadataPtr = UnmanagedMemory
                .calloc(metadata.size() * SizeOf.get(SeriesMetadataPointer.class));
        for (int i = 0; i < metadata.size(); i++) {
            SeriesMetadata colMetadata = metadata.get(i);
            SeriesMetadataPointer metadataPtr = seriesMetadataPtr.addressOf(i);
            metadataPtr.setName(CTypeUtil.toCharPtr(colMetadata.getName()));
            metadataPtr.setType(Util.convert(colMetadata.getType()));
            metadataPtr.setIndex(colMetadata.isIndex());
            metadataPtr.setModifiable(colMetadata.isModifiable());
            metadataPtr.setDefault(colMetadata.isDefaultAttribute());
        }
        cMetadata.setAttributesCount(metadata.size());
        cMetadata.setAttributesMetadata(seriesMetadataPtr);
    }
}
