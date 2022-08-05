/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.security;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.reporter.ReporterModel;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.python.commons.*;
import com.powsybl.python.commons.PyPowsyblApiHeader.SecurityAnalysisParametersPointer;
import com.powsybl.python.contingency.ContingencyContainer;
import com.powsybl.python.loadflow.LoadFlowCFunctions;
import com.powsybl.python.loadflow.LoadFlowCUtils;
import com.powsybl.python.network.Dataframes;
import com.powsybl.security.*;
import com.powsybl.security.monitor.StateMonitor;
import com.powsybl.security.results.PostContingencyResult;
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
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.python.commons.CTypeUtil.toStringList;
import static com.powsybl.python.commons.PyPowsyblApiHeader.allocArrayPointer;
import static com.powsybl.python.commons.PyPowsyblApiHeader.freeArrayPointer;
import static com.powsybl.python.commons.Util.*;

/**
 * C functions related to security analysis.
 *
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
@CContext(Directives.class)
public final class SecurityAnalysisCFunctions {

    private SecurityAnalysisCFunctions() {
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(SecurityAnalysisCFunctions.class);
    }

    @CEntryPoint(name = "getSecurityAnalysisProviderNames")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointerPointer> getSecurityAnalysisProviderNames(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(new ServiceLoaderCache<>(SecurityAnalysisProvider.class).getServices()
                .stream().map(SecurityAnalysisProvider::getName).collect(Collectors.toList())));
    }

    @CEntryPoint(name = "setDefaultSecurityAnalysisProvider")
    public static void setDefaultSecurityAnalysisProvider(IsolateThread thread, CCharPointer provider, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            PyPowsyblConfiguration.setDefaultSecurityAnalysisProvider(CTypeUtil.toString(provider));
        });
    }

    @CEntryPoint(name = "getDefaultSecurityAnalysisProvider")
    public static CCharPointer getDefaultSecurityAnalysisProvider(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> CTypeUtil.toCharPtr(PyPowsyblConfiguration.getDefaultSecurityAnalysisProvider()));
    }

    @CEntryPoint(name = "addMonitoredElements")
    public static void addMonitoredElements(IsolateThread thread, ObjectHandle securityAnalysisContextHandle, PyPowsyblApiHeader.RawContingencyContextType contingencyContextType,
                                            CCharPointerPointer branchIds, int branchIdsCount,
                                            CCharPointerPointer voltageLevelIds, int voltageLevelIdCount,
                                            CCharPointerPointer threeWindingsTransformerIds, int threeWindingsTransformerIdsCount,
                                            CCharPointerPointer contingencyIds, int contingencyIdsCount,
                                            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(securityAnalysisContextHandle);
            List<String> contingencies = toStringList(contingencyIds, contingencyIdsCount);
            contingencies.forEach(contingency -> {
                if (contingency.equals("")) {
                    contingency = null;
                }
                analysisContext.addMonitor(new StateMonitor(new ContingencyContext(contingency, convert(contingencyContextType)),
                        Set.copyOf(toStringList(branchIds, branchIdsCount)), Set.copyOf(toStringList(voltageLevelIds, voltageLevelIdCount)),
                        Set.copyOf(toStringList(threeWindingsTransformerIds, threeWindingsTransformerIdsCount))));
            });
        });
    }

    @CEntryPoint(name = "getBranchResults")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getBranchResults(IsolateThread thread, ObjectHandle securityAnalysisResult, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisResult result = ObjectHandles.getGlobal().get(securityAnalysisResult);
            return Dataframes.createCDataframe(Dataframes.branchResultsMapper(), result);
        });
    }

    @CEntryPoint(name = "getBusResults")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getBusResults(IsolateThread thread, ObjectHandle securityAnalysisResult, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisResult result = ObjectHandles.getGlobal().get(securityAnalysisResult);
            return Dataframes.createCDataframe(Dataframes.busResultsMapper(), result);
        });
    }

    @CEntryPoint(name = "getThreeWindingsTransformerResults")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getThreeWindingsTransformerResults(IsolateThread thread, ObjectHandle securityAnalysisResult, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisResult result = ObjectHandles.getGlobal().get(securityAnalysisResult);
            return Dataframes.createCDataframe(Dataframes.threeWindingsTransformerResultsMapper(), result);
        });
    }

    @CEntryPoint(name = "createSecurityAnalysis")
    public static ObjectHandle createSecurityAnalysis(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new SecurityAnalysisContext()));
    }

    @CEntryPoint(name = "addContingency")
    public static void addContingency(IsolateThread thread, ObjectHandle contingencyContainerHandle, CCharPointer contingencyIdPtr,
                                      CCharPointerPointer elementIdPtrPtr, int elementCount, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            ContingencyContainer contingencyContainer = ObjectHandles.getGlobal().get(contingencyContainerHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            List<String> elementIds = toStringList(elementIdPtrPtr, elementCount);
            contingencyContainer.addContingency(contingencyId, elementIds);
        });
    }

    private static LoadFlowResult.ComponentResult.Status getStatus(LimitViolationsResult result) {
        return result.isComputationOk() ? LoadFlowResult.ComponentResult.Status.CONVERGED : LoadFlowResult.ComponentResult.Status.FAILED;
    }

    private static void setSecurityAnalysisResultPointer(PyPowsyblApiHeader.ContingencyResultPointer contingencyPtr, String contingencyId, LimitViolationsResult limitViolationsResult) {
        contingencyPtr.setContingencyId(CTypeUtil.toCharPtr(contingencyId));
        contingencyPtr.setStatus(getStatus(limitViolationsResult).ordinal());
        List<LimitViolation> limitViolations = limitViolationsResult.getLimitViolations();
        PyPowsyblApiHeader.LimitViolationPointer limitViolationPtr = UnmanagedMemory.calloc(limitViolations.size() * SizeOf.get(PyPowsyblApiHeader.LimitViolationPointer.class));
        for (int i = 0; i < limitViolations.size(); i++) {
            LimitViolation limitViolation = limitViolations.get(i);
            PyPowsyblApiHeader.LimitViolationPointer limitViolationPtrPlus = limitViolationPtr.addressOf(i);
            limitViolationPtrPlus.setSubjectId(CTypeUtil.toCharPtr(limitViolation.getSubjectId()));
            limitViolationPtrPlus.setSubjectName(CTypeUtil.toCharPtr(Objects.toString(limitViolation.getSubjectName(), "")));
            limitViolationPtrPlus.setLimitType(limitViolation.getLimitType().ordinal());
            limitViolationPtrPlus.setLimit(limitViolation.getLimit());
            limitViolationPtrPlus.setLimitName(CTypeUtil.toCharPtr(Objects.toString(limitViolation.getLimitName(), "")));
            limitViolationPtrPlus.setAcceptableDuration(limitViolation.getAcceptableDuration());
            limitViolationPtrPlus.setLimitReduction(limitViolation.getLimitReduction());
            limitViolationPtrPlus.setValue(limitViolation.getValue());
            limitViolationPtrPlus.setSide(limitViolation.getSide() != null ? limitViolation.getSide().ordinal() : -1);
        }
        contingencyPtr.limitViolations().setLength(limitViolations.size());
        contingencyPtr.limitViolations().setPtr(limitViolationPtr);
    }

    private static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.ContingencyResultPointer> createContingencyResultArrayPointer(SecurityAnalysisResult result) {
        int resultCount = result.getPostContingencyResults().size() + 1; // + 1 for pre-contingency result
        PyPowsyblApiHeader.ContingencyResultPointer contingencyPtr = UnmanagedMemory.calloc(resultCount * SizeOf.get(PyPowsyblApiHeader.ContingencyResultPointer.class));
        setSecurityAnalysisResultPointer(contingencyPtr, "", result.getPreContingencyResult().getLimitViolationsResult());
        for (int i = 0; i < result.getPostContingencyResults().size(); i++) {
            PostContingencyResult postContingencyResult = result.getPostContingencyResults().get(i);
            PyPowsyblApiHeader.ContingencyResultPointer contingencyPtrPlus = contingencyPtr.addressOf(i + 1);
            setSecurityAnalysisResultPointer(contingencyPtrPlus, postContingencyResult.getContingency().getId(), postContingencyResult.getLimitViolationsResult());
        }
        return allocArrayPointer(contingencyPtr, resultCount);
    }

    private static SecurityAnalysisProvider getProvider(String name) {
        String actualName = name.isEmpty() ? PyPowsyblConfiguration.getDefaultSecurityAnalysisProvider() : name;
        return ServiceLoader.load(SecurityAnalysisProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(provider -> provider.getName().equals(actualName))
                .findFirst()
                .orElseThrow(() -> new PowsyblException("No security analysis provider for name '" + actualName + "'"));
    }

    @CEntryPoint(name = "runSecurityAnalysis")
    public static ObjectHandle runSecurityAnalysis(IsolateThread thread, ObjectHandle securityAnalysisContextHandle,
                                                   ObjectHandle networkHandle, SecurityAnalysisParametersPointer securityAnalysisParametersPointer,
                                                   CCharPointer providerName, boolean dc,  ObjectHandle reporterHandle,
                                                   PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(securityAnalysisContextHandle);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            SecurityAnalysisProvider provider = getProvider(CTypeUtil.toString(providerName));
            logger().info("Security analysis provider used for security analysis is : {}", provider.getName());
            SecurityAnalysisParameters securityAnalysisParameters = SecurityAnalysisCUtils.createSecurityAnalysisParameters(dc, securityAnalysisParametersPointer, provider);
            ReporterModel reporter = ObjectHandles.getGlobal().get(reporterHandle);
            SecurityAnalysisResult result = analysisContext.run(network, securityAnalysisParameters, provider.getName(), reporter);
            return ObjectHandles.getGlobal().create(result);
        });
    }

    @CEntryPoint(name = "getSecurityAnalysisResult")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.ContingencyResultPointer> getSecurityAnalysisResult(IsolateThread thread, ObjectHandle securityAnalysisResultHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisResult result = ObjectHandles.getGlobal().get(securityAnalysisResultHandle);
            return createContingencyResultArrayPointer(result);
        });
    }

    @CEntryPoint(name = "getLimitViolations")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getLimitViolations(IsolateThread thread, ObjectHandle securityAnalysisResultHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisResult result = ObjectHandles.getGlobal().get(securityAnalysisResultHandle);
            return Dataframes.createCDataframe(Dataframes.limitViolationsMapper(), result);
        });
    }

    @CEntryPoint(name = "freeContingencyResultArrayPointer")
    public static void freeContingencyResultArrayPointer(IsolateThread thread, PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.ContingencyResultPointer> contingencyResultArrayPtr,
                                                         PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            for (int i = 0; i < contingencyResultArrayPtr.getLength(); i++) {
                PyPowsyblApiHeader.ContingencyResultPointer contingencyResultPtrPlus = contingencyResultArrayPtr.getPtr().addressOf(i);
                UnmanagedMemory.free(contingencyResultPtrPlus.getContingencyId());
                for (int l = 0; l < contingencyResultPtrPlus.limitViolations().getLength(); l++) {
                    PyPowsyblApiHeader.LimitViolationPointer violation = contingencyResultPtrPlus.limitViolations().getPtr().addressOf(l);
                    UnmanagedMemory.free(violation.getSubjectId());
                    UnmanagedMemory.free(violation.getSubjectName());
                    UnmanagedMemory.free(violation.getLimitName());
                }
                UnmanagedMemory.free(contingencyResultPtrPlus.limitViolations().getPtr());
            }
            freeArrayPointer(contingencyResultArrayPtr);
        });
    }

    @CEntryPoint(name = "freeSecurityAnalysisParameters")
    public static void freeSecurityAnalysisParameters(IsolateThread thread, SecurityAnalysisParametersPointer parameters,
                                              PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            LoadFlowCUtils.freeLoadFlowParametersContent(parameters.getLoadFlowParameters());
            UnmanagedMemory.free(parameters);
        });
    }

    @CEntryPoint(name = "createSecurityAnalysisParameters")
    public static SecurityAnalysisParametersPointer createSecurityAnalysisParameters(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> convertToSecurityAnalysisParametersPointer(SecurityAnalysisCUtils.createSecurityAnalysisParameters()));
    }

    private static SecurityAnalysisParametersPointer convertToSecurityAnalysisParametersPointer(SecurityAnalysisParameters parameters) {
        SecurityAnalysisParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(SecurityAnalysisParametersPointer.class));
        LoadFlowCFunctions.copyToCLoadFlowParameters(parameters.getLoadFlowParameters(), paramsPtr.getLoadFlowParameters());
        paramsPtr.setFlowProportionalThreshold(parameters.getIncreasedViolationsParameters().getFlowProportionalThreshold());
        paramsPtr.setHighVoltageAbsoluteThreshold(parameters.getIncreasedViolationsParameters().getHighVoltageAbsoluteThreshold());
        paramsPtr.setHighVoltageProportionalThreshold(parameters.getIncreasedViolationsParameters().getHighVoltageProportionalThreshold());
        paramsPtr.setLowVoltageAbsoluteThreshold(parameters.getIncreasedViolationsParameters().getLowVoltageAbsoluteThreshold());
        paramsPtr.setLowVoltageProportionalThreshold(parameters.getIncreasedViolationsParameters().getLowVoltageProportionalThreshold());
        paramsPtr.setProviderParametersValuesCount(0);
        paramsPtr.setProviderParametersKeysCount(0);
        return paramsPtr;
    }

    @CEntryPoint(name = "getSecurityAnalysisProviderParametersNames")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointerPointer> getProviderParametersNames(IsolateThread thread, CCharPointer provider, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String providerStr = CTypeUtil.toString(provider);
            return Util.createCharPtrArray(SecurityAnalysisCUtils.getSecurityAnalysisProvider(providerStr).getSpecificParametersNames());
        });
    }
}
