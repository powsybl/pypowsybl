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
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;

import static com.powsybl.python.commons.Util.doCatch;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
@CContext(Directives.class)
public final class Grid2opCFunctions {

    private Grid2opCFunctions() {
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

    @CEntryPoint(name = "getGrid2opGeneratorName")
    public static ArrayPointer<CCharPointerPointer> getGeneratorName(IsolateThread thread, ObjectHandle backendHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Backend backend = ObjectHandles.getGlobal().get(backendHandle);
            return backend.getGeneratorName();
        });
    }

    @CEntryPoint(name = "getGrid2opGeneratorP")
    public static ArrayPointer<CDoublePointer> getGeneratorP(IsolateThread thread, ObjectHandle backendHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Backend backend = ObjectHandles.getGlobal().get(backendHandle);
            return backend.getGeneratorP();
        });
    }
}
