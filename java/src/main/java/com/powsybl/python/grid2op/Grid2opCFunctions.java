/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.grid2op;

import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.ExceptionHandlerPointer;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumLookup;
import org.graalvm.nativeimage.c.constant.CEnumValue;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CIntPointer;

import static com.powsybl.python.commons.Util.doCatch;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
@CContext(Directives.class)
public final class Grid2opCFunctions {

    private Grid2opCFunctions() {
    }

    @CEnum("Grid2opStringValueType")
    public enum Grid2opStringValueType {
        VOLTAGE_LEVEL_NAME,
        LOAD_NAME,
        GENERATOR_NAME,
        SHUNT_NAME,
        BRANCH_NAME;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native Grid2opStringValueType fromCValue(int value);
    }

    @CEnum("Grid2opIntegerValueType")
    public enum Grid2opIntegerValueType {
        LOAD_VOLTAGE_LEVEL_NUM,
        GENERATOR_VOLTAGE_LEVEL_NUM,
        SHUNT_VOLTAGE_LEVEL_NUM,
        BRANCH_VOLTAGE_LEVEL_NUM_1,
        BRANCH_VOLTAGE_LEVEL_NUM_2,
        TOPO_VECT;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native Grid2opIntegerValueType fromCValue(int value);
    }

    @CEnum("Grid2opDoubleValueType")
    public enum Grid2opDoubleValueType {
        LOAD_P,
        LOAD_Q,
        LOAD_V,
        GENERATOR_P,
        GENERATOR_Q,
        GENERATOR_V,
        SHUNT_P,
        SHUNT_Q,
        SHUNT_V,
        BRANCH_P1,
        BRANCH_P2,
        BRANCH_Q1,
        BRANCH_Q2,
        BRANCH_V1,
        BRANCH_V2,
        BRANCH_I1,
        BRANCH_I2;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native Grid2opDoubleValueType fromCValue(int value);
    }

    @CEnum("Grid2opUpdateDoubleValueType")
    public enum Grid2opUpdateDoubleValueType {
        UPDATE_LOAD_P,
        UPDATE_LOAD_Q,
        UPDATE_GENERATOR_P,
        UPDATE_GENERATOR_V;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native Grid2opUpdateDoubleValueType fromCValue(int value);
    }

    @CEnum("Grid2opUpdateIntegerValueType")
    public enum Grid2opUpdateIntegerValueType {
        UPDATE_LOAD_BUS,
        UPDATE_GENERATOR_BUS,
        UPDATE_BRANCH_BUS1,
        UPDATE_BRANCH_BUS2;

        @CEnumValue
        public native int getCValue();

        @CEnumLookup
        public static native Grid2opUpdateIntegerValueType fromCValue(int value);
    }

    @CEntryPoint(name = "createGrid2opBackend")
    public static ObjectHandle createBackend(IsolateThread thread, ObjectHandle networkHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            Backend backend = new Backend(network);
            return ObjectHandles.getGlobal().create(backend);
        });
    }

    @CEntryPoint(name = "freeGrid2opBackend")
    public static void freeBackend(IsolateThread thread, ObjectHandle backendHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Backend backend = ObjectHandles.getGlobal().get(backendHandle);
            backend.close();
            ObjectHandles.getGlobal().destroy(backendHandle);
        });
    }

    @CEntryPoint(name = "getGrid2opStringValue")
    public static ArrayPointer<CCharPointerPointer> getStringValue(IsolateThread thread, ObjectHandle backendHandle, Grid2opStringValueType valueType, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Backend backend = ObjectHandles.getGlobal().get(backendHandle);
            return backend.getStringValue(valueType);
        });
    }

    @CEntryPoint(name = "getGrid2opIntegerValue")
    public static ArrayPointer<CIntPointer> getIntegerValue(IsolateThread thread, ObjectHandle backendHandle, Grid2opIntegerValueType valueType, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Backend backend = ObjectHandles.getGlobal().get(backendHandle);
            return backend.getIntegerValue(valueType);
        });
    }

    @CEntryPoint(name = "getGrid2opDoubleValue")
    public static ArrayPointer<CDoublePointer> getDoubleValue(IsolateThread thread, ObjectHandle backendHandle, Grid2opDoubleValueType valueType, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Backend backend = ObjectHandles.getGlobal().get(backendHandle);
            return backend.getDoubleValue(valueType);
        });
    }

    @CEntryPoint(name = "updateGrid2opDoubleValue")
    public static void updateDoubleValue(IsolateThread thread, ObjectHandle backendHandle, Grid2opUpdateDoubleValueType valueType,
                                         CDoublePointer valuePtr, CIntPointer changedPtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Backend backend = ObjectHandles.getGlobal().get(backendHandle);
            backend.updateDoubleValue(valueType, valuePtr, changedPtr);
        });
    }

    @CEntryPoint(name = "updateGrid2opIntegerValue")
    public static void updateIntegerValue(IsolateThread thread, ObjectHandle backendHandle, Grid2opUpdateIntegerValueType valueType,
                                          CIntPointer valuePtr, CIntPointer changedPtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Backend backend = ObjectHandles.getGlobal().get(backendHandle);
            backend.updateIntegerValue(valueType, valuePtr, changedPtr);
        });
    }
}
