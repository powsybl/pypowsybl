/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.shortcircuit.adders;

import com.powsybl.python.commons.PyPowsyblApiHeader.ShortCircuitFaultType;

import java.util.Map;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
public final class ShortCircuitFaultAdderFactory {

    private static final Map<ShortCircuitFaultType, ShortCircuitContextFaultAdder> ADDERS = Map.ofEntries(
            Map.entry(ShortCircuitFaultType.BUS_FAULT, new BusFaultDataframeAdder()));

    public static ShortCircuitContextFaultAdder getAdder(ShortCircuitFaultType type) {
        return ADDERS.get(type);
    }

    private ShortCircuitFaultAdderFactory() {
    }
}
