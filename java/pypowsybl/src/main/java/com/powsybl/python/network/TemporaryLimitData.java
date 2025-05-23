/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.iidm.network.LimitType;

import java.util.Objects;

/**
 * @author Etienne Lesot {@literal <etienne.lesot at rte-france.com>}
 */
public class TemporaryLimitData {

    private final String id;
    private final String name;
    private final LimitType type;
    private final Side side;
    private final IdentifiableType elementType;
    private final double value;
    private final int acceptableDuration;
    private final boolean isFictitious;
    private final String groupId;
    private final boolean selected;
    private final double perUnitingNominalV;

    public enum Side {
        NONE,
        ONE,
        TWO,
        THREE
    }

    public TemporaryLimitData(String id, String name, Side side, double value, LimitType type, IdentifiableType elementType,
                              int acceptableDuration, boolean isFictitious, String groupId, boolean selected, double perUnitingNominalV) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.side = side;
        this.type = Objects.requireNonNull(type);
        this.elementType = Objects.requireNonNull(elementType);
        this.value = value;
        this.acceptableDuration = acceptableDuration;
        this.isFictitious = isFictitious;
        this.groupId = Objects.requireNonNull(groupId);
        this.selected = selected;
        this.perUnitingNominalV = perUnitingNominalV;
    }

    public TemporaryLimitData(String id, String name, Side side, double value, LimitType type, IdentifiableType elementType,
                              String groupId, boolean isSelected, double perUnitingNominalV) {
        this(id, name, side, value, type, elementType, -1, false, groupId, isSelected, perUnitingNominalV);
    }

    public String getId() {
        return id;
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

    public Side getSide() {
        return side;
    }

    public IdentifiableType getElementType() {
        return elementType;
    }

    public LimitType getType() {
        return type;
    }

    public boolean isFictitious() {
        return isFictitious;
    }

    public String getGroupId() {
        return groupId;
    }

    public boolean isSelected() {
        return selected;
    }

    public double getPerUnitingNominalV() {
        return perUnitingNominalV;
    }
}
