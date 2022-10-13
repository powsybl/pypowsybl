/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.network;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.reporter.ReporterModel;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.dataframe.network.adders.FeederBaysLineSeries;
import com.powsybl.dataframe.network.adders.FeederBaysTwtSeries;
import com.powsybl.dataframe.network.adders.NetworkElementAdders;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.modification.NetworkModification;
import com.powsybl.iidm.modification.topology.*;
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

import java.util.*;

import static com.powsybl.iidm.modification.topology.TopologyModificationUtils.getUnusedOrderPositionsAfter;
import static com.powsybl.iidm.modification.topology.TopologyModificationUtils.getUnusedOrderPositionsBefore;
import static com.powsybl.python.commons.Util.*;
import static com.powsybl.python.network.NetworkCFunctions.createDataframe;

/**
 * Defines the C functions for network modifications.
 *
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
@CContext(Directives.class)
public final class NetworkModificationsCFunctions {

    private NetworkModificationsCFunctions() {
    }

    private static String getVoltageLevelFromBusOrBusBar(Network network, String id) {
        Identifiable identifiable = network.getIdentifiable(id);
        if (identifiable instanceof Bus) {
            return ((Bus) identifiable).getVoltageLevel().getId();
        } else if (identifiable instanceof BusbarSection) {
            return ((BusbarSection) identifiable).getTerminal().getVoltageLevel().getId();
        } else {
            throw new PowsyblException("bbs_id_bus_id must be a busbar or a bus");
        }
    }

    @CEntryPoint(name = "createLineOnLine")
    public static void createLineOnLine(IsolateThread thread, ObjectHandle networkHandle,
                                        CCharPointer bbsIdBusId,
                                        CCharPointer newLineId,
                                        double newLineR,
                                        double newLineX,
                                        double newLineB1,
                                        double newLineB2,
                                        double newLineG1,
                                        double newLineG2,
                                        CCharPointer lineId,
                                        CCharPointer line1Id,
                                        CCharPointer line1Name,
                                        CCharPointer line2Id,
                                        CCharPointer line2Name,
                                        double positionPercent,
                                        boolean createFictitiousSubstation,
                                        CCharPointer fictitiousVoltageLevelId,
                                        CCharPointer fictitiousVoltageLevelName,
                                        CCharPointer fictitiousSubstationId,
                                        CCharPointer fictitiousSubstationName,
                                        PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String bbsIdBusIdStr = CTypeUtil.toString(bbsIdBusId);
            String lineIdStr = CTypeUtil.toString(lineId);
            String fictitiousVoltageLevelIdStr = CTypeUtil.toStringOrNull(fictitiousVoltageLevelId);
            String fictitiousVoltageLevelNameStr = CTypeUtil.toStringOrNull(fictitiousVoltageLevelName);
            String fictitiousSubstationIdStr = CTypeUtil.toStringOrNull(fictitiousSubstationId);
            String fictitiousSubstationNameStr = CTypeUtil.toStringOrNull(fictitiousSubstationName);
            String line1IdStr = CTypeUtil.toStringOrNull(line1Id);
            String line1NameStr = CTypeUtil.toStringOrNull(line1Name);
            String line2IdStr = CTypeUtil.toStringOrNull(line2Id);
            String line2NameStr = CTypeUtil.toStringOrNull(line2Name);
            String newLineIdStr = CTypeUtil.toString(newLineId);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String voltageLevelIdStr = getVoltageLevelFromBusOrBusBar(network, bbsIdBusIdStr);
            Line line = network.getLine(lineIdStr);
            LineAdder adder = network.newLine()
                    .setId(newLineIdStr)
                    .setR(newLineR)
                    .setB1(newLineB1)
                    .setB2(newLineB2)
                    .setX(newLineX)
                    .setG1(newLineG1)
                    .setG2(newLineG2);
            CreateLineOnLine createLineOnLine = new CreateLineOnLineBuilder()
                    .withPercent(positionPercent)
                    .withVoltageLevelId(voltageLevelIdStr)
                    .withBusbarSectionOrBusId(bbsIdBusIdStr)
                    .withFictitiousVoltageLevelId(fictitiousVoltageLevelIdStr)
                    .withFictitiousVoltageLevelName(fictitiousVoltageLevelNameStr)
                    .withCreateFictitiousSubstation(createFictitiousSubstation)
                    .withFictitiousSubstationId(fictitiousSubstationIdStr)
                    .withFictitiousSubstationName(fictitiousSubstationNameStr)
                    .withLine1Id(line1IdStr)
                    .withLine1Name(line1NameStr)
                    .withLine2Id(line2IdStr)
                    .withLine2Name(line2NameStr)
                    .withLine(line)
                    .withLineAdder(adder)
                    .build();
            createLineOnLine.apply(network);
        });
    }

    @CEntryPoint(name = "connectVoltageLevelOnLine")
    public static void connectVoltageLevelOnLine(IsolateThread thread, ObjectHandle networkHandle,
                                                 CCharPointer bbsIdBusId,
                                                 CCharPointer lineId,
                                                 CCharPointer line1Id,
                                                 CCharPointer line1Name,
                                                 CCharPointer line2Id,
                                                 CCharPointer line2Name,
                                                 double positionPercent,
                                                 PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String bbsIdBusIdStr = CTypeUtil.toString(bbsIdBusId);
            String lineIdStr = CTypeUtil.toString(lineId);
            String line1IdStr = CTypeUtil.toStringOrNull(line1Id);
            String line1NameStr = CTypeUtil.toStringOrNull(line1Name);
            String line2IdStr = CTypeUtil.toStringOrNull(line2Id);
            String line2NameStr = CTypeUtil.toStringOrNull(line2Name);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            String voltageLevelIdStr = getVoltageLevelFromBusOrBusBar(network, bbsIdBusIdStr);
            Line line = network.getLine(lineIdStr);
            ConnectVoltageLevelOnLine modification = new ConnectVoltageLevelOnLineBuilder()
                    .withLine1Id(line1IdStr)
                    .withLine1Name(line1NameStr)
                    .withLine2Id(line2IdStr)
                    .withLine2Name(line2NameStr)
                    .withLine(line)
                    .withBusbarSectionOrBusId(bbsIdBusIdStr)
                    .withVoltageLevelId(voltageLevelIdStr)
                    .withPercent(positionPercent)
                    .build();
            modification.apply(network);
        });
    }

    @CEntryPoint(name = "createFeederBay")
    public static void createFeederBay(IsolateThread thread, ObjectHandle networkHandle, boolean throwException,
                                       ObjectHandle reporterHandle, PyPowsyblApiHeader.DataframeArrayPointer cDataframes,
                                       PyPowsyblApiHeader.ElementType elementType,
                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {

        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            ReporterModel reporter = ObjectHandles.getGlobal().get(reporterHandle);
            DataframeElementType type = convert(elementType);
            List<UpdatingDataframe> dataframes = new ArrayList<>();
            for (int i = 0; i < cDataframes.getDataframesCount(); i++) {
                dataframes.add(createDataframe(cDataframes.getDataframes().addressOf(i)));
            }
            NetworkElementAdders.addElementsWithBay(type, network, dataframes, throwException, reporter);
        });
    }

    @CEntryPoint(name = "createBranchFeederBaysLine")
    public static void createBranchFeederBaysLine(IsolateThread thread, ObjectHandle networkHandle,
                                                  PyPowsyblApiHeader.DataframePointer cDataframeLine,
                                                  PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            UpdatingDataframe df = createDataframe(cDataframeLine);
            FeederBaysLineSeries fbLineSeries = new FeederBaysLineSeries();
            CreateBranchFeederBaysBuilder builder = fbLineSeries.createBuilder(network, df);
            NetworkModification modification = builder.build();
            modification.apply(network);
        });
    }

    @CEntryPoint(name = "createBranchFeederBaysTwt")
    public static void createBranchFeederBaysTwt(IsolateThread thread, ObjectHandle networkHandle,
                                                 PyPowsyblApiHeader.DataframePointer cDataframeLine, PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);

            UpdatingDataframe df = createDataframe(cDataframeLine);
            FeederBaysTwtSeries fbTwtSeries = new FeederBaysTwtSeries();
            CreateBranchFeederBaysBuilder builder = fbTwtSeries.createBuilder(network, df);
            NetworkModification modification = builder.build();
            modification.apply(network);
        });
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
