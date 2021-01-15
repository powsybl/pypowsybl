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

    static List<String> getElementsIds(Network network, ElementType elementType, double nominalVoltage, boolean mainCc) {
        List<String> elementsIds;
        switch (elementType) {
            case LINE:
                elementsIds = network.getLineStream()
                        .filter(l -> Double.isNaN(nominalVoltage) || l.getTerminal1().getVoltageLevel().getNominalV() == nominalVoltage)
                        .filter(l -> !mainCc || (isInMainCc(l.getTerminal1()) && isInMainCc(l.getTerminal2())))
                        .map(Identifiable::getId)
                        .collect(Collectors.toList());
                break;

            case TWO_WINDINGS_TRANSFORMER:
                elementsIds = network.getTwoWindingsTransformerStream()
                        .filter(twt -> Double.isNaN(nominalVoltage)
                                || twt.getTerminal1().getVoltageLevel().getNominalV() == nominalVoltage
                                || twt.getTerminal2().getVoltageLevel().getNominalV() == nominalVoltage)
                        .filter(l -> !mainCc || (isInMainCc(l.getTerminal1()) && isInMainCc(l.getTerminal2())))
                        .map(Identifiable::getId)
                        .collect(Collectors.toList());
                break;

            case GENERATOR:
                elementsIds = network.getGeneratorStream()
                        .filter(g -> Double.isNaN(nominalVoltage) || g.getTerminal().getVoltageLevel().getNominalV() == nominalVoltage)
                        .filter(g -> !mainCc || isInMainCc(g.getTerminal()))
                        .map(Identifiable::getId)
                        .collect(Collectors.toList());
                break;

            default:
                throw new PowsyblException("Unsupported element type:" + elementType);
        }
        return elementsIds;
    }
}
