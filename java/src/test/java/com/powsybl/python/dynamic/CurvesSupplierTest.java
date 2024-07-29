/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.dynamicsimulation.*;
import com.powsybl.dynawaltz.DynaWaltzCurve;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
class CurvesSupplierTest {

    private static Network network;

    @BeforeAll
    static void setup() {
        network = EurostagTutorialExample1Factory.create();
    }

    @Test
    void testCurveMappingSupplier() {
        PythonCurveSupplier curvesSupplier = new PythonCurveSupplier();
        curvesSupplier.addCurve("BBM_GEN", "generator_omegaPu");
        curvesSupplier.addCurves("GEN2", List.of("generator_omegaPu", "generator_PGen"));
        List<Curve> expectedResult = List.of(new DynaWaltzCurve("BBM_GEN", "generator_omegaPu"),
                new DynaWaltzCurve("GEN2", "generator_omegaPu"),
                new DynaWaltzCurve("GEN2", "generator_PGen"));
        assertThat(curvesSupplier.get(network)).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrderElementsOf(expectedResult);
    }
}
