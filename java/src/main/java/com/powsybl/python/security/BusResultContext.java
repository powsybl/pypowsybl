/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.security;

import com.powsybl.security.results.BusResult;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public class BusResultContext extends BusResult {

    private final String contingencyId;

    public BusResultContext(BusResult busResults, String contingency) {
        super(busResults.getVoltageLevelId(), busResults.getBusId(), busResults.getV(), busResults.getAngle());
        this.contingencyId = contingency;
    }

    public String getContingencyId() {
        return contingencyId;
    }
}
