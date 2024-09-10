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
import java.util.function.Supplier;

import com.powsybl.python.commons.Util.PointerProvider;
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
import com.powsybl.python.commons.PyPowsyblApiHeader.VoltageInitializerLogLevelAmpl;
import com.powsybl.python.commons.PyPowsyblApiHeader.VoltageInitializerLogLevelSolver;
import com.powsybl.python.commons.PyPowsyblApiHeader.VoltageInitializerReactiveSlackBusesMode;
import com.powsybl.python.commons.PyPowsyblApiHeader.VoltageInitializerStatus;
import com.powsybl.python.commons.Util;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 */
@SuppressWarnings({"java:S1602", "java:S1604"})
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
        return doCatch(exceptionHandlerPtr, new PointerProvider<ObjectHandle>() {
            @Override
            public ObjectHandle get() {
                return ObjectHandles.getGlobal().create(new OpenReacParameters());
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerAddSpecificLowVoltageLimits")
    public static void addSpecificLowVoltageLimits(IsolateThread thread, ObjectHandle paramsHandle,
                                                   CCharPointer idPtr, boolean isRelative, double limit,
                                                   PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                String voltageLevelId = CTypeUtil.toString(idPtr);
                params.addSpecificVoltageLimits(List.of(
                                new VoltageLimitOverride(voltageLevelId,
                                        VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT,
                                        isRelative,
                                        limit)
                        ));
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerAddSpecificHighVoltageLimits")
    public static void addSpecificHighVoltageLimits(IsolateThread thread, ObjectHandle paramsHandle,
                                                    CCharPointer idPtr, boolean isRelative, double limit,
                                                    PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                String voltageLevelId = CTypeUtil.toString(idPtr);
                params.addSpecificVoltageLimits(List.of(
                                new VoltageLimitOverride(voltageLevelId,
                                        VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT,
                                        isRelative,
                                        limit)
                        ));
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerAddVariableShuntCompensators")
    public static void addVariableShuntCompensators(IsolateThread thread, ObjectHandle paramsHandle,
            CCharPointer idPtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                String id = CTypeUtil.toString(idPtr);
                params.addVariableShuntCompensators(List.of(id));
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerAddConstantQGenerators")
    public static void addConstantQGenerators(IsolateThread thread, ObjectHandle paramsHandle,
            CCharPointer idPtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                String id = CTypeUtil.toString(idPtr);
                params.addConstantQGenerators(List.of(id));
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerAddVariableTwoWindingsTransformers")
    public static void addVariableTwoWindingsTransformers(IsolateThread thread, ObjectHandle paramsHandle,
            CCharPointer idPtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                String id = CTypeUtil.toString(idPtr);
                params.addVariableTwoWindingsTransformers(List.of(id));
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerAddConfiguredReactiveSlackBuses")
    public static void addConfiguredReactiveSlackBuses(IsolateThread thread, ObjectHandle paramsHandle,
            CCharPointer idPtr, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                String id = CTypeUtil.toString(idPtr);
                params.addConfiguredReactiveSlackBuses(List.of(id));
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetObjective")
    public static void setObjective(IsolateThread thread, ObjectHandle paramsHandle,
            VoltageInitializerObjective cObjective, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setObjective(Util.convert(cObjective));
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetObjectiveDistance")
    public static void setObjectiveDistance(IsolateThread thread, ObjectHandle paramsHandle, double dist,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setObjectiveDistance(dist);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetLogLevelAmpl")
    public static void setLogLevelAmpl(IsolateThread thread, ObjectHandle paramsHandle, VoltageInitializerLogLevelAmpl logLevelAmpl,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setLogLevelAmpl(Util.convert(logLevelAmpl));
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetLogLevelSolver")
    public static void setLogLevelSolver(IsolateThread thread, ObjectHandle paramsHandle, VoltageInitializerLogLevelSolver logLevelSolver,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setLogLevelSolver(Util.convert(logLevelSolver));
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetReactiveSlackBusesMode")
    public static void setReactiveSlackBusesMode(IsolateThread thread, ObjectHandle paramsHandle, VoltageInitializerReactiveSlackBusesMode reactiveSlackBusesMode,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setReactiveSlackBusesMode(Util.convert(reactiveSlackBusesMode));
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetMinPlausibleLowVoltageLimit")
    public static void voltageInitializerSetMinPlausibleLowVoltageLimit(IsolateThread thread, ObjectHandle paramsHandle, double minPlausibleLowVoltageLimit,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setMinPlausibleLowVoltageLimit(minPlausibleLowVoltageLimit);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetMaxPlausibleHighVoltageLimit")
    public static void voltageInitializerSetMaxPlausibleHighVoltageLimit(IsolateThread thread, ObjectHandle paramsHandle, double maxPlausibleHighVoltageLimit,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setMaxPlausibleHighVoltageLimit(maxPlausibleHighVoltageLimit);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetActivePowerVariationRate")
    public static void voltageInitializerSetActivePowerVariationRate(IsolateThread thread, ObjectHandle paramsHandle, double activePowerVariationRate,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setActivePowerVariationRate(activePowerVariationRate);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetMinPlausibleActivePowerThreshold")
    public static void voltageInitializerSetMinPlausibleActivePowerThreshold(IsolateThread thread, ObjectHandle paramsHandle, double minPlausibleActivePowerThreshold,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setMinPlausibleActivePowerThreshold(minPlausibleActivePowerThreshold);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetLowImpedanceThreshold")
    public static void voltageInitializerSetLowImpedanceThreshold(IsolateThread thread, ObjectHandle paramsHandle, double lowImpedanceThreshold,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setLowImpedanceThreshold(lowImpedanceThreshold);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetMinNominalVoltageIgnoredBus")
    public static void voltageInitializerSetMinNominalVoltageIgnoredBus(IsolateThread thread, ObjectHandle paramsHandle, double minNominalVoltageIgnoredBus,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setMinNominalVoltageIgnoredBus(minNominalVoltageIgnoredBus);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetMinNominalVoltageIgnoredVoltageBounds")
    public static void voltageInitializerSetMinNominalVoltageIgnoredVoltageBounds(IsolateThread thread, ObjectHandle paramsHandle, double minNominalVoltageIgnoredVoltageBounds,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setMinNominalVoltageIgnoredVoltageBounds(minNominalVoltageIgnoredVoltageBounds);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetMaxPlausiblePowerLimit")
    public static void voltageInitializerSetMaxPlausiblePowerLimit(IsolateThread thread, ObjectHandle paramsHandle, double maxPlausiblePowerLimit,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setPQMax(maxPlausiblePowerLimit);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetDefaultMinimalQPRange")
    public static void voltageInitializerSetDefaultMinimalQPRange(IsolateThread thread, ObjectHandle paramsHandle, double defaultMinimalQPRange,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setDefaultMinimalQPRange(defaultMinimalQPRange);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetHighActivePowerDefaultLimit")
    public static void voltageInitializerSetHighActivePowerDefaultLimit(IsolateThread thread, ObjectHandle paramsHandle, double highActivePowerDefaultLimit,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setHighActivePowerDefaultLimit(highActivePowerDefaultLimit);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetLowActivePowerDefaultLimit")
    public static void voltageInitializerSetLowActivePowerDefaultLimit(IsolateThread thread, ObjectHandle paramsHandle, double lowActivePowerDefaultLimit,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setLowActivePowerDefaultLimit(lowActivePowerDefaultLimit);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetDefaultQmaxPmaxRatio")
    public static void voltageInitializerSetDefaultQmaxPmaxRatio(IsolateThread thread, ObjectHandle paramsHandle, double defaultQmaxPmaxRatio,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setDefaultQmaxPmaxRatio(defaultQmaxPmaxRatio);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetDefaultVariableScalingFactor")
    public static void setDefaultVariableScalingFactor(IsolateThread thread, ObjectHandle paramsHandle, double defaultVariableScalingFactor,
                                            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setDefaultVariableScalingFactor(defaultVariableScalingFactor);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetDefaultConstraintScalingFactor")
    public static void setDefaultConstraintScalingFactor(IsolateThread thread, ObjectHandle paramsHandle, double defaultConstraintScalingFactor,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setDefaultConstraintScalingFactor(defaultConstraintScalingFactor);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetReactiveSlackVariableScalingFactor")
    public static void setReactiveSlackVariableScalingFactor(IsolateThread thread, ObjectHandle paramsHandle, double reactiveSlackVariableScalingFactor,
                                                         PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setReactiveSlackVariableScalingFactor(reactiveSlackVariableScalingFactor);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerSetTwoWindingTransformerRatioVariableScalingFactor")
    public static void setTwoWindingTransformerRatioVariableScalingFactor(IsolateThread thread, ObjectHandle paramsHandle, double twoWindingTransformerRatioVariableScalingFactor,
                                                             PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);
                params.setTwoWindingTransformerRatioVariableScalingFactor(twoWindingTransformerRatioVariableScalingFactor);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerApplyAllModifications")
    public static void applyAllModifications(IsolateThread thread, ObjectHandle resultHandle,
            ObjectHandle networkHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                OpenReacResult result = ObjectHandles.getGlobal().get(resultHandle);
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                result.applyAllModifications(network);
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerGetStatus")
    public static VoltageInitializerStatus getStatus(IsolateThread thread, ObjectHandle resultHandle,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new Supplier<VoltageInitializerStatus>() {
            @Override
            public VoltageInitializerStatus get() {
                OpenReacResult result = ObjectHandles.getGlobal().get(resultHandle);
                return Util.convert(result.getStatus());
            }
        });
    }

    @CEntryPoint(name = "voltageInitializerGetIndicators")
    public static StringMap getIndicators(IsolateThread thread, ObjectHandle resultHandle,
            PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<StringMap>() {
            @Override
            public StringMap get() {
                OpenReacResult result = ObjectHandles.getGlobal().get(resultHandle);
                return CTypeUtil.fromStringMap(result.getIndicators());
            }
        });
    }

    @CEntryPoint(name = "runVoltageInitializer")
    public static ObjectHandle runVoltageInitializer(IsolateThread thread, boolean debug, ObjectHandle networkHandle,
            ObjectHandle paramsHandle, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return Util.doCatch(exceptionHandlerPtr, new PointerProvider<ObjectHandle>() {
            @Override
            public ObjectHandle get() {
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                OpenReacParameters params = ObjectHandles.getGlobal().get(paramsHandle);

                logger().info("Running voltage initializer");
                OpenReacResult result = OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(), params,
                        new OpenReacConfig(debug), LocalComputationManager.getDefault());
                logger().info("Voltage initializer run done");

                return ObjectHandles.getGlobal().create(result);
            }
        });
    }

}
