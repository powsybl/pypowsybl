/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.loadflow;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.extensions.Extension;
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
import java.util.Map;
import java.util.ServiceLoader;
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

    private static LoadFlowProvider getLoadFlowProvider(String name) {
        String actualName = name.isEmpty() ? PyPowsyblConfiguration.getDefaultLoadFlowProvider() : name;
        return ServiceLoader.load(LoadFlowProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(provider -> provider.getName().equals(actualName))
                .findFirst()
                .orElseThrow(() -> new PowsyblException("No loadflow provider for name '" + actualName + "'"));
    }

    private static Map<String, String> getSpecificParameters(PyPowsyblApiHeader.LoadFlowParametersPointer loadFlowParametersPtr) {
        return CTypeUtil.toStringMap(loadFlowParametersPtr.getProviderParametersKeys(),
                loadFlowParametersPtr.getProviderParametersKeysCount(),
                loadFlowParametersPtr.getProviderParametersValues(),
                loadFlowParametersPtr.getProviderParametersValuesCount());
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
                                                                                                                 PyPowsyblApiHeader.LoadFlowParametersPointer loadFlowParametersPtr,
                                                                                                                 CCharPointer provider, ObjectHandle reporterHandle,
                                                                                                                 PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return Util.doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            LoadFlowParameters parameters = createLoadFlowParameters(dc, loadFlowParametersPtr);
            String providerStr = CTypeUtil.toString(provider);
            LoadFlowProvider loadFlowProvider = getLoadFlowProvider(providerStr);
            logger().info("loadflow provider used is : {}", loadFlowProvider.getName());

            Map<String, String> specificParametersProperties = getSpecificParameters(loadFlowParametersPtr);

            loadFlowProvider.loadSpecificParameters(specificParametersProperties).ifPresent(ext -> {
                // Dirty trick to get the class, and reload parameters if they exist.
                // TODO: SPI needs to be changed so that we don't need to read params to get the class
                Extension<LoadFlowParameters> configured = parameters.getExtension(ext.getClass());
                if (configured != null) {
                    loadFlowProvider.updateSpecificParameters(configured, specificParametersProperties);
                } else {
                    parameters.addExtension((Class) ext.getClass(), ext);
                }
            });

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
        }
        return allocArrayPointer(componentResultPtr, componentResults.size());
    }

    public static LoadFlowParameters createLoadFlowParameters(boolean dc, PyPowsyblApiHeader.LoadFlowParametersPointer loadFlowParametersPtr) {
        return createLoadFlowParameters()
                .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.values()[loadFlowParametersPtr.getVoltageInitMode()])
                .setTransformerVoltageControlOn(loadFlowParametersPtr.isTransformerVoltageControlOn())
                .setNoGeneratorReactiveLimits(loadFlowParametersPtr.isNoGeneratorReactiveLimits())
                .setPhaseShifterRegulationOn(loadFlowParametersPtr.isPhaseShifterRegulationOn())
                .setTwtSplitShuntAdmittance(loadFlowParametersPtr.isTwtSplitShuntAdmittance())
                .setShuntCompensatorVoltageControlOn(loadFlowParametersPtr.isSimulShunt())
                .setReadSlackBus(loadFlowParametersPtr.isReadSlackBus())
                .setWriteSlackBus(loadFlowParametersPtr.isWriteSlackBus())
                .setDistributedSlack(loadFlowParametersPtr.isDistributedSlack())
                .setDc(dc)
                .setBalanceType(LoadFlowParameters.BalanceType.values()[loadFlowParametersPtr.getBalanceType()])
                .setDcUseTransformerRatio(loadFlowParametersPtr.isDcUseTransformerRatio())
                .setCountriesToBalance(CTypeUtil.toStringList(loadFlowParametersPtr.getCountriesToBalance(), loadFlowParametersPtr.getCountriesToBalanceCount())
                        .stream().map(Country::valueOf).collect(Collectors.toSet()))
                .setConnectedComponentMode(LoadFlowParameters.ConnectedComponentMode.values()[loadFlowParametersPtr.getConnectedComponentMode()]);
    }

    @CEntryPoint(name = "createLoadFlowParameters")
    public static PyPowsyblApiHeader.LoadFlowParametersPointer createLoadFlowParameters(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> convertToLoadFlowParametersPointer(createLoadFlowParameters()));
    }

    private static LoadFlowParameters createLoadFlowParameters() {
        return PyPowsyblConfiguration.isReadConfig() ? LoadFlowParameters.load() : new LoadFlowParameters();
    }

    @CEntryPoint(name = "freeLoadFlowParameters")
    public static void freeLoadFlowParameters(IsolateThread thread, PyPowsyblApiHeader.LoadFlowParametersPointer loadFlowParametersPtr,
                                              PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            for (int i = 0; i < loadFlowParametersPtr.getCountriesToBalanceCount(); i++) {
                UnmanagedMemory.free(loadFlowParametersPtr.getCountriesToBalance().read(i));
            }
            UnmanagedMemory.free(loadFlowParametersPtr.getCountriesToBalance());
            UnmanagedMemory.free(loadFlowParametersPtr);
        });
    }

    private static PyPowsyblApiHeader.LoadFlowParametersPointer convertToLoadFlowParametersPointer(LoadFlowParameters parameters) {
        PyPowsyblApiHeader.LoadFlowParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.LoadFlowParametersPointer.class));
        paramsPtr.setVoltageInitMode(parameters.getVoltageInitMode().ordinal());
        paramsPtr.setTransformerVoltageControlOn(parameters.isTransformerVoltageControlOn());
        paramsPtr.setNoGeneratorReactiveLimits(parameters.isNoGeneratorReactiveLimits());
        paramsPtr.setPhaseShifterRegulationOn(parameters.isPhaseShifterRegulationOn());
        paramsPtr.setTwtSplitShuntAdmittance(parameters.isTwtSplitShuntAdmittance());
        paramsPtr.setSimulShunt(parameters.isSimulShunt());
        paramsPtr.setReadSlackBus(parameters.isReadSlackBus());
        paramsPtr.setWriteSlackBus(parameters.isWriteSlackBus());
        paramsPtr.setDistributedSlack(parameters.isDistributedSlack());
        paramsPtr.setBalanceType(parameters.getBalanceType().ordinal());
        paramsPtr.setReadSlackBus(parameters.isReadSlackBus());
        paramsPtr.setBalanceType(parameters.getBalanceType().ordinal());
        paramsPtr.setDcUseTransformerRatio(parameters.isDcUseTransformerRatio());
        CCharPointerPointer calloc = UnmanagedMemory.calloc(parameters.getCountriesToBalance().size() * SizeOf.get(CCharPointerPointer.class));
        ArrayList<Country> countries = new ArrayList<>(parameters.getCountriesToBalance());
        for (int i = 0; i < parameters.getCountriesToBalance().size(); i++) {
            calloc.write(i, CTypeUtil.toCharPtr(countries.get(i).toString()));
        }
        paramsPtr.setCountriesToBalance(calloc);
        paramsPtr.setCountriesToBalanceCount(countries.size());
        paramsPtr.setConnectedComponentMode(parameters.getConnectedComponentMode().ordinal());
        paramsPtr.setProviderParametersValuesCount(0);
        paramsPtr.setProviderParametersKeysCount(0);
        return paramsPtr;
    }

    @CEntryPoint(name = "getProviderParametersNames")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointerPointer> getProviderParametersNames(IsolateThread thread, CCharPointer provider, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String providerStr = CTypeUtil.toString(provider);
            return Util.createCharPtrArray(getLoadFlowProvider(providerStr).getSpecificParametersNames());
        });
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(CommonCFunctions.class);
    }
}
