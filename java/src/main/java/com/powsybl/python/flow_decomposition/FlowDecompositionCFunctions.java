/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.flow_decomposition;

import com.powsybl.flow_decomposition.FlowDecompositionComputer;
import com.powsybl.flow_decomposition.FlowDecompositionParameters;
import com.powsybl.flow_decomposition.FlowDecompositionResults;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowProvider;
import com.powsybl.python.commons.*;
import com.powsybl.python.loadflow.LoadFlowCUtils;
import com.powsybl.python.network.Dataframes;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.powsybl.python.commons.Util.doCatch;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
@CContext(Directives.class)
public final class FlowDecompositionCFunctions {

    public static final boolean DC = true;

    private FlowDecompositionCFunctions() {
    }

    @CEntryPoint(name = "runFlowDecomposition")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> runFlowDecomposition(IsolateThread thread,
                                                                                                         ObjectHandle networkHandle,
                                                                                                         PyPowsyblApiHeader.FlowDecompositionParametersPointer flowDecompositionParametersPtr,
                                                                                                         PyPowsyblApiHeader.LoadFlowParametersPointer loadFlowParametersPtr,
                                                                                                         PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);

            String providerStr = PyPowsyblConfiguration.getDefaultLoadFlowProvider();
            LoadFlowProvider loadFlowProvider = LoadFlowCUtils.getLoadFlowProvider(providerStr);
            logger().info("loadflow provider used is : {}", loadFlowProvider.getName());
            LoadFlowParameters loadFlowParameters = LoadFlowCUtils.createLoadFlowParameters(DC, loadFlowParametersPtr, loadFlowProvider);

            FlowDecompositionParameters flowDecompositionParameters = FlowDecompositionCUtils.createFlowDecompositionParameters(flowDecompositionParametersPtr);
            FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters, loadFlowParameters, providerStr, providerStr);
            FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);

            return Dataframes.createCDataframe(Dataframes.flowDecompositionMapper(flowDecompositionResults.getZoneSet()), flowDecompositionResults);
        });
    }

    @CEntryPoint(name = "createFlowDecompositionParameters")
    public static PyPowsyblApiHeader.FlowDecompositionParametersPointer createFlowDecompositionParameters(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> convertToFlowDecompositionParametersPointer(FlowDecompositionCUtils.createFlowDecompositionParameters()));
    }

    @CEntryPoint(name = "freeFlowDecompositionParameters")
    public static void freeFlowDecompositionParameters(IsolateThread thread, PyPowsyblApiHeader.FlowDecompositionParametersPointer flowDecompositionParametersPtr,
                                              PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            UnmanagedMemory.free(flowDecompositionParametersPtr);
        });
    }

    private static PyPowsyblApiHeader.FlowDecompositionParametersPointer convertToFlowDecompositionParametersPointer(FlowDecompositionParameters parameters) {
        PyPowsyblApiHeader.FlowDecompositionParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.FlowDecompositionParametersPointer.class));
        paramsPtr.setEnableLossesCompensation(parameters.isLossesCompensationEnabled());
        paramsPtr.setLossesCompensationEpsilon(parameters.getLossesCompensationEpsilon());
        paramsPtr.setSensitivityEpsilon(parameters.getSensitivityEpsilon());
        paramsPtr.setRescaleEnabled(parameters.isRescaleEnabled());
        paramsPtr.setXnecSelectionStrategy(parameters.getXnecSelectionStrategy().ordinal());
        paramsPtr.setDcFallbackEnabledAfterAcDivergence(parameters.isDcFallbackEnabledAfterAcDivergence());
        return paramsPtr;
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(CommonCFunctions.class);
    }
}
