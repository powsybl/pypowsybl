/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.rao;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.DataframeFilter;
import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracCreationContext;
import com.powsybl.openrao.data.crac.api.RaUsageLimits;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.api.parameters.JsonCracCreationParameters;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.LoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.MnecParameters;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.RelativeMarginsParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.FastRaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.OpenRaoSearchTreeParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoCostlyMinMarginParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoLoopFlowParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoMnecParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SearchTreeRaoRelativeMarginsParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.SecondPreventiveRaoParameters;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.ExceptionHandlerPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.RaoComputationStatus;
import com.powsybl.python.commons.PyPowsyblApiHeader.RaoParametersPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.SeriesPointer;
import com.powsybl.python.commons.Util;
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
import org.graalvm.nativeimage.c.type.CTypeConversion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import static com.powsybl.python.commons.CTypeUtil.arrayPointerToStringListList;
import static com.powsybl.python.commons.CTypeUtil.freeNestedArrayPointer;
import static com.powsybl.python.commons.CTypeUtil.stringListListToArrayPointer;
import static com.powsybl.python.commons.Util.PointerProvider;
import static com.powsybl.python.commons.Util.binaryBufferToBytes;
import static com.powsybl.python.commons.Util.doCatch;
import static com.powsybl.python.commons.Util.freeProviderParameters;
import static com.powsybl.python.loadflow.LoadFlowCUtils.createLoadFlowParameters;
import static com.powsybl.python.rao.RaoDataframes.cracAngleCnecs;
import static com.powsybl.python.rao.RaoDataframes.cracBoundaryLineActions;
import static com.powsybl.python.rao.RaoDataframes.cracContingencies;
import static com.powsybl.python.rao.RaoDataframes.cracContingencyElements;
import static com.powsybl.python.rao.RaoDataframes.cracCounterTradeRangeActions;
import static com.powsybl.python.rao.RaoDataframes.cracFlowCnecs;
import static com.powsybl.python.rao.RaoDataframes.cracGeneratorActions;
import static com.powsybl.python.rao.RaoDataframes.cracInjectionRaElements;
import static com.powsybl.python.rao.RaoDataframes.cracInjectionRangeActions;
import static com.powsybl.python.rao.RaoDataframes.cracInstantsMapper;
import static com.powsybl.python.rao.RaoDataframes.cracLoadActions;
import static com.powsybl.python.rao.RaoDataframes.cracNetworkActions;
import static com.powsybl.python.rao.RaoDataframes.cracOnConstraintUsageRules;
import static com.powsybl.python.rao.RaoDataframes.cracOnContingencyStateUsageRules;
import static com.powsybl.python.rao.RaoDataframes.cracOnFlowConstraintInCountryUsageRules;
import static com.powsybl.python.rao.RaoDataframes.cracOnInstantUsageRules;
import static com.powsybl.python.rao.RaoDataframes.cracPerTsoUsageLimits;
import static com.powsybl.python.rao.RaoDataframes.cracPstTapPositionActions;
import static com.powsybl.python.rao.RaoDataframes.cracRangeActions;
import static com.powsybl.python.rao.RaoDataframes.cracRanges;
import static com.powsybl.python.rao.RaoDataframes.cracRemedialActionsUsageLimits;
import static com.powsybl.python.rao.RaoDataframes.cracShuntCompensatorPositionActions;
import static com.powsybl.python.rao.RaoDataframes.cracSwitchActions;
import static com.powsybl.python.rao.RaoDataframes.cracSwitchPairs;
import static com.powsybl.python.rao.RaoDataframes.cracTerminalConnectionActions;
import static com.powsybl.python.rao.RaoDataframes.cracThresholds;
import static com.powsybl.python.rao.RaoDataframes.cracVoltageCnecs;
import static com.powsybl.python.rao.RaoDataframes.createVirtualCostResultMapper;
import static com.powsybl.python.sensitivity.SensitivityAnalysisCFunctions.convertToSensitivityAnalysisParametersPointer;
import static com.powsybl.python.sensitivity.SensitivityAnalysisCFunctions.getProvider;
import static com.powsybl.python.sensitivity.SensitivityAnalysisCUtils.createSensitivityAnalysisParameters;

/**
 * @author Bertrand Rix {@literal <bertrand.rix at artelys.com>}
 */
@CContext(Directives.class)
public final class RaoCFunctions {

    private RaoCFunctions() {
    }

