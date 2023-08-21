/**
 * Copyright (c) 2023 RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.shortcircuit;

import com.powsybl.commons.reporter.Reporter;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.CommonObjects;
import com.powsybl.shortcircuit.*;

import java.util.Collections;
import java.util.List;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
public class ShortCircuitAnalysisContext {

    public List<Fault> getFaults() {
        return faults;
    }

    public void setFaults(List<Fault> faults) {
        this.faults = faults;
    }

    List<Fault> faults = Collections.emptyList();

    ShortCircuitAnalysisResult run(Network network, ShortCircuitParameters shortCircuitAnalysisParameters, String provider, Reporter reporter) {
        List <FaultParameters> faultsParameters = Collections.emptyList();
        ShortCircuitAnalysisResult results = ShortCircuitAnalysis.find(provider)
                .run(
                        network,
                        faults,
                        shortCircuitAnalysisParameters,
                        CommonObjects.getComputationManager(),
                        faultsParameters,
                        (reporter == null) ? Reporter.NO_OP : reporter
                );
        return results;
    }
}
