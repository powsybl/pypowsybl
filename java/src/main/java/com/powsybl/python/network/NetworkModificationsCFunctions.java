/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.modifications.DataframeNetworkModificationType;
import com.powsybl.dataframe.network.modifications.NetworkModifications;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.modification.topology.RemoveFeederBayBuilder;
import com.powsybl.iidm.modification.topology.RemoveHvdcLineBuilder;
import com.powsybl.iidm.modification.topology.RemoveVoltageLevelBuilder;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.DataframeMetadataPointer;
import com.powsybl.python.commons.PyPowsyblApiHeader.SeriesPointer;
import org.apache.commons.lang3.Range;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;

import java.io.IOException;
import java.util.*;

import static com.powsybl.iidm.modification.topology.TopologyModificationUtils.*;
import static com.powsybl.python.commons.CTypeUtil.toStringList;
import static com.powsybl.python.commons.Util.*;
import static com.powsybl.python.network.NetworkCFunctions.createDataframe;

/**
 * Defines the C functions for network modifications.
 *
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
@SuppressWarnings({"java:S1602", "java:S1604"})
@CContext(Directives.class)
public final class NetworkModificationsCFunctions {

    private NetworkModificationsCFunctions() {
    }

    @CEntryPoint(name = "getConnectablesOrderPositions")
    public static ArrayPointer<SeriesPointer> getConnectablesOrderPositions(IsolateThread thread, ObjectHandle networkHandle,
                                                                            CCharPointer voltageLevelId, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ArrayPointer<SeriesPointer>>() {
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
                                                                                                  PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<ArrayPointer<CIntPointer>>() {
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
                                                 PyPowsyblApiHeader.DataframeArrayPointer cDataframes,
                                                 PyPowsyblApiHeader.NetworkModificationType networkModificationType,
                                                 boolean throwException, ObjectHandle reportNodeHandle,
                                                 PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
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
                                                                                      PyPowsyblApiHeader.NetworkModificationType networkModificationType,
                                                                                      PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, new PointerProvider<DataframeMetadataPointer>() {
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
                                                  PyPowsyblApiHeader.DataframePointer extraDataDfPtr,
                                                  PyPowsyblApiHeader.RemoveModificationType removeModificationType,
                                                  boolean throwException, ObjectHandle reportNodeHandle,
                                                  PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, new Runnable() {
            @Override
            public void run() {
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
            }
        });
    }

}
