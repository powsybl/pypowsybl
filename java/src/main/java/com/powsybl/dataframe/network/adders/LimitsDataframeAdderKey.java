/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import java.util.Objects;

public class LimitsDataframeAdderKey {
    private final String elementId;
    private final String side;
    private final String limitType;

    public LimitsDataframeAdderKey(String elementId, String side, String limitType) {
        this.elementId = elementId;
        this.side = side;
        this.limitType = limitType;
    }

    public String getElementId() {
        return elementId;
    }

    public String getSide() {
        return side;
    }

    public String getLimitType() {
        return limitType;
    }

    @Override
    public String toString() {
        return "LimitsDataframeAdderKey{" +
                "elementId='" + elementId + '\'' +
                ", side='" + side + '\'' +
                ", limitType='" + limitType + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LimitsDataframeAdderKey that = (LimitsDataframeAdderKey) o;
        return Objects.equals(elementId, that.elementId) && Objects.equals(side, that.side) && Objects.equals(limitType, that.limitType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementId, side, limitType);
    }
}
