/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.powsybl.python.TemporaryLimitData.Side.*;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public final class NetworkUtil {

    private NetworkUtil() {
    }

    public static Network createEurostagTutorialExample1() {
        Network network = EurostagTutorialExample1Factory.create();
        fix(network);
        return network;
    }

    public static Network createEurostagTutorialExample1WithFixedCurrentLimits() {
        Network network = EurostagTutorialExample1Factory.createWithFixedCurrentLimits();
        fix(network);
        return network;
    }

    public static Network createEurostagTutorialExample1WithFixedPowerLimits() {
        Network network = EurostagTutorialExample1Factory.createWithFixedLimits();
        fix(network);
        return network;
    }

    private static Network fix(Network network) {
        Generator gen = network.getGenerator("GEN");
        if (gen != null) {
            gen.setMaxP(4999);
        }
        Generator gen2 = network.getGenerator("GEN2");
        if (gen2 != null) {
            gen2.setMaxP(4999);
        }
        return network;
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
        if (equipment instanceof Injection) {
            Injection<?> injection = (Injection<?>) equipment;
            if (connected) {
                return injection.getTerminal().connect();
            } else {
                return injection.getTerminal().disconnect();
            }
        } else if (equipment instanceof Branch) {
            Branch<?> branch = (Branch<?>) equipment;
            if (connected) {
                boolean done1 = branch.getTerminal1().connect();
                boolean done2 = branch.getTerminal2().connect();
                return done1 || done2;
            } else {
                boolean done1 = branch.getTerminal1().disconnect();
                boolean done2 = branch.getTerminal2().disconnect();
                return done1 || done2;
            }
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

    private static boolean filter(Branch branch, Set<Double> nominalVoltages, Set<String> countries, boolean mainCc, boolean mainSc,
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
            if (bus1 != null && bus2 != null && bus1.getId().equals(bus2.getId())) {
                return false;
            }
        }
        return true;
    }

    private static boolean filter(Injection injection, Set<Double> nominalVoltages, Set<String> countries, boolean mainCc, boolean mainSc) {
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
        if (mainSc && !isInMainSc(terminal)) {
            return false;
        }
        return true;
    }

    static List<String> getElementsIds(Network network, PyPowsyblApiHeader.ElementType elementType, Set<Double> nominalVoltages,
                                       Set<String> countries, boolean mainCc, boolean mainSc, boolean notConnectedToSameBusAtBothSides) {
        List<String> elementsIds;
        switch (elementType) {
            case LINE:
                elementsIds = network.getLineStream()
                        .filter(l -> filter(l, nominalVoltages, countries, mainCc, mainSc, notConnectedToSameBusAtBothSides))
                        .map(Identifiable::getId)
                        .collect(Collectors.toList());
                break;

            case TWO_WINDINGS_TRANSFORMER:
                elementsIds = network.getTwoWindingsTransformerStream()
                        .filter(twt -> filter(twt, nominalVoltages, countries, mainCc, mainSc, notConnectedToSameBusAtBothSides))
                        .map(Identifiable::getId)
                        .collect(Collectors.toList());
                break;

            case GENERATOR:
                elementsIds = network.getGeneratorStream()
                        .filter(g -> filter(g, nominalVoltages, countries, mainCc, mainSc))
                        .map(Identifiable::getId)
                        .collect(Collectors.toList());
                break;

            case LOAD:
                elementsIds = network.getLoadStream()
                        .filter(g -> filter(g, nominalVoltages, countries, mainCc, mainSc))
                        .map(Identifiable::getId)
                        .collect(Collectors.toList());
                break;

            default:
                throw new PowsyblException("Unsupported element type:" + elementType);
        }
        return elementsIds;
    }

    public static Stream<TemporaryLimitData> getLimits(Network network) {
        Stream.Builder<TemporaryLimitData> limits = Stream.builder();
        network.getBranchStream().forEach(branch -> {
            addLimit(limits, branch,  branch.getCurrentLimits1(), ONE);
            addLimit(limits, branch,  branch.getCurrentLimits2(), TWO);
            addLimit(limits, branch,  branch.getActivePowerLimits1(), ONE);
            addLimit(limits, branch,  branch.getActivePowerLimits2(), TWO);
            addLimit(limits, branch,  branch.getApparentPowerLimits1(), ONE);
            addLimit(limits, branch,  branch.getApparentPowerLimits2(), TWO);
        });
        network.getDanglingLineStream().forEach(danglingLine -> {
            addLimit(limits, danglingLine, danglingLine.getCurrentLimits(), NONE);
            addLimit(limits, danglingLine, danglingLine.getActivePowerLimits(), NONE);
            addLimit(limits, danglingLine, danglingLine.getApparentPowerLimits(), NONE);
        });
        network.getThreeWindingsTransformerStream().forEach(threeWindingsTransformer -> {
            addLimit(limits, threeWindingsTransformer, threeWindingsTransformer.getLeg1().getCurrentLimits(), ONE);
            addLimit(limits, threeWindingsTransformer, threeWindingsTransformer.getLeg1().getActivePowerLimits(), ONE);
            addLimit(limits, threeWindingsTransformer, threeWindingsTransformer.getLeg1().getApparentPowerLimits(), ONE);
            addLimit(limits, threeWindingsTransformer, threeWindingsTransformer.getLeg2().getCurrentLimits(), TWO);
            addLimit(limits, threeWindingsTransformer, threeWindingsTransformer.getLeg2().getActivePowerLimits(), TWO);
            addLimit(limits, threeWindingsTransformer, threeWindingsTransformer.getLeg2().getApparentPowerLimits(), TWO);
            addLimit(limits, threeWindingsTransformer, threeWindingsTransformer.getLeg3().getCurrentLimits(), THREE);
            addLimit(limits, threeWindingsTransformer, threeWindingsTransformer.getLeg3().getActivePowerLimits(), THREE);
            addLimit(limits, threeWindingsTransformer, threeWindingsTransformer.getLeg3().getApparentPowerLimits(), THREE);
        });
        return limits.build();
    }

    private static void addLimit(Stream.Builder<TemporaryLimitData> temporaryLimitContexts, Identifiable<?> identifiable,
                                 LoadingLimits limits, TemporaryLimitData.Side side) {
        if (limits != null) {
            temporaryLimitContexts.add(new TemporaryLimitData(identifiable.getId(), "permanent_limit", side, limits.getPermanentLimit(), limits.getLimitType(), identifiable.getType()));
            limits.getTemporaryLimits().stream()
                    .map(temporaryLimit -> new TemporaryLimitData(identifiable.getId(), temporaryLimit.getName(), side, temporaryLimit.getValue(),
                            limits.getLimitType(), identifiable.getType(), temporaryLimit.getAcceptableDuration(), temporaryLimit.isFictitious()))
                    .forEach(temporaryLimitContexts::add);
        }
    }
}
