package com.powsybl.dataframe.network.adders;

import java.util.Objects;

public class LimitsDataframeAdderKey {
    private final String elementId;
    private final String side;
    private final String limitType;
    private final String groupId;

    public LimitsDataframeAdderKey(String elementId, String side, String limitType, String groupId) {
        this.elementId = elementId;
        this.side = side;
        this.limitType = limitType;
        this.groupId = groupId;
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

    public String getGroupId() {
        return groupId;
    }

    @Override
    public String toString() {
        return "LimitsDataframeAdderKey{" +
                "elementId='" + elementId + '\'' +
                ", side='" + side + '\'' +
                ", limitType='" + limitType + '\'' +
                ", groupId='" + groupId + '\'' +
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
        return Objects.equals(elementId, that.elementId) && Objects.equals(side, that.side) && Objects.equals(limitType, that.limitType) && Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementId, side, limitType, groupId);
    }
}
