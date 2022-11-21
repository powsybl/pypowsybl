/**
 * Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.dynamic;

import static com.powsybl.python.commons.Util.doCatch;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.dynamicsimulation.CurvesSupplier;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.EventModelsSupplier;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;

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
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new DynamicModelMapper()));
    }

    @CEntryPoint(name = "createTimeseriesMapping")
    public static ObjectHandle createTimeseriesMapping(IsolateThread thread,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new CurveMappingSupplier()));
    }

    @CEntryPoint(name = "createEventMapping")
    public static ObjectHandle createEventMapping(IsolateThread thread,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new EventSupplier()));
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
            DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
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

    @CEntryPoint(name = "addAlphaBetaLoad")
    public static void addAlphaBetaLoad(IsolateThread thread, ObjectHandle dynamicMappingHandle,
            CCharPointer staticIdPtr,
            CCharPointer dynamicParamPtr,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String staticId = CTypeUtil.toString(staticIdPtr);
            String dynamicParam = CTypeUtil.toString(dynamicParamPtr);
            DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
            dynamicMapping.addAlphaBetaLoad(staticId, dynamicParam);
        });
    }

    @CEntryPoint(name = "addOneTransformerLoad")
    public static void addOneTransformerLoad(IsolateThread thread, ObjectHandle dynamicMappingHandle,
            CCharPointer staticIdPtr,
            CCharPointer dynamicParamPtr,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String staticId = CTypeUtil.toString(staticIdPtr);
            String dynamicParam = CTypeUtil.toString(dynamicParamPtr);
            DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
            dynamicMapping.addOneTransformerLoad(staticId, dynamicParam);
        });
    }

    @CEntryPoint(name = "addOmegaRef")
    public static void addOmegaRef(IsolateThread thread, ObjectHandle dynamicMappingHandle, CCharPointer generatorIdPtr,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String generatorId = CTypeUtil.toString(generatorIdPtr);
            DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
            dynamicMapping.addOmegaRef(generatorId);
        });
    }

    @CEntryPoint(name = "addGeneratorSynchronousThreeWindings")
    public static void addGeneratorSynchronousThreeWindings(IsolateThread thread, ObjectHandle dynamicMappingHandle,
            CCharPointer staticIdPtr,
            CCharPointer dynamicParamPtr,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String staticId = CTypeUtil.toString(staticIdPtr);
            String dynamicParam = CTypeUtil.toString(dynamicParamPtr);
            DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
            dynamicMapping.addGeneratorSynchronousThreeWindings(staticId,
                    dynamicParam);
        });
    }

    @CEntryPoint(name = "addGeneratorSynchronousThreeWindingsProportionalRegulations")
    public static void addGeneratorSynchronousThreeWindingsProportionalRegulations(IsolateThread thread,
            ObjectHandle dynamicMappingHandle,
            CCharPointer staticIdPtr,
            CCharPointer dynamicParamPtr,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String staticId = CTypeUtil.toString(staticIdPtr);
            String dynamicParam = CTypeUtil.toString(dynamicParamPtr);
            DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
            dynamicMapping.addGeneratorSynchronousThreeWindingsProportionalRegulations(staticId,
                    dynamicParam);
        });
    }

    @CEntryPoint(name = "addGeneratorSynchronousFourWindings")
    public static void addGeneratorSynchronousFourWindings(IsolateThread thread, ObjectHandle dynamicMappingHandle,
            CCharPointer staticIdPtr,
            CCharPointer dynamicParamPtr,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String staticId = CTypeUtil.toString(staticIdPtr);
            String dynamicParam = CTypeUtil.toString(dynamicParamPtr);
            DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
            dynamicMapping.addGeneratorSynchronousFourWindings(staticId,
                    dynamicParam);
        });
    }

    @CEntryPoint(name = "addGeneratorSynchronousFourWindingsProportionalRegulations")
    public static void addGeneratorSynchronousFourWindingsProportionalRegulations(IsolateThread thread,
            ObjectHandle dynamicMappingHandle,
            CCharPointer staticIdPtr,
            CCharPointer dynamicParamPtr,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String staticId = CTypeUtil.toString(staticIdPtr);
            String dynamicParam = CTypeUtil.toString(dynamicParamPtr);
            DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
            dynamicMapping.addGeneratorSynchronousFourWindingsProportionalRegulations(staticId,
                    dynamicParam);
        });
    }

    @CEntryPoint(name = "addCurrentLimitAutomaton")
    public static void addCurrentLimitAutomaton(IsolateThread thread,
            ObjectHandle dynamicMappingHandle,
            CCharPointer staticIdPtr,
            CCharPointer dynamicParamPtr,
            CCharPointer sideStrPtr,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String staticId = CTypeUtil.toString(staticIdPtr);
            String dynamicParam = CTypeUtil.toString(dynamicParamPtr);
            String sideStr = CTypeUtil.toString(sideStrPtr);
            DynamicModelMapper dynamicMapping = ObjectHandles.getGlobal().get(dynamicMappingHandle);
            Branch.Side side;
            switch (sideStr) {
                case "one":
                    side = Branch.Side.ONE;
                    break;
                case "two":
                    side = Branch.Side.TWO;
                    break;
                default:
                    side = null; // will throw
            }
            dynamicMapping.addCurrentLimitAutomaton(staticId, dynamicParam,
                    side);
        });
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
            CurveMappingSupplier timeSeriesSupplier = ObjectHandles.getGlobal().get(timeseriesSupplier);
            timeSeriesSupplier.addCurve(dynamicId, variable);
        });
    }

    @CEntryPoint(name = "addEventQuadripoleDisconnection")
    public static void addEventQuadripoleDisconnection(IsolateThread thread,
            ObjectHandle eventSupplierHandle,
            CCharPointer eventModelIdPtr,
            CCharPointer staticIdPtr,
            CCharPointer parameterSetIdPtr,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String eventModelId = CTypeUtil.toString(eventModelIdPtr);
            String staticId = CTypeUtil.toString(staticIdPtr);
            String parameterSetId = CTypeUtil.toString(parameterSetIdPtr);
            EventSupplier eventSupplier = ObjectHandles.getGlobal().get(eventSupplierHandle);
            eventSupplier.addEventQuadripoleDisconnection(eventModelId,
                    staticId, parameterSetId);
        });
    }

    @CEntryPoint(name = "addEventSetPointBoolean")
    public static void addEventSetPointBoolean(IsolateThread thread,
            ObjectHandle eventSupplierHandle,
            CCharPointer eventModelIdPtr,
            CCharPointer staticIdPtr,
            CCharPointer parameterSetIdPtr,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String eventModelId = CTypeUtil.toString(eventModelIdPtr);
            String staticId = CTypeUtil.toString(staticIdPtr);
            String parameterSetId = CTypeUtil.toString(parameterSetIdPtr);
            EventSupplier eventSupplier = ObjectHandles.getGlobal().get(eventSupplierHandle);
            eventSupplier.addEventSetPointBoolean(eventModelId, staticId,
                    parameterSetId);
        });
    }

    @CEntryPoint(name = "setPowSyBlConfigLocation")
    public static void setPowSyBlConfigLocation(IsolateThread thread,
            CCharPointer absolutePathToConfig,
            CCharPointer configFileName,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        // TODO: create/find a way to programmatically modify a plateform config
        doCatch(exceptionHandlerPtr, () -> {
            System.setProperty("powsybl.config.dirs", CTypeUtil.toString(absolutePathToConfig));
            // will try to find first matching extension .yml .xml .property
            System.setProperty("powsybl.config.name", CTypeUtil.toString(configFileName));
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
}
