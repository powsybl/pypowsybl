package com.powsybl.python;

import com.powsybl.iidm.network.Branch;

import java.util.Objects;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public class TemporaryLimitContext {
    private final String branchId;
    private final String name;
    private final Branch.Side side;
    private final double value;
    private final int acceptableDuration;
    private final boolean isFictitious;

    public TemporaryLimitContext(String branchId, String name, Branch.Side side, double value, int acceptableDuration, boolean isFictitious) {
        this.branchId = Objects.requireNonNull(branchId);
        this.name = Objects.requireNonNull(name);
        this.side = Objects.requireNonNull(side);
        this.value = value;
        this.acceptableDuration = acceptableDuration;
        this.isFictitious = isFictitious;
    }

    public TemporaryLimitContext(String branchId, String name, Branch.Side side, double value) {
        this.branchId = Objects.requireNonNull(branchId);
        this.name = Objects.requireNonNull(name);
        this.side = Objects.requireNonNull(side);
        this.value = value;
        this.acceptableDuration = -1;
        this.isFictitious = false;
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
