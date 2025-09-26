/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.modifications.DataframeNetworkModificationType;
import com.powsybl.dataframe.network.modifications.NetworkModifications;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.modification.Replace3TwoWindingsTransformersByThreeWindingsTransformers;
import com.powsybl.iidm.modification.ReplaceThreeWindingsTransformersBy3TwoWindingsTransformers;
import com.powsybl.iidm.modification.scalable.ProportionalScalable;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.modification.scalable.ScalingParameters;
import com.powsybl.iidm.modification.topology.RemoveFeederBayBuilder;
import com.powsybl.iidm.modification.topology.RemoveHvdcLineBuilder;
import com.powsybl.iidm.modification.topology.RemoveVoltageLevelBuilder;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblConfiguration;
import org.apache.commons.lang3.Range;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;

import java.util.*;

import static com.powsybl.iidm.modification.topology.TopologyModificationUtils.*;
import static com.powsybl.python.commons.CTypeUtil.toStringList;
import static com.powsybl.python.commons.Util.*;
import static com.powsybl.python.network.NetworkCFunctions.createDataframe;
import static com.powsybl.python.network.NetworkUtil.freeScalingParametersContent;

/**
 * Defines the C functions for network modifications.
 *
 * @author Sylvain Leclerc {@literal <sylvain.leclerc@rte-france.com>}
 */
@CContext(Directives.class)
public final class NetworkModificationsCFunctions {

    private NetworkModificationsCFunctions() {
    }

