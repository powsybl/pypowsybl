/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import static com.powsybl.python.commons.Util.doCatch;

import java.util.ArrayList;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.dataframe.dynamic.CurvesSeries;
import com.powsybl.dataframe.dynamic.adders.DynamicMappingAdderFactory;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynamicsimulation.CurvesSupplier;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.EventModelsSupplier;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.DataframeMetadataPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.DataframePointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.DynamicMappingType;
import com.powsybl.python.commons.PyPowsyblApiHeader.SeriesPointer;
import com.powsybl.python.commons.Util;
import com.powsybl.python.network.Dataframes;
import com.powsybl.python.network.NetworkCFunctions;
import com.powsybl.timeseries.DoublePoint;
import com.powsybl.timeseries.TimeSeries;

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
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new DynamicSimulationContext()));
    }

    @CEntryPoint(name = "createDynamicModelMapping")
    public static ObjectHandle createDynamicModelMapping(IsolateThread thread,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new PythonDynamicModelsSupplier()));
    }

    @CEntryPoint(name = "createTimeseriesMapping")
    public static ObjectHandle createTimeseriesMapping(IsolateThread thread,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new PythonCurveSupplier()));
    }

    @CEntryPoint(name = "createEventMapping")
    public static ObjectHandle createEventMapping(IsolateThread thread,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new PythonEventModelsSupplier()));
    }

    @CEntryPoint(name = "runDynamicModel")
    public static ObjectHandle runDynamicModel(IsolateThread thread,
            ObjectHandle dynamicContextHandle,
            ObjectHandle networkHandle,
            ObjectHandle dynamicMappingHandle,
            ObjectHandle eventModelsSupplierHandle,
            ObjectHandle curvesSupplierHandle,
            int startTime, int stopTime,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DynamicSimulationContext dynamicContext = ObjectHandles.getGlobal().get(dynamicContextHandle);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            PythonDynamicModelsSupplier dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
            EventModelsSupplier eventModelsSupplier = ObjectHandles.getGlobal().get(eventModelsSupplierHandle);
            CurvesSupplier curvesSupplier = ObjectHandles.getGlobal().get(curvesSupplierHandle);
            DynamicSimulationParameters dynamicSimulationParameters = new DynamicSimulationParameters(startTime,
                    stopTime);
            DynamicSimulationResult result = dynamicContext.run(network,
                    dynamicMapping,
                    eventModelsSupplier,
                    curvesSupplier,
                    dynamicSimulationParameters);
            logger().info("Dynamic simulation ran successfully in java");
            return ObjectHandles.getGlobal().create(result);
        });
    }

    @CEntryPoint(name = "addDynamicMappings")
    public static void addDynamicMapping(IsolateThread thread, ObjectHandle dynamicMappingHandle,
            DynamicMappingType mappingType,
            DataframePointer mappingDataframePtr,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            PythonDynamicModelsSupplier dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
            UpdatingDataframe mappingDataframe = NetworkCFunctions.createDataframe(mappingDataframePtr);
            DynamicMappingAdderFactory.getAdder(mappingType).addElements(dynamicMapping, mappingDataframe);
        });
    }

    @CEntryPoint(name = "getDynamicMappingsMetaData")
    public static DataframeMetadataPointer getDynamicMappingsMetaData(IsolateThread thread,
            DynamicMappingType mappingType,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> CTypeUtil.createSeriesMetadata(DynamicMappingAdderFactory.getAdder(mappingType).getMetadata()));
    }

    @CEntryPoint(name = "addCurve")
    public static void addCurve(IsolateThread thread,
            ObjectHandle timeseriesSupplier,
            CCharPointer dynamicIdPtr,
            CCharPointer variablePtr,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String dynamicId = CTypeUtil.toString(dynamicIdPtr);
            String variable = CTypeUtil.toString(variablePtr);
            PythonCurveSupplier timeSeriesSupplier = ObjectHandles.getGlobal().get(timeseriesSupplier);
            timeSeriesSupplier.addCurve(dynamicId, variable);
        });
    }

    @CEntryPoint(name = "addEventDisconnection")
    public static void addEventDisconnection(IsolateThread thread,
            ObjectHandle eventSupplierHandle,
            CCharPointer staticIdPtr,
            double eventTime,
            int disconnectOnly,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String staticId = CTypeUtil.toString(staticIdPtr);
            PythonEventModelsSupplier pythonEventModelsSupplier = ObjectHandles.getGlobal().get(eventSupplierHandle);
            pythonEventModelsSupplier.addEventDisconnection(staticId, eventTime, Util.convert(PyPowsyblApiHeader.ThreeSideType.fromCValue(disconnectOnly)).toTwoSides());
        });
    }

    @CEntryPoint(name = "getDynamicSimulationResultsStatus")
    public static CCharPointer getDynamicSimulationResultsStatus(IsolateThread thread,
             ObjectHandle dynamicSimulationResultsHandle,
             PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DynamicSimulationResult simulationResult = ObjectHandles.getGlobal().get(dynamicSimulationResultsHandle);
            return CTypeUtil.toCharPtr(simulationResult.isOk() ? "Ok" : "Not OK");
        });
    }

    @CEntryPoint(name = "getDynamicCurve")
    public static ArrayPointer<SeriesPointer> getDynamicCurve(IsolateThread thread,
            ObjectHandle resultHandle,
            CCharPointer curveNamePtr,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
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
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DynamicSimulationResult result = ObjectHandles.getGlobal().get(resultHandle);
            return Util.createCharPtrArray(new ArrayList<>(result.getCurves().keySet()));
        });
    }

}
