/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.voltageinit;

import static com.powsybl.python.commons.Util.doCatch;

import java.util.List;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Network;
import com.powsybl.openreac.OpenReacConfig;
import com.powsybl.openreac.OpenReacRunner;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.VoltageLimitOverride;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblApiHeader.StringMap;
import com.powsybl.python.commons.PyPowsyblApiHeader.VoltageInitializerObjective;
import com.powsybl.python.commons.PyPowsyblApiHeader.VoltageInitializerStatus;
import com.powsybl.python.commons.Util;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 */
@CContext(Directives.class)
public final class VoltageInitializerCFunctions {
    private VoltageInitializerCFunctions() {
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(VoltageInitializerCFunctions.class);
    }

    @CEntryPoint(name = "createVoltageInitializerParams")
    public static ObjectHandle createVoltageInitializerParams(IsolateThread thread,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new OpenReacParameters()));
    }

    @CEntryPoint(name = "createLowVoltageLimitOverride")
    public static ObjectHandle createLowVoltageLimitOverride(IsolateThread thread,
                                                          CCharPointer idPtr, boolean isRelative, double limit,
                                                          PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr,
            () -> ObjectHandles.getGlobal().create(new VoltageLimitOverride(
                    CTypeUtil.toString(idPtr),
                    VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT,
                    isRelative,
                    limit))
        );
    }

    @CEntryPoint(name = "createHighVoltageLimitOverride")
    public static ObjectHandle createHighVoltageLimitOverride(IsolateThread thread,
                                                          CCharPointer idPtr, boolean isRelative, double limit,
                                                          PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr,
                () -> ObjectHandles.getGlobal().create(new VoltageLimitOverride(
                        CTypeUtil.toString(idPtr),
                        VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT,
                        isRelative,
                        limit))
        );
    }

    @CEntryPoint(name = "voltageInitializerAddSpecificLowVoltageLimits")
    public static void addSpecificLowVoltageLimits(IsolateThread thread, ObjectHandle paramsHandle,
                                                   CCharPointer idPtr, boolean isRelative, double limit,
                                                   PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
        String voltageLevelId = CTypeUtil.toString(idPtr);
        doCatch(exceptionHandlerPtr, () -> params
                .addSpecificVoltageLimits(List.of(
                        new VoltageLimitOverride(voltageLevelId,
                                                 VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT,
                                                 isRelative,
                                                 limit)
                )));
    }

    @CEntryPoint(name = "voltageInitializerAddSpecificHighVoltageLimits")
    public static void addSpecificHighVoltageLimits(IsolateThread thread, ObjectHandle paramsHandle,
                                                    CCharPointer idPtr, boolean isRelative, double limit,
                                                    PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
        String voltageLevelId = CTypeUtil.toString(idPtr);
        doCatch(exceptionHandlerPtr, () -> params
                .addSpecificVoltageLimits(List.of(
                        new VoltageLimitOverride(voltageLevelId,
                                                 VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT,
                                                 isRelative,
                                                 limit)
                )));
    }

    @CEntryPoint(name = "voltageInitializerAddVariableShuntCompensators")
    public static void addVariableShuntCompensators(IsolateThread thread, ObjectHandle paramsHandle,
            CCharPointer idPtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
        String id = CTypeUtil.toString(idPtr);
        doCatch(exceptionHandlerPtr, () -> params.addVariableShuntCompensators(List.of(id)));
    }

    @CEntryPoint(name = "voltageInitializerAddConstantQGenerators")
    public static void addConstantQGenerators(IsolateThread thread, ObjectHandle paramsHandle,
            CCharPointer idPtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
        String id = CTypeUtil.toString(idPtr);
        doCatch(exceptionHandlerPtr, () -> params.addConstantQGenerators(List.of(id)));
    }

    @CEntryPoint(name = "voltageInitializerAddVariableTwoWindingsTransformers")
    public static void addVariableTwoWindingsTransformers(IsolateThread thread, ObjectHandle paramsHandle,
            CCharPointer idPtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
        String id = CTypeUtil.toString(idPtr);
        doCatch(exceptionHandlerPtr, () -> params.addVariableTwoWindingsTransformers(List.of(id)));
    }

    @CEntryPoint(name = "voltageInitializerAddAlgorithmParam")
    public static void addAlgorithmParam(IsolateThread thread, ObjectHandle paramsHandle, CCharPointer keyPtr,
            CCharPointer valuePtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
        doCatch(exceptionHandlerPtr,
            () -> params.addAlgorithmParam(CTypeUtil.toString(keyPtr), CTypeUtil.toString(valuePtr)));
    }

    @CEntryPoint(name = "voltageInitializerSetObjective")
    public static void setObjective(IsolateThread thread, ObjectHandle paramsHandle,
            VoltageInitializerObjective cObjective, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
        doCatch(exceptionHandlerPtr, () -> params.setObjective(Util.convert(cObjective)));
    }

    @CEntryPoint(name = "voltageInitializerSetObjectiveDistance")
    public static void setObjectiveDistance(IsolateThread thread, ObjectHandle paramsHandle, double dist,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
        doCatch(exceptionHandlerPtr, () -> params.setObjectiveDistance(dist));
    }

    @CEntryPoint(name = "voltageInitializerApplyAllModifications")
    public static void applyAllModifications(IsolateThread thread, ObjectHandle resultHandle,
            ObjectHandle networkHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacResult result = ObjectHandles.getGlobal().get(resultHandle);
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        doCatch(exceptionHandlerPtr, () -> result.applyAllModifications(network));
    }

    @CEntryPoint(name = "voltageInitializerGetStatus")
    public static VoltageInitializerStatus getStatus(IsolateThread thread, ObjectHandle resultHandle,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacResult result = ObjectHandles.getGlobal().get(resultHandle);
        return doCatch(exceptionHandlerPtr, () -> Util.convert(result.getStatus()));
    }

    @CEntryPoint(name = "voltageInitializerGetIndicators")
    public static StringMap getIndicators(IsolateThread thread, ObjectHandle resultHandle,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacResult result = ObjectHandles.getGlobal().get(resultHandle);
        return doCatch(exceptionHandlerPtr, () -> CTypeUtil.fromStringMap(result.getIndicators()));
    }

    @CEntryPoint(name = "runVoltageInitializer")
    public static ObjectHandle runVoltageInitializer(IsolateThread thread, boolean debug, ObjectHandle networkHandle,
            ObjectHandle paramsHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return Util.doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);

            logger().info("Running voltage initializer");
            OpenReacResult result = OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(), params,
                    new OpenReacConfig(debug), LocalComputationManager.getDefault());
            logger().info("Voltage initializer run done");

            return ObjectHandles.getGlobal().create(result);
        });
    }

}
