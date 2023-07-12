/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.openreac;

import static com.powsybl.python.commons.Util.doCatch;

import java.util.List;
import java.util.Map;

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
import com.powsybl.python.commons.PyPowsyblApiHeader.OpenReacObjective;
import com.powsybl.python.commons.PyPowsyblApiHeader.OpenReacStatus;
import com.powsybl.python.commons.PyPowsyblApiHeader.StringMap;
import com.powsybl.python.commons.Util;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 */
@CContext(Directives.class)
public final class OpenReacCFunctions {
    private OpenReacCFunctions() {
    }

    private static Logger logger() {
        return LoggerFactory.getLogger(OpenReacCFunctions.class);
    }

    @CEntryPoint(name = "createOpenReacParams")
    public static ObjectHandle createOpenReacParams(IsolateThread thread,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> ObjectHandles.getGlobal().create(new OpenReacParameters()));
    }

    @CEntryPoint(name = "createVoltageLimitOverride")
    public static ObjectHandle createVoltageLimitOverride(IsolateThread thread, double minVoltage, double maxVoltage,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr,
            () -> ObjectHandles.getGlobal().create(new VoltageLimitOverride(minVoltage, maxVoltage)));
    }

    @CEntryPoint(name = "OpenReacAddSpecificVoltageLimits")
    public static void openReacAddSpecificVoltageLimits(IsolateThread thread, CCharPointer idPtr, double minVoltage,
            ObjectHandle paramsHandle, double maxVoltage,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
        String voltageId = CTypeUtil.toString(idPtr);
        doCatch(exceptionHandlerPtr, () -> params
            .addSpecificVoltageLimits(Map.of(voltageId, new VoltageLimitOverride(minVoltage, maxVoltage))));
    }

    @CEntryPoint(name = "OpenReacAddVariableShuntCompensators")
    public static void openReacAddVariableShuntCompensators(IsolateThread thread, ObjectHandle paramsHandle,
            CCharPointer idPtr,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
        String id = CTypeUtil.toString(idPtr);
        doCatch(exceptionHandlerPtr, () -> params.addVariableShuntCompensators(List.of(id)));
    }

    @CEntryPoint(name = "OpenReacAddConstantQGenerators")
    public static void openReacAddConstantQGenerators(IsolateThread thread, ObjectHandle paramsHandle,
            CCharPointer idPtr,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
        String id = CTypeUtil.toString(idPtr);
        doCatch(exceptionHandlerPtr, () -> params.addConstantQGenerators(List.of(id)));
    }

    @CEntryPoint(name = "OpenReacAddVariableTwoWindingsTransformers")
    public static void openReacAddVariableTwoWindingsTransformers(IsolateThread thread, ObjectHandle paramsHandle,
            CCharPointer idPtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
        String id = CTypeUtil.toString(idPtr);
        doCatch(exceptionHandlerPtr, () -> params.addVariableTwoWindingsTransformers(List.of(id)));
    }

    @CEntryPoint(name = "OpenReacAddAlgorithmParam")
    public static void openReacAddAlgorithmParam(IsolateThread thread, ObjectHandle paramsHandle, CCharPointer keyPtr,
            CCharPointer valuePtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
        doCatch(exceptionHandlerPtr,
            () -> params.addAlgorithmParam(CTypeUtil.toString(keyPtr), CTypeUtil.toString(valuePtr)));
    }

    @CEntryPoint(name = "OpenReacSetObjective")
    public static void openReacSetObjective(IsolateThread thread, ObjectHandle paramsHandle,
            OpenReacObjective cObjective,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
        doCatch(exceptionHandlerPtr, () -> params.setObjective(Util.convert(cObjective)));
    }

    @CEntryPoint(name = "OpenReacSetObjectiveDistance")
    public static void openReacSetObjectiveDistance(IsolateThread thread, ObjectHandle paramsHandle, double dist,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
        doCatch(exceptionHandlerPtr, () -> params.setObjectiveDistance(dist));
    }

    @CEntryPoint(name = "OpenReacApplyAllModifications")
    public static void openReacApplyAllModifications(IsolateThread thread, ObjectHandle resultHandle,
            ObjectHandle networkHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacResult result = ObjectHandles.getGlobal().get(resultHandle);
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        doCatch(exceptionHandlerPtr, () -> result.applyAllModifications(network));
    }

    @CEntryPoint(name = "OpenReacGetStatus")
    public static OpenReacStatus openReacGetStatus(IsolateThread thread, ObjectHandle resultHandle,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacResult result = ObjectHandles.getGlobal().get(resultHandle);
        return doCatch(exceptionHandlerPtr, () -> Util.convert(result.getStatus()));
    }

    @CEntryPoint(name = "OpenReacGetIndicators")
    public static StringMap openReacGetIndicators(IsolateThread thread, ObjectHandle resultHandle,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        OpenReacResult result = ObjectHandles.getGlobal().get(resultHandle);
        return doCatch(exceptionHandlerPtr, () -> CTypeUtil.fromStringMap(result.getIndicators()));
    }

    @CEntryPoint(name = "runOpenReac")
    public static ObjectHandle runOpenReac(IsolateThread thread, boolean debug, ObjectHandle networkHandle,
            ObjectHandle paramsHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);

        logger().info("Launching an OpenReac run");
        OpenReacResult result = OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(), params,
                new OpenReacConfig(debug), LocalComputationManager.getDefault());
        logger().info("OpenReac run done.");
        return ObjectHandles.getGlobal().create(result);
    }

}
