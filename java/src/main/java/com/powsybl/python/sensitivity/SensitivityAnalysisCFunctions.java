/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.sensitivity;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblConfiguration;
import com.powsybl.python.loadflow.LoadFlowCUtils;
import com.powsybl.python.security.SecurityAnalysisCFunctions;
import com.powsybl.sensitivity.SensitivityAnalysisProvider;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.WeightedSensitivityVariable;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
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
        return LoggerFactory.getLogger(SecurityAnalysisCFunctions.class);
    }

    @CEntryPoint(name = "getSensitivityAnalysisProviderNames")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointerPointer> getSensitivityAnalysisProviderNames(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(new ServiceLoaderCache<>(SensitivityAnalysisProvider.class).getServices()
                .stream().map(SensitivityAnalysisProvider::getName).collect(Collectors.toList())));
    }

    @CEntryPoint(name = "setDefaultSensitivityAnalysisProvider")
    public static void setDefaultSensitivityAnalysisProvider(IsolateThread thread, CCharPointer provider, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            PyPowsyblConfiguration.setDefaultSensitivityAnalysisProvider(CTypeUtil.toString(provider));
        });
    }

    @CEntryPoint(name = "getDefaultSensitivityAnalysisProvider")
    public static CCharPointer getDefaultSensitivityAnalysisProvider(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> CTypeUtil.toCharPtr(PyPowsyblConfiguration.getDefaultSensitivityAnalysisProvider()));
    }

    @CEntryPoint(name = "createSensitivityAnalysis")
    public static ObjectHandle createSensitivityAnalysis(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new SensitivityAnalysisContext()));
    }

    @CEntryPoint(name = "setZones")
    public static void setZones(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                PyPowsyblApiHeader.ZonePointerPointer zonePtrPtr, int zoneCount,
                                PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
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
                                                 PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
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
                                                               PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
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
                                                                PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
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
                                                 PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            List<String> busVoltageIds = toStringList(busVoltageIdPtrPtr, branchIdCount);
            List<String> targetVoltageIds = toStringList(targetVoltageIdPtrPtr, injectionOrTransfoIdCount);
            analysisContext.setBusVoltageFactorMatrix(busVoltageIds, targetVoltageIds);
        });
    }

    @CEntryPoint(name = "runSensitivityAnalysis")
    public static ObjectHandle runSensitivityAnalysis(IsolateThread thread, ObjectHandle sensitivityAnalysisContextHandle,
                                                      ObjectHandle networkHandle, boolean dc, PyPowsyblApiHeader.LoadFlowParametersPointer loadFlowParametersPtr,
                                                      CCharPointer providerName,
                                                      PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisContext analysisContext = ObjectHandles.getGlobal().get(sensitivityAnalysisContextHandle);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            SensitivityAnalysisProvider provider = getProvider(CTypeUtil.toString(providerName));
            logger().info("Sensitivity analysis provider used for sensitivity analysis is : {}", provider.getName());
            LoadFlowParameters loadFlowParameters = provider.getLoadFlowProviderName()
                    .map(lfName -> LoadFlowCUtils.createLoadFlowParameters(dc, loadFlowParametersPtr, lfName))
                    .orElseGet(() -> LoadFlowCUtils.createLoadFlowParameters(dc, loadFlowParametersPtr));
            SensitivityAnalysisResultContext resultContext = analysisContext.run(network, loadFlowParameters, provider.getName());
            return ObjectHandles.getGlobal().create(resultContext);
        });
    }

    @CEntryPoint(name = "getBranchFlowsSensitivityMatrix")
    public static PyPowsyblApiHeader.MatrixPointer getBranchFlowsSensitivityMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                                                   CCharPointer matrixIdPtr, CCharPointer contingencyIdPtr,
                                                                                   PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            String matrixId = CTypeUtil.toString(matrixIdPtr);
            return resultContext.createBranchFlowsSensitivityMatrix(matrixId, contingencyId);
        });
    }

    @CEntryPoint(name = "getBusVoltagesSensitivityMatrix")
    public static PyPowsyblApiHeader.MatrixPointer getBusVoltagesSensitivityMatrix(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                                                   CCharPointer contingencyIdPtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            return resultContext.createBusVoltagesSensitivityMatrix(contingencyId);
        });
    }

    @CEntryPoint(name = "getReferenceFlows")
    public static PyPowsyblApiHeader.MatrixPointer getReferenceFlows(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                                     CCharPointer matrixIdPtr, CCharPointer contingencyIdPtr,
                                                                     PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            SensitivityAnalysisResultContext resultContext = ObjectHandles.getGlobal().get(sensitivityAnalysisResultContextHandle);
            String contingencyId = CTypeUtil.toString(contingencyIdPtr);
            String matrixId = CTypeUtil.toString(matrixIdPtr);
            return resultContext.createReferenceFlowsActivePower(matrixId, contingencyId);
        });
    }

    @CEntryPoint(name = "getReferenceVoltages")
    public static PyPowsyblApiHeader.MatrixPointer getReferenceVoltages(IsolateThread thread, ObjectHandle sensitivityAnalysisResultContextHandle,
                                                                        CCharPointer contingencyIdPtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
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
}
