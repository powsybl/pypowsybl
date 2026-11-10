/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.fast;

import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.ExceptionHandlerPointer;
import com.powsybl.python.commons.Util.PointerProvider;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CIntPointer;

import java.util.function.IntSupplier;

import static com.powsybl.python.commons.Util.createCharPtrArray;
import static com.powsybl.python.commons.Util.doCatch;

/**
 * C entry points of the positional fast path, see {@link FastNetwork}.
 * Element types and columns are plain ints; their names are published by {@code getFastTypeNames} and
 * {@code getFastColumnDescriptions} so that the Python side has no table of its own to keep in sync.
 */
@CContext(Directives.class)
public final class FastCFunctions {

    private FastCFunctions() {
    }

    private static FastNetwork fast(ObjectHandle handle) {
        return ObjectHandles.getGlobal().get(handle);
    }

    @CEntryPoint(name = "createFastNetwork")
    public static ObjectHandle create(IsolateThread thread, ObjectHandle networkHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ObjectHandle>() {
            @Override
            public ObjectHandle get() {
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                return ObjectHandles.getGlobal().create(new FastNetwork(network));
            }
        });
    }

    @CEntryPoint(name = "refreshFastNetwork")
    public static void refresh(IsolateThread thread, ObjectHandle handle, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                fast(handle).refresh();
            }
        });
    }

    @CEntryPoint(name = "getFastTypeNames")
    public static ArrayPointer<CCharPointerPointer> getTypeNames(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ArrayPointer<CCharPointerPointer>>() {
            @Override
            public ArrayPointer<CCharPointerPointer> get() {
                return createCharPtrArray(FastNetwork.getTypeNames());
            }
        });
    }

    @CEntryPoint(name = "getFastColumnDescriptions")
    public static ArrayPointer<CCharPointerPointer> getColumnDescriptions(IsolateThread thread, ObjectHandle handle, int type,
                                                                          ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ArrayPointer<CCharPointerPointer>>() {
            @Override
            public ArrayPointer<CCharPointerPointer> get() {
                return createCharPtrArray(fast(handle).getColumnDescriptions(type));
            }
        });
    }

    @CEntryPoint(name = "getFastIds")
    public static ArrayPointer<CCharPointerPointer> getIds(IsolateThread thread, ObjectHandle handle, int type,
                                                           ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ArrayPointer<CCharPointerPointer>>() {
            @Override
            public ArrayPointer<CCharPointerPointer> get() {
                return createCharPtrArray(fast(handle).getIds(type));
            }
        });
    }

    @CEntryPoint(name = "getFastCount")
    public static int getCount(IsolateThread thread, ObjectHandle handle, int type, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new IntSupplier() {
            @Override
            public int getAsInt() {
                return fast(handle).getCount(type);
            }
        });
    }

    @CEntryPoint(name = "getFastDoubles")
    public static int getDoubles(IsolateThread thread, ObjectHandle handle, int type, int attribute, CDoublePointer out, int length,
                                 ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new IntSupplier() {
            @Override
            public int getAsInt() {
                return fast(handle).getDoubles(type, attribute, out, length);
            }
        });
    }

    @CEntryPoint(name = "getFastInts")
    public static int getInts(IsolateThread thread, ObjectHandle handle, int type, int attribute, CIntPointer out, int length,
                              ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new IntSupplier() {
            @Override
            public int getAsInt() {
                return fast(handle).getInts(type, attribute, out, length);
            }
        });
    }

    @CEntryPoint(name = "setFastDoubles")
    public static void setDoubles(IsolateThread thread, ObjectHandle handle, int type, int attribute, CIntPointer positions,
                                  CDoublePointer values, int count, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                fast(handle).setDoubles(type, attribute, positions, values, count);
            }
        });
    }

    @CEntryPoint(name = "setFastInts")
    public static void setInts(IsolateThread thread, ObjectHandle handle, int type, int attribute, CIntPointer positions,
                               CIntPointer values, int count, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                fast(handle).setInts(type, attribute, positions, values, count);
            }
        });
    }

    @CEntryPoint(name = "getFastTerminalCount")
    public static int getTerminalCount(IsolateThread thread, ObjectHandle handle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new IntSupplier() {
            @Override
            public int getAsInt() {
                return fast(handle).getTerminalCount();
            }
        });
    }

    @CEntryPoint(name = "applyFastSwitchesAndGetTopology")
    public static int applySwitchesAndGetTopology(IsolateThread thread, ObjectHandle handle,
                                                  CIntPointer switchPositions, CIntPointer switchOpen, int switchCount,
                                                  CIntPointer terminalBus, int terminalCount,
                                                  CIntPointer busComponent, int busCapacity,
                                                  ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new IntSupplier() {
            @Override
            public int getAsInt() {
                return fast(handle).applySwitchesAndGetTopology(switchPositions, switchOpen, switchCount,
                        terminalBus, terminalCount, busComponent, busCapacity);
            }
        });
    }

    @CEntryPoint(name = "getFastInternalConnections")
    public static int getInternalConnections(IsolateThread thread, ObjectHandle handle, CIntPointer out, int capacity,
                                             ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new IntSupplier() {
            @Override
            public int getAsInt() {
                return fast(handle).getInternalConnections(out, capacity);
            }
        });
    }
}
