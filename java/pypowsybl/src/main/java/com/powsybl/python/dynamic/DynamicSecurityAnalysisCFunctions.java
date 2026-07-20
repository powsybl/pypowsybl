/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.dynamicsimulation.EventModelsSupplier;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblApiHeader.DynamicSecurityAnalysisParametersPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.ExceptionHandlerPointer;
import com.powsybl.python.commons.PyPowsyblConfiguration;
import com.powsybl.python.commons.Util.PointerProvider;
import com.powsybl.python.report.ReportCUtils;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters;
import com.powsybl.security.monitor.StateMonitor;
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

import java.util.List;
import java.util.Set;

import static com.powsybl.python.commons.CTypeUtil.toStringList;
import static com.powsybl.python.commons.Util.convert;
import static com.powsybl.python.commons.Util.doCatch;
import static com.powsybl.python.commons.Util.freeProviderParameters;
import static com.powsybl.python.dynamic.DynamicSecurityAnalysisParametersCUtils.copyToCDynamicSecurityAnalysisParameters;

/**
 * C functions related to dynamic security analysis.
 *
 * <p>Contingencies are added through the generic contingency container entry points
 * ({@code addContingency}, {@code addContingencyFromJsonFile}) and the resulting
 * {@link SecurityAnalysisResult} is read back through the generic security analysis
 * result entry points ({@code getPostContingencyResults}, {@code getLimitViolations}, ...).
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@SuppressWarnings({"java:S1602", "java:S1604", "Convert2Lambda"})
@CContext(Directives.class)
public final class DynamicSecurityAnalysisCFunctions {

    private DynamicSecurityAnalysisCFunctions() {
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(DynamicSecurityAnalysisCFunctions.class);
    }

    @CEntryPoint(name = "createDynamicSecurityAnalysis")
    public static ObjectHandle createDynamicSecurityAnalysis(IsolateThread thread,
                                                             ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new DynamicSecurityAnalysisContext()));
    }

    @CEntryPoint(name = "createDynamicSecurityAnalysisParameters")
    public static DynamicSecurityAnalysisParametersPointer createDynamicSecurityAnalysisParameters(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<DynamicSecurityAnalysisParametersPointer>() {
            @Override
            public DynamicSecurityAnalysisParametersPointer get() {
                DynamicSecurityAnalysisParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(DynamicSecurityAnalysisParametersPointer.class));
                copyToCDynamicSecurityAnalysisParameters(paramsPtr);
                return paramsPtr;
            }
        });
    }

    @CEntryPoint(name = "freeDynamicSecurityAnalysisParameters")
    public static void freeDynamicSecurityAnalysisParameters(IsolateThread thread, DynamicSecurityAnalysisParametersPointer parametersPtr,
                                                             ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                // debug_dir is a char* scalar: it is freed on the C++ side when read (toString),
                // like other char* scalar fields, so it must not be freed here.
                freeProviderParameters(parametersPtr.getProviderParameters());
                UnmanagedMemory.free(parametersPtr);
            }
        });
    }

    @CEntryPoint(name = "addDynamicMonitoredElements")
    public static void addDynamicMonitoredElements(IsolateThread thread, ObjectHandle dynamicSecurityAnalysisContextHandle,
                                                   PyPowsyblApiHeader.RawContingencyContextType contingencyContextType,
                                                   CCharPointerPointer branchIds, int branchIdsCount,
                                                   CCharPointerPointer voltageLevelIds, int voltageLevelIdCount,
                                                   CCharPointerPointer threeWindingsTransformerIds, int threeWindingsTransformerIdsCount,
                                                   CCharPointerPointer contingencyIds, int contingencyIdsCount,
                                                   ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                DynamicSecurityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(dynamicSecurityAnalysisContextHandle);
                List<String> contingencies = toStringList(contingencyIds, contingencyIdsCount);
                Set<String> branchIdsJava = Set.copyOf(toStringList(branchIds, branchIdsCount));
                Set<String> voltageLevelIdsJava = Set.copyOf(toStringList(voltageLevelIds, voltageLevelIdCount));
                Set<String> threeWindingsTransformerIdsJava = Set.copyOf(toStringList(threeWindingsTransformerIds, threeWindingsTransformerIdsCount));
                contingencies.forEach(contingency ->
                        analysisContext.addMonitor(new StateMonitor(new ContingencyContext(contingency.isEmpty() ? null : contingency, convert(contingencyContextType)),
                                branchIdsJava, voltageLevelIdsJava, threeWindingsTransformerIdsJava)));
            }
        });
    }

    @CEntryPoint(name = "runDynamicSecurityAnalysis")
    public static ObjectHandle runDynamicSecurityAnalysis(IsolateThread thread,
                                                          ObjectHandle dynamicSecurityAnalysisContextHandle,
                                                          ObjectHandle networkHandle,
                                                          ObjectHandle dynamicMappingHandle,
                                                          ObjectHandle eventModelsSupplierHandle,
                                                          DynamicSecurityAnalysisParametersPointer parametersPtr,
                                                          CCharPointer providerName,
                                                          ObjectHandle reportNodeHandle,
                                                          ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ObjectHandle>() {
            @Override
            public ObjectHandle get() {
                DynamicSecurityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(dynamicSecurityAnalysisContextHandle);
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                PythonDynamicModelsSupplier dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
                EventModelsSupplier eventModelsSupplier = ObjectHandles.getGlobal().get(eventModelsSupplierHandle);
                if (eventModelsSupplier == null) {
                    eventModelsSupplier = EventModelsSupplier.empty();
                }
                String provider = CTypeUtil.toString(providerName);
                if (provider.isEmpty()) {
                    provider = PyPowsyblConfiguration.getDefaultDynamicSecurityAnalysisProvider();
                }
                ReportNode reportNode = ReportCUtils.getReportNode(reportNodeHandle);
                DynamicSecurityAnalysisParameters dynamicSecurityAnalysisParameters =
                        DynamicSecurityAnalysisParametersCUtils.createDynamicSecurityAnalysisParameters(parametersPtr);
                SecurityAnalysisResult result = analysisContext.run(network,
                        dynamicMapping,
                        eventModelsSupplier,
                        dynamicSecurityAnalysisParameters,
                        provider,
                        reportNode);
                logger().info("Dynamic security analysis ran successfully in java");
                return ObjectHandles.getGlobal().create(result);
            }
        });
    }
}
