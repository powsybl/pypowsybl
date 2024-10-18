/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.network.extensions.ConnectablePositionFeederData;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import com.powsybl.python.commons.PyPowsyblApiHeader;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
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

    static boolean updateConnectableStatus(Network network, String id, boolean connected) {
        Identifiable<?> equipment = network.getIdentifiable(id);
        if (equipment == null) {
            throw new PowsyblException("Equipment '" + id + "' not found");
        }
        if (!(equipment instanceof Connectable)) {
            throw new PowsyblException("Equipment '" + id + "' is not a connectable");
        }
        if (equipment instanceof Injection<?> injection) {
            if (connected) {
                return injection.getTerminal().connect();
            } else {
                return injection.getTerminal().disconnect();
            }
        } else if (equipment instanceof Branch<?> branch) {
            boolean done1;
            boolean done2;
            if (connected) {
                done1 = branch.getTerminal1().connect();
                done2 = branch.getTerminal2().connect();
            } else {
                done1 = branch.getTerminal1().disconnect();
                done2 = branch.getTerminal2().disconnect();
            }
            return done1 || done2;
        }
        return false;
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

    public static Stream<TemporaryLimitData> getLimits(Network network) {
        Stream.Builder<TemporaryLimitData> limits = Stream.builder();
        network.getBranchStream().forEach(branch -> {
            addOperationalLimitGroupsLimits(limits, branch.getOperationalLimitsGroups1(), branch, ONE,
                    (String) branch.getSelectedOperationalLimitsGroupId1().orElse(null));
            addOperationalLimitGroupsLimits(limits, branch.getOperationalLimitsGroups2(), branch, TWO,
                    (String) branch.getSelectedOperationalLimitsGroupId2().orElse(null));
        });
        network.getDanglingLineStream().forEach(danglingLine ->
            addOperationalLimitGroupsLimits(limits, danglingLine.getOperationalLimitsGroups(), danglingLine, NONE,
                    danglingLine.getSelectedOperationalLimitsGroupId().orElse(null))
        );
        network.getThreeWindingsTransformerStream().forEach(twt -> {
            addOperationalLimitGroupsLimits(limits, twt.getLeg1().getOperationalLimitsGroups(), twt, ONE,
                    twt.getLeg1().getSelectedOperationalLimitsGroupId().orElse(null));
            addOperationalLimitGroupsLimits(limits, twt.getLeg2().getOperationalLimitsGroups(), twt, TWO,
                    twt.getLeg2().getSelectedOperationalLimitsGroupId().orElse(null));
            addOperationalLimitGroupsLimits(limits, twt.getLeg3().getOperationalLimitsGroups(), twt, THREE,
                    twt.getLeg3().getSelectedOperationalLimitsGroupId().orElse(null));
        });
        return limits.build();
    }

    public static Stream<TemporaryLimitData> getSelectedLimits(Network network) {
        return getLimits(network).filter(TemporaryLimitData::isSelected);
    }

    private static void addOperationalLimitGroupsLimits(Stream.Builder<TemporaryLimitData> limits, Collection<OperationalLimitsGroup> groups,
                                                        Identifiable<?> element, TemporaryLimitData.Side side, String selectedGroupId) {
        groups.forEach(group -> {
            String groupId1 = group.getId();
            boolean isSelected1 = groupId1.equals(selectedGroupId);
            addLimit(limits, element, group.getCurrentLimits().orElse(null), side, groupId1, isSelected1);
            addLimit(limits, element, group.getActivePowerLimits().orElse(null), side, groupId1, isSelected1);
            addLimit(limits, element, group.getApparentPowerLimits().orElse(null), side, groupId1, isSelected1);
        });
    }

    private static void addLimit(Stream.Builder<TemporaryLimitData> temporaryLimitContexts, Identifiable<?> identifiable,
                                 LoadingLimits limits, TemporaryLimitData.Side side, String groupId, boolean isSelected) {
        if (limits != null) {
            temporaryLimitContexts.add(new TemporaryLimitData(identifiable.getId(), "permanent_limit", side, limits.getPermanentLimit(),
                    limits.getLimitType(), identifiable.getType(), groupId, isSelected));
            limits.getTemporaryLimits().stream()
                    .map(temporaryLimit -> new TemporaryLimitData(identifiable.getId(), temporaryLimit.getName(), side, temporaryLimit.getValue(),
                            limits.getLimitType(), identifiable.getType(), temporaryLimit.getAcceptableDuration(), temporaryLimit.isFictitious(),
                            groupId, isSelected))
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

    public static String getRegulatedElementId(Supplier<Terminal> regulatingTerminalGetter) {
        Terminal terminal = regulatingTerminalGetter.get();
        return terminal.getConnectable() != null ? terminal.getConnectable().getId() : null;
    }
}
