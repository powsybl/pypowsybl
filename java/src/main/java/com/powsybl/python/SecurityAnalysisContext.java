/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.commons.PowsyblException;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.sa.OpenSecurityAnalysisFactory;
import com.powsybl.security.SecurityAnalysis;
import com.powsybl.security.SecurityAnalysisFactory;
import com.powsybl.security.SecurityAnalysisParameters;
import com.powsybl.security.SecurityAnalysisResult;
import com.rte_france.powsybl.hades2.Hades2SecurityAnalysisFactory;

import java.util.List;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SecurityAnalysisContext extends AbstractContingencyContainer {

    SecurityAnalysisResult run(Network network, LoadFlowParameters loadFlowParameters, String provider) {
        SecurityAnalysisFactory securityAnalysisFactory;
        switch (provider) {
            case "OpenLoadFlow":
                securityAnalysisFactory = new OpenSecurityAnalysisFactory();
                break;

            case "Hades2":
                securityAnalysisFactory = new Hades2SecurityAnalysisFactory();
                break;

            default:
                throw new PowsyblException("Security analysis provider not supported: " + provider);
        }
        SecurityAnalysis securityAnalysis = securityAnalysisFactory.create(network, LocalComputationManager.getDefault(), 0);
        SecurityAnalysisParameters securityAnalysisParameters = SecurityAnalysisParameters.load();
        securityAnalysisParameters.setLoadFlowParameters(loadFlowParameters);
        List<Contingency> contingencies = createContingencies(network);
        return securityAnalysis
                .run(VariantManagerConstants.INITIAL_VARIANT_ID, securityAnalysisParameters, n -> contingencies)
                .join();
    }
}
