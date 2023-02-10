/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.network;

import com.powsybl.commons.reporter.ReporterModel;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.FeederBaysLineSeries;
import com.powsybl.dataframe.network.adders.FeederBaysTwtSeries;
import com.powsybl.dataframe.network.adders.NetworkElementAdders;
import com.powsybl.dataframe.network.modifications.DataframeNetworkModificationType;
import com.powsybl.dataframe.network.modifications.NetworkModifications;
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

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;
import static com.powsybl.iidm.modification.topology.TopologyModificationUtils.getUnusedOrderPositionsAfter;
import static com.powsybl.iidm.modification.topology.TopologyModificationUtils.getUnusedOrderPositionsBefore;
import static com.powsybl.iidm.modification.topology.TopologyModificationUtils.getFeedersByConnectable;
import static com.powsybl.python.commons.Util.*;
import static com.powsybl.python.network.NetworkCFunctions.createDataframe;
import static com.powsybl.python.network.NetworkCFunctions.createSeriesMetadata;

/**
 * Defines the C functions for network modifications.
 *
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
@CContext(Directives.class)
public final class NetworkModificationsCFunctions {

    private NetworkModificationsCFunctions() {
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
                    .withPositionPercent(positionPercent)
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

    @CEntryPoint(name = "revertCreateLineOnLine")
    public static void revertCreateLineOnLine(IsolateThread thread, ObjectHandle networkHandle,
                                              CCharPointer lineToBeMerged1Id,
                                              CCharPointer lineToBeMerged2Id,
                                              CCharPointer lineToBeDeletedId,
                                              CCharPointer mergedLineId,
                                              CCharPointer mergedLineName,
                                              PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String lineToBeMerged1IdStr = CTypeUtil.toString(lineToBeMerged1Id);
            String lineToBeMerged2IdStr = CTypeUtil.toString(lineToBeMerged2Id);
            String lineToBeDeletedIdStr = CTypeUtil.toString(lineToBeDeletedId);
            String mergedLineIdStr = CTypeUtil.toString(mergedLineId);
            String mergedLineNameStr = CTypeUtil.toString(mergedLineName);

            Network network = ObjectHandles.getGlobal().get(networkHandle);
            RevertCreateLineOnLine modification = new RevertCreateLineOnLineBuilder()
                    .withLineToBeMerged1Id(lineToBeMerged1IdStr)
                    .withLineToBeMerged2Id(lineToBeMerged2IdStr)
                    .withLineToBeDeletedId(lineToBeDeletedIdStr)
                    .withMergedLineId(mergedLineIdStr)
                    .withMergedLineName(mergedLineNameStr)
                    .build();
            modification.apply(network);
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
            Line line = network.getLine(lineIdStr);
            ConnectVoltageLevelOnLine modification = new ConnectVoltageLevelOnLineBuilder()
                    .withLine1Id(line1IdStr)
                    .withLine1Name(line1NameStr)
                    .withLine2Id(line2IdStr)
                    .withLine2Name(line2NameStr)
                    .withLine(line)
                    .withBusbarSectionOrBusId(bbsIdBusIdStr)
                    .withPositionPercent(positionPercent)
                    .build();
            modification.apply(network);
        });
    }

    @CEntryPoint(name = "revertConnectVoltageLevelOnLine")
    public static void revertConnectVoltageLevelOnLine(IsolateThread thread, ObjectHandle networkHandle,
                                                       CCharPointer line1Id,
                                                       CCharPointer line2Id,
                                                       CCharPointer lineId,
                                                       CCharPointer lineName,
                                                       PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            String line1IdStr = CTypeUtil.toString(line1Id);
            String line2IdStr = CTypeUtil.toString(line2Id);
            String lineIdStr = CTypeUtil.toString(lineId);
            String lineNameStr = CTypeUtil.toString(lineName);
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            RevertConnectVoltageLevelOnLine modification = new RevertConnectVoltageLevelOnLineBuilder()
                    .withLine1Id(line1IdStr)
                    .withLine2Id(line2IdStr)
                    .withLineId(lineIdStr)
                    .withLineName(lineNameStr)
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
                return createIntegerArray(Collections.emptyList());
            }
        });
    }

    static CreateCouplingDeviceBuilder createCouplingDeviceBuilder(UpdatingDataframe df) {
        CreateCouplingDeviceBuilder builder = new CreateCouplingDeviceBuilder();
        for (int row = 0; row < df.getRowCount(); row++) {
            applyIfPresent(df.getStrings("busbar_section_id_1"), row, builder::withBusbarSectionId1);
            applyIfPresent(df.getStrings("busbar_section_id_2"), row, builder::withBusbarSectionId2);
            applyIfPresent(df.getStrings("switch_prefix_id"), row, builder::withSwitchPrefixId);
        }
        return builder;
    }

    @CEntryPoint(name = "createNetworkModification")
    public static void createNetworkModification(IsolateThread thread, ObjectHandle networkHandle,
                                                 PyPowsyblApiHeader.DataframePointer cDataframe,
                                                 PyPowsyblApiHeader.NetworkModificationType networkModificationType,
                                                 boolean throwException, ObjectHandle reporterHandle,
                                                 PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        doCatch(exceptionHandlerPtr, () -> {
            Network network = ObjectHandles.getGlobal().get(networkHandle);
            ReporterModel reporter = ObjectHandles.getGlobal().get(reporterHandle);
            UpdatingDataframe df = createDataframe(cDataframe);
            DataframeNetworkModificationType type = convert(networkModificationType);
            NetworkModifications.applyModification(type, network, df, throwException, reporter);
        });
    }

    @CEntryPoint(name = "replaceTeePointByVoltageLevelOnLine")
    public static void replaceTeePointByVoltageLevelOnLine(IsolateThread thread, ObjectHandle networkHandle,
                                                           CCharPointer teePointLine1,
                                                           CCharPointer teePointLine2,
                                                           CCharPointer teePointLineToRemove,
                                                           CCharPointer bbsOrBusId,
                                                           CCharPointer newLine1Id,
                                                           CCharPointer newLine1Name,
                                                           CCharPointer newLine2Id,
                                                           CCharPointer newLine2Name,
                                                           PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {

        String teePointLine1Str = CTypeUtil.toString(teePointLine1);
        String teePointLineStr = CTypeUtil.toString(teePointLine2);
        String teePointLineToRemoveStr = CTypeUtil.toStringOrNull(teePointLineToRemove);
        String bbsOrBusIdStr = CTypeUtil.toStringOrNull(bbsOrBusId);
        String newLine1IdStr = CTypeUtil.toStringOrNull(newLine1Id);
        String newLine1NameStr = CTypeUtil.toStringOrNull(newLine1Name);
        String newLine2IdStr = CTypeUtil.toStringOrNull(newLine2Id);
        String newLine2NameStr = CTypeUtil.toStringOrNull(newLine2Name);
        Network network = ObjectHandles.getGlobal().get(networkHandle);
        NetworkModification modification = new ReplaceTeePointByVoltageLevelOnLineBuilder()
                .withTeePointLine1(teePointLine1Str)
                .withTeePointLine2(teePointLineStr)
                .withTeePointLineToRemove(teePointLineToRemoveStr)
                .withBbsOrBusId(bbsOrBusIdStr)
                .withNewLine1Id(newLine1IdStr)
                .withNewLine1Name(newLine1NameStr)
                .withNewLine2Id(newLine2IdStr)
                .withNewLine2Name(newLine2NameStr).build();
        modification.apply(network);
    }

    @CEntryPoint(name = "getModificationMetadata")
    public static PyPowsyblApiHeader.DataframeMetadataPointer getModificationMetadata(IsolateThread thread,
                                                                                      PyPowsyblApiHeader.NetworkModificationType networkModificationType,
                                                                                      PyPowsyblApiHeader.ExceptionHandlerPointer exceptionHandlerPtr) {
        return doCatch(exceptionHandlerPtr, () -> {
            DataframeNetworkModificationType type = convert(networkModificationType);
            List<SeriesMetadata> metadata = NetworkModifications.getModification(type).getMetadata();
            return createSeriesMetadata(metadata);
        });
    }
}
