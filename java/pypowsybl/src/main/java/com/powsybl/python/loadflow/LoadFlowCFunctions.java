/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.loadflow;

import com.powsybl.commons.parameters.Parameter;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowProvider;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;
import com.powsybl.python.commons.*;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.LoadFlowComponentResultPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.LoadFlowParametersPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.SeriesPointer;
import com.powsybl.python.commons.Util.PointerProvider;
import com.powsybl.python.network.Dataframes;
import com.powsybl.python.report.ReportCUtils;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.powsybl.python.commons.PyPowsyblApiHeader.allocArrayPointer;
import static com.powsybl.python.commons.PyPowsyblApiHeader.freeArrayPointer;
import static com.powsybl.python.commons.Util.createCharPtrArray;
import static com.powsybl.python.commons.Util.doCatch;

/**
 * C functions related to loadflow.
 *
 * @author Sylvain Leclerc {@literal <sylvain.leclerc@rte-france.com>}
 */
@SuppressWarnings({"java:S1602", "java:S1604", "Convert2Lambda"})
@CContext(Directives.class)
public final class LoadFlowCFunctions {

    private LoadFlowCFunctions() {
    }

    @CEntryPoint(name = "setDefaultLoadFlowProvider")
    public static void setDefaultLoadFlowProvider(IsolateThread thread, CCharPointer provider, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                PyPowsyblConfiguration.setDefaultLoadFlowProvider(CTypeUtil.toString(provider));
            }
        });
    }

    @CEntryPoint(name = "getDefaultLoadFlowProvider")
    public static CCharPointer getDefaultLoadFlowProvider(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public CCharPointer get() {
                return CTypeUtil.toCharPtr(PyPowsyblConfiguration.getDefaultLoadFlowProvider());
            }
        });
    }

    @CEntryPoint(name = "freeLoadFlowComponentResultPointer")
    public static void freeLoadFlowComponentResultPointer(IsolateThread thread, ArrayPointer<LoadFlowComponentResultPointer> componentResultArrayPtr,
                                                          PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < componentResultArrayPtr.getLength(); i++) {
                    LoadFlowComponentResultPointer loadFlowComponentResultPointer = componentResultArrayPtr.getPtr().addressOf(i);
                    UnmanagedMemory.free(loadFlowComponentResultPointer.getStatusText());
                    UnmanagedMemory.free(loadFlowComponentResultPointer.getReferenceBusId());
                    for (int j = 0; j < loadFlowComponentResultPointer.slackBusResults().getLength(); j++) {
                        PyPowsyblApiHeader.SlackBusResultPointer slackBusResultPointer = loadFlowComponentResultPointer.slackBusResults().getPtr().addressOf(j);
                        UnmanagedMemory.free(slackBusResultPointer.getId());
                    }
                }
                freeArrayPointer(componentResultArrayPtr);
            }
        });
    }

    @CEntryPoint(name = "getLoadFlowProviderNames")
    public static ArrayPointer<CCharPointerPointer> getLoadFlowProviderNames(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<CCharPointerPointer> get() {
                return createCharPtrArray(LoadFlowProvider.findAll()
                        .stream().map(LoadFlowProvider::getName).collect(Collectors.toList()));
            }
        });
    }

    @CEntryPoint(name = "runLoadFlow")
    public static ArrayPointer<LoadFlowComponentResultPointer> runLoadFlow(IsolateThread thread, ObjectHandle networkHandle, boolean dc,
                                                                           LoadFlowParametersPointer loadFlowParametersPtr,
                                                                           CCharPointer provider, ObjectHandle reportNodeHandle,
                                                                           PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return Util.doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<LoadFlowComponentResultPointer> get() {
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                String providerStr = CTypeUtil.toString(provider);
                LoadFlowProvider loadFlowProvider = LoadFlowCUtils.getLoadFlowProvider(providerStr);
                logger().info("loadflow provider used is : {}", loadFlowProvider.getName());

                LoadFlowParameters parameters = LoadFlowCUtils.createLoadFlowParameters(dc, loadFlowParametersPtr, loadFlowProvider);
                LoadFlow.Runner runner = new LoadFlow.Runner(loadFlowProvider);
                ReportNode reportNode = ReportCUtils.getReportNode(reportNodeHandle);
                LoadFlowResult result = runner.run(network, network.getVariantManager().getWorkingVariantId(),
                        CommonObjects.getComputationManager(), parameters, reportNode);
                return createLoadFlowComponentResultArrayPointer(result);
            }
        });
    }

    public interface LoadFlowResultCallback extends CFunctionPointer {
        @InvokeCFunctionPointer
        void invoke(ArrayPointer<PyPowsyblApiHeader.LoadFlowComponentResultPointer> resultsPtr, VoidPointer resultFuturePtr);
    }

    public interface LoadFlowExceptionCallback extends CFunctionPointer {
        @InvokeCFunctionPointer
        void invoke(CCharPointer message, VoidPointer resultFuturePtr);
    }

    @CEntryPoint(name = "runLoadFlowAsync")
    public static void runLoadFlowAsync(IsolateThread thread,
                                        ObjectHandle networkHandle,
                                        CCharPointer variantId,
                                        boolean dc,
                                        LoadFlowParametersPointer loadFlowParametersPtr,
                                        CCharPointer provider, ObjectHandle reportNodeHandle,
                                        LoadFlowResultCallback loadFlowResultCallback,
                                        LoadFlowExceptionCallback loadFlowExceptionCallback,
                                        VoidPointer resultFuturePtr,
                                        PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        Util.doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                String variantIdStr = CTypeUtil.toString(variantId);
                String providerStr = CTypeUtil.toString(provider);
                LoadFlowProvider loadFlowProvider = LoadFlowCUtils.getLoadFlowProvider(providerStr);
                logger().debug("loadflow provider used is : {}", loadFlowProvider.getName());

                LoadFlowParameters parameters = LoadFlowCUtils.createLoadFlowParameters(dc, loadFlowParametersPtr, loadFlowProvider);
                LoadFlow.Runner runner = new LoadFlow.Runner(loadFlowProvider);
                ReportNode reportNode = ReportCUtils.getReportNode(reportNodeHandle);
                runner.runAsync(network, variantIdStr,
                                CommonObjects.getComputationManager(), parameters, reportNode)
                        .whenComplete(new BiConsumer<>() {
                            @Override
                            public void accept(LoadFlowResult loadFlowResult, Throwable throwable) {
                                if (throwable != null) {
                                    var messagePtr = CTypeUtil.toCharPtr(Util.getNonNullMessage(throwable));
                                    loadFlowExceptionCallback.invoke(messagePtr, resultFuturePtr);
                                } else {
                                    var resultsPtr = createLoadFlowComponentResultArrayPointer(loadFlowResult);
                                    loadFlowResultCallback.invoke(resultsPtr, resultFuturePtr);
                                }
                            }
                        });
            }
        });
    }

    @CEntryPoint(name = "createLoadFlowParameters")
    public static LoadFlowParametersPointer createLoadFlowParameters(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public LoadFlowParametersPointer get() {
                return convertToLoadFlowParametersPointer(LoadFlowCUtils.createLoadFlowParameters());
            }
        });
    }

    @CEntryPoint(name = "createLoadFlowParametersFromJson")
    public static LoadFlowParametersPointer createLoadFlowParametersFromJson(IsolateThread thread, CCharPointer parametersJsonPtr,
                                                                             PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public LoadFlowParametersPointer get() throws IOException {
                String parametersJson = CTypeUtil.toString(parametersJsonPtr);
                try (InputStream is = new ByteArrayInputStream(parametersJson.getBytes(StandardCharsets.UTF_8))) {
                    return convertToLoadFlowParametersPointer(JsonLoadFlowParameters.read(is));
                }
            }
        });
    }

    @CEntryPoint(name = "writeLoadFlowParametersToJson")
    public static CCharPointer writeLoadFlowParametersToJson(IsolateThread thread, LoadFlowParametersPointer loadFlowParametersPtr,
                                                             PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public CCharPointer get() {
                String providerName = PyPowsyblConfiguration.getDefaultLoadFlowProvider();
                LoadFlowParameters parameters = LoadFlowCUtils.createLoadFlowParameters(false, loadFlowParametersPtr, providerName);
                try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    JsonLoadFlowParameters.write(parameters, os);
                    os.flush();
                    String parametersJson = os.toString(StandardCharsets.UTF_8);
                    return CTypeUtil.toCharPtr(parametersJson);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
    }

    @CEntryPoint(name = "freeLoadFlowParameters")
    public static void freeLoadFlowParameters(IsolateThread thread, LoadFlowParametersPointer loadFlowParametersPtr,
                                              PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                freeLoadFlowParametersPointer(loadFlowParametersPtr);
            }
        });
    }

    public static void freeLoadFlowParametersPointer(LoadFlowParametersPointer loadFlowParametersPtr) {
        LoadFlowCUtils.freeLoadFlowParametersContent(loadFlowParametersPtr);
        UnmanagedMemory.free(loadFlowParametersPtr);
    }

    public static ArrayPointer<LoadFlowComponentResultPointer> createLoadFlowComponentResultArrayPointer(LoadFlowResult result) {
        List<LoadFlowResult.ComponentResult> componentResults = result.getComponentResults();
        LoadFlowComponentResultPointer componentResultPtr = UnmanagedMemory.calloc(componentResults.size() * SizeOf.get(LoadFlowComponentResultPointer.class));
        for (int index = 0; index < componentResults.size(); index++) {
            LoadFlowResult.ComponentResult componentResult = componentResults.get(index);
            LoadFlowComponentResultPointer ptr = componentResultPtr.addressOf(index);
            ptr.setConnectedComponentNum(componentResult.getConnectedComponentNum());
            ptr.setSynchronousComponentNum(componentResult.getSynchronousComponentNum());
            ptr.setStatus(componentResult.getStatus().ordinal());
            ptr.setStatusText(CTypeUtil.toCharPtr(componentResult.getStatusText()));
            ptr.setIterationCount(componentResult.getIterationCount());
            ptr.setReferenceBusId(CTypeUtil.toCharPtr(componentResult.getReferenceBusId()));
            createSlackBusResultPtr(ptr, componentResult.getSlackBusResults());
            ptr.setDistributedActivePower(componentResult.getDistributedActivePower());
        }
        return allocArrayPointer(componentResultPtr, componentResults.size());
    }

    private static void createSlackBusResultPtr(LoadFlowComponentResultPointer ptr, List<LoadFlowResult.SlackBusResult> slackBusResults) {
        PyPowsyblApiHeader.SlackBusResultPointer slackBusResultPointer = UnmanagedMemory.calloc(slackBusResults.size() * SizeOf.get(PyPowsyblApiHeader.SlackBusResultPointer.class));
        for (int i = 0; i < slackBusResults.size(); i++) {
            LoadFlowResult.SlackBusResult slackBusResult = slackBusResults.get(i);
            PyPowsyblApiHeader.SlackBusResultPointer slackBusResultPtrPlus = slackBusResultPointer.addressOf(i);
            slackBusResultPtrPlus.setId(CTypeUtil.toCharPtr(slackBusResult.getId()));
            slackBusResultPtrPlus.setActivePowerMismatch(slackBusResult.getActivePowerMismatch());
        }
        ptr.slackBusResults().setLength(slackBusResults.size());
        ptr.slackBusResults().setPtr(slackBusResultPointer);
    }

    public static void copyToCLoadFlowParameters(LoadFlowParameters parameters, LoadFlowParametersPointer cParameters) {
        cParameters.setVoltageInitMode(parameters.getVoltageInitMode().ordinal());
        cParameters.setTransformerVoltageControlOn(parameters.isTransformerVoltageControlOn());
        cParameters.setUseReactiveLimits(parameters.isUseReactiveLimits());
        cParameters.setPhaseShifterRegulationOn(parameters.isPhaseShifterRegulationOn());
        cParameters.setTwtSplitShuntAdmittance(parameters.isTwtSplitShuntAdmittance());
        cParameters.setShuntCompensatorVoltageControlOn(parameters.isShuntCompensatorVoltageControlOn());
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
        cParameters.setHvdcAcEmulation(parameters.isHvdcAcEmulation());
        cParameters.setDcPowerFactor(parameters.getDcPowerFactor());
        cParameters.getProviderParameters().setProviderParametersValuesCount(0);
        cParameters.getProviderParameters().setProviderParametersKeysCount(0);
    }

    public static LoadFlowParametersPointer convertToLoadFlowParametersPointer(LoadFlowParameters parameters) {
        LoadFlowParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(LoadFlowParametersPointer.class));
        copyToCLoadFlowParameters(parameters, paramsPtr);
        return paramsPtr;
    }

    @CEntryPoint(name = "getLoadFlowProviderParametersNames")
    public static ArrayPointer<CCharPointerPointer> getProviderParametersNames(IsolateThread thread, CCharPointer provider, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<CCharPointerPointer> get() {
                String providerStr = CTypeUtil.toString(provider);
                return Util.createCharPtrArray(LoadFlowCUtils.getLoadFlowProvider(providerStr).getSpecificParameters().stream().map(Parameter::getName).collect(Collectors.toList()));
            }
        });
    }

    @CEntryPoint(name = "createLoadFlowProviderParametersSeriesArray")
    static ArrayPointer<SeriesPointer> createLoadFlowProviderParametersSeriesArray(IsolateThread thread, CCharPointer providerNamePtr,
                                                                                   PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<SeriesPointer> get() {
                String providerName = CTypeUtil.toString(providerNamePtr);
                LoadFlowProvider provider = LoadFlowCUtils.getLoadFlowProvider(providerName);
                return Dataframes.createCDataframe(Util.SPECIFIC_PARAMETERS_MAPPER, provider.getSpecificParameters());
            }
        });
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(CommonCFunctions.class);
    }
}
