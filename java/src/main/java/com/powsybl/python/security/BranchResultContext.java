/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.security;

import com.powsybl.security.results.BranchResult;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public class BranchResultContext extends BranchResult {

    private final String contingencyId;

    public BranchResultContext(BranchResult branchResult, String contingency) {
        super(branchResult.getBranchId(), branchResult.getP1(), branchResult.getQ1(), branchResult.getI1(),
            branchResult.getP2(), branchResult.getQ2(), branchResult.getI2(), branchResult.getFlowTransfer());
        this.contingencyId = contingency;
    }

    public String getContingencyId() {
        return contingencyId;
    }
}
