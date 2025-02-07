/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.rao;

import com.powsybl.commons.PowsyblException;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowProvider;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.raoresult.api.RaoResult;
import com.powsybl.openrao.raoapi.json.JsonRaoParameters;
import com.powsybl.openrao.raoapi.parameters.ObjectiveFunctionParameters;
import com.powsybl.openrao.raoapi.parameters.RangeActionsOptimizationParameters;
import com.powsybl.openrao.raoapi.parameters.RaoParameters;
import com.powsybl.openrao.raoapi.parameters.SecondPreventiveRaoParameters;
import com.powsybl.openrao.raoapi.parameters.extensions.LoopFlowParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.MnecParametersExtension;
import com.powsybl.openrao.raoapi.parameters.extensions.RelativeMarginsParametersExtension;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.Util;
import com.powsybl.python.loadflow.LoadFlowCUtils;
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
        raoParameters.getObjectiveFunctionParameters().setType(
            ObjectiveFunctionParameters.ObjectiveFunctionType.values()[paramPointer.getObjectiveFunctionType()]);
        raoParameters.getObjectiveFunctionParameters().setForbidCostIncrease(paramPointer.getForbidCostIncrease());
        raoParameters.getObjectiveFunctionParameters().setCurativeMinObjImprovement(paramPointer.getCurativeMinObjImprovement());
        raoParameters.getObjectiveFunctionParameters().setPreventiveStopCriterion(ObjectiveFunctionParameters.PreventiveStopCriterion.values()[paramPointer.getPreventiveStopCriterion()]);
        raoParameters.getObjectiveFunctionParameters().setCurativeStopCriterion(ObjectiveFunctionParameters.CurativeStopCriterion.values()[paramPointer.getCurativeStopCriterion()]);
        raoParameters.getObjectiveFunctionParameters().setOptimizeCurativeIfPreventiveUnsecure(paramPointer.getOptimizeCurativeIfPreventiveUnsecure());

        // Range action optimization solver
        raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver()
            .setSolver(RangeActionsOptimizationParameters.Solver.values()[paramPointer.getSolver()]);
        raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver()
            .setRelativeMipGap(paramPointer.getRelativeMipGap());
        raoParameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver()
            .setSolverSpecificParameters(CTypeUtil.toString(paramPointer.getSolverSpecificParameters()));

        // Range action optimization parameters
        raoParameters.getRangeActionsOptimizationParameters().setMaxMipIterations(paramPointer.getMaxMipIterations());
        raoParameters.getRangeActionsOptimizationParameters().setPstPenaltyCost(paramPointer.getPstPenaltyCost());
        raoParameters.getRangeActionsOptimizationParameters().setPstSensitivityThreshold(paramPointer.getPstSensitivityThreshold());
        raoParameters.getRangeActionsOptimizationParameters().setPstModel(RangeActionsOptimizationParameters.PstModel.values()[paramPointer.getPstModel()]);
        raoParameters.getRangeActionsOptimizationParameters().setHvdcPenaltyCost(paramPointer.getHvdcPenaltyCost());
        raoParameters.getRangeActionsOptimizationParameters().setInjectionRaPenaltyCost(paramPointer.getInjectionRaPenaltyCost());
        raoParameters.getRangeActionsOptimizationParameters().setInjectionRaSensitivityThreshold(paramPointer.getInjectionRaSensitivityThreshold());
        raoParameters.getRangeActionsOptimizationParameters().setRaRangeShrinking(RangeActionsOptimizationParameters.RaRangeShrinking.values()[paramPointer.getRaRangeShrinking()]);

        // Topo optimization parameters
        raoParameters.getTopoOptimizationParameters().setMaxPreventiveSearchTreeDepth(paramPointer.getMaxPreventiveSearchTreeDepth());
        raoParameters.getTopoOptimizationParameters().setMaxAutoSearchTreeDepth(paramPointer.getMaxAutoSearchTreeDepth());
        raoParameters.getTopoOptimizationParameters().setMaxCurativeSearchTreeDepth(paramPointer.getMaxCurativeSearchTreeDepth());
        raoParameters.getTopoOptimizationParameters().setPredefinedCombinations(arrayPointerToStringListList(paramPointer.getPredefinedCombinations()));
        raoParameters.getTopoOptimizationParameters().setRelativeMinImpactThreshold(paramPointer.getRelativeMinImpactThreshold());
        raoParameters.getTopoOptimizationParameters().setAbsoluteMinImpactThreshold(paramPointer.getAbsoluteMinImpactThreshold());
        raoParameters.getTopoOptimizationParameters().setSkipActionsFarFromMostLimitingElement(paramPointer.getSkipActionsFarFromMostLimitingElement());
        raoParameters.getTopoOptimizationParameters().setMaxNumberOfBoundariesForSkippingActions(paramPointer.getMaxNumberOfBoundariesForSkippingActions());

        // Multithreading parameters
        raoParameters.getMultithreadingParameters().setContingencyScenariosInParallel(paramPointer.getContingencyScenariosInParallel());
        raoParameters.getMultithreadingParameters().setPreventiveLeavesInParallel(paramPointer.getPreventiveLeavesInParallel());
        raoParameters.getMultithreadingParameters().setAutoLeavesInParallel(paramPointer.getAutoLeavesInParallel());
        raoParameters.getMultithreadingParameters().setCurativeLeavesInParallel(paramPointer.getCurativeLeavesInParallel());

        // Second preventive parameters
        raoParameters.getSecondPreventiveRaoParameters().setExecutionCondition(SecondPreventiveRaoParameters.ExecutionCondition.values()[paramPointer.getExecutionCondition()]);
        raoParameters.getSecondPreventiveRaoParameters().setReOptimizeCurativeRangeActions(paramPointer.getReOptimizeCurativeRangeActions());
        raoParameters.getSecondPreventiveRaoParameters().setHintFromFirstPreventiveRao(paramPointer.getHintFromFirstPreventiveRao());

        // Not opitmized cnec parameters
        raoParameters.getNotOptimizedCnecsParameters().setDoNotOptimizeCurativeCnecsForTsosWithoutCras(paramPointer.getDoNotOptimizeCurativeCnecsForTsosWithoutCras());

        // Load flow and sensitivity providers
        raoParameters.getLoadFlowAndSensitivityParameters().setLoadFlowProvider(CTypeUtil.toString(paramPointer.getLoadFlowProvider()));
        raoParameters.getLoadFlowAndSensitivityParameters().setSensitivityProvider(CTypeUtil.toString(paramPointer.getSensitivityProvider()));
        raoParameters.getLoadFlowAndSensitivityParameters().setSensitivityFailureOvercost(paramPointer.getSensitivityFailureOvercost());
        raoParameters.getLoadFlowAndSensitivityParameters().setSensitivityWithLoadFlowParameters(createSensitivityAnalysisParameters(
            false, paramPointer.getSensitivityParameters(),
            getProvider(CTypeUtil.toString(paramPointer.getSensitivityProvider()))));

        Map<String, String> extensionData = getExtensionData(paramPointer);
        MnecParametersExtension mnecExt = RaoUtils.buildMnecParametersExtension(extensionData);
        RelativeMarginsParametersExtension relativeMargingExt = RaoUtils.buildRelativeMarginsParametersExtension(extensionData);
        LoopFlowParametersExtension loopFlowExt = RaoUtils.buildLoopFlowParametersExtension(extensionData);
        raoParameters.addExtension(MnecParametersExtension.class, mnecExt);
        raoParameters.addExtension(RelativeMarginsParametersExtension.class, relativeMargingExt);
        raoParameters.addExtension(LoopFlowParametersExtension.class, loopFlowExt);

        return raoParameters;
    }

    private static PyPowsyblApiHeader.RaoParametersPointer convertToRaoParametersPointer(RaoParameters parameters) {
        PyPowsyblApiHeader.RaoParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.RaoParametersPointer.class));
        // Objective function parameters
        paramsPtr.setObjectiveFunctionType(parameters.getObjectiveFunctionParameters().getType().ordinal());
        paramsPtr.setForbidCostIncrease(parameters.getObjectiveFunctionParameters().getForbidCostIncrease());
        paramsPtr.setCurativeMinObjImprovement(parameters.getObjectiveFunctionParameters().getCurativeMinObjImprovement());
        paramsPtr.setPreventiveStopCriterion(parameters.getObjectiveFunctionParameters().getPreventiveStopCriterion().ordinal());
        paramsPtr.setCurativeStopCriterion(parameters.getObjectiveFunctionParameters().getCurativeStopCriterion().ordinal());
        paramsPtr.setOptimizeCurativeIfPreventiveUnsecure(parameters.getObjectiveFunctionParameters().getOptimizeCurativeIfPreventiveUnsecure());

        // Range action optimization solver
        paramsPtr.setSolver(parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getSolver().ordinal());
        paramsPtr.setRelativeMipGap(parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getRelativeMipGap());
        paramsPtr.setSolverSpecificParameters(CTypeUtil.toCharPtr(parameters.getRangeActionsOptimizationParameters().getLinearOptimizationSolver().getSolverSpecificParameters()));

        // Range action optimization parameters
        paramsPtr.setMaxMipIterations(parameters.getRangeActionsOptimizationParameters().getMaxMipIterations());
        paramsPtr.setPstPenaltyCost(parameters.getRangeActionsOptimizationParameters().getPstPenaltyCost());
        paramsPtr.setPstSensitivityThreshold(parameters.getRangeActionsOptimizationParameters().getPstSensitivityThreshold());
        paramsPtr.setPstModel(parameters.getRangeActionsOptimizationParameters().getPstModel().ordinal());
        paramsPtr.setHvdcPenaltyCost(parameters.getRangeActionsOptimizationParameters().getHvdcPenaltyCost());
        paramsPtr.setInjectionRaPenaltyCost(parameters.getRangeActionsOptimizationParameters().getInjectionRaPenaltyCost());
        paramsPtr.setInjectionRaSensitivityThreshold(parameters.getRangeActionsOptimizationParameters().getInjectionRaSensitivityThreshold());
        paramsPtr.setRaRangeShrinking(parameters.getRangeActionsOptimizationParameters().getRaRangeShrinking().ordinal());

        // Topo optimization parameters
        paramsPtr.setMaxPreventiveSearchTreeDepth(parameters.getTopoOptimizationParameters().getMaxPreventiveSearchTreeDepth());
        paramsPtr.setMaxAutoSearchTreeDepth(parameters.getTopoOptimizationParameters().getMaxAutoSearchTreeDepth());
        paramsPtr.setMaxCurativeSearchTreeDepth(parameters.getTopoOptimizationParameters().getMaxCurativeSearchTreeDepth());
        paramsPtr.setRelativeMinImpactThreshold(parameters.getTopoOptimizationParameters().getRelativeMinImpactThreshold());
        paramsPtr.setAbsoluteMinImpactThreshold(parameters.getTopoOptimizationParameters().getAbsoluteMinImpactThreshold());
        paramsPtr.setSkipActionsFarFromMostLimitingElement(parameters.getTopoOptimizationParameters().getSkipActionsFarFromMostLimitingElement());
        paramsPtr.setMaxNumberOfBoundariesForSkippingActions(parameters.getTopoOptimizationParameters().getMaxNumberOfBoundariesForSkippingActions());

        stringListListToArrayPointer(paramsPtr.getPredefinedCombinations(), parameters.getTopoOptimizationParameters().getPredefinedCombinations());

        // Multithreading parameters
        paramsPtr.setContingencyScenariosInParallel(parameters.getMultithreadingParameters().getContingencyScenariosInParallel());
        paramsPtr.setPreventiveLeavesInParallel(parameters.getMultithreadingParameters().getPreventiveLeavesInParallel());
        paramsPtr.setAutoLeavesInParallel(parameters.getMultithreadingParameters().getAutoLeavesInParallel());
        paramsPtr.setCurativeLeavesInParallel(parameters.getMultithreadingParameters().getCurativeLeavesInParallel());

        // Second preventive parameters
        paramsPtr.setExecutionCondition(parameters.getSecondPreventiveRaoParameters().getExecutionCondition().ordinal());
        paramsPtr.setReOptimizeCurativeRangeActions(parameters.getSecondPreventiveRaoParameters().getReOptimizeCurativeRangeActions());
        paramsPtr.setHintFromFirstPreventiveRao(parameters.getSecondPreventiveRaoParameters().getHintFromFirstPreventiveRao());

        // Not opitmized cnec parameters
        paramsPtr.setDoNotOptimizeCurativeCnecsForTsosWithoutCras(parameters.getNotOptimizedCnecsParameters().getDoNotOptimizeCurativeCnecsForTsosWithoutCras());

        // Load flow and sensitivity providers
        paramsPtr.setLoadFlowProvider(CTypeUtil.toCharPtr(parameters.getLoadFlowAndSensitivityParameters().getLoadFlowProvider()));
        paramsPtr.setSensitivityProvider(CTypeUtil.toCharPtr(parameters.getLoadFlowAndSensitivityParameters().getSensitivityProvider()));
        paramsPtr.setSensitivityParameters(
            convertToSensitivityAnalysisParametersPointer(
                parameters.getLoadFlowAndSensitivityParameters().getSensitivityWithLoadFlowParameters()));
        paramsPtr.setSensitivityFailureOvercost(parameters.getLoadFlowAndSensitivityParameters().getSensitivityFailureOvercost());

        convertExtensionData(parameters, paramsPtr);
        return paramsPtr;
    }

    private static Map<String, String> getExtensionData(PyPowsyblApiHeader.RaoParametersPointer parameterPointer) {
        return CTypeUtil.toStringMap(parameterPointer.getProviderParameters().getProviderParametersKeys(),
            parameterPointer.getProviderParameters().getProviderParametersKeysCount(),
            parameterPointer.getProviderParameters().getProviderParametersValues(),
            parameterPointer.getProviderParameters().getProviderParametersValuesCount());
    }

    private static void convertExtensionData(RaoParameters parameters, PyPowsyblApiHeader.RaoParametersPointer parameterPointer) {
        Map<String, String> extensionData = new HashMap<>();
        MnecParametersExtension mnecExt = parameters.getExtension(MnecParametersExtension.class);
        if (mnecExt != null) {
            extensionData.putAll(RaoUtils.mnecParametersExtensionToMap(mnecExt));
        }
        RelativeMarginsParametersExtension relativeMarginExt = parameters.getExtension(RelativeMarginsParametersExtension.class);
        if (relativeMarginExt != null) {
            extensionData.putAll(RaoUtils.relativeMarginsParametersExtensionToMap(relativeMarginExt));
        }
        LoopFlowParametersExtension loopFlowExt = parameters.getExtension(LoopFlowParametersExtension.class);
        if (loopFlowExt != null) {
            extensionData.putAll(RaoUtils.loopFlowParametersExtensionToMap(loopFlowExt));
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
