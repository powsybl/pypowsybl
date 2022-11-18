/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.sensitivity;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.reporter.Reporter;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.*;
import com.powsybl.python.commons.PyPowsyblApiHeader.ExceptionHandlerPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.SensitivityAnalysisParametersPointer;
import com.powsybl.python.loadflow.LoadFlowCFunctions;
import com.powsybl.python.loadflow.LoadFlowCUtils;
import com.powsybl.python.report.ReportCUtils;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityAnalysisProvider;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.WeightedSensitivityVariable;
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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static com.powsybl.python.commons.CTypeUtil.toStringList;
import static com.powsybl.python.commons.Util.createCharPtrArray;
import static com.powsybl.python.commons.Util.doCatch;

/**
 * C functions related to sensitivity analysis.
 *
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
@CContext(Directives.class)
public final class SensitivityAnalysisCFunctions {

    private SensitivityAnalysisCFunctions() {
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(SensitivityAnalysisCFunctions.class);
    }

    @CEntryPoint(name = "getSensitivityAnalysisProviderNames")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointerPointer> getSensitivityAnalysisProviderNames(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(new ServiceLoaderCache<>(SensitivityAnalysisProvider.class).getServices()
                .stream().map(SensitivityAnalysisProvider::getName).collect(Collectors.toList())));
    }

    @CEntryPoint(name = "setDefaultSensitivityAnalysisProvider")
    public static void setDefaultSensitivityAnalysisProvider(IsolateThread thread, CCharPointer provider, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            PyPowsyblConfiguration.setDefaultSensitivityAnalysisProvider(CTypeUtil.toString(provider));
        });
    }

    @CEntryPoint(name = "getDefaultSensitivityAnalysisProvider")
    public static CCharPointer getDefaultSensitivityAnalysisProvider(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> CTypeUtil.toCharPtr(PyPowsyblConfiguration.getDefaultSensitivityAnalysisProvider()));
    }

    @CEntryPoint(name = "createSensitivityAnalysis")
    public static ObjectHandle createSensitivityAnalysis(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new SensitivityAnalysisContext()));
    }

    @CEntryPoint(name = "setZones")
    public static void setZones(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                PyPowsyblApiHeader.ZonePointerPointer zonePtrPtr, int zoneCount,
                                ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            List<SensitivityVariableSet> variableSets = new ArrayList<>(zoneCount);
            for (int zoneIndex = 0; zoneIndex < zoneCount; zoneIndex++) {
                PyPowsyblApiHeader.ZonePointer zonePtrI = zonePtrPtr.read(zoneIndex);
                String zoneId = CTypeUtil.toString(zonePtrI.getId());
                List<String> injectionsIds = toStringList(zonePtrI.getInjectionsIds(), zonePtrI.getLength());
                List<Double> injectionsShiftKeys = CTypeUtil.toDoubleList(zonePtrI.getinjectionsShiftKeys(), zonePtrI.getLength());
                List<WeightedSensitivityVariable> variables = new ArrayList<>(injectionsIds.size());
                for (int injectionIndex = 0; injectionIndex < injectionsIds.size(); injectionIndex++) {
                    variables.add(new WeightedSensitivityVariable(injectionsIds.get(injectionIndex), injectionsShiftKeys.get(injectionIndex)));
                }
                variableSets.add(new SensitivityVariableSet(zoneId, variables));
            }
            analysisContext.setVariableSets(variableSets);
        });
    }

    @CEntryPoint(name = "addBranchFlowFactorMatrix")
    public static void addBranchFlowFactorMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                                 CCharPointerPointer branchIdPtrPtr, int branchIdCount,
                                                 CCharPointerPointer variableIdPtrPtr, int variableIdCount,
                                                 CCharPointer matrixIdPtr,
                                                 ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            List<String> branchesIds = toStringList(branchIdPtrPtr, branchIdCount);
            List<String> variablesIds = toStringList(variableIdPtrPtr, variableIdCount);
            String matrixId = CTypeUtil.toString(matrixIdPtr);
            analysisContext.addBranchFlowFactorMatrix(matrixId, branchesIds, variablesIds);
        });
    }

    @CEntryPoint(name = "addPreContingencyBranchFlowFactorMatrix")
    public static void addPreContingencyBranchFlowFactorMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                                               CCharPointerPointer branchIdPtrPtr, int branchIdCount,
                                                               CCharPointerPointer variableIdPtrPtr, int variableIdCount,
                                                               CCharPointer matrixIdPtr,
                                                               ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            List<String> branchesIds = toStringList(branchIdPtrPtr, branchIdCount);
            List<String> variablesIds = toStringList(variableIdPtrPtr, variableIdCount);
            String matrixId = CTypeUtil.toString(matrixIdPtr);
            analysisContext.addPreContingencyBranchFlowFactorMatrix(matrixId, branchesIds, variablesIds);
        });
    }

    @CEntryPoint(name = "addPostContingencyBranchFlowFactorMatrix")
    public static void addPostContingencyBranchFlowFactorMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                                                CCharPointerPointer branchIdPtrPtr, int branchIdCount,
                                                                CCharPointerPointer variableIdPtrPtr, int variableIdCount,
                                                                CCharPointerPointer contingenciesIdPtrPtr, int contingenciesIdCount,
                                                                CCharPointer matrixIdPtr,
                                                                ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            List<String> branchesIds = toStringList(branchIdPtrPtr, branchIdCount);
            List<String> variablesIds = toStringList(variableIdPtrPtr, variableIdCount);
            List<String> contingencies = toStringList(contingenciesIdPtrPtr, contingenciesIdCount);
            String matrixId = CTypeUtil.toString(matrixIdPtr);
            analysisContext.addPostContingencyBranchFlowFactorMatrix(matrixId, branchesIds, variablesIds, contingencies);
        });
    }

    @CEntryPoint(name = "setBusVoltageFactorMatrix")
    public static void setBusVoltageFactorMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                                 CCharPointerPointer busVoltageIdPtrPtr, int branchIdCount,
                                                 CCharPointerPointer targetVoltageIdPtrPtr, int injectionOrTransfoIdCount,
                                                 ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            List<String> busVoltageIds = toStringList(busVoltageIdPtrPtr, branchIdCount);
            List<String> targetVoltageIds = toStringList(targetVoltageIdPtrPtr, injectionOrTransfoIdCount);
            analysisContext.setBusVoltageFactorMatrix(busVoltageIds, targetVoltageIds);
        });
    }

    @CEntryPoint(name = "runSensitivityAnalysis")
    public static ObjectHandle runSensitivityAnalysis(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                                      ObjectHandle networkHandle, boolean dc, SensitivityAnalysisParametersPointer sensitivityAnalysisParametersPtr,
                                                      CCharPointer providerName, ObjectHandle reporterHandle,
                                                      ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            SensitivityAnalysisProvider provider = getProvider(CTypeUtil.toString(providerName));
            logger().info("Sensitivity analysis provider used for sensitivity analysis is : {}", provider.getName());
            SensitivityAnalysisParameters sensitivityAnalysisParameters = SensitivityAnalysisCUtils.createSensitivityAnalysisParameters(dc, sensitivityAnalysisParametersPtr, provider);
            Reporter reporter = ReportCUtils.getReporter(reporterHandle);
            SensitivityAnalysisResultContext resultContext = analysisContext.run(network, sensitivityAnalysisParameters, provider.getName(), reporter);
            return ObjectHandles.getGlobal().create(resultContext);
        });
    }

    @CEntryPoint(name = "getBranchFlowsSensitivityMatrix")
    public static PyPowsyblApiHeader.MatrixPointer getBranchFlowsSensitivityMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                                                   CCharPointer matrixIdPtr, CCharPointer contingencyIdPtr,
                                                                                   ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            String matrixId = CTypeUtil.toString(matrixIdPtr);
            return resultContext.createBranchFlowsSensitivityMatrix(matrixId, contingencyId);
        });
    }

    @CEntryPoint(name = "getBusVoltagesSensitivityMatrix")
    public static PyPowsyblApiHeader.MatrixPointer getBusVoltagesSensitivityMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                                                   CCharPointer contingencyIdPtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            return resultContext.createBusVoltagesSensitivityMatrix(contingencyId);
        });
    }

    @CEntryPoint(name = "getReferenceFlows")
    public static PyPowsyblApiHeader.MatrixPointer getReferenceFlows(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                                     CCharPointer matrixIdPtr, CCharPointer contingencyIdPtr,
                                                                     ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            String matrixId = CTypeUtil.toString(matrixIdPtr);
            return resultContext.createReferenceFlowsActivePower(matrixId, contingencyId);
        });
    }

    @CEntryPoint(name = "getReferenceVoltages")
    public static PyPowsyblApiHeader.MatrixPointer getReferenceVoltages(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                                        CCharPointer contingencyIdPtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            return resultContext.createReferenceVoltages(contingencyId);
        });
    }

    private static SensitivityAnalysisProvider getProvider(String name) {
        String actualName = name.isEmpty() ? PyPowsyblConfiguration.getDefaultSensitivityAnalysisProvider() : name;
        return ServiceLoader.load(SensitivityAnalysisProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(provider -> provider.getName().equals(actualName))
                .findFirst()
                .orElseThrow(() -> new PowsyblException("No sensitivity analysis provider for name '" + actualName + "'"));
    }

    @CEntryPoint(name = "freeSensitivityAnalysisParameters")
    public static void freeSensitivityAnalysisParameters(IsolateThread thread, SensitivityAnalysisParametersPointer parameters,
                                                         ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            LoadFlowCUtils.freeLoadFlowParametersContent(parameters.getLoadFlowParameters());
            UnmanagedMemory.free(parameters);
        });
    }

    @CEntryPoint(name = "createSensitivityAnalysisParameters")
    public static SensitivityAnalysisParametersPointer createSensitivityAnalysisParameters(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> convertToSensitivityAnalysisParametersPointer(SensitivityAnalysisCUtils.createSensitivityAnalysisParameters()));
    }

    private static SensitivityAnalysisParametersPointer convertToSensitivityAnalysisParametersPointer(SensitivityAnalysisParameters parameters) {
        SensitivityAnalysisParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(SensitivityAnalysisParametersPointer.class));
        LoadFlowCFunctions.copyToCLoadFlowParameters(parameters.getLoadFlowParameters(), paramsPtr.getLoadFlowParameters());
        paramsPtr.setProviderParametersValuesCount(0);
        paramsPtr.setProviderParametersKeysCount(0);
        return paramsPtr;
    }

    @CEntryPoint(name = "getSensitivityAnalysisProviderParametersNames")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointerPointer> getProviderParametersNames(IsolateThread thread, CCharPointer provider, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String providerStr = CTypeUtil.toString(provider);
            return Util.createCharPtrArray(SensitivityAnalysisCUtils.getSensitivityAnalysisProvider(providerStr).getSpecificParametersNames());
        });
    }
}
