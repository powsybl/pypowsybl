/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.security;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.reporter.ReporterModel;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.*;
import com.powsybl.python.commons.PyPowsyblApiHeader.SecurityAnalysisParametersPointer;
import com.powsybl.python.contingency.ContingencyContainer;
import com.powsybl.python.loadflow.LoadFlowCFunctions;
import com.powsybl.python.loadflow.LoadFlowCUtils;
import com.powsybl.python.network.Dataframes;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.SecurityAnalysisParameters;
import com.powsybl.security.SecurityAnalysisProvider;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.action.*;
import com.powsybl.security.condition.TrueCondition;
import com.powsybl.security.monitor.StateMonitor;
import com.powsybl.security.results.OperatorStrategyResult;
import com.powsybl.security.results.PostContingencyResult;
import com.powsybl.security.results.PreContingencyResult;
import com.powsybl.security.strategy.OperatorStrategy;
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
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(SecurityAnalysisProvider.findAll()
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

    private static void setPostContingencyResultInSecurityAnalysisResultPointer(PyPowsyblApiHeader.PostContingencyResultPointer contingencyPtr, PostContingencyResult postContingencyResult) {
        contingencyPtr.setContingencyId(CTypeUtil.toCharPtr(postContingencyResult.getContingency().getId()));
        contingencyPtr.setStatus(postContingencyResult.getStatus().ordinal());
        List<LimitViolation> limitViolations = postContingencyResult.getLimitViolationsResult().getLimitViolations();
        PyPowsyblApiHeader.LimitViolationPointer limitViolationPtr = UnmanagedMemory.calloc(limitViolations.size() * SizeOf.get(PyPowsyblApiHeader.LimitViolationPointer.class));
        createLimitViolationPtr(limitViolationPtr, limitViolations);
        contingencyPtr.limitViolations().setLength(limitViolations.size());
        contingencyPtr.limitViolations().setPtr(limitViolationPtr);
    }

    private static void setOperatorStrategyResultInSecurityAnalysisResultPointer(PyPowsyblApiHeader.OperatorStrategyResultPointer operatorStrategyPtr, OperatorStrategyResult result) {
        operatorStrategyPtr.setOperatorStrategyId(CTypeUtil.toCharPtr(result.getOperatorStrategy().getId()));
        operatorStrategyPtr.setStatus(result.getStatus().ordinal());
        List<LimitViolation> limitViolations = result.getLimitViolationsResult().getLimitViolations();
        PyPowsyblApiHeader.LimitViolationPointer limitViolationPtr = UnmanagedMemory.calloc(limitViolations.size() * SizeOf.get(PyPowsyblApiHeader.LimitViolationPointer.class));
        createLimitViolationPtr(limitViolationPtr, limitViolations);
        operatorStrategyPtr.limitViolations().setLength(limitViolations.size());
        operatorStrategyPtr.limitViolations().setPtr(limitViolationPtr);
    }

    private static void setPreContingencyResultInSecurityAnalysisResultPointer(PyPowsyblApiHeader.PreContingencyResultPointer contingencyPtr, PreContingencyResult preContingencyResult) {
        contingencyPtr.setStatus(preContingencyResult.getStatus().ordinal());
        List<LimitViolation> limitViolations = preContingencyResult.getLimitViolationsResult().getLimitViolations();
        PyPowsyblApiHeader.LimitViolationPointer limitViolationPtr = UnmanagedMemory.calloc(limitViolations.size() * SizeOf.get(PyPowsyblApiHeader.LimitViolationPointer.class));
        createLimitViolationPtr(limitViolationPtr, limitViolations);
        contingencyPtr.limitViolations().setLength(limitViolations.size());
        contingencyPtr.limitViolations().setPtr(limitViolationPtr);
    }

    private static void createLimitViolationPtr(PyPowsyblApiHeader.LimitViolationPointer limitViolationPtr, List<LimitViolation> limitViolations) {
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
    }

    private static PyPowsyblApiHeader.PreContingencyResultPointer createPreContingencyResultArrayPointer(SecurityAnalysisResult result) {
        PyPowsyblApiHeader.PreContingencyResultPointer contingencyPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.PreContingencyResultPointer.class));
        setPreContingencyResultInSecurityAnalysisResultPointer(contingencyPtr, result.getPreContingencyResult());
        return contingencyPtr;
    }

    private static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.PostContingencyResultPointer> createPostContingencyResultArrayPointer(SecurityAnalysisResult result) {
        int resultCount = result.getPostContingencyResults().size(); // + 1 for pre-contingency result
        PyPowsyblApiHeader.PostContingencyResultPointer contingencyPtr = UnmanagedMemory.calloc(resultCount * SizeOf.get(PyPowsyblApiHeader.PostContingencyResultPointer.class));
        for (int i = 0; i < result.getPostContingencyResults().size(); i++) {
            PostContingencyResult postContingencyResult = result.getPostContingencyResults().get(i);
            PyPowsyblApiHeader.PostContingencyResultPointer contingencyPtrPlus = contingencyPtr.addressOf(i);
            setPostContingencyResultInSecurityAnalysisResultPointer(contingencyPtrPlus, postContingencyResult);
        }
        return allocArrayPointer(contingencyPtr, resultCount);
    }

    private static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.OperatorStrategyResultPointer> createOperatorStrategyResultsArrayPointer(SecurityAnalysisResult result) {
        int resultCount = result.getOperatorStrategyResults().size();
        PyPowsyblApiHeader.OperatorStrategyResultPointer strategyPtr = UnmanagedMemory.calloc(resultCount * SizeOf.get(PyPowsyblApiHeader.OperatorStrategyResultPointer.class));
        for (int i = 0; i < result.getOperatorStrategyResults().size(); i++) {
            OperatorStrategyResult resultOp = result.getOperatorStrategyResults().get(i);
            PyPowsyblApiHeader.OperatorStrategyResultPointer operatorStrategyPlus = strategyPtr.addressOf(i);
            setOperatorStrategyResultInSecurityAnalysisResultPointer(operatorStrategyPlus, resultOp);
        }
        return allocArrayPointer(strategyPtr, resultCount);
    }

    private static SecurityAnalysisProvider getProvider(String name) {
        String actualName = name.isEmpty() ? PyPowsyblConfiguration.getDefaultSecurityAnalysisProvider() : name;
        return SecurityAnalysisProvider.findAll().stream()
                .filter(provider -> provider.getName().equals(actualName))
                .findFirst()
                .orElseThrow(() -> new PowsyblException("No security analysis provider for name '" + actualName + "'"));
    }

    @CEntryPoint(name = "runSecurityAnalysis")
    public static ObjectHandle runSecurityAnalysis(IsolateThread thread, ObjectHandle securityAnalysisContextHandle,
                                                   ObjectHandle networkHandle, SecurityAnalysisParametersPointer securityAnalysisParametersPointer,
                                                   CCharPointer providerName, boolean dc, ObjectHandle reporterHandle,
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

    @CEntryPoint(name = "getPostContingencyResults")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.PostContingencyResultPointer> getPostContingencyResults(IsolateThread thread, ObjectHandle securityAnalysisResultHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisResult result = ObjectHandles.getGlobal().get(securityAnalysisResultHandle);
            return createPostContingencyResultArrayPointer(result);
        });
    }

    @CEntryPoint(name = "getOperatorStrategyResults")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.OperatorStrategyResultPointer> getOperatorStrategyResults(IsolateThread thread, ObjectHandle securityAnalysisResultHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisResult result = ObjectHandles.getGlobal().get(securityAnalysisResultHandle);
            return createOperatorStrategyResultsArrayPointer(result);
        });
    }

    @CEntryPoint(name = "getPreContingencyResult")
    public static PyPowsyblApiHeader.PreContingencyResultPointer getPreContingencyResult(IsolateThread thread, ObjectHandle securityAnalysisResultHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisResult result = ObjectHandles.getGlobal().get(securityAnalysisResultHandle);
            return createPreContingencyResultArrayPointer(result);
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
    public static void freeContingencyResultArrayPointer(IsolateThread thread, PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.PostContingencyResultPointer> contingencyResultArrayPtr,
                                                         PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            for (int i = 0; i < contingencyResultArrayPtr.getLength(); i++) {
                PyPowsyblApiHeader.PostContingencyResultPointer contingencyResultPtrPlus = contingencyResultArrayPtr.getPtr().addressOf(i);
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

    @CEntryPoint(name = "freeOperatorStrategyResultArrayPointer")
    public static void freeOperatorStrategyResultArrayPointer(IsolateThread thread, PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.OperatorStrategyResultPointer> operatorStrategyResultArrayPtr,
                                                         PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            for (int i = 0; i < operatorStrategyResultArrayPtr.getLength(); i++) {
                PyPowsyblApiHeader.OperatorStrategyResultPointer strategyResultPtrPlus = operatorStrategyResultArrayPtr.getPtr().addressOf(i);
                UnmanagedMemory.free(strategyResultPtrPlus.getOperatorStrategyId());
                for (int l = 0; l < strategyResultPtrPlus.limitViolations().getLength(); l++) {
                    PyPowsyblApiHeader.LimitViolationPointer violation = strategyResultPtrPlus.limitViolations().getPtr().addressOf(l);
                    UnmanagedMemory.free(violation.getSubjectId());
                    UnmanagedMemory.free(violation.getSubjectName());
                    UnmanagedMemory.free(violation.getLimitName());
                }
                UnmanagedMemory.free(strategyResultPtrPlus.limitViolations().getPtr());
            }
            freeArrayPointer(operatorStrategyResultArrayPtr);
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

    @CEntryPoint(name = "addLoadActivePowerAction")
    public static void addLoadActivePowerAction(IsolateThread thread, ObjectHandle securityAnalysisContextHandle,
                                                CCharPointer actionId, CCharPointer loadId, boolean relativeValue,
                                                double activePowerValue,
                                                PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(securityAnalysisContextHandle);
            String actionIdStr = CTypeUtil.toString(actionId);
            String loadIdStr = CTypeUtil.toString(loadId);
            LoadAction action = new LoadActionBuilder().withId(actionIdStr)
                    .withLoadId(loadIdStr)
                    .withRelativeValue(relativeValue)
                    .withActivePowerValue(activePowerValue)
                    .build();
            analysisContext.addAction(action);
            System.out.println("Creating action " + actionIdStr + " on load " + loadIdStr + " relativeV " + relativeValue + " P " + activePowerValue);
        });
    }

    @CEntryPoint(name = "addLoadReactivePowerAction")
    public static void addLoadReactivePowerAction(IsolateThread thread, ObjectHandle securityAnalysisContextHandle,
                                                CCharPointer actionId, CCharPointer loadId, boolean relativeValue,
                                                double reactivePowerValue,
                                                PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(securityAnalysisContextHandle);
            String actionIdStr = CTypeUtil.toString(actionId);
            String loadIdStr = CTypeUtil.toString(loadId);
            LoadAction action = new LoadActionBuilder().withId(actionIdStr)
                    .withLoadId(loadIdStr)
                    .withRelativeValue(relativeValue)
                    .withReactivePowerValue(reactivePowerValue)
                    .build();
            analysisContext.addAction(action);
            System.out.println("Creating action " + actionIdStr + " on load " + loadIdStr + " relativeV " + relativeValue + " Q " + reactivePowerValue);
        });
    }

    @CEntryPoint(name = "addGeneratorActivePowerAction")
    public static void addGeneratorActivePowerAction(IsolateThread thread, ObjectHandle securityAnalysisContextHandle,
                                          CCharPointer actionId, CCharPointer generatorId, boolean relativeValue, double activePower,
                                          PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(securityAnalysisContextHandle);
            String actionIdStr = CTypeUtil.toString(actionId);
            String generatorIdStr = CTypeUtil.toString(generatorId);
            GeneratorActionBuilder builder = new GeneratorActionBuilder().withId(actionIdStr)
                    .withGeneratorId(generatorIdStr);
            if (relativeValue) {
                builder.withActivePowerRelativeValue(relativeValue);
            }
            builder.withActivePowerValue(activePower);
            analysisContext.addAction(builder.build());
            System.out.println("Creating action " + actionIdStr + " on generator " + generatorIdStr + " relativeV " + relativeValue + " P " + activePower);
        });
    }

    @CEntryPoint(name = "addSwitchAction")
    public static void addSwitchAction(IsolateThread thread, ObjectHandle securityAnalysisContextHandle,
                                         CCharPointer actionId, CCharPointer switchId, boolean open,
                                         PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(securityAnalysisContextHandle);
            String actionIdStr = CTypeUtil.toString(actionId);
            String switchIdStr = CTypeUtil.toString(switchId);
            SwitchAction action = new SwitchAction(actionIdStr, switchIdStr, open);
            analysisContext.addAction(action);
            System.out.println("Creating action " + actionIdStr + " on switch " + switchIdStr + " open " + open);
        });
    }

    @CEntryPoint(name = "addPhaseTapChangerPositionAction")
    public static void addPhaseTapChangerPositionAction(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
        });
    }

    @CEntryPoint(name = "addPhaseTapChangerRegulationAction")
    public static void addPhaseTapChangerRegulationAction(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
        });
    }

    @CEntryPoint(name = "addRatioTapChangerPositionAction")
    public static void addRatioTapChangerPositionAction(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
        });
    }

    @CEntryPoint(name = "addRatioTapChangerRegulationAction")
    public static void addRatioTapChangerRegulationAction(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
        });
    }

    @CEntryPoint(name = "addShuntCompensatorPositionAction")
    public static void addShuntCompensatorPositionAction(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
        });
    }

    @CEntryPoint(name = "addOperatorStrategy")
    public static void addOperatorStrategy(IsolateThread thread, ObjectHandle securityAnalysisContextHandle,
                                         CCharPointer operationStrategyId, CCharPointer contingencyId,
                                         CCharPointerPointer actions, int actionCount,
                                         PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SecurityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(securityAnalysisContextHandle);
            String operationStrategyIdStr = CTypeUtil.toString(operationStrategyId);
            String contingencyIdStr = CTypeUtil.toString(contingencyId);
            List<String> actionsStrList = CTypeUtil.toStringList(actions, actionCount);
            OperatorStrategy op = new OperatorStrategy(operationStrategyIdStr,
                    ContingencyContext.specificContingency(contingencyIdStr), new TrueCondition(), actionsStrList);
            analysisContext.addOperatorStrategy(op);
            System.out.println("Add operator strategy " + operationStrategyIdStr);
        });
    }
}
