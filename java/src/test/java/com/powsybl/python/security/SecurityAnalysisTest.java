/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.security;

import com.powsybl.commons.reporter.Reporter;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.dataframe.impl.Series;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.python.network.Dataframes;
import com.powsybl.python.network.Networks;
import com.powsybl.security.SecurityAnalysisParameters;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.monitor.StateMonitor;
import com.powsybl.security.results.BranchResult;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
class SecurityAnalysisTest {

    @Test
    void testStateMonitors() {
        SecurityAnalysisContext analysisContext = new SecurityAnalysisContext();
        analysisContext.addContingency("NHV1_NHV2_1", Collections.singletonList("NHV1_NHV2_1"));
        analysisContext.addMonitor(new StateMonitor(new ContingencyContext("NHV1_NHV2_1", ContingencyContextType.SPECIFIC),
            Collections.singleton("NHV1_NHV2_2"), Collections.emptySet(), Collections.emptySet()));
        analysisContext.addMonitor(new StateMonitor(new ContingencyContext(null, ContingencyContextType.NONE),
            Collections.singleton("NHV1_NHV2_2"), Collections.emptySet(), Collections.emptySet()));
        Network network = Networks.createEurostagTutorialExample1();
        SecurityAnalysisResult result = analysisContext.run(network, new SecurityAnalysisParameters(), "OpenLoadFlow", Reporter.NO_OP);
        assertThat(result.getPreContingencyResult().getNetworkResult().getBranchResults()).hasSize(1);
        BranchResult branchResult = result.getPreContingencyResult().getNetworkResult().getBranchResults().get(0);
        var eps = Offset.offset(1e-12);
        assertThat(branchResult.getP1()).isCloseTo(302.44404914466014, eps);
        assertThat(branchResult.getQ1()).isCloseTo(98.74027438014933, eps);
        assertThat(branchResult.getI1()).isCloseTo(456.7689759899916, eps);
        assertThat(branchResult.getP2()).isCloseTo(-300.43389523337316, eps);
        assertThat(branchResult.getQ2()).isCloseTo(-137.18849307164064, eps);
        assertThat(branchResult.getI2()).isCloseTo(488.99279636727357, eps);
        assertThat(result.getPostContingencyResults()).hasSize(1);
        assertThat(result.getPreContingencyResult().getNetworkResult().getBranchResults()).hasSize(1);
        branchResult = result.getPostContingencyResults().get(0).getNetworkResult().getBranchResults().get(0);
        assertThat(branchResult.getP1()).isCloseTo(610.5621535433198, eps);
        assertThat(branchResult.getQ1()).isCloseTo(334.0562715296571, eps);
        assertThat(branchResult.getI1()).isCloseTo(1008.9287882269945, eps);
        assertThat(branchResult.getP2()).isCloseTo(-600.9961559564284, eps);
        assertThat(branchResult.getQ2()).isCloseTo(-285.3791465506596, eps);
        assertThat(branchResult.getI2()).isCloseTo(1047.8257691455574, eps);
    }

    @Test
    void testSecurityAnalysis() {
        Network network = EurostagTutorialExample1Factory.createWithFixedCurrentLimits();
        SecurityAnalysisContext analysisContext = new SecurityAnalysisContext();
        analysisContext.addContingency("First contingency", Collections.singletonList("NHV1_NHV2_1"));
        SecurityAnalysisResult result = analysisContext.run(network, new SecurityAnalysisParameters(), "OpenLoadFlow", Reporter.NO_OP);

        List<Series> series = Dataframes.createSeries(Dataframes.limitViolationsMapper(), result, false);
        Assertions.assertThat(series)
            .extracting(Series::getName)
            .containsExactly("contingency_id", "subject_id", "subject_name", "limit_type", "limit_name", "limit",
                             "acceptable_duration", "limit_reduction", "value", "side");
        Assertions.assertThat(series.get(0).getStrings())
            .containsExactly("First contingency", "First contingency");
        Assertions.assertThat(series.get(1).getStrings())
            .containsExactly("NHV1_NHV2_2", "VLHV1");
    }
}
