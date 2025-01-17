/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.flow_decomposition;

import com.powsybl.flow_decomposition.FlowDecompositionComputer;
import com.powsybl.flow_decomposition.FlowDecompositionParameters;
import com.powsybl.flow_decomposition.FlowDecompositionResults;
import com.powsybl.flow_decomposition.XnecProvider;
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
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

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

    @CEntryPoint(name = "addContingencyForFlowDecomposition")
    public static void addContingency(IsolateThread thread, ObjectHandle flowDecompositionContextHandle,
                                      CCharPointer contingencyIdPtr,
                                      CCharPointerPointer elementIdPtrPtr, int elementCount,
                                      PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            FlowDecompositionContext flowDecompositionContext = ObjectHandles.getGlobal().get(flowDecompositionContextHandle);
            Set<String> elementsIds = new HashSet<>(toStringList(elementIdPtrPtr, elementCount));
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            flowDecompositionContext.getXnecProviderByIdsBuilder().addContingency(contingencyId, elementsIds);
        });
    }

    @CEntryPoint(name = "addPrecontingencyMonitoredElementsForFlowDecomposition")
    public static void addPrecontingencyMonitoredElements(IsolateThread thread, ObjectHandle flowDecompositionContextHandle,
                                                          CCharPointerPointer elementIdPtrPtr, int elementCount,
                                                          PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            FlowDecompositionContext flowDecompositionContext = ObjectHandles.getGlobal().get(flowDecompositionContextHandle);
            Set<String> elementsIds = new HashSet<>(toStringList(elementIdPtrPtr, elementCount));
            flowDecompositionContext.getXnecProviderByIdsBuilder().addNetworkElementsOnBasecase(elementsIds);
        });
    }

    @CEntryPoint(name = "addPostcontingencyMonitoredElementsForFlowDecomposition")
    public static void addPostcontingencyMonitoredElements(IsolateThread thread, ObjectHandle flowDecompositionContextHandle,
                                                           CCharPointerPointer elementIdPtrPtr, int elementCount,
                                                           CCharPointerPointer contingenciesIdPtrPtr, int contingenciesCount,
                                                           PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            FlowDecompositionContext flowDecompositionContext = ObjectHandles.getGlobal().get(flowDecompositionContextHandle);
            Set<String> elementsIds = new HashSet<>(toStringList(elementIdPtrPtr, elementCount));
            Set<String> contingenciesIds = new HashSet<>(toStringList(contingenciesIdPtrPtr, contingenciesCount));
            flowDecompositionContext.getXnecProviderByIdsBuilder().addNetworkElementsAfterContingencies(elementsIds, contingenciesIds);
        });
    }

    @CEntryPoint(name = "addAdditionalXnecProviderForFlowDecomposition")
    public static void addAdditionalXnecProvider(IsolateThread thread, ObjectHandle flowDecompositionContextHandle,
                                                 int additionalXnecProviderId,
                                                 PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        FlowDecompositionContext flowDecompositionContext = ObjectHandles.getGlobal().get(flowDecompositionContextHandle);
        flowDecompositionContext.addAdditionalXnecProviderList(FlowDecompositionContext.DefaultXnecProvider.values()[additionalXnecProviderId]);
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
            LoadFlowParameters loadFlowParameters = LoadFlowCUtils.createLoadFlowParameters(DC, loadFlowParametersPtr, loadFlowProvider);
            FlowDecompositionParameters flowDecompositionParameters = FlowDecompositionCUtils.createFlowDecompositionParameters(flowDecompositionParametersPtr);

            logger().debug("Loadflow provider used is : {}", loadFlowProvider.getName());
            logger().debug("Sensitivity analysis provider used is : {}", sensiProviderName);
            logger().debug("Load flow parameters : {}", loadFlowParameters);
            logger().debug("Flow decomposition parameters : {}", flowDecompositionParameters);

            FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters, loadFlowParameters, lfProviderName, sensiProviderName);
            XnecProvider xnecProvider = flowDecompositionContext.getXnecProvider();
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
        paramsPtr.setRescaleMode(parameters.getRescaleMode().ordinal());
        paramsPtr.setDcFallbackEnabledAfterAcDivergence(parameters.isDcFallbackEnabledAfterAcDivergence());
        paramsPtr.setSensitivityVariableBatchSize(parameters.getSensitivityVariableBatchSize());
        return paramsPtr;
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(CommonCFunctions.class);
    }
}
