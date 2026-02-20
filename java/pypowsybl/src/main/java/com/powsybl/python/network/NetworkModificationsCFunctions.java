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
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.modification.scalable.ScalingParameters;
import com.powsybl.iidm.modification.topology.RemoveFeederBayBuilder;
import com.powsybl.iidm.modification.topology.RemoveHvdcLineBuilder;
import com.powsybl.iidm.modification.topology.RemoveVoltageLevelBuilder;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader.*;
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

import java.io.IOException;
import java.util.*;
import java.util.function.DoubleSupplier;

import static com.powsybl.iidm.modification.topology.TopologyModificationUtils.*;
import static com.powsybl.python.commons.CTypeUtil.toStringList;
import static com.powsybl.python.commons.Util.*;
import static com.powsybl.python.network.NetworkCFunctions.createDataframe;
import static com.powsybl.python.network.ScalableUtils.freeScalingParametersContent;

/**
 * Defines the C functions for network modifications.
 *
 * @author Sylvain Leclerc {@literal <sylvain.leclerc@rte-france.com>}
 */
@SuppressWarnings({"java:S1602", "java:S1604", "Convert2Lambda"})
@CContext(Directives.class)
public final class NetworkModificationsCFunctions {

    private NetworkModificationsCFunctions() {
    }

