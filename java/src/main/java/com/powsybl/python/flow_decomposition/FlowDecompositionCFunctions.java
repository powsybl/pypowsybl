/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.flow_decomposition;

import com.powsybl.flow_decomposition.FlowDecompositionComputer;
import com.powsybl.flow_decomposition.FlowDecompositionResults;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.network.Dataframes;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;

import static com.powsybl.python.commons.Util.doCatch;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
@CContext(Directives.class)
public final class FlowDecompositionCFunctions {
    private FlowDecompositionCFunctions() {
    }

    @CEntryPoint(name = "runFlowDecomposition")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> runFlowDecomposition(IsolateThread thread,
                                                                                                         ObjectHandle networkHandle,
                                                                                                         PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);

            FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer();
            FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);
            return Dataframes.createCDataframe(Dataframes.flowDecompositionMapper(flowDecompositionResults.getZoneSet()), flowDecompositionResults);
        });
    }
}