    @CEntryPoint(name = "createRao")
    public static ObjectHandle createRao(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ObjectHandle get() {
                return ObjectHandles.getGlobal().create(new RaoContext());
            }
        });
    }

    @CEntryPoint(name = "createDefaultRaoParameters")
    public static ObjectHandle createDefaultRaoParameters(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new RaoParameters()));
    }

    @CEntryPoint(name = "loadRaoParameters")
    public static RaoParametersPointer loadRaoParameters(IsolateThread thread, CCharPointer parametersBuffer, int paramersBufferSize, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public RaoParametersPointer get() throws IOException {
                ByteBuffer bufferParameters = CTypeConversion.asByteBuffer(parametersBuffer, paramersBufferSize);
                InputStream streamedParameters = new ByteArrayInputStream(binaryBufferToBytes(bufferParameters));
                return convertToRaoParametersPointer(JsonRaoParameters.read(streamedParameters));
            }
        });
    }

    @CEntryPoint(name = "serializeRaoParameters")
    public static ArrayPointer<CCharPointer> serializeRaoParameters(IsolateThread thread, RaoParametersPointer raoParameters,
                                                                    ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<CCharPointer> get() throws IOException {
                RaoParameters parameters = convertToRaoParameters(raoParameters);
                try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    JsonRaoParameters.write(parameters, output);
                    return Util.createByteArray(output.toByteArray());
                } catch (IOException e) {
                    throw new PowsyblException("Could not serialize rao parameters : " + e.getMessage());
                }
            }
        });
    }

    @CEntryPoint(name = "loadCracWithParameters")
    public static ObjectHandle loadCracWithParameters(IsolateThread thread, ObjectHandle networkHandle, CCharPointer cracFilePtr, CCharPointer creationParametersFilePtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ObjectHandle get() throws IOException {
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                String cracFile = CTypeUtil.toString(cracFilePtr);
                String creationParametersFile = CTypeUtil.toString(creationParametersFilePtr);
                return ObjectHandles.getGlobal().create(readCracWithParameters(network, cracFile, creationParametersFile));
            }
        });
    }

    public static Crac readCracWithParameters(Network network, String cracFile, String creationParametersFile) {
        try (FileInputStream cracInputStream = new FileInputStream(cracFile)) {
            CracCreationParameters cracCreationParameters = JsonCracCreationParameters.read(Path.of(creationParametersFile));
            CracCreationContext cracCreationContext = Crac.readWithContext(cracFile, cracInputStream, network, cracCreationParameters);
            Crac crac = cracCreationContext.getCrac();
            if (crac != null) {
                return crac;
            } else {
                throw new PowsyblException("Error while reading CRAC file, please enable detailed log for more information.");
            }
        } catch (IOException e) {
            throw new PowsyblException("Error while reading CRAC file, please enable detailed log for more information: %s.".formatted(e.getMessage()));
        }
    }

    @CEntryPoint(name = "loadCracBufferedSource")
    public static ObjectHandle loadCracBufferedSource(IsolateThread thread, ObjectHandle networkHandle, CCharPointer cracBuffer, int cracBufferSize, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ObjectHandle get() throws IOException {
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                return ObjectHandles.getGlobal().create(createCrac(network, cracBuffer, cracBufferSize));
            }
        });
    }

    public static Crac createCrac(Network network, CCharPointer cracBuffer, int cracBufferSize) {
        ByteBuffer bufferCrac = CTypeConversion.asByteBuffer(cracBuffer, cracBufferSize);
        InputStream streamedCrac = new ByteArrayInputStream(binaryBufferToBytes(bufferCrac));
        try {
            Crac crac = Crac.read("crac.json", streamedCrac, network);
            if (crac != null) {
                return crac;
            } else {
                throw new PowsyblException("Error while reading json crac, please enable detailed log for more information.");
            }
        } catch (IOException e) {
            throw new PowsyblException("Cannot read provided crac data : " + e.getMessage());
        }
    }

    @CEntryPoint(name = "setLoopFlowGlsk")
    public static void setLoopFlowGlsk(IsolateThread thread, ObjectHandle raoContextHandle, ObjectHandle glsk, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);
                raoContext.setLoopFlowGlsk(ObjectHandles.getGlobal().get(glsk));
            }
        });
    }

    @CEntryPoint(name = "setMonitoringGlsk")
    public static void setMonitoringGlsk(IsolateThread thread, ObjectHandle raoContextHandle, ObjectHandle glsk, ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);
                raoContext.setMonitoringGlsk(ObjectHandles.getGlobal().get(glsk));
            }
        });
    }

    @CEntryPoint(name = "loadGlskBufferedSource")
    public static ObjectHandle loadGlskBufferedSource(IsolateThread thread, CCharPointer glsksBuffer, int glsksBufferSize, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ObjectHandle get() {
                return ObjectHandles.getGlobal().create(createGlskDocument(glsksBuffer, glsksBufferSize));
            }
        });
    }

    public static GlskDocument createGlskDocument(CCharPointer glsksBuffer, int glsksBufferSize) {
        ByteBuffer bufferGlsks = CTypeConversion.asByteBuffer(glsksBuffer, glsksBufferSize);
        InputStream glsksStream = new ByteArrayInputStream(binaryBufferToBytes(bufferGlsks));
        return GlskDocumentImporters.importGlsk(glsksStream);
    }

    @CEntryPoint(name = "loadResultFromBufferedSource")
    public static ObjectHandle loadResultFromBufferedSource(IsolateThread thread, ObjectHandle cracHandle, CCharPointer resultBuffer, int resultBufferSize, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ObjectHandle get() {
                Crac crac = ObjectHandles.getGlobal().get(cracHandle);
                ByteBuffer buffer = CTypeConversion.asByteBuffer(resultBuffer, resultBufferSize);

                InputStream resultStream = new ByteArrayInputStream(binaryBufferToBytes(buffer));
                try {
                    return ObjectHandles.getGlobal().create(RaoResult.read(resultStream, crac));
                } catch (IOException e) {
                    throw new PowsyblException("Cannot read rao result " + e.getMessage());
                }
            }
        });
    }

    @CEntryPoint(name = "runRao")
    public static ObjectHandle runRao(IsolateThread thread, ObjectHandle networkHandle, ObjectHandle cracHandle, ObjectHandle raoContextHandle,
                                      RaoParametersPointer parametersPointer, CCharPointer raoProviderPtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ObjectHandle get() throws IOException {
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);
                RaoParameters raoParameters = convertToRaoParameters(parametersPointer);
                raoContext.setCrac(ObjectHandles.getGlobal().get(cracHandle));
                String raoProvider = CTypeUtil.toString(raoProviderPtr);
                return ObjectHandles.getGlobal().create(raoContext.run(network, raoParameters, raoProvider));
            }
        });
    }

    @CEntryPoint(name = "runVoltageMonitoring")
    public static ObjectHandle runVoltageMonitoring(IsolateThread thread, ObjectHandle networkHandle, ObjectHandle resultHandle, ObjectHandle cracHandle, ObjectHandle contextHandle,
                                                    PyPowsyblApiHeader.LoadFlowParametersPointer loadFlowParametersPtr,
                                                    CCharPointer provider, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ObjectHandle get() throws IOException {
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                RaoResult result = ObjectHandles.getGlobal().get(resultHandle);
                RaoContext raoContext = ObjectHandles.getGlobal().get(contextHandle);
                raoContext.setCrac(ObjectHandles.getGlobal().get(cracHandle));
                String providerStr = CTypeUtil.toString(provider);
                LoadFlowProvider loadFlowProvider = LoadFlowCUtils.getLoadFlowProvider(providerStr);
                LoadFlowParameters lfParameters = createLoadFlowParameters(loadFlowParametersPtr, loadFlowProvider);
                return ObjectHandles.getGlobal().create(raoContext.runVoltageMonitoring(network, result, providerStr, lfParameters));

            }
        });
    }

    @CEntryPoint(name = "runAngleMonitoring")
    public static ObjectHandle runAngleMonitoring(IsolateThread thread, ObjectHandle networkHandle, ObjectHandle resultHandle, ObjectHandle cracHandle, ObjectHandle contextHandle,
                                                  PyPowsyblApiHeader.LoadFlowParametersPointer loadFlowParametersPtr,
                                                  CCharPointer provider, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ObjectHandle get() throws IOException {
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                RaoResult result = ObjectHandles.getGlobal().get(resultHandle);
                RaoContext raoContext = ObjectHandles.getGlobal().get(contextHandle);
                raoContext.setCrac(ObjectHandles.getGlobal().get(cracHandle));
                String providerStr = CTypeUtil.toString(provider);
                LoadFlowProvider loadFlowProvider = LoadFlowCUtils.getLoadFlowProvider(providerStr);
                LoadFlowParameters lfParameters = createLoadFlowParameters(loadFlowParametersPtr, loadFlowProvider);
                return ObjectHandles.getGlobal().create(raoContext.runAngleMonitoring(network, result, providerStr, lfParameters));
            }
        });
    }

    @CEntryPoint(name = "serializeRaoResultsToBuffer")
    public static ArrayPointer<CCharPointer> serializeRaoResultsToBuffer(IsolateThread thread, ObjectHandle raoResultHandle,
                                                                         ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<CCharPointer> get() throws IOException {
                RaoResult raoResult = ObjectHandles.getGlobal().get(raoResultHandle);
                Crac crac = ObjectHandles.getGlobal().get(cracHandle);
                try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    Properties properties = new Properties();
                    properties.setProperty("rao-result.export.json.flows-in-amperes", "true");
                    properties.setProperty("rao-result.export.json.flows-in-megawatts", "true");
                    raoResult.write("JSON", crac, properties, output);
                    return Util.createByteArray(output.toByteArray());
                } catch (IOException e) {
                    throw new PowsyblException("Could not serialize rao results : " + e.getMessage());
                }
            }
        });
    }

    @CEntryPoint(name = "getRaoResultStatus")
    public static RaoComputationStatus getRaoResultStatus(IsolateThread thread, ObjectHandle raoResultHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new Supplier<>() {
            @Override
            public RaoComputationStatus get() {
                RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
                switch (result.getComputationStatus()) {
                    case DEFAULT -> {
                        return RaoComputationStatus.DEFAULT;
                    }
                    case FAILURE -> {
                        return RaoComputationStatus.FAILURE;
                    }
                    case PARTIAL_FAILURE -> {
                        return RaoComputationStatus.PARTIAL_FAILURE;
                    }
                    default ->
                        throw new PowsyblException("Unexpected computation status : " + result.getComputationStatus());
                }
            }
        });
    }

    @CEntryPoint(name = "getCrac")
    public static ObjectHandle getCrac(IsolateThread thread, ObjectHandle raoContextHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ObjectHandle get() throws IOException {
                RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);
                return ObjectHandles.getGlobal().create(raoContext.getCrac());
            }
        });
    }

    @CEntryPoint(name = "createRaoParameters")
    public static RaoParametersPointer createRaoParameters(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> convertToRaoParametersPointer(new RaoParameters()));
    }

    @CEntryPoint(name = "getFlowCnecResults")
    public static ArrayPointer<SeriesPointer> getFlowCnecResults(IsolateThread thread, ObjectHandle cracHandle, ObjectHandle raoResultHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<SeriesPointer> get() throws IOException {
                Crac crac = ObjectHandles.getGlobal().get(cracHandle);
                RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
                return Dataframes.createCDataframe(RaoDataframes.flowCnecMapper(), crac, new DataframeFilter(), result);
            }
        });
    }

    @CEntryPoint(name = "getAngleCnecResults")
    public static ArrayPointer<SeriesPointer> getAngleCnecResults(IsolateThread thread, ObjectHandle cracHandle, ObjectHandle raoResultHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<SeriesPointer> get() throws IOException {
                Crac crac = ObjectHandles.getGlobal().get(cracHandle);
                RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
                return Dataframes.createCDataframe(RaoDataframes.angleCnecMapper(), crac, new DataframeFilter(), result);
            }
        });
    }

    @CEntryPoint(name = "getVoltageCnecResults")
    public static ArrayPointer<SeriesPointer> getVoltageCnecResults(IsolateThread thread, ObjectHandle cracHandle, ObjectHandle raoResultHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<SeriesPointer> get() throws IOException {
                Crac crac = ObjectHandles.getGlobal().get(cracHandle);
                RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
                return Dataframes.createCDataframe(RaoDataframes.voltageCnecMapper(), crac, new DataframeFilter(), result);
            }
        });
    }

    @CEntryPoint(name = "getRemedialActionResults")
    public static ArrayPointer<SeriesPointer> getRemedialActionResults(IsolateThread thread, ObjectHandle cracHandle, ObjectHandle raoResultHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<SeriesPointer> get() throws IOException {
                Crac crac = ObjectHandles.getGlobal().get(cracHandle);
                RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
                return Dataframes.createCDataframe(RaoDataframes.remedialActionResultMapper(), crac, new DataframeFilter(), result);
            }
        });
    }

    @CEntryPoint(name = "getNetworkActionResults")
    public static ArrayPointer<SeriesPointer> getNetworkActionResults(IsolateThread thread, ObjectHandle cracHandle, ObjectHandle raoResultHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<SeriesPointer> get() throws IOException {
                Crac crac = ObjectHandles.getGlobal().get(cracHandle);
                RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
                return Dataframes.createCDataframe(RaoDataframes.networkActionResultMapper(), crac, new DataframeFilter(), result);

            }
        });
    }

    @CEntryPoint(name = "getPstRangeActionResults")
    public static ArrayPointer<SeriesPointer> getPstRangeActionResults(IsolateThread thread, ObjectHandle cracHandle, ObjectHandle raoResultHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<SeriesPointer> get() throws IOException {
                Crac crac = ObjectHandles.getGlobal().get(cracHandle);
                RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
                return Dataframes.createCDataframe(RaoDataframes.pstRangeActionResultMapper(), crac, new DataframeFilter(), result);
            }
        });
    }

    @CEntryPoint(name = "getRangeActionResults")
    public static ArrayPointer<SeriesPointer> getRangeActionResults(IsolateThread thread, ObjectHandle cracHandle, ObjectHandle raoResultHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<SeriesPointer> get() throws IOException {
                Crac crac = ObjectHandles.getGlobal().get(cracHandle);
                RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
                return Dataframes.createCDataframe(RaoDataframes.rangeActionResultMapper(), crac, new DataframeFilter(), result);
            }
        });
    }

    @CEntryPoint(name = "getCostResults")
    public static ArrayPointer<SeriesPointer> getCostResults(IsolateThread thread, ObjectHandle cracHandle, ObjectHandle raoResultHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<SeriesPointer> get() throws IOException {
                Crac crac = ObjectHandles.getGlobal().get(cracHandle);
                RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
                return Dataframes.createCDataframe(RaoDataframes.costResultMapper(), crac, new DataframeFilter(), result);

            }
        });
    }

    @CEntryPoint(name = "getVirtualCostNames")
    public static ArrayPointer<CCharPointerPointer> getVirtualCostNames(IsolateThread thread, ObjectHandle raoResultHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<CCharPointerPointer> get() throws IOException {
                RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
                return Util.createCharPtrArray(result.getVirtualCostNames().stream().toList());
            }
        });
    }

    @CEntryPoint(name = "getVirtualCostResults")
    public static ArrayPointer<SeriesPointer> getVirtualCostResults(IsolateThread thread, ObjectHandle cracHandle, ObjectHandle raoResultHandle, CCharPointer virtualCostNamePtr, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<SeriesPointer> get() throws IOException {
                Crac crac = ObjectHandles.getGlobal().get(cracHandle);
                RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
                String virtualCostName = CTypeUtil.toString(virtualCostNamePtr);
                DataframeMapper<Crac, RaoResult> virtualCostResultMapper = createVirtualCostResultMapper(virtualCostName);
                return Dataframes.createCDataframe(virtualCostResultMapper, crac, new DataframeFilter(), result);

            }
        });
    }

    @CEntryPoint(name = "freeRaoParameters")
    public static void freeRaoParameters(IsolateThread thread, RaoParametersPointer parametersPointer,
                                         ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {

                if (parametersPointer.getSearchTreeParameters()) {
                    // Top level string parameters are freed by toString call on c side
                    // Free predefined combinations
                    freeNestedArrayPointer(parametersPointer.getPredefinedCombinations());

                    // Free sensitivity parameters
                    LoadFlowCUtils.freeLoadFlowParametersContent(parametersPointer.getSensitivityParameters().getLoadFlowParameters());
                    UnmanagedMemory.free(parametersPointer.getSensitivityParameters());
                }

                // Free extensions
                freeProviderParameters(parametersPointer.getProviderParameters());

                // Free main pointer
                UnmanagedMemory.free(parametersPointer);
            }
        });
    }

    private static RaoParameters convertToRaoParameters(RaoParametersPointer paramPointer) {
        RaoParameters raoParameters = new RaoParameters();
        raoParameters.getObjectiveFunctionParameters().setType(
            ObjectiveFunctionParameters.ObjectiveFunctionType.values()[paramPointer.getObjectiveFunctionType()]);
        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(paramPointer.getEnforceCurativeSecurity());

        // Range action optimization parameters
        raoParameters.getRangeActionsOptimizationParameters().setPstRAMinImpactThreshold(paramPointer.getPstRaMinImpactThreshold());
        raoParameters.getRangeActionsOptimizationParameters().setHvdcRAMinImpactThreshold(paramPointer.getHvdcRaMinImpactThreshold());
        raoParameters.getRangeActionsOptimizationParameters().setInjectionRAMinImpactThreshold(paramPointer.getInjectionRaMinImpactThreshold());

        // Topo optimization parameters
        raoParameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(paramPointer.getRelativeMinImpactThreshold());
        raoParameters.getTopoOptimizationParameters().setAbsoluteMinImpactThreshold(paramPointer.getAbsoluteMinImpactThreshold());

        // Not optimized cnec parameters
        raoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCurativeCnecsForTsosWithoutCras(paramPointer.getDoNotOptimizeCurativeCnecsForTsosWithoutCras());

        Map<String, String> extensionData = getExtensionData(paramPointer);

        if (hasExtensionData(extensionData, RaoUtils.MNEC_EXT_PREFIX)) {
            MnecParameters mnecExt = RaoUtils.buildMnecParametersExtension(extensionData);
            raoParameters.setMnecParameters(mnecExt);
        }
        if (hasExtensionData(extensionData, RaoUtils.RELATIVE_MARGIN_EXT_PREFIX)) {
            RelativeMarginsParameters relativeMargingExt = RaoUtils.buildRelativeMarginsParametersExtension(extensionData);
            raoParameters.setRelativeMarginsParameters(relativeMargingExt);
        }
        if (hasExtensionData(extensionData, RaoUtils.LOOP_FLOW_EXT_PREFIX)) {
            LoopFlowParameters loopFlowExt = RaoUtils.buildLoopFlowParametersExtension(extensionData);
            raoParameters.setLoopFlowParameters(loopFlowExt);
        }

        if (paramPointer.getSearchTreeParameters()) {
            raoParameters.addExtension(OpenRaoSearchTreeParameters.class, new OpenRaoSearchTreeParameters());
            OpenRaoSearchTreeParameters searchTreeParameters = raoParameters.getExtension(OpenRaoSearchTreeParameters.class);

            searchTreeParameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(paramPointer.getCurativeMinObjImprovement());

            // Range action optimization solver
            searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver()
                .setSolver(SearchTreeRaoRangeActionsOptimizationParameters.Solver.values()[paramPointer.getSolver()]);
            searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver()
                .setRelativeMipGap(paramPointer.getRelativeMipGap());
            searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver()
                .setSolverSpecificParameters(CTypeUtil.toString(paramPointer.getSolverSpecificParameters()));

            searchTreeParameters.getRangeActionsOptimizationParameters().setMaxMipIterations(paramPointer.getMaxMipIterations());
            searchTreeParameters.getRangeActionsOptimizationParameters().setPstSensitivityThreshold(paramPointer.getPstSensitivityThreshold());
            searchTreeParameters.getRangeActionsOptimizationParameters().setHvdcSensitivityThreshold(paramPointer.getHvdcSensitivityThreshold());
            searchTreeParameters.getRangeActionsOptimizationParameters().setPstModel(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.values()[paramPointer.getPstModel()]);
            searchTreeParameters.getRangeActionsOptimizationParameters().setInjectionRaSensitivityThreshold(paramPointer.getInjectionRaSensitivityThreshold());
            searchTreeParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.values()[paramPointer.getRaRangeShrinking()]);

            searchTreeParameters.getTopoOptimizationParameters().setMaxPreventiveSearchTreeDepth(paramPointer.getMaxPreventiveSearchTreeDepth());
            searchTreeParameters.getTopoOptimizationParameters().setMaxCurativeSearchTreeDepth(paramPointer.getMaxCurativeSearchTreeDepth());
            searchTreeParameters.getTopoOptimizationParameters().setPredefinedCombinations(arrayPointerToStringListList(paramPointer.getPredefinedCombinations()));
            searchTreeParameters.getTopoOptimizationParameters().setSkipActionsFarFromMostLimitingElement(paramPointer.getSkipActionsFarFromMostLimitingElement());
            searchTreeParameters.getTopoOptimizationParameters().setMaxNumberOfBoundariesForSkippingActions(paramPointer.getMaxNumberOfBoundariesForSkippingActions());

            // Multithreading parameters
            searchTreeParameters.getMultithreadingParameters().setAvailableCPUs(paramPointer.getAvailableCpus());

            // Second preventive parameters
            searchTreeParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.values()[paramPointer.getExecutionCondition()]);
            searchTreeParameters.getSecondPreventiveRaoParameters().setHintFromFirstPreventiveRao(paramPointer.getHintFromFirstPreventiveRao());

            // Load flow and sensitivity providers
            searchTreeParameters.getLoadFlowAndSensitivityParameters().setLoadFlowProvider(CTypeUtil.toString(paramPointer.getLoadFlowProvider()));
            searchTreeParameters.getLoadFlowAndSensitivityParameters().setSensitivityProvider(CTypeUtil.toString(paramPointer.getSensitivityProvider()));
            searchTreeParameters.getLoadFlowAndSensitivityParameters().setSensitivityFailureOvercost(paramPointer.getSensitivityFailureOvercost());
            searchTreeParameters.getLoadFlowAndSensitivityParameters().setSensitivityWithLoadFlowParameters(createSensitivityAnalysisParameters(
                paramPointer.getSensitivityParameters(),
                getProvider(CTypeUtil.toString(paramPointer.getSensitivityProvider()))));

            if (hasExtensionData(extensionData, RaoUtils.MNEC_ST_EXT_PREFIX)) {
                SearchTreeRaoMnecParameters searchTreeMnecExt = RaoUtils.buildMnecSearchTreeParametersExtension(extensionData);
                searchTreeParameters.setMnecParameters(searchTreeMnecExt);
            }
            if (hasExtensionData(extensionData, RaoUtils.RELATIVE_MARGIN_ST_EXT_PREFIX)) {
                SearchTreeRaoRelativeMarginsParameters searchTreeRelativeMarginsParameters = RaoUtils.buildRelativeMarginsSearchTreeParametersExtension(extensionData);
                searchTreeParameters.setRelativeMarginsParameters(searchTreeRelativeMarginsParameters);
            }
            if (hasExtensionData(extensionData, RaoUtils.LOOP_FLOW_ST_EXT_PREFIX)) {
                SearchTreeRaoLoopFlowParameters searchTreeLoopFlowExt = RaoUtils.buildLoopFlowSearchTreeParametersExtension(extensionData);
                searchTreeParameters.setLoopFlowParameters(searchTreeLoopFlowExt);
            }
            if (hasExtensionData(extensionData, RaoUtils.COSTLY_MIN_MARGIN_ST_EXT_PREFIX)) {
                SearchTreeRaoCostlyMinMarginParameters searchTreeCostlyMinMarginExt = RaoUtils.buildCostlyMinMarginSearchTreeParametersExtension(extensionData);
                searchTreeParameters.setMinMarginsParameters(searchTreeCostlyMinMarginExt);
            }
        }

        // Fast Rao Extension
        if (paramPointer.getFastRaoExt()) {
            FastRaoParameters fastRaoParameters = new FastRaoParameters();
            fastRaoParameters.setNumberOfCnecsToAdd(paramPointer.getNumberOfCnecsToAdd());
            fastRaoParameters.setAddUnsecureCnecs(paramPointer.getAddUnsecureCnecs());
            fastRaoParameters.setMarginLimit(paramPointer.getMarginLimit());
            raoParameters.addExtension(FastRaoParameters.class, fastRaoParameters);
        }
        return raoParameters;
    }

    private static RaoParametersPointer convertToRaoParametersPointer(RaoParameters parameters) {
        RaoParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(RaoParametersPointer.class));

        // Objective function parameters
        paramsPtr.setObjectiveFunctionType(parameters.getObjectiveFunctionParameters().getType().ordinal());
        paramsPtr.setEnforceCurativeSecurity(parameters.getObjectiveFunctionParameters().getEnforceCurativeSecurity());

        // Range action optimization parameters
        paramsPtr.setPstRaMinImpactThreshold(parameters.getRangeActionsOptimizationParameters().getPstRAMinImpactThreshold());
        paramsPtr.setHvdcRaMinImpactThreshold(parameters.getRangeActionsOptimizationParameters().getHvdcRAMinImpactThreshold());
        paramsPtr.setInjectionRaMinImpactThreshold(parameters.getRangeActionsOptimizationParameters().getInjectionRAMinImpactThreshold());

        // Topo optimization parameters
        paramsPtr.setRelativeMinImpactThreshold(parameters.getTopoOptimizationParameters().getRelativeMinImpactThreshold());
        paramsPtr.setAbsoluteMinImpactThreshold(parameters.getTopoOptimizationParameters().getAbsoluteMinImpactThreshold());

        // Not optimized cnec parameters
        paramsPtr.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(parameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCurativeCnecsForTsosWithoutCras());

        OpenRaoSearchTreeParameters searchTreeParameters = parameters.getExtension(OpenRaoSearchTreeParameters.class);
        if (searchTreeParameters != null) {
            paramsPtr.setSearchTreeParameters(true);
            paramsPtr.setCurativeMinObjImprovement(searchTreeParameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement());

            // Range action optimization solver
            paramsPtr.setSolver(searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getSolver().ordinal());
            paramsPtr.setRelativeMipGap(searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getRelativeMipGap());
            paramsPtr.setSolverSpecificParameters(CTypeUtil.toCharPtr(searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getSolverSpecificParameters()));

            paramsPtr.setMaxMipIterations(searchTreeParameters.getRangeActionsOptimizationParameters().getMaxMipIterations());
            paramsPtr.setPstSensitivityThreshold(searchTreeParameters.getRangeActionsOptimizationParameters().getPstSensitivityThreshold());
            paramsPtr.setHvdcSensitivityThreshold(searchTreeParameters.getRangeActionsOptimizationParameters().getHvdcSensitivityThreshold());
            paramsPtr.setPstModel(searchTreeParameters.getRangeActionsOptimizationParameters().getPstModel().ordinal());
            paramsPtr.setInjectionRaSensitivityThreshold(searchTreeParameters.getRangeActionsOptimizationParameters().getInjectionRaSensitivityThreshold());
            paramsPtr.setRaRangeShrinking(searchTreeParameters.getRangeActionsOptimizationParameters().getRaRangeShrinking().ordinal());

            paramsPtr.setMaxPreventiveSearchTreeDepth(searchTreeParameters.getTopoOptimizationParameters().getMaxPreventiveSearchTreeDepth());
            paramsPtr.setMaxCurativeSearchTreeDepth(searchTreeParameters.getTopoOptimizationParameters().getMaxCurativeSearchTreeDepth());
            paramsPtr.setSkipActionsFarFromMostLimitingElement(searchTreeParameters.getTopoOptimizationParameters().getSkipActionsFarFromMostLimitingElement());
            paramsPtr.setMaxNumberOfBoundariesForSkippingActions(searchTreeParameters.getTopoOptimizationParameters().getMaxNumberOfBoundariesForSkippingActions());

            stringListListToArrayPointer(paramsPtr.getPredefinedCombinations(), searchTreeParameters.getTopoOptimizationParameters().getPredefinedCombinations());

            // Multithreading parameters
            paramsPtr.setAvailableCpus(searchTreeParameters.getMultithreadingParameters().getAvailableCPUs());

            // Second preventive parameters
            paramsPtr.setExecutionCondition(searchTreeParameters.getSecondPreventiveRaoParameters().getExecutionCondition().ordinal());
            paramsPtr.setHintFromFirstPreventiveRao(searchTreeParameters.getSecondPreventiveRaoParameters().getHintFromFirstPreventiveRao());

            // Load flow and sensitivity providers
            paramsPtr.setLoadFlowProvider(CTypeUtil.toCharPtr(searchTreeParameters.getLoadFlowAndSensitivityParameters().getLoadFlowProvider()));
            paramsPtr.setSensitivityProvider(CTypeUtil.toCharPtr(searchTreeParameters.getLoadFlowAndSensitivityParameters().getSensitivityProvider()));
            paramsPtr.setSensitivityParameters(
                convertToSensitivityAnalysisParametersPointer(
                    searchTreeParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters(),
                    searchTreeParameters.getLoadFlowAndSensitivityParameters().getLoadFlowProvider()));
            paramsPtr.setSensitivityFailureOvercost(searchTreeParameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost());
        } else {
            paramsPtr.setSearchTreeParameters(false);
        }

        convertExtensionData(parameters, paramsPtr);

        // Fast Rao parameters extension
        FastRaoParameters fastRaoParameters = parameters.getExtension(FastRaoParameters.class);
        if (fastRaoParameters != null) {
            paramsPtr.setFastRaoExt(true);
            paramsPtr.setNumberOfCnecsToAdd(fastRaoParameters.getNumberOfCnecsToAdd());
            paramsPtr.setAddUnsecureCnecs(fastRaoParameters.getAddUnsecureCnecs());
            paramsPtr.setMarginLimit(fastRaoParameters.getMarginLimit());
        } else {
            paramsPtr.setFastRaoExt(false);
        }
        return paramsPtr;
    }

    private static Map<String, String> getExtensionData(RaoParametersPointer parameterPointer) {
        return CTypeUtil.toStringMap(parameterPointer.getProviderParameters().getProviderParametersKeys(),
            parameterPointer.getProviderParameters().getProviderParametersKeysCount(),
            parameterPointer.getProviderParameters().getProviderParametersValues(),
            parameterPointer.getProviderParameters().getProviderParametersValuesCount());
    }

    private static boolean hasExtensionData(Map<String, String> data, String prefix) {
        for (String key : data.keySet()) {
            if (key.contains(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static void convertExtensionData(RaoParameters parameters, RaoParametersPointer parameterPointer) {
        Map<String, String> extensionData = new HashMap<>();

        parameters.getMnecParameters().ifPresent(
            mnecParam -> extensionData.putAll(RaoUtils.mnecParametersExtensionToMap(mnecParam)));
        parameters.getRelativeMarginsParameters().ifPresent(
            relativeMarginsParameters -> extensionData.putAll(RaoUtils.relativeMarginsParametersExtensionToMap(relativeMarginsParameters)));
        parameters.getLoopFlowParameters().ifPresent(
            loopFlowParameters -> extensionData.putAll(RaoUtils.loopFlowParametersExtensionToMap(loopFlowParameters)));

        OpenRaoSearchTreeParameters searchTreeParameters = parameters.getExtension(OpenRaoSearchTreeParameters.class);
        if (searchTreeParameters != null) {
            searchTreeParameters.getMnecParameters().ifPresent(
                mnecParam -> extensionData.putAll(RaoUtils.mnecParametersExtensionToMap(mnecParam)));
            searchTreeParameters.getRelativeMarginsParameters().ifPresent(
                relativeMarginsParameters -> extensionData.putAll(RaoUtils.relativeMarginsParametersExtensionToMap(relativeMarginsParameters)));
            searchTreeParameters.getLoopFlowParameters().ifPresent(
                loopFlowParameters -> extensionData.putAll(RaoUtils.loopFlowParametersExtensionToMap(loopFlowParameters)));
            searchTreeParameters.getMinMarginsParameters().ifPresent(
                costlyMinMarginParameters -> extensionData.putAll(RaoUtils.costlyMinMarginParametersExtensionToMap(costlyMinMarginParameters)));
        }

        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();
        extensionData.forEach((key, value) -> {
            keys.add(key);
            values.add(value);
        });
        parameterPointer.getProviderParameters().setProviderParametersKeys(Util.getStringListAsPtr(keys));
        parameterPointer.getProviderParameters().setProviderParametersKeysCount(keys.size());
        parameterPointer.getProviderParameters().setProviderParametersValues(Util.getStringListAsPtr(values));
        parameterPointer.getProviderParameters().setProviderParametersValuesCount(values.size());
    }

    @CEntryPoint(name = "getInstants")
    public static ArrayPointer<SeriesPointer> getInstants(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracInstantsMapper(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getMaxRemedialActionsUsageLimits")
    public static ArrayPointer<SeriesPointer> getMaxRemedialActionsUsageLimits(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracRemedialActionsUsageLimits(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getMaxTopologicalActionsPerTsoUsageLimits")
    public static ArrayPointer<SeriesPointer> getMaxTopologicalActionsPerTsoUsageLimits(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracPerTsoUsageLimits(RaUsageLimits::getMaxTopoPerTso), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getMaxPstActionsPerTsoUsageLimits")
    public static ArrayPointer<SeriesPointer> getMaxPstActionsPerTsoUsageLimits(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracPerTsoUsageLimits(RaUsageLimits::getMaxPstPerTso), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getMaxRemedialActionsPerTsoUsageLimits")
    public static ArrayPointer<SeriesPointer> getMaxRemedialActionsPerTsoUsageLimits(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracPerTsoUsageLimits(RaUsageLimits::getMaxRaPerTso), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getMaxElementaryActionsPerTsoUsageLimits")
    public static ArrayPointer<SeriesPointer> getMaxElementaryActionsPerTsoUsageLimits(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracPerTsoUsageLimits(RaUsageLimits::getMaxElementaryActionsPerTso), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getCracContingencies")
    public static ArrayPointer<SeriesPointer> getCracContingencies(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracContingencies(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getCracContingencyElements")
    public static ArrayPointer<SeriesPointer> getCracContingencyElements(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracContingencyElements(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getFlowCnecs")
    public static ArrayPointer<SeriesPointer> getFlowCnecs(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracFlowCnecs(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getAngleCnecs")
    public static ArrayPointer<SeriesPointer> getAngleCnecs(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracAngleCnecs(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getVoltageCnecs")
    public static ArrayPointer<SeriesPointer> getVoltageCnecs(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracVoltageCnecs(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getThresholds")
    public static ArrayPointer<SeriesPointer> getThresholds(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracThresholds(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getCracPstRangeActions")
    public static ArrayPointer<SeriesPointer> getCracPstRangeActions(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracRangeActions(crac -> crac.getPstRangeActions().stream().toList()), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getCracHvdcRangeActions")
    public static ArrayPointer<SeriesPointer> getCracHvdcRangeActions(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracRangeActions(crac -> crac.getHvdcRangeActions().stream().toList()), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getCracInjectionRangeActions")
    public static ArrayPointer<SeriesPointer> getCracInjectionRangeActions(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracInjectionRangeActions(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getNetworkElementIdsAndKeys")
    public static ArrayPointer<SeriesPointer> getNetworkElementIdsAndKeys(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracInjectionRaElements(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getCracCounterTradeRangeActions")
    public static ArrayPointer<SeriesPointer> getCracCounterTradeRangeActions(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracCounterTradeRangeActions(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getCracRangeActionRanges")
    public static ArrayPointer<SeriesPointer> getCracRangeActionRanges(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracRanges(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getCracNetworkActions")
    public static ArrayPointer<SeriesPointer> getCracNetworkActions(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracNetworkActions(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getCracTerminalConnectionActions")
    public static ArrayPointer<SeriesPointer> getCracTerminalConnectionActions(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracTerminalConnectionActions(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getCracPstTapPositionActions")
    public static ArrayPointer<SeriesPointer> getCracPstTapPositionActions(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracPstTapPositionActions(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getCracGeneratorActions")
    public static ArrayPointer<SeriesPointer> getCracGeneratorActions(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracGeneratorActions(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getCracLoadActions")
    public static ArrayPointer<SeriesPointer> getCracLoadActions(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracLoadActions(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getCracBoundaryLineActions")
    public static ArrayPointer<SeriesPointer> getCracBoundaryLineActions(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracBoundaryLineActions(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getCracShuntCompensatorPositionActions")
    public static ArrayPointer<SeriesPointer> getCracShuntCompensatorPositionActions(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracShuntCompensatorPositionActions(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getCracSwitchActions")
    public static ArrayPointer<SeriesPointer> getCracSwitchActions(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracSwitchActions(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getCracSwitchPairs")
    public static ArrayPointer<SeriesPointer> getCracSwitchPairs(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracSwitchPairs(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getOnInstantUsageRules")
    public static ArrayPointer<SeriesPointer> getOnInstantUsageRules(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracOnInstantUsageRules(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getOnContingencyStateUsageRules")
    public static ArrayPointer<SeriesPointer> getOnContingencyStateUsageRules(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracOnContingencyStateUsageRules(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getOnConstraintUsageRules")
    public static ArrayPointer<SeriesPointer> getOnConstraintUsageRules(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracOnConstraintUsageRules(), exceptionHandlerPtr);
    }

    @CEntryPoint(name = "getOnFlowConstraintInCountryUsageRules")
    public static ArrayPointer<SeriesPointer> getOnFlowConstraintInCountryUsageRules(IsolateThread thread, ObjectHandle cracHandle, ExceptionHandlerPointer exceptionHandlerPtr) {
        return cracGenericMethod(thread, cracHandle, cracOnFlowConstraintInCountryUsageRules(), exceptionHandlerPtr);
    }

    public static ArrayPointer<SeriesPointer> cracGenericMethod(IsolateThread thread, ObjectHandle cracHandle, DataframeMapper<Crac, Void> mapper, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<SeriesPointer> get() {
                Crac crac = ObjectHandles.getGlobal().get(cracHandle);
                return Dataframes.createCDataframe(mapper, crac);
            }
        });
    }
}
