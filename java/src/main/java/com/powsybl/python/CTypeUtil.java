/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.oracle.svm.core.SubstrateUtil;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.*;
import org.graalvm.word.WordFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
final class CTypeUtil {

    private CTypeUtil() {
    }

    static CCharPointer toCharPtr(String str) {
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

    static String toString(CCharPointer charPtr) {
        // pybind11 convert std::string and char* to python utf-8 string
        return CTypeConversion.toJavaString(charPtr, SubstrateUtil.strlen(charPtr), StandardCharsets.UTF_8);
    }

    static List<String> toStringList(CCharPointerPointer charPtrPtr, int length) {
        List<String> stringList = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            CCharPointer charPtr = charPtrPtr.read(i);
            String str = toString(charPtr);
            stringList.add(str);
        }
        return stringList;
    }

    static List<Double> toDoubleList(CDoublePointer doublePtr, int length) {
        List<Double> doubleList = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            double d = doublePtr.read(i);
            doubleList.add(d);
        }
        return doubleList;
    }

    static List<Integer> toIntegerList(CIntPointer intPointer, int length) {
        List<Integer> ints = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            int j = intPointer.read(i);
            ints.add(j);
        }
        return ints;
    }
}
