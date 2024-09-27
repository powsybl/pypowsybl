/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.sensitivity;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
class SensitivityAnalysisTest {

    @Test
    void testNoOutputMatricesAvailableErrors() {
        Network network = EurostagTutorialExample1Factory.create();
        LoadFlowParameters lfParams = new LoadFlowParameters()
                .setDc(false)
                .setDistributedSlack(false);
        SensitivityAnalysisParameters parameters = new SensitivityAnalysisParameters();
        parameters.setLoadFlowParameters(lfParams);
        SensitivityAnalysisContext sensitivityContext = new SensitivityAnalysisContext();
        SensitivityAnalysisResultContext result = sensitivityContext.run(network, parameters, "OpenLoadFlow", ReportNode.NO_OP);

        try {
            result.createSensitivityMatrix("", "");
            fail();
        } catch (PowsyblException ignored) {
        }

        try {
            result.createSensitivityMatrix("m", "");
            fail();
        } catch (PowsyblException ignored) {
        }
    }
}
