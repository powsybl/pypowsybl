/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.security;

import com.powsybl.security.results.ThreeWindingsTransformerResult;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public class ThreeWindingsTransformerResultContext extends ThreeWindingsTransformerResult {

    private final String contingencyId;

    public ThreeWindingsTransformerResultContext(ThreeWindingsTransformerResult threeWindingsTransformerResult, String contingency) {
        super(threeWindingsTransformerResult.getThreeWindingsTransformerId(),
            threeWindingsTransformerResult.getP1(), threeWindingsTransformerResult.getQ1(), threeWindingsTransformerResult.getI1(),
            threeWindingsTransformerResult.getP2(), threeWindingsTransformerResult.getQ2(), threeWindingsTransformerResult.getI2(),
            threeWindingsTransformerResult.getP3(), threeWindingsTransformerResult.getQ3(), threeWindingsTransformerResult.getI3());
        this.contingencyId = contingency;
    }

    public String getContingencyId() {
        return contingencyId;
    }
}

