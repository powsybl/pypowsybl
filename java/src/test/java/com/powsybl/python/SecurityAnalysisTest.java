/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.dataframe.impl.Series;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.monitor.StateMonitor;
import com.powsybl.security.results.BranchResult;
import org.assertj.core.api.Assertions;
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
        Network network = EurostagTutorialExample1Factory.create();
        SecurityAnalysisResult result = analysisContext.run(network, new LoadFlowParameters(), "OpenSecurityAnalysis");
        assertThat(result.getPreContingencyResult().getPreContingencyBranchResults()).containsExactly(new BranchResult("NHV1_NHV2_2",
            302.44404914466014, 98.74027438014933, 456.7689759899916, -300.43389523337316,
            -137.18849307164064, 488.99279636727357));
        assertThat(result.getPostContingencyResults()).hasSize(1);
        assertThat(result.getPostContingencyResults().get(0).getBranchResults()).containsExactly(new BranchResult("NHV1_NHV2_2",
            610.5621535433195, 334.0562715296563, 1008.9287882269937, -600.9961559564283,
            -285.379146550659, 1047.825769145557));
    }

    @Test
    void testSecurityAnalysis() {
        Network network = EurostagTutorialExample1Factory.createWithFixedCurrentLimits();
        SecurityAnalysisContext analysisContext = new SecurityAnalysisContext();
        analysisContext.addContingency("First contingency", Collections.singletonList("NHV1_NHV2_1"));
        SecurityAnalysisResult result = analysisContext.run(network, new LoadFlowParameters(), "OpenSecurityAnalysis");

        List<Series> series = Dataframes.createSeries(Dataframes.limitViolationsMapper(), result);
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
