/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElementFactory;
import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.network.extensions.ConnectablePositionFeederData;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import com.powsybl.iidm.network.util.SwitchPredicates;
import com.powsybl.iidm.network.util.SwitchesFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.math.matrix.SparseMatrixFactory;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.graph.EvenShiloachGraphDecrementalConnectivityFactory;
import com.powsybl.openloadflow.network.LfBranch;
import com.powsybl.openloadflow.network.LfBus;
import com.powsybl.openloadflow.network.LfNetwork;
import com.powsybl.openloadflow.network.LfNetworkParameters;
import com.powsybl.openloadflow.network.LfTopoConfig;
import com.powsybl.openloadflow.network.impl.LfNetworkList;
import com.powsybl.openloadflow.network.impl.Networks;
import com.powsybl.openloadflow.network.impl.PropagatedContingency;
import com.powsybl.openloadflow.network.impl.PropagatedContingencyCreationParameters;
import com.powsybl.python.commons.PyPowsyblApiHeader;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.powsybl.python.network.TemporaryLimitData.Side.*;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public final class NetworkUtil {

    private NetworkUtil() {
    }

    static boolean updateSwitchPosition(Network network, String switchId, boolean open) {
        Switch sw = network.getSwitch(switchId);
        if (sw == null) {
            throw new PowsyblException("Switch '" + switchId + "' not found");
        }
        if (open && !sw.isOpen()) {
            sw.setOpen(true);
            return true;
        } else if (!open && sw.isOpen()) {
            sw.setOpen(false);
            return true;
        }
        return false;
    }

    /**
     * Change the position of a DC Switch.
     * @param network where to find the DC switch.
     * @param dcSwitchId id in the network.
     * @param open new position of the switch (open <=> true)
     * @return true iff the switch position was actually changed.
     */
    static boolean updateDcSwitchPosition(Network network, String dcSwitchId, boolean open) {
        DcSwitch sw = network.getDcSwitch(dcSwitchId);
        if (sw == null) {
            throw new PowsyblException("DcSwitch '" + dcSwitchId + "' not found");
        }
        boolean wasOpen = sw.isOpen();
        // only call setOpen on actual change to avoid triggering listeners
        if (open != wasOpen) {
            sw.setOpen(open);
            return true;
        } else {
            return false;
        }
    }

    static boolean updateConnectableStatus(Network network, String id, boolean connected, boolean allowDisconnectors, boolean allowFictitious) {
        Identifiable<?> equipment = network.getIdentifiable(id);
        if (equipment == null) {
            throw new PowsyblException("Equipment '" + id + "' not found");
        } else if (equipment instanceof TieLine tieLine) {
            boolean connected1 = updateConnectableStatus(network, tieLine.getBoundaryLine1().getId(), connected,
                    allowDisconnectors, allowFictitious);
            boolean connected2 = updateConnectableStatus(network, tieLine.getBoundaryLine2().getId(), connected,
                    allowDisconnectors, allowFictitious);
            return connected1 && connected2;
        }
        if (!(equipment instanceof Connectable<?> connectable)) {
            throw new PowsyblException("Equipment '" + id + "' is not a connectable");
        }
        Predicate<Switch> predicate;
        if (allowFictitious) {
            if (allowDisconnectors) {
                predicate = SwitchPredicates.IS_BREAKER_OR_DISCONNECTOR;
            } else {
                predicate = SwitchPredicates.IS_BREAKER;
            }
        } else {
            if (allowDisconnectors) {
                predicate = SwitchPredicates.IS_NONFICTIONAL;
            } else {
                predicate = SwitchPredicates.IS_NONFICTIONAL_BREAKER;
            }
        }

        if (connected) {
            return connectable.connect(predicate);
        } else {
            return connectable.disconnect(predicate);
        }
    }

    private static boolean isInMainCc(Terminal t) {
        Bus bus = t.getBusView().getBus();
        return bus != null && bus.getConnectedComponent().getNum() == ComponentConstants.MAIN_NUM;
    }

    private static boolean isInMainSc(Terminal t) {
        Bus bus = t.getBusView().getBus();
        return bus != null && bus.getSynchronousComponent().getNum() == ComponentConstants.MAIN_NUM;
    }

    private static boolean filter(Branch<?> branch, Set<Double> nominalVoltages, Set<String> countries, boolean mainCc, boolean mainSc,
                                  boolean notConnectedToSameBusAtBothSides) {
        Terminal terminal1 = branch.getTerminal1();
        Terminal terminal2 = branch.getTerminal2();
        VoltageLevel voltageLevel1 = terminal1.getVoltageLevel();
        VoltageLevel voltageLevel2 = terminal2.getVoltageLevel();
        if (!(nominalVoltages.isEmpty()
                || nominalVoltages.contains(voltageLevel1.getNominalV())
                || nominalVoltages.contains(voltageLevel2.getNominalV()))) {
            return false;
        }
        if (!(countries.isEmpty()
                || countries.contains(voltageLevel1.getSubstation().flatMap(Substation::getCountry).map(Country::name).orElse(null))
                || countries.contains(voltageLevel2.getSubstation().flatMap(Substation::getCountry).map(Country::name).orElse(null)))) {
            return false;
        }
        if (mainCc && !(isInMainCc(terminal1) && isInMainCc(terminal2))) {
            return false;
        }
        if (mainSc && !(isInMainSc(terminal1) && isInMainSc(terminal2))) {
            return false;
        }
        if (notConnectedToSameBusAtBothSides) {
            Bus bus1 = branch.getTerminal1().getBusView().getBus();
            Bus bus2 = branch.getTerminal2().getBusView().getBus();
            return bus1 == null || bus2 == null || !bus1.getId().equals(bus2.getId());
        }
        return true;
    }

    private static boolean filter(Injection<?> injection, Set<Double> nominalVoltages, Set<String> countries, boolean mainCc, boolean mainSc) {
        Terminal terminal = injection.getTerminal();
        VoltageLevel voltageLevel = terminal.getVoltageLevel();
        if (!(nominalVoltages.isEmpty()
                || nominalVoltages.contains(voltageLevel.getNominalV()))) {
            return false;
        }
        if (!(countries.isEmpty()
                || countries.contains(voltageLevel.getSubstation().flatMap(Substation::getCountry).map(Country::name).orElse(null)))) {
            return false;
        }
        if (mainCc && !isInMainCc(terminal)) {
            return false;
        }
        return !mainSc || isInMainSc(terminal);
    }

    static List<String> getElementsIds(Network network, PyPowsyblApiHeader.ElementType elementType, Set<Double> nominalVoltages,
                                       Set<String> countries, boolean mainCc, boolean mainSc, boolean notConnectedToSameBusAtBothSides) {
        return switch (elementType) {
            case LINE -> network.getLineStream()
                    .filter(l -> filter(l, nominalVoltages, countries, mainCc, mainSc, notConnectedToSameBusAtBothSides))
                    .map(Identifiable::getId)
                    .collect(Collectors.toList());
            case TWO_WINDINGS_TRANSFORMER -> network.getTwoWindingsTransformerStream()
                    .filter(twt -> filter(twt, nominalVoltages, countries, mainCc, mainSc, notConnectedToSameBusAtBothSides))
                    .map(Identifiable::getId)
                    .collect(Collectors.toList());
            case GENERATOR -> network.getGeneratorStream()
                    .filter(g -> filter(g, nominalVoltages, countries, mainCc, mainSc))
                    .map(Identifiable::getId)
                    .collect(Collectors.toList());
            case LOAD -> network.getLoadStream()
                    .filter(g -> filter(g, nominalVoltages, countries, mainCc, mainSc))
                    .map(Identifiable::getId)
                    .collect(Collectors.toList());
            default -> throw new PowsyblException("Unsupported element type:" + elementType);
        };
    }

    static List<SwitchFlowContext> getSwitchFlowResults(Network network, List<String> switchIds) {
        Map<VoltageLevel, SwitchesFlow> switchesFlowByVoltageLevel = new HashMap<>();
        return switchIds.stream().map(switchId -> {
            Switch sw = network.getSwitch(switchId);
            if (sw == null) {
                throw new PowsyblException("Switch '" + switchId + "' not found");
            }
            SwitchesFlow switchesFlow = switchesFlowByVoltageLevel.computeIfAbsent(sw.getVoltageLevel(), SwitchesFlow::new);
            return new SwitchFlowContext(switchId,
                    switchesFlow.getP1(switchId),
                    switchesFlow.getQ1(switchId));
        }).toList();
    }

    /**
     * Computes the propagated outage group created by tripping a single equipment.
     *
     * @param network network containing the equipment
     * @param equipmentId identifier of the initiating equipment
     * @return sorted identifiers of disconnected equipments caused by the outage, following
     * the connectivity-result semantics used for outage-group computation
     */
    static List<String> getOutageGroup(Network network, String equipmentId) {
        return computeOutageGroupsByEquipmentId(network, List.of(equipmentId)).get(equipmentId);
    }

    /**
     * Computes propagated outage groups for the requested initiating equipments.
     *
     * @param network network containing the initiating equipments
     * @param equipmentIds identifiers of the equipments to trip
     * @return map keyed by initiating equipment identifier with sorted disconnected equipment identifiers as values,
     * following the connectivity-result semantics used for outage-group computation
     */
    static Map<String, List<String>> getOutageGroups(Network network, List<String> equipmentIds) {
        if (equipmentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return computeOutageGroupsByEquipmentId(network, new HashSet<>(equipmentIds));
    }

    /**
     * Builds propagated outage groups for a set of initiating equipments using the OpenLoadFlow contingency path.
     *
     * @param network network containing the initiating equipments
     * @param equipmentIds identifiers of the equipments to trip
     * @return map keyed by initiating equipment identifier with sorted disconnected equipment identifiers as values,
     * following the connectivity-result semantics used for outage-group computation
     */
    private static Map<String, List<String>> computeOutageGroupsByEquipmentId(Network network, Collection<String> equipmentIds) {
        if (equipmentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        LfTopoConfig topoConfig = new LfTopoConfig();
        List<Contingency> contingencies = equipmentIds.stream()
            .map(equipmentId -> createOutageGroupContingency(network, equipmentId))
            .toList();
        List<PropagatedContingency> propagatedContingencies = PropagatedContingency.createList(
            network,
            contingencies,
            topoConfig,
            createOutageGroupContingencyCreationParameters());

        Map<String, List<String>> disconnectedElementsByEquipmentId = new HashMap<>();
        try (LfNetworkList lfNetworks = Networks.loadWithReconnectableElements(network, topoConfig,
            createOutageGroupLfNetworkParameters(network, loadFlowParameters, topoConfig.isBreaker()),
            ReportNode.NO_OP)) {
            getOutageGroupNetworksToEvaluate(lfNetworks, loadFlowParameters.getComponentMode()).forEach(lfNetwork ->
                propagatedContingencies.forEach(propagatedContingency ->
                    propagatedContingency.toLfContingency(lfNetwork).ifPresent(lfContingency ->
                        disconnectedElementsByEquipmentId
                            .putIfAbsent(propagatedContingency.getContingency().getId(),
                                lfContingency.getDisconnectedElementIds().stream().sorted().toList()))));
        }

        equipmentIds.forEach(equipmentId -> disconnectedElementsByEquipmentId.putIfAbsent(equipmentId, List.of()));
        return disconnectedElementsByEquipmentId;
    }

    /**
     * Creates a validated contingency for one initiating equipment.
     *
     * @param network network containing the equipment
     * @param equipmentId identifier of the equipment to convert into a contingency
     * @return contingency ready to be propagated by OpenLoadFlow
     */
    private static Contingency createOutageGroupContingency(Network network, String equipmentId) {
        Identifiable<?> identifiable = network.getIdentifiable(equipmentId);
        if (identifiable == null) {
            throw new PowsyblException("Equipment '" + equipmentId + "' not found");
        }
        try {
            return new Contingency(equipmentId, List.of(ContingencyElementFactory.create(identifiable)));
        } catch (PowsyblException e) {
            throw new PowsyblException("Equipment '" + equipmentId + "' is not supported for outage groups");
        }
    }

    /**
     * Creates the contingency propagation options used to derive outage groups.
     *
     * @return contingency creation parameters with propagation enabled
     */
    private static PropagatedContingencyCreationParameters createOutageGroupContingencyCreationParameters() {
        return new PropagatedContingencyCreationParameters()
            .setContingencyPropagation(true);
    }

    /**
     * Creates the load-flow network parameters required for outage-group evaluation.
     *
     * @param network network being evaluated
     * @param loadFlowParameters base load-flow parameters used to derive OpenLoadFlow settings
     * @param breakers whether breaker topology must be modeled in the load-flow network
     * @return OpenLoadFlow network parameters configured for outage-group computation
     */
    private static LfNetworkParameters createOutageGroupLfNetworkParameters(Network network, LoadFlowParameters loadFlowParameters,
                                                                            boolean breakers) {
        return OpenLoadFlowParameters.createAcParameters(network, loadFlowParameters,
            OpenLoadFlowParameters.get(loadFlowParameters), new SparseMatrixFactory(), new EvenShiloachGraphDecrementalConnectivityFactory<LfBus, LfBranch>(), breakers, false)
            .getNetworkParameters();
    }

    /**
     * Selects the load-flow networks whose connectivity results should contribute to outage-group evaluation.
     *
     * @param lfNetworks load-flow networks derived from the IIDM network
     * @param componentMode component selection mode coming from the load-flow parameters
     * @return list of valid load-flow networks that match the requested component mode
     */
    private static List<LfNetwork> getOutageGroupNetworksToEvaluate(LfNetworkList lfNetworks,
                                                                    LoadFlowParameters.ComponentMode componentMode) {
        return switch (componentMode) {
            case MAIN_CONNECTED -> lfNetworks.getList().stream()
                .filter(lfNetwork -> lfNetwork.getNumCC() == ComponentConstants.MAIN_NUM
                    && lfNetwork.getValidity().equals(LfNetwork.Validity.VALID))
                .toList();
            case MAIN_SYNCHRONOUS -> lfNetworks.getList().stream()
                .filter(lfNetwork -> lfNetwork.getSynchronousNetworks().size() == 1
                    && lfNetwork.getSynchronousNetworks().getFirst().getNumSC() == ComponentConstants.MAIN_NUM
                    && lfNetwork.getValidity().equals(LfNetwork.Validity.VALID))
                .toList();
            case ALL_CONNECTED -> lfNetworks.getList().stream()
                .filter(lfNetwork -> lfNetwork.getValidity().equals(LfNetwork.Validity.VALID))
                .toList();
        };
    }

    public static Stream<TemporaryLimitData> getLimits(Network network) {
        Stream.Builder<TemporaryLimitData> limits = Stream.builder();
        network.getBranchStream().forEach(branch -> {
            addOperationalLimitGroupsLimits(limits, branch.getOperationalLimitsGroups1(), branch, ONE,
                    (String) branch.getSelectedOperationalLimitsGroupId1().orElse(null),
                    branch.getTerminal1().getVoltageLevel().getNominalV());
            addOperationalLimitGroupsLimits(limits, branch.getOperationalLimitsGroups2(), branch, TWO,
                    (String) branch.getSelectedOperationalLimitsGroupId2().orElse(null),
                    branch.getTerminal2().getVoltageLevel().getNominalV());
        });
        network.getBoundaryLineStream().forEach(boundaryLine ->
            addOperationalLimitGroupsLimits(limits, boundaryLine.getOperationalLimitsGroups(), boundaryLine, NONE,
                    boundaryLine.getSelectedOperationalLimitsGroupId().orElse(null),
                    boundaryLine.getTerminal().getVoltageLevel().getNominalV())
        );
        network.getThreeWindingsTransformerStream().forEach(twt -> {
            addOperationalLimitGroupsLimits(limits, twt.getLeg1().getOperationalLimitsGroups(), twt, ONE,
                    twt.getLeg1().getSelectedOperationalLimitsGroupId().orElse(null),
                    twt.getLeg1().getTerminal().getVoltageLevel().getNominalV());
            addOperationalLimitGroupsLimits(limits, twt.getLeg2().getOperationalLimitsGroups(), twt, TWO,
                    twt.getLeg2().getSelectedOperationalLimitsGroupId().orElse(null),
                    twt.getLeg2().getTerminal().getVoltageLevel().getNominalV());
            addOperationalLimitGroupsLimits(limits, twt.getLeg3().getOperationalLimitsGroups(), twt, THREE,
                    twt.getLeg3().getSelectedOperationalLimitsGroupId().orElse(null),
                    twt.getLeg3().getTerminal().getVoltageLevel().getNominalV());
        });
        return limits.build();
    }

    public static Stream<TemporaryLimitData> getSelectedLimits(Network network) {
        return getLimits(network).filter(TemporaryLimitData::isSelected);
    }

    private static void addOperationalLimitGroupsLimits(Stream.Builder<TemporaryLimitData> limits, Collection<OperationalLimitsGroup> groups,
                                                        Identifiable<?> element, TemporaryLimitData.Side side, String selectedGroupId,
                                                        double perUnitingNominalV) {
        groups.forEach(group -> {
            String groupId1 = group.getId();
            boolean isSelected1 = groupId1.equals(selectedGroupId);
            addLimit(limits, element, group.getCurrentLimits().orElse(null), side, groupId1, isSelected1, perUnitingNominalV);
            addLimit(limits, element, group.getActivePowerLimits().orElse(null), side, groupId1, isSelected1, perUnitingNominalV);
            addLimit(limits, element, group.getApparentPowerLimits().orElse(null), side, groupId1, isSelected1, perUnitingNominalV);
        });
    }

    private static void addLimit(Stream.Builder<TemporaryLimitData> temporaryLimitContexts, Identifiable<?> identifiable,
                                 LoadingLimits limits, TemporaryLimitData.Side side, String groupId, boolean isSelected,
                                 double perUnitingNominalV) {
        if (limits != null) {
            temporaryLimitContexts.add(new TemporaryLimitData(identifiable.getId(), "permanent_limit", side, limits.getPermanentLimit(),
                    limits.getLimitType(), identifiable.getType(), groupId, isSelected, perUnitingNominalV, limits));
            limits.getTemporaryLimits().stream()
                    .map(temporaryLimit -> new TemporaryLimitData(identifiable.getId(), temporaryLimit.getName(), side, temporaryLimit.getValue(),
                            limits.getLimitType(), identifiable.getType(), temporaryLimit.getAcceptableDuration(), temporaryLimit.isFictitious(),
                            groupId, isSelected, perUnitingNominalV, limits))
                    .forEach(temporaryLimitContexts::add);
        }
    }

    public static Stream<ConnectablePositionFeederData> getFeeders(Network network) {
        Stream.Builder<ConnectablePositionFeederData> feeders = Stream.builder();
        network.getConnectableStream().forEach(connectable -> {
            ConnectablePosition<?> connectablePosition = (ConnectablePosition<?>) connectable.getExtension(ConnectablePosition.class);
            if (connectablePosition != null) {
                if (connectablePosition.getFeeder() != null) {
                    feeders.add(new ConnectablePositionFeederData(connectablePosition.getExtendable().getId(),
                            connectablePosition.getFeeder(), null));
                }
                if (connectablePosition.getFeeder1() != null) {
                    feeders.add(new ConnectablePositionFeederData(connectablePosition.getExtendable().getId(),
                            connectablePosition.getFeeder1(), SideEnum.ONE));
                }
                if (connectablePosition.getFeeder2() != null) {
                    feeders.add(new ConnectablePositionFeederData(connectablePosition.getExtendable().getId(),
                            connectablePosition.getFeeder2(), SideEnum.TWO));
                }
                if (connectablePosition.getFeeder3() != null) {
                    feeders.add(new ConnectablePositionFeederData(connectablePosition.getExtendable().getId(),
                            connectablePosition.getFeeder3(), SideEnum.THREE));
                }
            }
        });
        return feeders.build();
    }

    public static void setRegulatingTerminal(Consumer<Terminal> adder, Network network, String elementId) {
        Identifiable<?> injection = network.getIdentifiable(elementId);
        if (injection instanceof Injection<?>) {
            adder.accept(((Injection<?>) injection).getTerminal());
        } else {
            throw new UnsupportedOperationException("Cannot set regulated element to " + elementId +
                    ": the regulated element may only be a busbar section or an injection.");
        }
    }

    public static void setPccTerminal(Consumer<Terminal> adder, Network network, String elementId) {
        //It may be necessary to precise which type of Connectable and which Terminal is needed
        Connectable<?> connectable = network.getConnectable(elementId);
        adder.accept(connectable.getTerminals().getFirst());
    }

    public static String getRegulatedElementId(Supplier<Terminal> regulatingTerminalGetter) {
        Terminal terminal = regulatingTerminalGetter.get();
        return terminal.getConnectable() != null ? terminal.getConnectable().getId() : null;
    }

    /**
     * @param b bus in Bus/Breaker view
     * @return bus in bus view containing b if there is one.
     */
    public static Optional<Bus> getBusViewBus(Bus b) {
        VoltageLevel voltageLevel = b.getVoltageLevel();
        if (voltageLevel.getTopologyKind() == TopologyKind.BUS_BREAKER) {
            // Bus/Breaker. There is an easy method directly available.
            return Optional.ofNullable(voltageLevel.getBusView().getMergedBus(b.getId()));
        } else {
            // Node/Breaker.
            // First we try the fast and easy way using connected terminals. Works for the vast majority of buses.
            Optional<Bus> busInBusView = b.getConnectedTerminalStream().map(t -> t.getBusView().getBus())
                    .filter(Objects::nonNull)
                    .findFirst();
            if (busInBusView.isPresent()) {
                return busInBusView;
            }
            // Didn't find using connected terminals. There is the possibility that the bus has zero connected terminal
            // on its own but is still part of a Merged Bus via a closed retained switch. We examine this case below.
            // We should probably build something more efficient on powsybl-core side to avoid having
            // to loop over all buses in the voltage level.
            return voltageLevel.getBusView().getBusStream()
                    .filter(busViewBus -> voltageLevel.getBusBreakerView().getBusStreamFromBusViewBusId(busViewBus.getId())
                            .anyMatch(b2 -> b.getId().equals(b2.getId())))
                    .findFirst();
        }
    }
}
