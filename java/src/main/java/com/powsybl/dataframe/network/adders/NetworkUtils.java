/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.VoltageLevel;

/**
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public final class NetworkUtils {

    private NetworkUtils() {
    }

    public static VoltageLevel getVoltageLevelOrThrow(Network network, String id) {
        VoltageLevel voltageLevel = network.getVoltageLevel(id);
        if (voltageLevel == null) {
            throw new PowsyblException("Voltage level " + id + " does not exist.");
        }
        return voltageLevel;
    }

    public static Substation getSubstationOrThrow(Network network, String id) {
        Substation substation = network.getSubstation(id);
        if (substation == null) {
            throw new PowsyblException("Substation " + id + " does not exist.");
        }
        return substation;
    }

    public static Identifiable<?> getIdentifiableOrThrow(Network network, String id) {
        Identifiable<?> identifiable = network.getIdentifiable(id);
        if (identifiable == null) {
            throw new PowsyblException("Network element " + id + " does not exist.");
        }
        return identifiable;
    }
}
