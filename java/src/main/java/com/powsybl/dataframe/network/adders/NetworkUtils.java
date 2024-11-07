/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.iidm.network.*;

import java.util.Optional;

/**
 * @author Sylvain Leclerc {@literal <sylvain.leclerc@rte-france.com>}
 */
public final class NetworkUtils {

    private static final String DOES_NOT_EXIST = "' does not exist.";

    private NetworkUtils() {
    }

    public static VoltageLevel getVoltageLevelOrThrow(Network network, String id) {
        VoltageLevel voltageLevel = network.getVoltageLevel(id);
        if (voltageLevel == null) {
            throw new PowsyblException("Voltage level '" + id + DOES_NOT_EXIST);
        }
        return voltageLevel;
    }

    public static Optional<VoltageLevel> getVoltageLevelOrThrowWithBusOrBusbarSectionId(Network network, int row, StringSeries voltageLevels, StringSeries busOrBusbarSections, boolean throwException) {
        if (voltageLevels == null) {
            if (busOrBusbarSections != null) {
                Identifiable<?> busOrBusbarSection = network.getIdentifiable(busOrBusbarSections.get(row));
                if (busOrBusbarSection == null && throwException) {
                    throw new PowsyblException(String.format("Bus or busbar section %s not found.", busOrBusbarSections.get(row)));
                }
                if (busOrBusbarSection instanceof BusbarSection bbs) {
                    return Optional.of(bbs.getTerminal().getVoltageLevel());
                } else if (busOrBusbarSection instanceof Bus bus) {
                    return Optional.of(bus.getVoltageLevel());
                } else {
                    if (throwException) {
                        throw new PowsyblException(String.format("Unsupported type %s for identifiable %s", busOrBusbarSection.getType(), busOrBusbarSection.getId()));
                    }
                }
            } else {
                if (throwException) {
                    throw new PowsyblException("Voltage level id and bus or busbar section id missing.");
                }
            }
        } else {
            return Optional.of(getVoltageLevelOrThrow(network, voltageLevels.get(row)));
        }
        return Optional.empty();
    }

    public static Substation getSubstationOrThrow(Network network, String id) {
        Substation substation = network.getSubstation(id);
        if (substation == null) {
            throw new PowsyblException("Substation '" + id + DOES_NOT_EXIST);
        }
        return substation;
    }

    public static Identifiable<?> getIdentifiableOrThrow(Network network, String id) {
        Identifiable<?> identifiable = network.getIdentifiable(id);
        if (identifiable == null) {
            throw new PowsyblException("Network element '" + id + DOES_NOT_EXIST);
        }
        return identifiable;
    }

    public static Injection<?> getGenOrLoadOrBusbarSectionOrThrow(Network network, String id) {
        Identifiable<?> identifiable = getIdentifiableOrThrow(network, id);
        if (!(identifiable instanceof Generator) && !(identifiable instanceof Load) && !(identifiable instanceof BusbarSection)) {
            throw new PowsyblException("Network element  '" + id + "' is not a generator, bus bar section, or load");
        }
        return (Injection<?>) identifiable;
    }

    public static Area getAreaOrThrow(Network network, String id) {
        Area area = network.getArea(id);
        if (area == null) {
            throw new PowsyblException("Area '" + id + DOES_NOT_EXIST);
        }
        return area;
    }

    public static DanglingLine getDanglingLineOrThrow(Network network, String id) {
        DanglingLine danglingLine = network.getDanglingLine(id);
        if (danglingLine == null) {
            throw new PowsyblException("Dangling Line '" + id + DOES_NOT_EXIST);
        }
        return danglingLine;
    }

    public static Connectable<?> getConnectableOrThrow(Network network, String id) {
        Connectable<?> connectable = network.getConnectable(id);
        if (connectable == null) {
            throw new PowsyblException("Connectable '" + id + DOES_NOT_EXIST);
        }
        return connectable;
    }

    public static Terminal getTerminalOrThrow(Network network, String id, String side) {
        Connectable<?> connectable = getConnectableOrThrow(network, id);
        Terminal terminal;
        if (connectable instanceof Injection<?> injection) {
            terminal = injection.getTerminal();
        } else if (connectable instanceof Branch<?> branch) {
            terminal = branch.getTerminal(TwoSides.valueOf(side));
        } else if (connectable instanceof ThreeWindingsTransformer t3wt) {
            terminal = t3wt.getTerminal(ThreeSides.valueOf(side));
        } else {
            // Never supposed to happen
            throw new PowsyblException("Connectable '" + id + "' is not an injection, branch, or three windings transformer");
        }
        return terminal;
    }
}
