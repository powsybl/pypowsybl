/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.shortcircuit;

import com.powsybl.shortcircuit.FortescueShortCircuitBusResults;

import java.util.Objects;

/**
 * @author Etienne Lesot {@literal <etienne.lesot@rte-france.com>}
 */
public class FortescueBusResultsContext extends FortescueShortCircuitBusResults {

    private final String faultId;

    public FortescueBusResultsContext(String faultId, FortescueShortCircuitBusResults fortescueShortCircuitBusResults) {
        super(fortescueShortCircuitBusResults.getVoltageLevelId(), fortescueShortCircuitBusResults.getBusId(),
                fortescueShortCircuitBusResults.getInitialVoltageMagnitude(),
                fortescueShortCircuitBusResults.getVoltage(),
                fortescueShortCircuitBusResults.getVoltageDropProportional());
        this.faultId = Objects.requireNonNull(faultId);
    }

    public String getFaultId() {
        return faultId;
    }
}
