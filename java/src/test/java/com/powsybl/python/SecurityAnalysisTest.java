/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.ContingencyContextType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.monitor.StateMonitor;
import com.powsybl.security.results.BranchResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public class SecurityAnalysisTest {

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
            302.44404914466014, 0.9874027438014933, 456.7689759899915, -300.43389523337316,
            -137.18849307164064, 488.9927963672735));
        assertThat(result.getPostContingencyResults()).hasSize(1);
        assertThat(result.getPostContingencyResults().get(0).getBranchResults()).containsExactly(new BranchResult("NHV1_NHV2_2",
            610.5621535433195, 3.3405627152965636, 1008.9287882269936, -600.9961559564283,
            -285.379146550659, 1047.8257691455567));
    }

    @Test
    void testSecurityAnalysis() {
        Network network = EurostagTutorialExample1Factory.createWithFixedCurrentLimits();
        SecurityAnalysisContext analysisContext = new SecurityAnalysisContext();
        analysisContext.addContingency("First contingency", Collections.singletonList("NHV1_NHV2_1"));
        SecurityAnalysisResult result = analysisContext.run(network, new LoadFlowParameters(), "OpenSecurityAnalysis");

        List<LimitViolationContext> limitViolations = result.getPreContingencyResult().getLimitViolationsResult().getLimitViolations()
            .stream().map(limitViolation -> new LimitViolationContext("", limitViolation)).collect(Collectors.toList());
        result.getPostContingencyResults()
            .forEach(postContingencyResult -> limitViolations.addAll(postContingencyResult.getLimitViolationsResult()
                .getLimitViolations().stream()
                .map(limitViolation -> new LimitViolationContext(postContingencyResult.getContingency().getId(), limitViolation))
                .collect(Collectors.toList())));
        List<List> series = new SeriesPointerArrayBuilder<>(limitViolations)
                .addStringSeries("contingency_id", true, LimitViolationContext::getContingencyId)
                .addStringSeries("violation_id", true, LimitViolation::getSubjectId)
                .addStringSeries("violation_name", p -> Objects.toString(p.getSubjectName(), ""))
                .addEnumSeries("limit_type", LimitViolation::getLimitType)
                .addStringSeries("limit_name", p -> Objects.toString(p.getLimitName(), ""))
                .addDoubleSeries("limit", LimitViolation::getLimit)
                .addIntSeries("acceptable_duration", LimitViolation::getAcceptableDuration)
                .addDoubleSeries("limit_reduction", LimitViolation::getLimitReduction)
                .addDoubleSeries("value", LimitViolation::getValue)
                .addStringSeries("side", p -> Objects.toString(p.getSide(), ""))
                .buildJavaSeries();
        assertThat(series).hasSize(10).contains(Arrays.asList("First contingency", "First contingency"))
                .contains(Arrays.asList("NHV1_NHV2_2", "VLHV1"))
                .contains(Arrays.asList("CURRENT", "LOW_VOLTAGE"));

    }
}
