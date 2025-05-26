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
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowProvider;
import com.powsybl.openrao.commons.Unit;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.*;
import com.powsybl.openrao.raoapi.parameters.extensions.*;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
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
import org.graalvm.nativeimage.c.type.CTypeConversion;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

import static com.powsybl.python.commons.CTypeUtil.*;
import static com.powsybl.python.commons.Util.*;
import static com.powsybl.python.loadflow.LoadFlowCUtils.createLoadFlowParameters;
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
    public static ObjectHandle createRao(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new RaoContext()));
    }

    @CEntryPoint(name = "createDefaultRaoParameters")
    public static ObjectHandle createDefaultRaoParameters(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new RaoParameters()));
    }

    @CEntryPoint(name = "loadRaoParameters")
    public static PyPowsyblApiHeader.RaoParametersPointer loadRaoParameters(IsolateThread thread, CCharPointer parametersBuffer, int paramersBufferSize, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            ByteBuffer bufferParameters = CTypeConversion.asByteBuffer(parametersBuffer, paramersBufferSize);
            InputStream streamedParameters = new ByteArrayInputStream(binaryBufferToBytes(bufferParameters));
            return convertToRaoParametersPointer(JsonRaoParameters.read(streamedParameters));
        });
    }

    @CEntryPoint(name = "serializeRaoParameters")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointer> serializeRaoParameters(IsolateThread thread, PyPowsyblApiHeader.RaoParametersPointer raoParameters,
                                                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            RaoParameters parameters = convertToRaoParameters(raoParameters);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                JsonRaoParameters.write(parameters, output);
                return Util.createByteArray(output.toByteArray());
            } catch (IOException e) {
                throw new PowsyblException("Could not serialize rao parameters : " + e.getMessage());
            }
        });
    }

    @CEntryPoint(name = "setCracBufferedSource")
    public static void setCracBufferedSource(IsolateThread thread, ObjectHandle networkHandle, ObjectHandle raoContextHandle, CCharPointer cracBuffer, int cracBufferSize, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);

            ByteBuffer bufferCrac = CTypeConversion.asByteBuffer(cracBuffer, cracBufferSize);
            InputStream streamedCrac = new ByteArrayInputStream(binaryBufferToBytes(bufferCrac));
            try {
                Crac crac = Crac.read("crac.json", streamedCrac, network);
                if (crac != null) {
                    raoContext.setCrac(crac);
                } else {
                    throw new PowsyblException("Error while reading json crac, please enable detailed log for more information.");
                }
            } catch (IOException e) {
                throw new PowsyblException("Cannot read provided crac data : " + e.getMessage());
            }
        });
    }

    @CEntryPoint(name = "setGlskBufferedSource")
    public static void setGlskBufferedSource(IsolateThread thread, ObjectHandle networkHandle, ObjectHandle raoContextHandle, CCharPointer glsksBuffer, int glsksBufferSize, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);
            ByteBuffer bufferGlsks = CTypeConversion.asByteBuffer(glsksBuffer, glsksBufferSize);

            InputStream glsksStream = new ByteArrayInputStream(binaryBufferToBytes(bufferGlsks));
            GlskDocument glsks = GlskDocumentImporters.importGlsk(glsksStream);
            raoContext.setGlsks(glsks);
        });
    }

    @CEntryPoint(name = "runRao")
    public static ObjectHandle runRao(IsolateThread thread, ObjectHandle networkHandle, ObjectHandle raoContextHandle,
                              PyPowsyblApiHeader.RaoParametersPointer parametersPointer, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);
            RaoParameters raoParameters = convertToRaoParameters(parametersPointer);
            return ObjectHandles.getGlobal().create(raoContext.run(network, raoParameters));
        });
    }

    @CEntryPoint(name = "runVoltageMonitoring")
    public static ObjectHandle runVoltageMonitoring(IsolateThread thread, ObjectHandle networkHandle, ObjectHandle resultHandle, ObjectHandle contextHandle,
                                     PyPowsyblApiHeader.LoadFlowParametersPointer loadFlowParametersPtr,
                                     CCharPointer provider, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            RaoResult result = ObjectHandles.getGlobal().get(resultHandle);
            RaoContext raoContext = ObjectHandles.getGlobal().get(contextHandle);
            String providerStr = CTypeUtil.toString(provider);
            LoadFlowProvider loadFlowProvider = LoadFlowCUtils.getLoadFlowProvider(providerStr);
            LoadFlowParameters lfParameters = createLoadFlowParameters(false, loadFlowParametersPtr, loadFlowProvider);
            return ObjectHandles.getGlobal().create(raoContext.runVoltageMonitoring(network, result, providerStr, lfParameters));
        });
    }

    @CEntryPoint(name = "runAngleMonitoring")
    public static ObjectHandle runAngleMonitoring(IsolateThread thread, ObjectHandle networkHandle, ObjectHandle resultHandle, ObjectHandle contextHandle,
                                     PyPowsyblApiHeader.LoadFlowParametersPointer loadFlowParametersPtr,
                                     CCharPointer provider, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            RaoResult result = ObjectHandles.getGlobal().get(resultHandle);
            RaoContext raoContext = ObjectHandles.getGlobal().get(contextHandle);
            String providerStr = CTypeUtil.toString(provider);
            LoadFlowProvider loadFlowProvider = LoadFlowCUtils.getLoadFlowProvider(providerStr);
            LoadFlowParameters lfParameters = createLoadFlowParameters(false, loadFlowParametersPtr, loadFlowProvider);
            return ObjectHandles.getGlobal().create(raoContext.runAngleMonitoring(network, result, providerStr, lfParameters));
        });
    }

    @CEntryPoint(name = "serializeRaoResultsToBuffer")
    public static PyPowsyblApiHeader.ArrayPointer<CCharPointer> serializeRaoResultsToBuffer(IsolateThread thread, ObjectHandle raoResultHandle,
        ObjectHandle cracHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
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
        });
    }

    @CEntryPoint(name = "getRaoResultStatus")
    public static PyPowsyblApiHeader.RaoComputationStatus getRaoResultStatus(IsolateThread thread, ObjectHandle raoResultHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
            switch (result.getComputationStatus()) {
                case DEFAULT -> {
                    return PyPowsyblApiHeader.RaoComputationStatus.DEFAULT;
                }
                case FAILURE -> {
                    return PyPowsyblApiHeader.RaoComputationStatus.FAILURE;
                }
                default -> throw new PowsyblException("Unexpected computation status : " + result.getComputationStatus());
            }
        });
    }

    @CEntryPoint(name = "getCrac")
    public static ObjectHandle getCrac(IsolateThread thread, ObjectHandle raoContextHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            RaoContext raoContext = ObjectHandles.getGlobal().get(raoContextHandle);
            return ObjectHandles.getGlobal().create(raoContext.getCrac());
        });
    }

    @CEntryPoint(name = "createRaoParameters")
    public static PyPowsyblApiHeader.RaoParametersPointer createRaoParameters(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> convertToRaoParametersPointer(new RaoParameters()));
    }

    @CEntryPoint(name = "getFlowCnecResults")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getFlowCnecResults(IsolateThread thread, ObjectHandle cracHandle, ObjectHandle raoResultHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        Crac crac = ObjectHandles.getGlobal().get(cracHandle);
        RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
        return Dataframes.createCDataframe(Dataframes.flowCnecMapper(), crac, new DataframeFilter(), result);
    }

    @CEntryPoint(name = "getAngleCnecResults")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getAngleCnecResults(IsolateThread thread, ObjectHandle cracHandle, ObjectHandle raoResultHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        Crac crac = ObjectHandles.getGlobal().get(cracHandle);
        RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
        return Dataframes.createCDataframe(Dataframes.angleCnecMapper(), crac, new DataframeFilter(), result);
    }

    @CEntryPoint(name = "getVoltageCnecResults")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getVoltageCnecResults(IsolateThread thread, ObjectHandle cracHandle, ObjectHandle raoResultHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        Crac crac = ObjectHandles.getGlobal().get(cracHandle);
        RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
        return Dataframes.createCDataframe(Dataframes.voltageCnecMapper(), crac, new DataframeFilter(), result);
    }

    @CEntryPoint(name = "getRaResults")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getRAResults(IsolateThread thread, ObjectHandle cracHandle, ObjectHandle raoResultHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        Crac crac = ObjectHandles.getGlobal().get(cracHandle);
        RaoResult result = ObjectHandles.getGlobal().get(raoResultHandle);
        return Dataframes.createCDataframe(Dataframes.raResultMapper(), crac, new DataframeFilter(), result);
    }

    @CEntryPoint(name = "freeRaoParameters")
    public static void freeRaoParameters(IsolateThread thread, PyPowsyblApiHeader.RaoParametersPointer parametersPointer,
                                              PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {

        // Top level string parameters are freed by toString call on c side
        // Free predefined combinations
        freeNestedArrayPointer(parametersPointer.getPredefinedCombinations());

        // Free sensitivity parameters
        LoadFlowCUtils.freeLoadFlowParametersContent(parametersPointer.getSensitivityParameters().getLoadFlowParameters());
        UnmanagedMemory.free(parametersPointer.getSensitivityParameters());

        // Free extensions
        freeProviderParameters(parametersPointer.getProviderParameters());

        // Free main pointer
        doCatch(exceptionHandlerPtr, () -> UnmanagedMemory.free(parametersPointer));
    }

    private static RaoParameters convertToRaoParameters(PyPowsyblApiHeader.RaoParametersPointer paramPointer) {
        RaoParameters raoParameters = new RaoParameters();

        OpenRaoSearchTreeParameters searchTreeParameters = new OpenRaoSearchTreeParameters();
        raoParameters.getObjectiveFunctionParameters().setType(
            ObjectiveFunctionParameters.ObjectiveFunctionType.values()[paramPointer.getObjectiveFunctionType()]);
        searchTreeParameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(paramPointer.getCurativeMinObjImprovement());
        raoParameters.getObjectiveFunctionParameters().setUnit(Unit.values()[paramPointer.getUnit()]);
        raoParameters.getObjectiveFunctionParameters().setEnforceCurativeSecurity(paramPointer.getEnforceCurativeSecurity());

        // Range action optimization solver
        searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver()
            .setSolver(SearchTreeRaoRangeActionsOptimizationParameters.Solver.values()[paramPointer.getSolver()]);
        searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver()
            .setRelativeMipGap(paramPointer.getRelativeMipGap());
        searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver()
            .setSolverSpecificParameters(CTypeUtil.toString(paramPointer.getSolverSpecificParameters()));

        // Range action optimization parameters
        raoParameters.getRangeActionsOptimizationParameters().setPstRAMinImpactThreshold(paramPointer.getPstRaMinImpactThreshold());
        raoParameters.getRangeActionsOptimizationParameters().setHvdcRAMinImpactThreshold(paramPointer.getHvdcRaMinImpactThreshold());
        raoParameters.getRangeActionsOptimizationParameters().setInjectionRAMinImpactThreshold(paramPointer.getInjectionRaMinImpactThreshold());
        searchTreeParameters.getRangeActionsOptimizationParameters().setMaxMipIterations(paramPointer.getMaxMipIterations());
        searchTreeParameters.getRangeActionsOptimizationParameters().setPstSensitivityThreshold(paramPointer.getPstSensitivityThreshold());
        searchTreeParameters.getRangeActionsOptimizationParameters().setHvdcSensitivityThreshold(paramPointer.getHvdcSensitivityThreshold());
        searchTreeParameters.getRangeActionsOptimizationParameters().setPstModel(SearchTreeRaoRangeActionsOptimizationParameters.PstModel.values()[paramPointer.getPstModel()]);
        searchTreeParameters.getRangeActionsOptimizationParameters().setInjectionRaSensitivityThreshold(paramPointer.getInjectionRaSensitivityThreshold());
        searchTreeParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(SearchTreeRaoRangeActionsOptimizationParameters.RaRangeShrinking.values()[paramPointer.getRaRangeShrinking()]);

        // Topo optimization parameters
        raoParameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(paramPointer.getRelativeMinImpactThreshold());
        raoParameters.getTopoOptimizationParameters().setAbsoluteMinImpactThreshold(paramPointer.getAbsoluteMinImpactThreshold());
        searchTreeParameters.getTopoOptimizationParameters().setMaxPreventiveSearchTreeDepth(paramPointer.getMaxPreventiveSearchTreeDepth());
        searchTreeParameters.getTopoOptimizationParameters().setMaxAutoSearchTreeDepth(paramPointer.getMaxAutoSearchTreeDepth());
        searchTreeParameters.getTopoOptimizationParameters().setMaxCurativeSearchTreeDepth(paramPointer.getMaxCurativeSearchTreeDepth());
        searchTreeParameters.getTopoOptimizationParameters().setPredefinedCombinations(arrayPointerToStringListList(paramPointer.getPredefinedCombinations()));
        searchTreeParameters.getTopoOptimizationParameters().setSkipActionsFarFromMostLimitingElement(paramPointer.getSkipActionsFarFromMostLimitingElement());
        searchTreeParameters.getTopoOptimizationParameters().setMaxNumberOfBoundariesForSkippingActions(paramPointer.getMaxNumberOfBoundariesForSkippingActions());

        // Multithreading parameters
        searchTreeParameters.getMultithreadingParameters().setAvailableCPUs(paramPointer.getAvailableCpus());

        // Second preventive parameters
        searchTreeParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.values()[paramPointer.getExecutionCondition()]);
        searchTreeParameters.getSecondPreventiveRaoParameters().setReOptimizeCurativeRangeActions(paramPointer.getReOptimizeCurativeRangeActions());
        searchTreeParameters.getSecondPreventiveRaoParameters().setHintFromFirstPreventiveRao(paramPointer.getHintFromFirstPreventiveRao());

        // Not opitmized cnec parameters
        raoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCurativeCnecsForTsosWithoutCras(paramPointer.getDoNotOptimizeCurativeCnecsForTsosWithoutCras());

        // Load flow and sensitivity providers
        searchTreeParameters.getLoadFlowAndSensitivityParameters().setLoadFlowProvider(CTypeUtil.toString(paramPointer.getLoadFlowProvider()));
        searchTreeParameters.getLoadFlowAndSensitivityParameters().setSensitivityProvider(CTypeUtil.toString(paramPointer.getSensitivityProvider()));
        searchTreeParameters.getLoadFlowAndSensitivityParameters().setSensitivityFailureOvercost(paramPointer.getSensitivityFailureOvercost());
        searchTreeParameters.getLoadFlowAndSensitivityParameters().setSensitivityWithLoadFlowParameters(createSensitivityAnalysisParameters(
            false, paramPointer.getSensitivityParameters(),
            getProvider(CTypeUtil.toString(paramPointer.getSensitivityProvider()))));

        Map<String, String> extensionData = getExtensionData(paramPointer);
        if (hasExtensionData(extensionData, RaoUtils.MNEC_EXT_PREFIX)) {
            MnecParameters mnecExt = RaoUtils.buildMnecParametersExtension(extensionData);
            raoParameters.setMnecParameters(mnecExt);
        }
        if (hasExtensionData(extensionData, RaoUtils.MNEC_ST_EXT_PREFIX)) {
            SearchTreeRaoMnecParameters searchTreeMnecExt = RaoUtils.buildMnecSearchTreeParametersExtension(extensionData);
            searchTreeParameters.setMnecParameters(searchTreeMnecExt);
        }
        if (hasExtensionData(extensionData, RaoUtils.RELATIVE_MARGIN_EXT_PREFIX)) {
            RelativeMarginsParameters relativeMargingExt = RaoUtils.buildRelativeMarginsParametersExtension(extensionData);
            raoParameters.setRelativeMarginsParameters(relativeMargingExt);
        }
        if (hasExtensionData(extensionData, RaoUtils.RELATIVE_MARGIN_ST_EXT_PREFIX)) {
            SearchTreeRaoRelativeMarginsParameters searchTreeRelativeMarginsParameters = RaoUtils.buildRelativeMarginsSearchTreeParametersExtension(extensionData);
            searchTreeParameters.setRelativeMarginsParameters(searchTreeRelativeMarginsParameters);
        }
        if (hasExtensionData(extensionData, RaoUtils.LOOP_FLOW_EXT_PREFIX)) {
            LoopFlowParameters loopFlowExt = RaoUtils.buildLoopFlowParametersExtension(extensionData);
            raoParameters.setLoopFlowParameters(loopFlowExt);
        }
        if (hasExtensionData(extensionData, RaoUtils.LOOP_FLOW_ST_EXT_PREFIX)) {
            SearchTreeRaoLoopFlowParameters searchTreeLoopFlowExt = RaoUtils.buildLoopFlowSearchTreeParametersExtension(extensionData);
            searchTreeParameters.setLoopFlowParameters(searchTreeLoopFlowExt);
        }
        return raoParameters;
    }

    private static PyPowsyblApiHeader.RaoParametersPointer convertToRaoParametersPointer(RaoParameters parameters) {
        PyPowsyblApiHeader.RaoParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.RaoParametersPointer.class));
        OpenRaoSearchTreeParameters searchTreeParameters = parameters.getExtension(OpenRaoSearchTreeParameters.class);
        if (searchTreeParameters == null) {
            searchTreeParameters = new OpenRaoSearchTreeParameters();
        }

        // Objective function parameters
        paramsPtr.setObjectiveFunctionType(parameters.getObjectiveFunctionParameters().getType().ordinal());
        paramsPtr.setUnit(parameters.getObjectiveFunctionParameters().getUnit().ordinal());
        paramsPtr.setCurativeMinObjImprovement(searchTreeParameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement());

        // Range action optimization solver
        paramsPtr.setSolver(searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getSolver().ordinal());
        paramsPtr.setRelativeMipGap(searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getRelativeMipGap());
        paramsPtr.setSolverSpecificParameters(CTypeUtil.toCharPtr(searchTreeParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getSolverSpecificParameters()));

        // Range action optimization parameters
        paramsPtr.setPstRaMinImpactThreshold(parameters.getRangeActionsOptimizationParameters().getPstRAMinImpactThreshold());
        paramsPtr.setHvdcRaMinImpactThreshold(parameters.getRangeActionsOptimizationParameters().getHvdcRAMinImpactThreshold());
        paramsPtr.setInjectionRaMinImpactThreshold(parameters.getRangeActionsOptimizationParameters().getInjectionRAMinImpactThreshold());

        paramsPtr.setMaxMipIterations(searchTreeParameters.getRangeActionsOptimizationParameters().getMaxMipIterations());
        paramsPtr.setPstSensitivityThreshold(searchTreeParameters.getRangeActionsOptimizationParameters().getPstSensitivityThreshold());
        paramsPtr.setHvdcSensitivityThreshold(searchTreeParameters.getRangeActionsOptimizationParameters().getHvdcSensitivityThreshold());
        paramsPtr.setPstModel(searchTreeParameters.getRangeActionsOptimizationParameters().getPstModel().ordinal());
        paramsPtr.setInjectionRaSensitivityThreshold(searchTreeParameters.getRangeActionsOptimizationParameters().getInjectionRaSensitivityThreshold());
        paramsPtr.setRaRangeShrinking(searchTreeParameters.getRangeActionsOptimizationParameters().getRaRangeShrinking().ordinal());

        // Topo optimization parameters
        paramsPtr.setRelativeMinImpactThreshold(parameters.getTopoOptimizationParameters().getRelativeMinImpactThreshold());
        paramsPtr.setAbsoluteMinImpactThreshold(parameters.getTopoOptimizationParameters().getAbsoluteMinImpactThreshold());
        paramsPtr.setMaxPreventiveSearchTreeDepth(searchTreeParameters.getTopoOptimizationParameters().getMaxPreventiveSearchTreeDepth());
        paramsPtr.setMaxAutoSearchTreeDepth(searchTreeParameters.getTopoOptimizationParameters().getMaxAutoSearchTreeDepth());
        paramsPtr.setMaxCurativeSearchTreeDepth(searchTreeParameters.getTopoOptimizationParameters().getMaxCurativeSearchTreeDepth());
        paramsPtr.setSkipActionsFarFromMostLimitingElement(searchTreeParameters.getTopoOptimizationParameters().getSkipActionsFarFromMostLimitingElement());
        paramsPtr.setMaxNumberOfBoundariesForSkippingActions(searchTreeParameters.getTopoOptimizationParameters().getMaxNumberOfBoundariesForSkippingActions());

        stringListListToArrayPointer(paramsPtr.getPredefinedCombinations(), searchTreeParameters.getTopoOptimizationParameters().getPredefinedCombinations());

        // Multithreading parameters
        paramsPtr.setAvailableCpus(searchTreeParameters.getMultithreadingParameters().getAvailableCPUs());

        // Second preventive parameters
        paramsPtr.setExecutionCondition(searchTreeParameters.getSecondPreventiveRaoParameters().getExecutionCondition().ordinal());
        paramsPtr.setReOptimizeCurativeRangeActions(searchTreeParameters.getSecondPreventiveRaoParameters().getReOptimizeCurativeRangeActions());
        paramsPtr.setHintFromFirstPreventiveRao(searchTreeParameters.getSecondPreventiveRaoParameters().getHintFromFirstPreventiveRao());

        // Not opitmized cnec parameters
        paramsPtr.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(parameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCurativeCnecsForTsosWithoutCras());

        // Load flow and sensitivity providers
        paramsPtr.setLoadFlowProvider(CTypeUtil.toCharPtr(searchTreeParameters.getLoadFlowAndSensitivityParameters().getLoadFlowProvider()));
        paramsPtr.setSensitivityProvider(CTypeUtil.toCharPtr(searchTreeParameters.getLoadFlowAndSensitivityParameters().getSensitivityProvider()));
        paramsPtr.setSensitivityParameters(
            convertToSensitivityAnalysisParametersPointer(
                searchTreeParameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters()));
        paramsPtr.setSensitivityFailureOvercost(searchTreeParameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost());

        convertExtensionData(parameters, paramsPtr);
        return paramsPtr;
    }

    private static Map<String, String> getExtensionData(PyPowsyblApiHeader.RaoParametersPointer parameterPointer) {
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

    private static void convertExtensionData(RaoParameters parameters, PyPowsyblApiHeader.RaoParametersPointer parameterPointer) {
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
}