    @CEntryPoint(name = "getConnectablesOrderPositions")
    public static ArrayPointer<SeriesPointer> getConnectablesOrderPositions(IsolateThread thread, ObjectHandle networkHandle,
                                                                            CCharPointer voltageLevelId, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<SeriesPointer> get() throws IOException {
                String voltageLevelIdStr = CTypeUtil.toString(voltageLevelId);
                Network network = ObjectHandles.getGlobal().get(networkHandle);

                VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelIdStr);
                Map<String, List<ConnectablePosition.Feeder>> feederPositionsOrders = getFeedersByConnectable(voltageLevel);
                return Dataframes.createCDataframe(Dataframes.feederMapMapper(), feederPositionsOrders);
            }
        });
    }

    @CEntryPoint(name = "getUnusedConnectableOrderPositions")
    public static ArrayPointer<CIntPointer> getUnusedConnectableOrderPositions(IsolateThread thread, ObjectHandle networkHandle,
                                                                               CCharPointer busbarSectionId, CCharPointer beforeOrAfter,
                                                                               ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public ArrayPointer<CIntPointer> get() throws IOException {
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
            }
        });
    }

    @CEntryPoint(name = "createNetworkModification")
    public static void createNetworkModification(IsolateThread thread, ObjectHandle networkHandle,
                                                 DataframeArrayPointer cDataframes,
                                                 NetworkModificationType networkModificationType,
                                                 boolean throwException, ObjectHandle reportNodeHandle,
                                                 ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                ReportNode reportNode = ObjectHandles.getGlobal().get(reportNodeHandle);
                List<UpdatingDataframe> dfs = new ArrayList<>();
                for (int i = 0; i < cDataframes.getDataframesCount(); i++) {
                    dfs.add(createDataframe(cDataframes.getDataframes().addressOf(i)));
                }
                DataframeNetworkModificationType type = convert(networkModificationType);
                NetworkModifications.applyModification(type, network, dfs, throwException, reportNode);
            }
        });
    }

    @CEntryPoint(name = "getModificationMetadata")
    public static DataframeMetadataPointer getModificationMetadata(IsolateThread thread,
                                                                   NetworkModificationType networkModificationType,
                                                                   ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<>() {
            @Override
            public DataframeMetadataPointer get() throws IOException {
                DataframeNetworkModificationType type = convert(networkModificationType);
                List<SeriesMetadata> metadata = NetworkModifications.getModification(type).getMetadata();
                return CTypeUtil.createSeriesMetadata(metadata);
            }
        });
    }

    @CEntryPoint(name = "removeElementsModification")
    public static void removeElementsModification(IsolateThread thread, ObjectHandle networkHandle,
                                                  CCharPointerPointer connectableIdsPtrPtr, int connectableIdsCount,
                                                  DataframePointer extraDataDfPtr,
                                                  RemoveModificationType removeModificationType,
                                                  boolean throwException, ObjectHandle reportNodeHandle,
                                                  ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                List<String> ids = toStringList(connectableIdsPtrPtr, connectableIdsCount);
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                ReportNode reportNode = ObjectHandles.getGlobal().get(reportNodeHandle);
                if (removeModificationType == RemoveModificationType.REMOVE_FEEDER) {
                    ids.forEach(id -> new RemoveFeederBayBuilder().withConnectableId(id).build().apply(network, throwException, reportNode == null ? ReportNode.NO_OP : reportNode));
                } else if (removeModificationType == RemoveModificationType.REMOVE_VOLTAGE_LEVEL) {
                    ids.forEach(id -> new RemoveVoltageLevelBuilder().withVoltageLevelId(id).build().apply(network, throwException, reportNode == null ? ReportNode.NO_OP : reportNode));
                } else if (removeModificationType == RemoveModificationType.REMOVE_HVDC_LINE) {
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
            }
        });
    }

    @CEntryPoint(name = "splitOrMergeTransformers")
    public static void splitOrMergeTransformers(IsolateThread thread, ObjectHandle networkHandle,
                                               CCharPointerPointer transformerIdsPtrPtr,
                                               int transformerIdsCount, boolean merge, ObjectHandle reportNodeHandle,
                                               ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
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
            }
        });
    }

    enum ScalableType {
        ELEMENT,
        STACK;
    }

    @CEntryPoint(name = "createScalable")
    public static ObjectHandle createScalable(IsolateThread thread, int scalableTypePos, CCharPointer injectionIdPtr, double minValue,
                                              double maxValue, VoidPointerPointer childrenHandles, int childrenCount,
                                              ExceptionHandlerPointer exceptionHandlerPointer) {
        return doCatch(exceptionHandlerPointer, new PointerProvider<>() {
            @Override
            public ObjectHandle get() {
                Scalable scalable;
                ScalableType scalableType = ScalableType.values()[scalableTypePos];
                boolean withValues = minValue != Double.MIN_VALUE || maxValue != Double.MAX_VALUE;
                switch (scalableType) {
                    case ELEMENT:
                        String injectionId = CTypeUtil.toString(injectionIdPtr);
                        if (injectionId.isEmpty()) {
                            throw new PowsyblException("An injection id must be given for ELEMENT type scalable.");
                        }
                        scalable = withValues ? Scalable.scalable(injectionId, minValue, maxValue) : Scalable.scalable(injectionId);
                        break;
                    case STACK:
                        if (childrenCount == 0) {
                            throw new PowsyblException("Scalable children not found for STACK type scalable.");
                        }
                        Scalable[] children = new Scalable[childrenCount];
                        for (int i = 0; i < childrenCount; ++i) {
                            ObjectHandle childrenHandle = childrenHandles.read(i);
                            Scalable child = ObjectHandles.getGlobal().get(childrenHandle);
                            children[i] = child;
                        }
                        scalable = withValues ? Scalable.stack(minValue, maxValue, children) : Scalable.stack(children);
                        break;
                    default:
                        throw new PowsyblException("Scalable type not supported: " + scalableType);
                }
                return ObjectHandles.getGlobal().create(scalable);
            }
        });
    }

    @CEntryPoint(name = "scale")
    public static double scale(IsolateThread thread, ObjectHandle networkHandle, ObjectHandle scalableHandle,
                               ScalingParametersPointer scalingParametersPointer, double asked,
                               ExceptionHandlerPointer exceptionHandlerPointer) {
        return doCatch(exceptionHandlerPointer, new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                Scalable scalable = ObjectHandles.getGlobal().get(scalableHandle);
                Network network = ObjectHandles.getGlobal().get(networkHandle);
                ScalingParameters parameters = convertScalingParameters(scalingParametersPointer);
                return scalable.scale(network, asked, parameters);
            }
        });
    }

    public static void copyToCScalingParameters(ScalingParameters parameters, ScalingParametersPointer cParameters) {
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

    public static ScalingParametersPointer convertToScalingParametersPointer(ScalingParameters parameters) {
        ScalingParametersPointer paramsPtr = UnmanagedMemory.calloc(SizeOf.get(ScalingParametersPointer.class));
        copyToCScalingParameters(parameters, paramsPtr);
        return paramsPtr;
    }

    public static ScalingParameters createScalingParameters() {
        return PyPowsyblConfiguration.isReadConfig() ? ScalingParameters.load() : new ScalingParameters();
    }

    public static ScalingParameters convertScalingParameters(ScalingParametersPointer scalingParametersPtr) {

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

    public static void freeScalingParametersPointer(ScalingParametersPointer scalingParametersPtr) {
        freeScalingParametersContent(scalingParametersPtr);
        UnmanagedMemory.free(scalingParametersPtr);
    }

    @CEntryPoint(name = "createScalingParameters")
    public static ScalingParametersPointer createScalingParametersPointer(IsolateThread thread, ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> convertToScalingParametersPointer(createScalingParameters()));
    }

    @CEntryPoint(name = "freeScalingParameters")
    public static void freeScalingParameters(IsolateThread thread, ScalingParametersPointer scalingParametersPtr,
                                             ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
                freeScalingParametersPointer(scalingParametersPtr);
            }
        });
    }
}
