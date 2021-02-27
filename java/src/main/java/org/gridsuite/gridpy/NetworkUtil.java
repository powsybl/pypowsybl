/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.gridpy;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import org.gridsuite.gridpy.GridPyApiHeader.ElementType;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
                || countries.contains(voltageLevel1.getSubstation().getCountry().map(Country::name).orElse(null))
                || countries.contains(voltageLevel2.getSubstation().getCountry().map(Country::name).orElse(null)))) {
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
                || countries.contains(voltageLevel.getSubstation().getCountry().map(Country::name).orElse(null)))) {
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

    static List<String> getElementsIds(Network network, ElementType elementType, Set<Double> nominalVoltages,
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
}
