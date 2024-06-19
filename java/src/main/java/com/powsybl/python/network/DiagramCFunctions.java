/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;

import static com.powsybl.python.commons.Util.doCatch;

/**
 * C functions related to diagrams.
 *
 * @author Florian Dupuy {@literal <florian.dupuy at rte-france.com>}
 */
@CContext(Directives.class)
public final class DiagramCFunctions {

    public static CFunctionPointer voltageLevelDetailsProvider;

    private DiagramCFunctions() {
    }

    public interface VoltageLevelDetailsProvider extends CFunctionPointer {
        @InvokeCFunctionPointer
        String get(String voltageLevelId);
    }

    @CEntryPoint(name = "setupVoltageLevelDetailsProvider")
    public static void setupVoltageLevelDetailsProvider(IsolateThread thread, VoltageLevelDetailsProvider fpointer, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            voltageLevelDetailsProvider = fpointer;
        });
    }
}
