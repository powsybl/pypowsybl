/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import static com.powsybl.python.commons.Util.doCatch;
import static com.powsybl.python.network.NetworkCFunctions.createDataframe;

import java.util.ArrayList;
import java.util.List;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.python.report.ReportCUtils;
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

import com.powsybl.dataframe.dynamic.CurvesSeries;
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
import com.powsybl.python.network.Dataframes;
import com.powsybl.timeseries.DoublePoint;
import com.powsybl.timeseries.TimeSeries;

import static com.powsybl.python.commons.PyPowsyblApiHeader.*;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
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
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new PythonCurveSupplier()));
    }

    @CEntryPoint(name = "createEventMapping")
    public static ObjectHandle createEventMapping(IsolateThread thread,
            ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new PythonEventModelsSupplier()));
    }

    @CEntryPoint(name = "runDynamicModel")
    public static ObjectHandle runDynamicModel(IsolateThread thread,
                                               ObjectHandle dynamicContextHandle,
                                               ObjectHandle networkHandle,
                                               ObjectHandle dynamicMappingHandle,
                                               ObjectHandle eventModelsSupplierHandle,
                                               ObjectHandle curvesSupplierHandle,
                                               int startTime,
                                               int stopTime,
                                               ObjectHandle reportNodeHandle,
            ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DynamicSimulationContext dynamicContext = ObjectHandles.getGlobal().get(dynamicContextHandle);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            PythonDynamicModelsSupplier dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
            EventModelsSupplier eventModelsSupplier = ObjectHandles.getGlobal().get(eventModelsSupplierHandle);
            OutputVariablesSupplier curvesSupplier = ObjectHandles.getGlobal().get(curvesSupplierHandle);
            ReportNode reportNode = ReportCUtils.getReportNode(reportNodeHandle);
            DynamicSimulationParameters dynamicSimulationParameters = new DynamicSimulationParameters(startTime,
                    stopTime);
            DynamicSimulationResult result = dynamicContext.run(network,
                    dynamicMapping,
                    eventModelsSupplier,
                    curvesSupplier,
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

    @CEntryPoint(name = "addCurve")
    public static void addCurve(IsolateThread thread,
            ObjectHandle timeseriesSupplier,
            CCharPointer dynamicIdPtr,
            CCharPointer variablePtr,
            ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String dynamicId = CTypeUtil.toString(dynamicIdPtr);
            String variable = CTypeUtil.toString(variablePtr);
            PythonCurveSupplier timeSeriesSupplier = ObjectHandles.getGlobal().get(timeseriesSupplier);
            timeSeriesSupplier.addCurve(dynamicId, variable);
        });
    }

    @CEntryPoint(name = "getDynamicSimulationResultsStatus")
    public static CCharPointer getDynamicSimulationResultsStatus(IsolateThread thread,
             ObjectHandle dynamicSimulationResultsHandle,
             ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DynamicSimulationResult simulationResult = ObjectHandles.getGlobal().get(dynamicSimulationResultsHandle);
            return CTypeUtil.toCharPtr(simulationResult.isOk() ? "Ok" : "Not OK");
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
            TimeSeries<DoublePoint, ?> curve = result.getCurve(curveName);
            return Dataframes.createCDataframe(CurvesSeries.curvesDataFrameMapper(curveName), curve);
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

}
