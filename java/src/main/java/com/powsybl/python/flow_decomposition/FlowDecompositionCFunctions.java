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
import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.flow_decomposition.XnecProviderByIds;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowProvider;
import com.powsybl.python.commons.CommonCFunctions;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblConfiguration;
import com.powsybl.python.loadflow.LoadFlowCUtils;
import com.powsybl.python.network.Dataframes;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.powsybl.python.commons.CTypeUtil.toStringList;
import static com.powsybl.python.commons.Util.doCatch;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
@CContext(Directives.class)
public final class FlowDecompositionCFunctions {

    public static final boolean DC = true;

    private FlowDecompositionCFunctions() {
    }

    @CEntryPoint(name = "createFlowDecomposition")
    public static ObjectHandle createFlowDecomposition(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new FlowDecompositionContext()));
    }

    @CEntryPoint(name = "addPrecontingencyMonitoredElementsForFlowDecomposition")
    public static void addPrecontingencyMonitoredElementsForFlowDecomposition(IsolateThread thread, ObjectHandle flowDecompositionContextHandle,
                                                                              CCharPointerPointer elementIdPtrPtr, int elementCount, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            FlowDecompositionContext flowDecompositionContext = ObjectHandles.getGlobal().get(flowDecompositionContextHandle);
            List<String> elementIds = toStringList(elementIdPtrPtr, elementCount);
            flowDecompositionContext.addPrecontingencyMonitoredElements(elementIds);
        });
    }

    @CEntryPoint(name = "runFlowDecomposition")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> runFlowDecomposition(IsolateThread thread,
                                                                                                         ObjectHandle flowDecompositionContextHandle,
                                                                                                         ObjectHandle networkHandle,
                                                                                                         PyPowsyblApiHeader.FlowDecompositionParametersPointer flowDecompositionParametersPtr,
                                                                                                         PyPowsyblApiHeader.LoadFlowParametersPointer loadFlowParametersPtr,
                                                                                                         PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            FlowDecompositionContext flowDecompositionContext = ObjectHandles.getGlobal().get(flowDecompositionContextHandle);
            Network network = ObjectHandles.getGlobal().get(networkHandle);

            String lfProviderName = PyPowsyblConfiguration.getDefaultLoadFlowProvider();
            LoadFlowProvider loadFlowProvider = LoadFlowCUtils.getLoadFlowProvider(lfProviderName);
            String sensiProviderName = PyPowsyblConfiguration.getDefaultSensitivityAnalysisProvider();
            logger().debug("Loadflow provider used is : {}", loadFlowProvider.getName());
            logger().debug("Sensitivity analysis provider used is : {}", sensiProviderName);
            LoadFlowParameters loadFlowParameters = LoadFlowCUtils.createLoadFlowParameters(DC, loadFlowParametersPtr, loadFlowProvider);

            FlowDecompositionParameters flowDecompositionParameters = FlowDecompositionCUtils.createFlowDecompositionParameters(flowDecompositionParametersPtr);
            FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters, loadFlowParameters, lfProviderName, sensiProviderName);
            List<String> precontingencyMonitoredElements = flowDecompositionContext.getPrecontingencyMonitoredElements();
            XnecProvider xnecProvider = new XnecProviderByIds(precontingencyMonitoredElements);
            FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(xnecProvider, network);

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
        doCatch(exceptionHandlerPtr, () -> UnmanagedMemory.free(flowDecompositionParametersPtr));
    }

    private static PyPowsyblApiHeader.FlowDecompositionParametersPointer convertToFlowDecompositionParametersPointer(FlowDecompositionParameters parameters) {
        PyPowsyblApiHeader.FlowDecompositionParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.FlowDecompositionParametersPointer.class));
        paramsPtr.setEnableLossesCompensation(parameters.isLossesCompensationEnabled());
        paramsPtr.setLossesCompensationEpsilon(parameters.getLossesCompensationEpsilon());
        paramsPtr.setSensitivityEpsilon(parameters.getSensitivityEpsilon());
        paramsPtr.setRescaleEnabled(parameters.isRescaleEnabled());
        paramsPtr.setDcFallbackEnabledAfterAcDivergence(parameters.isDcFallbackEnabledAfterAcDivergence());
        paramsPtr.setSensitivityVariableBatchSize(parameters.getSensitivityVariableBatchSize());
        return paramsPtr;
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(CommonCFunctions.class);
    }
}
