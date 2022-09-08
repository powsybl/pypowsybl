/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.network;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.Directives;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import org.apache.commons.lang3.Range;
import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.ObjectHandle;
import org.graalvm.nativeimage.ObjectHandles;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.powsybl.iidm.modification.topology.TopologyModificationUtils.getUnusedOrderPositionsAfter;
import static com.powsybl.iidm.modification.topology.TopologyModificationUtils.getUnusedOrderPositionsBefore;
import static com.powsybl.python.commons.Util.createIntegerArray;
import static com.powsybl.python.commons.Util.doCatch;

/**
 * @author Coline Piloquet <coline.piloquet at rte-france.com>
 */
@CContext(Directives.class)
public final class NetworkModificationCFunctions {

    private NetworkModificationCFunctions() {
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
                return createIntegerArray(Collections.EMPTY_LIST);
            }
        });

    }

    static Map<String, List<ConnectablePosition.Feeder>> getFeedersByConnectable(VoltageLevel voltageLevel) {
        Map<String, List<ConnectablePosition.Feeder>> feedersByConnectable = new HashMap<>();
        voltageLevel.getConnectables().forEach(connectable -> {
            ConnectablePosition<?> position = (ConnectablePosition<?>) connectable.getExtension(ConnectablePosition.class);
            if (position != null) {
                List<ConnectablePosition.Feeder> feeder = getFeeders(position, voltageLevel, connectable);
                feedersByConnectable.put(connectable.getId(), feeder);
            }
        });
        return feedersByConnectable;
    }

    private static List<ConnectablePosition.Feeder> getFeeders(ConnectablePosition<?> position, VoltageLevel voltageLevel, Connectable<?> connectable) {
        if (connectable instanceof Injection) {
            return getInjectionFeeder(position);
        } else if (connectable instanceof Branch) {
            return getBranchFeeders(position, voltageLevel, (Branch<?>) connectable);
        } else if (connectable instanceof ThreeWindingsTransformer) {
            return get3wtFeeders(position, voltageLevel, (ThreeWindingsTransformer) connectable);
        }
        return Collections.emptyList();
    }

    private static List<ConnectablePosition.Feeder> getInjectionFeeder(ConnectablePosition<?> position) {
        return Collections.singletonList(position.getFeeder());
    }

    private static List<ConnectablePosition.Feeder> getBranchFeeders(ConnectablePosition<?> position, VoltageLevel voltageLevel, Branch<?> branch) {
        List<ConnectablePosition.Feeder> feeders = new ArrayList<>();
        if (branch.getTerminal1().getVoltageLevel() == voltageLevel) {
            feeders.add(position.getFeeder1());
        }
        if (branch.getTerminal2().getVoltageLevel() == voltageLevel) {
            feeders.add(position.getFeeder2());
        }
        return feeders;
    }

    private static List<ConnectablePosition.Feeder> get3wtFeeders(ConnectablePosition<?> position, VoltageLevel voltageLevel, ThreeWindingsTransformer twt) {
        List<ConnectablePosition.Feeder> feeders = new ArrayList<>();
        if (twt.getLeg1().getTerminal().getVoltageLevel() == voltageLevel) {
            feeders.add(position.getFeeder1());
        }
        if (twt.getLeg2().getTerminal().getVoltageLevel() == voltageLevel) {
            feeders.add(position.getFeeder2());
        }
        if (twt.getLeg3().getTerminal().getVoltageLevel() == voltageLevel) {
            feeders.add(position.getFeeder3());
        }
        return feeders;
    }

}
