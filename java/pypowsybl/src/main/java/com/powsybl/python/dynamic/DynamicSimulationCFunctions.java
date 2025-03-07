/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import static com.powsybl.python.commons.CTypeUtil.toStringList;
import static com.powsybl.python.commons.Util.convert;
import static com.powsybl.python.commons.Util.doCatch;
import static com.powsybl.python.dynamic.DynamicSimulationParametersCUtils.copyToCDynamicSimulationParameters;
import static com.powsybl.python.network.NetworkCFunctions.createDataframe;

import java.util.ArrayList;
import java.util.List;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.dynamic.DynamicSimulationDataframeMappersUtils;
import com.powsybl.python.network.Dataframes;
import com.powsybl.python.report.ReportCUtils;
import com.powsybl.timeseries.DoubleTimeSeries;
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

import com.powsybl.dataframe.dynamic.adders.DynamicMappingHandler;
import com.powsybl.dataframe.dynamic.adders.EventMappingHandler;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynamicsimulation.OutputVariablesSupplier;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.EventModelsSupplier;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.DataframeMetadataPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.DataframePointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.DynamicMappingType;
import com.powsybl.python.commons.PyPowsyblApiHeader.EventMappingType;
import com.powsybl.python.commons.PyPowsyblApiHeader.SeriesPointer;
import com.powsybl.python.commons.Util;

import static com.powsybl.python.commons.PyPowsyblApiHeader.*;

/**
 * @author Nicolas Pierre {@literal <nicolas.pierre@artelys.com>}
 */
@CContext(Directives.class)
public final class DynamicSimulationCFunctions {

    private DynamicSimulationCFunctions() {
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(DynamicSimulationCFunctions.class);
    }

