/**
 * Copyright (c) 2026, Elia Group (http://www.eliagroup.eu)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.security;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.contingency.strategy.OperatorStrategy;
import com.powsybl.contingency.strategy.condition.TrueCondition;
import com.powsybl.dataframe.DataframeHandler;
import com.powsybl.dataframe.impl.DefaultDataframeHandler;
import com.powsybl.dataframe.impl.Series;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.security.LimitViolationsResult;
import com.powsybl.security.PostContingencyComputationStatus;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.results.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nico Westerbeck {@literal <nico.westerbeck at 50hertz.com>}
 */
class NetworkResultDataframesTest {

    private static final LimitViolationsResult NO_VIOLATIONS = new LimitViolationsResult(List.of());

    private static NetworkResult networkResult(List<BranchResult> branches, List<BusResult> buses) {
        return new NetworkResult(branches, buses, List.of());
    }

    private static PostContingencyResult post(String contingencyId, NetworkResult networkResult) {
        return new PostContingencyResult(new Contingency(contingencyId), PostContingencyComputationStatus.CONVERGED,
                NO_VIOLATIONS, networkResult, new ConnectivityResult(0, 0, 0, 0, Set.of()), 0);
    }

    private static OperatorStrategyResult strategy(String strategyId, String contingencyId, NetworkResult networkResult) {
        OperatorStrategy operatorStrategy = new OperatorStrategy(strategyId, ContingencyContext.specificContingency(contingencyId),
                new TrueCondition(), List.of("action"));
        return new OperatorStrategyResult(operatorStrategy, List.of(new OperatorStrategyResult.ConditionalActionsResult(
                "action", PostContingencyComputationStatus.CONVERGED, NO_VIOLATIONS, networkResult, 0)));
    }

    private static SecurityAnalysisResult result(NetworkResult pre, List<PostContingencyResult> post, List<OperatorStrategyResult> strategies) {
        return new SecurityAnalysisResult(new PreContingencyResult(LoadFlowResult.ComponentResult.Status.CONVERGED, NO_VIOLATIONS, pre, 0),
                post, strategies);
    }

