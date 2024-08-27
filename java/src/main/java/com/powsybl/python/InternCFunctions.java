/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python;

import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CIntPointer;

import static com.powsybl.python.commons.Util.doCatch;

/**
 * Defines the basic C functions for a network.
 *
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
@CContext(Directives.class)
public final class InternCFunctions {

    private InternCFunctions() {
    }

    @CEntryPoint(name = "initializeSeriesArray")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> initializeSeriesArray(IsolateThread thread,
                                                                                                    int seriesCount,
                                                                                                    PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            PyPowsyblApiHeader.SeriesPointer seriesPtr = UnmanagedMemory.calloc(seriesCount * SizeOf.get(PyPowsyblApiHeader.SeriesPointer.class));
            return PyPowsyblApiHeader.allocArrayPointer(seriesPtr, seriesCount);
        });
    }

    @CEntryPoint(name = "initializeDataframeArray")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.DataframePointer> initializeDataframeArray(IsolateThread thread,
                                                                                                          int dataframesCount,
                                                                                                          PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            PyPowsyblApiHeader.DataframePointer seriesPtr = UnmanagedMemory.calloc(dataframesCount * SizeOf.get(PyPowsyblApiHeader.DataframePointer.class));
            return PyPowsyblApiHeader.allocArrayPointer(seriesPtr, dataframesCount);
        });
    }

    @CEntryPoint(name = "initializeDataframeArrayObject")
    public static PyPowsyblApiHeader.DataframeArrayPointer initializeDataframeArrayObject(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> UnmanagedMemory.<PyPowsyblApiHeader.DataframeArrayPointer>calloc(SizeOf.get(PyPowsyblApiHeader.DataframeArrayPointer.class)));
    }

    @CEntryPoint(name = "freeDataframeArrayObject")
    public static void freeDataframeArray(IsolateThread thread, PyPowsyblApiHeader.DataframeArrayPointer cDataframeArray, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> UnmanagedMemory.free(cDataframeArray));
    }

    @CEntryPoint(name = "initializeDataframePointer")
    public static PyPowsyblApiHeader.DataframePointer initializeDataframePointer(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> UnmanagedMemory.<PyPowsyblApiHeader.DataframePointer>calloc(SizeOf.get(PyPowsyblApiHeader.DataframePointer.class)));
    }

    @CEntryPoint(name = "freeDataframePointer")
    public static void freeDataframePointer(IsolateThread thread, PyPowsyblApiHeader.DataframePointer cDataframe, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            freeDataframeContent(cDataframe);
            UnmanagedMemory.free(cDataframe);
        });
    }

    private static void freeDataframeContent(PyPowsyblApiHeader.DataframePointer dataframePointer) {
        for (int i = 0; i < dataframePointer.getSeriesCount(); i++) {
            PyPowsyblApiHeader.SeriesPointer attrMetadata = dataframePointer.getSeries().addressOf(i);
            UnmanagedMemory.free(attrMetadata.getName());
        }
        UnmanagedMemory.free(dataframePointer.getSeries());
    }

    @CEntryPoint(name = "initializeCharCharPointer")
    public static CCharPointerPointer initializeCharCharPointer(IsolateThread thread, int size, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> UnmanagedMemory.<CCharPointerPointer>calloc(size * SizeOf.get(CCharPointerPointer.class)));
    }

    @CEntryPoint(name = "freeCharCharPointer")
    public static void freeCharCharPointer(IsolateThread thread, CCharPointerPointer cCharPointerPointer, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> UnmanagedMemory.free(cCharPointerPointer));
    }

    @CEntryPoint(name = "initializeCharPointer")
    public static CCharPointer initializeCharPointer(IsolateThread thread, int size, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> UnmanagedMemory.<CCharPointer>calloc(size * SizeOf.get(CCharPointer.class)));
    }

    @CEntryPoint(name = "freeCharPointer")
    public static void freeCharPointer(IsolateThread thread, CCharPointer cCharPointer, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> UnmanagedMemory.free(cCharPointer));
    }

    @CEntryPoint(name = "initializeIntPointer")
    public static CIntPointer initializeIntPointer(IsolateThread thread, int size, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> UnmanagedMemory.<CIntPointer>calloc(size * SizeOf.get(CIntPointer.class)));
    }

    @CEntryPoint(name = "freeIntPointer")
    public static void freeIntPointer(IsolateThread thread, CIntPointer cIntPointer, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> UnmanagedMemory.free(cIntPointer));
    }

    @CEntryPoint(name = "initializeDoublePointer")
    public static CDoublePointer initializeDoublePointer(IsolateThread thread, int size, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> UnmanagedMemory.<CDoublePointer>calloc(size * SizeOf.get(CDoublePointer.class)));
    }

    @CEntryPoint(name = "freeDoublePointer")
    public static void freeDoublePointer(IsolateThread thread, CDoublePointer cDoublePointer, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> UnmanagedMemory.free(cDoublePointer));
    }
}
