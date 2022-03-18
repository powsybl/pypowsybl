/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.iidm.network.Branch;

import java.util.Objects;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public class TemporaryCurrentLimitData {

    private final String branchId;
    private final String name;
    private final Branch.Side side;
    private final double value;
    private final int acceptableDuration;
    private final boolean isFictitious;

    public TemporaryCurrentLimitData(String branchId, String name, Branch.Side side, double value, int acceptableDuration, boolean isFictitious) {
        this.branchId = Objects.requireNonNull(branchId);
        this.name = Objects.requireNonNull(name);
        this.side = Objects.requireNonNull(side);
        this.value = value;
        this.acceptableDuration = acceptableDuration;
        this.isFictitious = isFictitious;
    }

    public TemporaryCurrentLimitData(String branchId, String name, Branch.Side side, double value) {
        this(branchId, name, side, value, -1, false);
    }

    public String getBranchId() {
        return branchId;
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }

    public int getAcceptableDuration() {
        return acceptableDuration;
    }

    public Branch.Side getSide() {
        return side;
    }

    public boolean isFictitious() {
        return isFictitious;
    }

    @Override
    public String toString() {
        return "TemporaryLimitContext{" +
            "branchId='" + branchId + '\'' +
            ", name='" + name + '\'' +
            ", side=" + side +
            ", value=" + value +
            ", acceptableDuration=" + acceptableDuration +
            ", isFictitious=" + isFictitious +
            '}';
    }
}