    @CEntryPoint(name = "getConnectablesOrderPositions")
    public static PyPowsyblApiHeader.ArrayPointer<PyPowsyblApiHeader.SeriesPointer> getConnectablesOrderPositions(IsolateThread thread, ObjectHandle networkHandle,
                                                                                                                  CCharPointer voltageLevelId, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            String voltageLevelIdStr = CTypeUtil.toString(voltageLevelId);
            Network network = ObjectHandles.getGlobal().get(networkHandle);

            VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelIdStr);
            Map<String, List<ConnectablePosition.Feeder>> feederPositionsOrders = getFeedersByConnectable(voltageLevel);
            return Dataframes.createCDataframe(Dataframes.feederMapMapper(), feederPositionsOrders);
        });
    }

    @CEntryPoint(name = "getUnusedConnectableOrderPositions")
    public static PyPowsyblApiHeader.ArrayPointer<CIntPointer> getUnusedConnectableOrderPositions(IsolateThread thread, ObjectHandle networkHandle,
                                                                                                  CCharPointer busbarSectionId, CCharPointer beforeOrAfter,
                                                                                                  PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String busbarSectionIdStr = CTypeUtil.toString(busbarSectionId);
            BusbarSection busbarSection = network.getBusbarSection(busbarSectionIdStr);
            Optional<Range<Integer>> positionsOrders;
            if (CTypeUtil.toString(beforeOrAfter).equals("BEFORE")) {
                positionsOrders = getUnusedOrderPositionsBefore(busbarSection);
            } else {
                positionsOrders = getUnusedOrderPositionsAfter(busbarSection);
            }
            if (positionsOrders.isPresent()) {
                int max = positionsOrders.get().getMaximum();
                int min = positionsOrders.get().getMinimum();
                return createIntegerArray(Arrays.asList(min, max));
            } else {
                return createIntegerArray(Collections.emptyList());
            }
        });
    }

    @CEntryPoint(name = "createNetworkModification")
    public static void createNetworkModification(IsolateThread thread, ObjectHandle networkHandle,
                                                 PyPowsyblApiHeader.DataframeArrayPointer cDataframes,
                                                 PyPowsyblApiHeader.NetworkModificationType networkModificationType,
                                                 boolean throwException, ObjectHandle reportNodeHandle,
                                                 PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            ReportNode reportNode = ObjectHandles.getGlobal().get(reportNodeHandle);
            List<UpdatingDataframe> dfs = new ArrayList<>();
            for (int i = 0; i < cDataframes.getDataframesCount(); i++) {
                dfs.add(createDataframe(cDataframes.getDataframes().addressOf(i)));
            }
            DataframeNetworkModificationType type = convert(networkModificationType);
            NetworkModifications.applyModification(type, network, dfs, throwException, reportNode);
        });
    }

    @CEntryPoint(name = "getModificationMetadata")
    public static PyPowsyblApiHeader.DataframeMetadataPointer getModificationMetadata(IsolateThread thread,
                                                                                      PyPowsyblApiHeader.NetworkModificationType networkModificationType,
                                                                                      PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DataframeNetworkModificationType type = convert(networkModificationType);
            List<SeriesMetadata> metadata = NetworkModifications.getModification(type).getMetadata();
            return CTypeUtil.createSeriesMetadata(metadata);
        });
    }

    @CEntryPoint(name = "removeElementsModification")
    public static void removeElementsModification(IsolateThread thread, ObjectHandle networkHandle,
                                                  CCharPointerPointer connectableIdsPtrPtr, int connectableIdsCount,
                                                  PyPowsyblApiHeader.DataframePointer extraDataDfPtr,
                                                  PyPowsyblApiHeader.RemoveModificationType removeModificationType,
                                                  boolean throwException, ObjectHandle reportNodeHandle,
                                                  PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            List<String> ids = toStringList(connectableIdsPtrPtr, connectableIdsCount);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            ReportNode reportNode = ObjectHandles.getGlobal().get(reportNodeHandle);
            if (removeModificationType == PyPowsyblApiHeader.RemoveModificationType.REMOVE_FEEDER) {
                ids.forEach(id -> new RemoveFeederBayBuilder().withConnectableId(id).build().apply(network, throwException, reportNode == null ? ReportNode.NO_OP : reportNode));
            } else if (removeModificationType == PyPowsyblApiHeader.RemoveModificationType.REMOVE_VOLTAGE_LEVEL) {
                ids.forEach(id -> new RemoveVoltageLevelBuilder().withVoltageLevelId(id).build().apply(network, throwException, reportNode == null ? ReportNode.NO_OP : reportNode));
            } else if (removeModificationType == PyPowsyblApiHeader.RemoveModificationType.REMOVE_HVDC_LINE) {
                UpdatingDataframe extraDataDf = createDataframe(extraDataDfPtr);
                ids.forEach(hvdcId -> {
                    List<String> shuntCompensatorList = Collections.emptyList();
                    if (extraDataDf != null) {
                        Optional<String> shuntCompensatorOptional = extraDataDf.getStringValue(hvdcId, 0);
                        String shuntCompensator = shuntCompensatorOptional.isEmpty() || shuntCompensatorOptional.get().isEmpty() ? "," :
                                shuntCompensatorOptional.get();
                        shuntCompensatorList = Arrays.stream(shuntCompensator.split(",")).toList();
                    }
                    new RemoveHvdcLineBuilder().withHvdcLineId(hvdcId).withShuntCompensatorIds(shuntCompensatorList).build().apply(network, throwException, reportNode == null ? ReportNode.NO_OP : reportNode);

                });
            }
        });
    }

    @CEntryPoint(name = "splitOrMergeTransformers")
    public static void splitOrMergeTransformers(IsolateThread thread, ObjectHandle networkHandle,
                                               CCharPointerPointer transformerIdsPtrPtr,
                                               int transformerIdsCount, boolean merge, ObjectHandle reportNodeHandle,
                                               PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            List<String> transformerIds = toStringList(transformerIdsPtrPtr, transformerIdsCount);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            ReportNode reportNode = ObjectHandles.getGlobal().get(reportNodeHandle);
            if (merge) {
                Replace3TwoWindingsTransformersByThreeWindingsTransformers modification;
                if (transformerIds.isEmpty()) {
                    modification = new Replace3TwoWindingsTransformersByThreeWindingsTransformers();
                } else {
                    modification = new Replace3TwoWindingsTransformersByThreeWindingsTransformers(transformerIds);
                }
                modification.apply(network, reportNode == null ? ReportNode.NO_OP : reportNode);
            } else {
                ReplaceThreeWindingsTransformersBy3TwoWindingsTransformers modification;
                if (transformerIds.isEmpty()) {
                    modification = new ReplaceThreeWindingsTransformersBy3TwoWindingsTransformers();
                } else {
                    modification = new ReplaceThreeWindingsTransformersBy3TwoWindingsTransformers(transformerIds);
                }
                modification.apply(network, reportNode == null ? ReportNode.NO_OP : reportNode);
            }
        });
    }

    @CEntryPoint(name = "scaleProportional")
    public static int scaleProportional(IsolateThread thread, ObjectHandle networkHandle,
                                        double asked,
                                        PyPowsyblApiHeader.DistributionMode distributionModeHandle,
                                        CCharPointerPointer injectionsIdsPtrPtr, int injectionCount,
                                        double limitMin, double limitMax,
                                        PyPowsyblApiHeader.ScalingParametersPointer scalingParametersHandle,
                                        PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            List<String> injectionsIdsStringList = toStringList(injectionsIdsPtrPtr, injectionCount);
            List<Injection<?>> injections = new ArrayList<>();

            try {
                injectionsIdsStringList.forEach(injection ->
                        injections.add((Injection<?>) network.getConnectable(injection)));
            } catch (NullPointerException e) {
                throw new PowsyblException("Can't find injections", e);
            }
            ScalingParameters parameters = convertScalingParameters(scalingParametersHandle);
            ProportionalScalable.DistributionMode distributionMode = convert(distributionModeHandle);
            ProportionalScalable proportionalScalable = Scalable.proportional(injections, distributionMode, limitMin, limitMax);
            return (int) proportionalScalable.scale(network, asked, parameters);
        });
    }

    public static void copyToCScalingParameters(ScalingParameters parameters, PyPowsyblApiHeader.ScalingParametersPointer cParameters) {
        cParameters.setScalingConvention(parameters.getScalingConvention().ordinal());
        cParameters.setConstantPowerFactor(parameters.isConstantPowerFactor());
        cParameters.setReconnect(parameters.isReconnect());
        cParameters.setAllowsGeneratorOutOfActivePowerLimits(parameters.isAllowsGeneratorOutOfActivePowerLimits());
        cParameters.setPriority(parameters.getPriority().ordinal());
        cParameters.setScalingType(parameters.getScalingType().ordinal());
        CCharPointerPointer calloc = UnmanagedMemory.calloc(parameters.getIgnoredInjectionIds().size() * SizeOf.get(CCharPointerPointer.class));
        ArrayList<String> ignoredInjectionIds = new ArrayList<>(parameters.getIgnoredInjectionIds());
        for (int i = 0; i < parameters.getIgnoredInjectionIds().size(); i++) {
            calloc.write(i, CTypeUtil.toCharPtr(ignoredInjectionIds.get(i)));
        }
        cParameters.setIgnoredInjectionIds(calloc);
    }

    public static PyPowsyblApiHeader.ScalingParametersPointer convertToScalingParametersPointer(ScalingParameters parameters) {
        PyPowsyblApiHeader.ScalingParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(PyPowsyblApiHeader.ScalingParametersPointer.class));
        copyToCScalingParameters(parameters, paramsPtr);
        return paramsPtr;
    }

    public static ScalingParameters createScalingParameters() {
        return PyPowsyblConfiguration.isReadConfig() ? ScalingParameters.load() : new ScalingParameters();
    }

    public static ScalingParameters convertScalingParameters(PyPowsyblApiHeader.ScalingParametersPointer scalingParametersPtr) {

        List<String> injectionsIdsStringList = toStringList(scalingParametersPtr.getIgnoredInjectionIds(), scalingParametersPtr.getIgnoredInjectionIdsCount());

        Set<String> ignoredInjectionIds = new HashSet<>();
        injectionsIdsStringList.forEach(ignoredInjectionIds::add);

        return createScalingParameters()
                .setScalingConvention(Scalable.ScalingConvention.values()[scalingParametersPtr.getScalingConvention()])
                .setConstantPowerFactor(scalingParametersPtr.isConstantPowerFactor())
                .setReconnect(scalingParametersPtr.isReconnect())
                .setAllowsGeneratorOutOfActivePowerLimits(scalingParametersPtr.isAllowsGeneratorOutOfActivePowerLimits())
                .setPriority(ScalingParameters.Priority.values()[scalingParametersPtr.getPriority()])
                .setScalingType(ScalingParameters.ScalingType.values()[scalingParametersPtr.getScalingType()])
                .setIgnoredInjectionIds(ignoredInjectionIds);
    }

    public static void freeScalingParametersPointer(PyPowsyblApiHeader.ScalingParametersPointer scalingParametersPtr) {
        freeScalingParametersContent(scalingParametersPtr);
        UnmanagedMemory.free(scalingParametersPtr);
    }

    @CEntryPoint(name = "createScalingParameters")
    public static PyPowsyblApiHeader.ScalingParametersPointer createScalingParameters(IsolateThread thread, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> convertToScalingParametersPointer(NetworkUtil.createScalingParameters()));
    }

    @CEntryPoint(name = "freeScalingParameters")
    public static void freeScalingParameters(IsolateThread thread, PyPowsyblApiHeader.ScalingParametersPointer scalingParametersPtr,
                                              PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> freeScalingParametersPointer(scalingParametersPtr));
    }
}
