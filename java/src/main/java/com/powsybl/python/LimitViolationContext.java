package com.powsybl.python;

import com.powsybl.security.LimitViolation;

import java.util.Objects;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public class LimitViolationContext extends LimitViolation {

    private final String contingencyId;

    public LimitViolationContext(String contingencyId, LimitViolation limitViolation) {
        super(limitViolation.getSubjectId(), limitViolation.getSubjectName(), limitViolation.getLimitType(),
            limitViolation.getLimitName(), limitViolation.getAcceptableDuration(), limitViolation.getLimit(),
            limitViolation.getLimitReduction(), limitViolation.getValue(), limitViolation.getSide());
        this.contingencyId = Objects.requireNonNull(contingencyId);
    }

    public String getContingencyId() {
        return contingencyId;
    }
}