    @CEntryPoint(name = "createDynamicSimulationContext")
    public static ObjectHandle createDynamicSimulationContext(IsolateThread thread,
            ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new DynamicSimulationContext()));
    }

    @CEntryPoint(name = "createDynamicModelMapping")
    public static ObjectHandle createDynamicModelMapping(IsolateThread thread,
            ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new PythonDynamicModelsSupplier()));
    }

    @CEntryPoint(name = "createTimeseriesMapping")
    public static ObjectHandle createTimeseriesMapping(IsolateThread thread,
            ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new PythonOutputVariablesSupplier()));
    }

    @CEntryPoint(name = "createEventMapping")
    public static ObjectHandle createEventMapping(IsolateThread thread,
            ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new PythonEventModelsSupplier()));
    }

    @CEntryPoint(name = "createDynamicSimulationParameters")
    public static DynamicSimulationParametersPointer createDynamicSimulationParameters(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DynamicSimulationParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(DynamicSimulationParametersPointer.class));
            copyToCDynamicSimulationParameters(paramsPtr);
            return paramsPtr;
        });
    }

    @CEntryPoint(name = "freeDynamicSimulationParameters")
    public static void freeDynamicSimulationParameters(IsolateThread thread, DynamicSimulationParametersPointer parametersPtr,
                                              ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> UnmanagedMemory.free(parametersPtr));
    }

    @CEntryPoint(name = "runDynamicSimulation")
    public static ObjectHandle runDynamicSimulation(IsolateThread thread,
                                                    ObjectHandle dynamicContextHandle,
                                                    ObjectHandle networkHandle,
                                                    ObjectHandle dynamicMappingHandle,
                                                    ObjectHandle eventModelsSupplierHandle,
                                                    ObjectHandle outputVariablesSupplierHandle,
                                                    DynamicSimulationParametersPointer parametersPtr,
                                                    ObjectHandle reportNodeHandle,
                                                    ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DynamicSimulationContext dynamicContext = ObjectHandles.getGlobal().get(dynamicContextHandle);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            PythonDynamicModelsSupplier dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
            EventModelsSupplier eventModelsSupplier = ObjectHandles.getGlobal().get(eventModelsSupplierHandle);
            OutputVariablesSupplier outputVariablesSupplier = ObjectHandles.getGlobal().get(outputVariablesSupplierHandle);
            ReportNode reportNode = ReportCUtils.getReportNode(reportNodeHandle);
            if (reportNode == null) {
                reportNode = ReportNode.NO_OP;
            }
            DynamicSimulationParameters dynamicSimulationParameters =
                    DynamicSimulationParametersCUtils.createDynamicSimulationParameters(parametersPtr);
            DynamicSimulationResult result = dynamicContext.run(network,
                    dynamicMapping,
                    eventModelsSupplier,
                    outputVariablesSupplier,
                    dynamicSimulationParameters,
                    reportNode);
            logger().info("Dynamic simulation ran successfully in java");
            return ObjectHandles.getGlobal().create(result);
        });
    }

    @CEntryPoint(name = "addDynamicMappings")
    public static void addDynamicMappings(IsolateThread thread, ObjectHandle dynamicMappingHandle,
                                          DynamicMappingType mappingType,
                                          DataframeArrayPointer mappingDataframePtr,
                                          ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            PythonDynamicModelsSupplier dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
            List<UpdatingDataframe> mappingDataframes = new ArrayList<>();
            for (int i = 0; i < mappingDataframePtr.getDataframesCount(); i++) {
                mappingDataframes.add(createDataframe(mappingDataframePtr.getDataframes().addressOf(i)));
            }
            DynamicMappingHandler.addElements(mappingType, dynamicMapping, mappingDataframes);
        });
    }

    @CEntryPoint(name = "getDynamicMappingsMetaData")
    public static DataframesMetadataPointer getDynamicMappingsMetaData(IsolateThread thread,
                                                                                          DynamicMappingType mappingType,
                                                                                          ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            List<List<SeriesMetadata>> metadata = DynamicMappingHandler.getMetadata(mappingType);
            DataframeMetadataPointer dataframeMetadataArray = UnmanagedMemory.calloc(metadata.size() * SizeOf.get(DataframeMetadataPointer.class));
            int i = 0;
            for (List<SeriesMetadata> dataframeMetadata : metadata) {
                CTypeUtil.createSeriesMetadata(dataframeMetadata, dataframeMetadataArray.addressOf(i));
                i++;
            }
            DataframesMetadataPointer res = UnmanagedMemory.calloc(SizeOf.get(DataframesMetadataPointer.class));
            res.setDataframesMetadata(dataframeMetadataArray);
            res.setDataframesCount(metadata.size());
            return res;
        });
    }

    @CEntryPoint(name = "getSupportedModels")
    public static ArrayPointer<CCharPointerPointer> getSupportedModels(IsolateThread thread,
                                                                      DynamicMappingType mappingType,
                                                                      ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> Util.createCharPtrArray(List.copyOf(DynamicMappingHandler.getSupportedModels(mappingType))));
    }

    @CEntryPoint(name = "addEventMappings")
    public static void addEventMappings(IsolateThread thread, ObjectHandle eventMappingHandle,
                                        EventMappingType mappingType,
                                        DataframePointer mappingDataframePtr,
                                        ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            PythonEventModelsSupplier eventMapping = ObjectHandles.getGlobal().get(eventMappingHandle);
            UpdatingDataframe mappingDataframe = createDataframe(mappingDataframePtr);
            EventMappingHandler.addElements(mappingType, eventMapping, mappingDataframe);
        });
    }

    @CEntryPoint(name = "getEventMappingsMetaData")
    public static DataframeMetadataPointer getEventMappingsMetaData(IsolateThread thread,
                                                                    EventMappingType mappingType,
                                                                    ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> CTypeUtil.createSeriesMetadata(EventMappingHandler.getMetadata(mappingType)));
    }

    @CEntryPoint(name = "addOutputVariables")
    public static void addOutputVariables(IsolateThread thread,
                                          ObjectHandle outputVariablesHandle,
                                          CCharPointer dynamicIdPtr,
                                          CCharPointerPointer variablesPtrPtr,
                                          int variableCount,
                                          boolean isDynamic,
                                          OutputVariableType variableType,
                                          ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String dynamicId = CTypeUtil.toString(dynamicIdPtr);
            List<String> variables = toStringList(variablesPtrPtr, variableCount);
            PythonOutputVariablesSupplier outputVariablesSupplier = ObjectHandles.getGlobal().get(outputVariablesHandle);
            outputVariablesSupplier.addOutputVariables(dynamicId, variables, isDynamic, convert(variableType));
        });
    }

    @CEntryPoint(name = "getDynamicSimulationResultsStatus")
    public static DynamicSimulationStatus getDynamicSimulationResultsStatus(IsolateThread thread,
             ObjectHandle resultsHandle,
             ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DynamicSimulationResult simulationResult = ObjectHandles.getGlobal().get(resultsHandle);
            return convert(simulationResult.getStatus());
        });
    }

    @CEntryPoint(name = "getDynamicSimulationResultsStatusText")
    public static CCharPointer getDynamicSimulationResultsStatusText(IsolateThread thread,
                                                                 ObjectHandle resultsHandle,
                                                                 ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DynamicSimulationResult simulationResult = ObjectHandles.getGlobal().get(resultsHandle);
            return CTypeUtil.toCharPtr(simulationResult.getStatusText());
        });
    }

    @CEntryPoint(name = "getDynamicCurve")
    public static ArrayPointer<SeriesPointer> getDynamicCurve(IsolateThread thread,
                                                              ObjectHandle resultHandle,
                                                              CCharPointer curveNamePtr,
                                                              ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DynamicSimulationResult result = ObjectHandles.getGlobal().get(resultHandle);
            String curveName = CTypeUtil.toString(curveNamePtr);
            DoubleTimeSeries curve = result.getCurve(curveName);
            return Dataframes.createCDataframe(DynamicSimulationDataframeMappersUtils.curvesDataFrameMapper(curveName), curve);
        });
    }

    @CEntryPoint(name = "getAllDynamicCurvesIds")
    public static ArrayPointer<CCharPointerPointer> getAllDynamicCurvesIds(IsolateThread thread,
                                                                           ObjectHandle resultHandle,
                                                                           ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DynamicSimulationResult result = ObjectHandles.getGlobal().get(resultHandle);
            return Util.createCharPtrArray(new ArrayList<>(result.getCurves().keySet()));
        });
    }

    @CEntryPoint(name = "getFinalStateValues")
    public static ArrayPointer<SeriesPointer> getFinalStateValues(IsolateThread thread, ObjectHandle resultHandle,
                                                                 ExceptionHandlerPointer exceptionHandlerPtr) {
        DynamicSimulationResult result = ObjectHandles.getGlobal().get(resultHandle);
        return Dataframes.createCDataframe(DynamicSimulationDataframeMappersUtils.fsvDataFrameMapper(), result.getFinalStateValues());
    }

    @CEntryPoint(name = "getTimeline")
    public static ArrayPointer<SeriesPointer> getTimeline(IsolateThread thread,
                                                                     ObjectHandle resultsHandle,
                                                                     ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DynamicSimulationResult simulationResult = ObjectHandles.getGlobal().get(resultsHandle);
            return Dataframes.createCDataframe(DynamicSimulationDataframeMappersUtils.timelineEventDataFrameMapper(), simulationResult.getTimeLine());
        });
    }
}
