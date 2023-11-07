/**
 * Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.commons;

import com.powsybl.iidm.network.Network;
import com.powsybl.python.dataframe.CDataframeHandler;
import com.powsybl.tools.Version;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.word.PointerBase;

import static com.powsybl.python.commons.PyPowsyblApiHeader.*;
import static com.powsybl.python.commons.Util.doCatch;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
@CContext(Directives.class)
public final class CommonCFunctions {

    private CommonCFunctions() {
    }

    @CEntryPoint(name = "setJavaLibraryPath")
    public static void setJavaLibraryPath(IsolateThread thread, CCharPointer javaLibraryPath, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                System.setProperty("java.library.path", CTypeUtil.toString(javaLibraryPath));
            }
        });
    }

    @CEntryPoint(name = "setConfigRead")
    public static void setConfigRead(IsolateThread thread, boolean read, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                PyPowsyblConfiguration.setReadConfig(read);
            }
        });
    }

    @CEntryPoint(name = "isConfigRead")
    public static boolean isConfigRead(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, PyPowsyblConfiguration::isReadConfig);
    }

    @CEntryPoint(name = "getVersionTable")
    public static CCharPointer getVersionTable(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new Util.PointerProvider<CCharPointer>() {
            @Override
            public CCharPointer get() {
                return CTypeUtil.toCharPtr(Version.getTableString());
            }
        });
    }

    @CEntryPoint(name = "freeStringArray")
    public static void freeStringArray(IsolateThread thread, ArrayPointer<CCharPointerPointer> arrayPtr,
                                       ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                freeArrayContent(arrayPtr);
            }
        });
    }

    @CEntryPoint(name = "freeArray")
    public static <T extends PointerBase> void freeArray(IsolateThread thread, ArrayPointer<T> arrayPointer,
                                                         ExceptionHandlerPointer exceptionHandlerPtr) {
        UnmanagedMemory.free(arrayPointer.getPtr());
        UnmanagedMemory.free(arrayPointer);
    }

    @CEntryPoint(name = "freeSeriesArray")
    public static void freeSeriesArray(IsolateThread thread, ArrayPointer<SeriesPointer> seriesPtrArrayPtr,
                                       ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < seriesPtrArrayPtr.getLength(); i++) {
                    freeSeries(seriesPtrArrayPtr.getPtr().addressOf(i));
                }
                freeArrayPointer(seriesPtrArrayPtr);
            }
        });
    }

    private static void freeSeries(SeriesPointer seriesPointer) {
        if (seriesPointer.getType() == CDataframeHandler.STRING_SERIES_TYPE) {
            freeArrayContent(seriesPointer.data());
        }
        UnmanagedMemory.free(seriesPointer.data().getPtr());
        UnmanagedMemory.free(seriesPointer.getName());
    }

    /**
     * Frees C strings memory
     *
     * @param array
     */
    private static void freeArrayContent(ArrayPointer<CCharPointerPointer> array) {
        for (int i = 0; i < array.getLength(); i++) {
            UnmanagedMemory.free(array.getPtr().read(i));
        }
    }

    @CEntryPoint(name = "destroyObjectHandle")
    public static void destroyObjectHandle(IsolateThread thread, ObjectHandle objectHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                ObjectHandles.getGlobal().destroy(objectHandle);
            }
        });
    }

    @CEntryPoint(name = "getWorkingVariantId")
    public static CCharPointer getWorkingVariantId(IsolateThread thread, ObjectHandle networkHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new Util.PointerProvider<CCharPointer>() {
            @Override
            public CCharPointer get() {
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                return CTypeUtil.toCharPtr(network.getVariantManager().getWorkingVariantId());
            }
        });
    }

    @CEntryPoint(name = "freeString")
    public static void freeString(IsolateThread thread, CCharPointer string, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                UnmanagedMemory.free(string);
            }
        });
    }

    @CEntryPoint(name = "closePypowsybl")
    public static void closePypowsybl(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, CommonObjects::close);
    }

    @CEntryPoint(name = "freeStringMap")
    public static void freeStringMap(IsolateThread thread, StringMap map, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < map.getLength(); i++) {
                    UnmanagedMemory.free(map.getKeys().read(i));
                    UnmanagedMemory.free(map.getValues().read(i));
                }
                UnmanagedMemory.free(map);
            }
        });
    }
}
