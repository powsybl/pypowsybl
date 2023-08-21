/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.shortcircuit;

import com.powsybl.security.LimitViolation;

import java.util.Objects;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
public class LimitViolationFaultContext extends LimitViolation {

    private final String faultId;

    public LimitViolationFaultContext(String faultId, LimitViolation limitViolation) {
        super(limitViolation.getSubjectId(), limitViolation.getSubjectName(), limitViolation.getLimitType(),
            limitViolation.getLimitName(), limitViolation.getAcceptableDuration(), limitViolation.getLimit(),
            limitViolation.getLimitReduction(), limitViolation.getValue(), limitViolation.getSide());
        this.faultId = Objects.requireNonNull(faultId);
    }

    public String getFaultId() {
        return faultId;
    }

}