    private static Map<String, Series> write(SecurityAnalysisResult result, BiConsumer<SecurityAnalysisResult, DataframeHandler> writer) {
        List<Series> series = new ArrayList<>();
        writer.accept(result, new DefaultDataframeHandler(series::add));
        return series.stream().collect(Collectors.toMap(Series::getName, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * The values of a dictionary-encoded index level.
     */
    private static List<String> decode(Map<String, Series> series, String level) {
        Series s = series.get(level);
        assertThat(s.isIndex()).isTrue();
        assertThat(s.isDictionary()).isTrue();
        return Arrays.asList(s.getStrings());
    }

    @Test
    void testDictionariesHoldOnlyReportedValues() {
        // C2 has no result and must not appear in the dictionary; the strategy rows carry their contingency id
        SecurityAnalysisResult result = result(
                networkResult(List.of(new BranchResult("L1", 1, 2, 3, 4, 5, 6, Double.NaN), new BranchResult("L2", 7, 8, 9, 10, 11, 12, Double.NaN)), List.of()),
                List.of(post("C1", networkResult(List.of(new BranchResult("L2", 13, 14, 15, 16, 17, 18, 0.5)), List.of())),
                        post("C2", networkResult(List.of(), List.of())),
                        post("C3", networkResult(List.of(new BranchResult("L1", 19, 20, 21, 22, 23, 24, Double.NaN)), List.of()))),
                List.of(strategy("S1", "C3", networkResult(List.of(new BranchResult("L1", 25, 26, 27, 28, 29, 30, Double.NaN)), List.of()))));

        Map<String, Series> series = write(result, NetworkResultDataframes::writeBranchResults);

        assertThat(series.keySet()).containsExactly("contingency_id", "operator_strategy_id", "branch_id",
                "p1", "q1", "i1", "p2", "q2", "i2", "flow_transfer");
        assertThat(series.get("contingency_id").getDictionary()).containsExactly("", "C1", "C3");
        assertThat(series.get("operator_strategy_id").getDictionary()).containsExactly("", "S1");
        assertThat(series.get("branch_id").getDictionary()).containsExactly("L1", "L2");
        assertThat(decode(series, "contingency_id")).containsExactly("", "", "C1", "C3", "C3");
        assertThat(decode(series, "operator_strategy_id")).containsExactly("", "", "", "", "S1");
        assertThat(decode(series, "branch_id")).containsExactly("L1", "L2", "L2", "L1", "L1");
        assertThat(series.get("p1").getDoubles()).containsExactly(1, 7, 13, 19, 25);
        assertThat(series.get("flow_transfer").getDoubles()).containsExactly(Double.NaN, Double.NaN, 0.5, Double.NaN, Double.NaN);

        // no pre-contingency result: the empty contingency id must not appear either
        SecurityAnalysisResult postOnly = result(networkResult(List.of(), List.of()),
                List.of(post("C1", networkResult(List.of(), List.of(new BusResult("VL1", "B1", 400, 0.1), new BusResult("VL2", "B2", 225, Double.NaN))))),
                List.of(strategy("S1", "C1", networkResult(List.of(), List.of(new BusResult("VL2", "B2", 226, -0.2))))));

        Map<String, Series> buses = write(postOnly, NetworkResultDataframes::writeBusResults);

        assertThat(buses.get("contingency_id").getDictionary()).containsExactly("C1");
        assertThat(decode(buses, "operator_strategy_id")).containsExactly("", "", "S1");
        assertThat(decode(buses, "voltage_level_id")).containsExactly("VL1", "VL2", "VL2");
        assertThat(decode(buses, "bus_id")).containsExactly("B1", "B2", "B2");
        assertThat(buses.get("v_mag").getDoubles()).containsExactly(400, 225, 226);
    }

    @Test
    void testSameElementsInSeveralStates() {
        // same id instances at the same positions (fast path), then a state with removed and reordered elements
        // (new String("L1") is an equal but distinct instance: the identity check misses and the id is looked up by value)
        List<BranchResult> all = List.of(new BranchResult("L1", 1, 0, 0, 0, 0, 0), new BranchResult("L2", 2, 0, 0, 0, 0, 0),
                new BranchResult("L3", 3, 0, 0, 0, 0, 0));
        List<BranchResult> shifted = List.of(new BranchResult("L3", 4, 0, 0, 0, 0, 0), new BranchResult(new String("L1"), 5, 0, 0, 0, 0, 0));
        SecurityAnalysisResult result = result(networkResult(all, List.of()),
                List.of(post("C1", networkResult(all, List.of())), post("C2", networkResult(shifted, List.of())),
                        post("C3", networkResult(all, List.of()))),
                List.of());

        Map<String, Series> series = write(result, NetworkResultDataframes::writeBranchResults);

        assertThat(series.get("branch_id").getDictionary()).containsExactly("L1", "L2", "L3");
        assertThat(decode(series, "branch_id")).containsExactly("L1", "L2", "L3", "L1", "L2", "L3", "L3", "L1", "L1", "L2", "L3");
        assertThat(decode(series, "contingency_id")).containsExactly("", "", "", "C1", "C1", "C1", "C2", "C2", "C3", "C3", "C3");
        assertThat(series.get("p1").getDoubles()).containsExactly(1, 2, 3, 1, 2, 3, 4, 5, 1, 2, 3);
    }

    @Test
    void testThreeWindingsTransformerColumns() {
        // no operator strategy level: operator strategy results do not report three windings transformers
        Map<String, Series> series = write(result(networkResult(List.of(), List.of()), List.of(), List.of()),
                NetworkResultDataframes::writeThreeWindingsTransformerResults);

        assertThat(series.keySet()).containsExactly("contingency_id", "transformer_id",
                "p1", "q1", "i1", "p2", "q2", "i2", "p3", "q3", "i3");
    }

    @Test
    void testOperatorStrategyWithoutConditionalActionsResult() {
        // getNetworkResult() of such an operator strategy result throws: it must be skipped, not fail the whole table
        OperatorStrategy empty = new OperatorStrategy("S0", ContingencyContext.specificContingency("C0"), new TrueCondition(), List.of("action"));
        SecurityAnalysisResult result = result(
                networkResult(List.of(new BranchResult("L1", 1, 0, 0, 0, 0, 0)), List.of(new BusResult("VL1", "B1", 400, 0))),
                List.of(post("C1", networkResult(List.of(new BranchResult("L1", 2, 0, 0, 0, 0, 0)), List.of(new BusResult("VL1", "B1", 401, 0))))),
                List.of(new OperatorStrategyResult(empty, List.of()),
                        strategy("S1", "C1", networkResult(List.of(new BranchResult("L1", 3, 0, 0, 0, 0, 0)), List.of(new BusResult("VL1", "B1", 402, 0))))));

        Map<String, Series> branches = write(result, NetworkResultDataframes::writeBranchResults);
        assertThat(branches.get("contingency_id").getDictionary()).containsExactly("", "C1");
        assertThat(branches.get("operator_strategy_id").getDictionary()).containsExactly("", "S1");
        assertThat(decode(branches, "operator_strategy_id")).containsExactly("", "", "S1");
        assertThat(branches.get("p1").getDoubles()).containsExactly(1, 2, 3);

        Map<String, Series> buses = write(result, NetworkResultDataframes::writeBusResults);
        assertThat(decode(buses, "operator_strategy_id")).containsExactly("", "", "S1");
        assertThat(buses.get("v_mag").getDoubles()).containsExactly(400, 401, 402);
    }

    @Test
    void testEmptyResults() {
        SecurityAnalysisResult result = result(networkResult(List.of(), List.of()),
                List.of(post("C1", networkResult(List.of(), List.of()))), List.of());
        Map<String, Series> series = write(result, NetworkResultDataframes::writeBranchResults);
        assertThat(series).hasSize(10);
        assertThat(series.get("contingency_id").getDictionary()).isEmpty();
        assertThat(series.get("branch_id").getInts()).isEmpty();
        assertThat(series.get("p1").getDoubles()).isEmpty();
    }
}
