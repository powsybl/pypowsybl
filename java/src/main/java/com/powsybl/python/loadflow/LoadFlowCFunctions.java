/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.loadflow;

import com.powsybl.commons.reporter.ReporterModel;
import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowProvider;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.python.commons.*;
import com.powsybl.python.commons.PyPowsyblApiHeader.LoadFlowParametersPointer;
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
import java.util.stream.Collectors;

import static com.powsybl.python.commons.PyPowsyblApiHeader.allocArrayPointer;
import static com.powsybl.python.commons.PyPowsyblApiHeader.freeArrayPointer;
import static com.powsybl.python.commons.Util.createCharPtrArray;
import static com.powsybl.python.commons.Util.doCatch;

/**
 * C functions related to loadflow.
 *
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
@CContext(Directives.class)
public final class LoadFlowCFunctions {

    private LoadFlowCFunctions() {
    }

    @CEntryPoint(name = "setDefaultLoadFlowProvider")
    public static void setDefaultLoadFlowProvider(IsolateThread thread, CCharPointer provider, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            PyPowsyblConfiguration.setDefaultLoadFlowProvider(CTypeUtil.toString(provider));
        });
    }

    @CEntryPoint(name = "getDefaultLoadFlowProvider")
    public static CCharPointer getDefaultLoadFlowProvider(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> CTypeUtil.toCharPtr(PyPowsyblConfiguration.getDefaultLoadFlowProvider()));
    }

    @CEntryPoint(name = "freeLoadFlowComponentResultPointer")
    public static void freeLoadFlowComponentResultPointer(IsolateThread thread, PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.LoadFlowComponentResultPointer> componentResultArrayPtr,
                                                          PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            for (int i = 0; i < componentResultArrayPtr.getLength(); i++) {
                UnmanagedMemory.free(componentResultArrayPtr.getPtr().addressOf(i).getSlackBusId());
            }
            freeArrayPointer(componentResultArrayPtr);
        });
    }

    @CEntryPoint(name = "getLoadFlowProviderNames")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointerPointer> getLoadFlowProviderNames(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> createCharPtrArray(new ServiceLoaderCache<>(LoadFlowProvider.class).getServices()
                .stream().map(LoadFlowProvider::getName).collect(Collectors.toList())));
    }

    @CEntryPoint(name = "runLoadFlow")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.LoadFlowComponentResultPointer> runLoadFlow(IsolateThread thread, ObjectHandle networkHandle, boolean dc,
                                                                                                                 LoadFlowParametersPointer loadFlowParametersPtr,
                                                                                                                 CCharPointer provider, ObjectHandle reporterHandle,
                                                                                                                 PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return Util.doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String providerStr = CTypeUtil.toString(provider);
            LoadFlowProvider loadFlowProvider = LoadFlowCUtils.getLoadFlowProvider(providerStr);
            logger().info("loadflow provider used is : {}", loadFlowProvider.getName());

            LoadFlowParameters parameters = LoadFlowCUtils.createLoadFlowParameters(dc, loadFlowParametersPtr, loadFlowProvider);
            LoadFlowResult result;
            LoadFlow.Runner runner = new LoadFlow.Runner(loadFlowProvider);
            ReporterModel reporter = ObjectHandles.getGlobal().get(reporterHandle);
            if (reporter == null) {
                result = runner.run(network, parameters);
            } else {
                result = runner.run(network, network.getVariantManager().getWorkingVariantId(),
                        LocalComputationManager.getDefault(), parameters, reporter);
            }
            return createLoadFlowComponentResultArrayPointer(result);
        });
    }

    @CEntryPoint(name = "createLoadFlowParameters")
    public static LoadFlowParametersPointer createLoadFlowParameters(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> convertToLoadFlowParametersPointer(LoadFlowCUtils.createLoadFlowParameters()));
    }

    @CEntryPoint(name = "freeLoadFlowParameters")
    public static void freeLoadFlowParameters(IsolateThread thread, LoadFlowParametersPointer loadFlowParametersPtr,
                                              PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            freeLoadFlowParametersPointer(loadFlowParametersPtr);
        });
    }

    public static void freeLoadFlowParametersPointer(LoadFlowParametersPointer loadFlowParametersPtr) {
        for (int i = 0; i < loadFlowParametersPtr.getCountriesToBalanceCount(); i++) {
            UnmanagedMemory.free(loadFlowParametersPtr.getCountriesToBalance().read(i));
        }
        UnmanagedMemory.free(loadFlowParametersPtr.getCountriesToBalance());
        UnmanagedMemory.free(loadFlowParametersPtr);
    }

    private static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.LoadFlowComponentResultPointer> createLoadFlowComponentResultArrayPointer(LoadFlowResult result) {
        List<LoadFlowResult.ComponentResult> componentResults = result.getComponentResults();
        PyPowsyblApiHeader.LoadFlowComponentResultPointer componentResultPtr = UnmanagedMemory.calloc(componentResults.size() * SizeOf.get(PyPowsyblApiHeader.LoadFlowComponentResultPointer.class));
        for (int index = 0; index < componentResults.size(); index++) {
            LoadFlowResult.ComponentResult componentResult = componentResults.get(index);
            PyPowsyblApiHeader.LoadFlowComponentResultPointer ptr = componentResultPtr.addressOf(index);
            ptr.setConnectedComponentNum(componentResult.getConnectedComponentNum());
            ptr.setSynchronousComponentNum(componentResult.getSynchronousComponentNum());
            ptr.setStatus(componentResult.getStatus().ordinal());
            ptr.setIterationCount(componentResult.getIterationCount());
            ptr.setSlackBusId(CTypeUtil.toCharPtr(componentResult.getSlackBusId()));
            ptr.setSlackBusActivePowerMismatch(componentResult.getSlackBusActivePowerMismatch());
            ptr.setDistributedActivePower(componentResult.getDistributedActivePower());
        }
        return allocArrayPointer(componentResultPtr, componentResults.size());
    }

    public static void copyToCLoadFlowParameters(LoadFlowParameters parameters, LoadFlowParametersPointer cParameters) {
        cParameters.setVoltageInitMode(parameters.getVoltageInitMode().ordinal());
        cParameters.setTransformerVoltageControlOn(parameters.isTransformerVoltageControlOn());
        cParameters.setNoGeneratorReactiveLimits(parameters.isNoGeneratorReactiveLimits());
        cParameters.setPhaseShifterRegulationOn(parameters.isPhaseShifterRegulationOn());
        cParameters.setTwtSplitShuntAdmittance(parameters.isTwtSplitShuntAdmittance());
        cParameters.setSimulShunt(parameters.isSimulShunt());
        cParameters.setReadSlackBus(parameters.isReadSlackBus());
        cParameters.setWriteSlackBus(parameters.isWriteSlackBus());
        cParameters.setDistributedSlack(parameters.isDistributedSlack());
        cParameters.setBalanceType(parameters.getBalanceType().ordinal());
        cParameters.setReadSlackBus(parameters.isReadSlackBus());
        cParameters.setBalanceType(parameters.getBalanceType().ordinal());
        cParameters.setDcUseTransformerRatio(parameters.isDcUseTransformerRatio());
        CCharPointerPointer calloc = UnmanagedMemory.calloc(parameters.getCountriesToBalance().size() * SizeOf.get(CCharPointerPointer.class));
        ArrayList<Country> countries = new ArrayList<>(parameters.getCountriesToBalance());
        for (int i = 0; i < parameters.getCountriesToBalance().size(); i++) {
            calloc.write(i, CTypeUtil.toCharPtr(countries.get(i).toString()));
        }
        cParameters.setCountriesToBalance(calloc);
        cParameters.setCountriesToBalanceCount(countries.size());
        cParameters.setConnectedComponentMode(parameters.getConnectedComponentMode().ordinal());
        cParameters.setProviderParametersValuesCount(0);
        cParameters.setProviderParametersKeysCount(0);
    }

    public static LoadFlowParametersPointer convertToLoadFlowParametersPointer(LoadFlowParameters parameters) {
        LoadFlowParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(LoadFlowParametersPointer.class));
        copyToCLoadFlowParameters(parameters, paramsPtr);
        return paramsPtr;
    }

    @CEntryPoint(name = "getProviderParametersNames")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointerPointer> getProviderParametersNames(IsolateThread thread, CCharPointer provider, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String providerStr = CTypeUtil.toString(provider);
            return Util.createCharPtrArray(LoadFlowCUtils.getLoadFlowProvider(providerStr).getSpecificParametersNames());
        });
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(CommonCFunctions.class);
    }
}
