/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.sensitivity;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
class SensitivityAnalysisTest {

    @Test
    void testNoOutputMatricesAvailableErrors() {
        Network network = EurostagTutorialExample1Factory.create();
        LoadFlowParameters params = new LoadFlowParameters()
                .setDc(false)
                .setDistributedSlack(false);
        SensitivityAnalysisContext sensitivityContext = new SensitivityAnalysisContext();
        SensitivityAnalysisResultContext result = sensitivityContext.run(network, params, "OpenLoadFlow");

        try {
            result.createBranchFlowsSensitivityMatrix("", "");
            fail();
        } catch (PowsyblException ignored) {
        }

        try {
            result.createBusVoltagesSensitivityMatrix("");
            fail();
        } catch (PowsyblException ignored) {
        }
    }
}
