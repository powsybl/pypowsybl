/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.shortcircuit;

import com.powsybl.shortcircuit.MagnitudeShortCircuitBusResults;

import java.util.Objects;

/**
 * @author Christian Biasuzzi {@literal <christian.biasuzzi@soft.it>}
 */
public class MagnitudeBusResultsContext extends MagnitudeShortCircuitBusResults {

    private final String faultId;

    public MagnitudeBusResultsContext(String faultId, MagnitudeShortCircuitBusResults busResults) {
        super(busResults.getVoltageLevelId(), busResults.getBusId(), busResults.getInitialVoltageMagnitude(), busResults.getVoltage(), busResults.getVoltageDropProportional());
        this.faultId = Objects.requireNonNull(faultId);
    }

    public String getFaultId() {
        return faultId;
    }

}
