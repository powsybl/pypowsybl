/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.shortcircuit;

import com.powsybl.shortcircuit.MagnitudeFeederResult;

import java.util.Objects;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
public class MagnitudeFeederResultContext extends MagnitudeFeederResult {
    private final String faultId;

    public MagnitudeFeederResultContext(String faultId, MagnitudeFeederResult result) {
        super(result.getConnectableId(), result.getCurrent(), result.getSide());
        this.faultId = Objects.requireNonNull(faultId);
    }

    public String getFaultId() {
        return faultId;
    }
}
