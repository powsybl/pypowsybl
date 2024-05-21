/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network;

/**
 * @author Etienne Lesot {@literal <etienne.lesot at rte-france.com>}
 */
public class NetworkDataframeContext {

    public static final NetworkDataframeContext DEFAULT = new NetworkDataframeContext(false, 0);

    private final boolean perUnit;
    private final double nominalApparentPower;

    public NetworkDataframeContext(boolean perUnit, double nominalApparentPower) {
        this.perUnit = perUnit;
        this.nominalApparentPower = nominalApparentPower;
    }

    public boolean isPerUnit() {
        return perUnit;
    }

    public double getNominalApparentPower() {
        return nominalApparentPower;
    }
}
