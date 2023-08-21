/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.shortcircuit;

import com.powsybl.dataframe.impl.Series;
import com.powsybl.python.network.Dataframes;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationType;
import com.powsybl.shortcircuit.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.Double.NaN;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
class ShortCircuitAnalysisTest {

    @Test
    void testShortCircuitAnalysis() {
        ShortCircuitParameters parameters = new ShortCircuitParameters()
                .setWithFortescueResult(false)
                .setWithLimitViolations(false)
                .setWithFeederResult(false)
                .setStudyType(StudyType.TRANSIENT);

        assertThat(parameters).isNotNull();
        ShortCircuitAnalysisContext analysisContext = new ShortCircuitAnalysisContext();
        assertThat(analysisContext).isNotNull();
        List<Fault> faults = new ArrayList<>();
        BusFault bf1 = new BusFault("F1", "B2", 0., 0., Fault.ConnectionType.SERIES, Fault.FaultType.THREE_PHASE);
        BusFault bf2 = new BusFault("F2", "B3", 0., 0., Fault.ConnectionType.SERIES, Fault.FaultType.THREE_PHASE);
        BusFault bf3 = new BusFault("F3", "B9", 0., 0., Fault.ConnectionType.SERIES, Fault.FaultType.THREE_PHASE);
        faults.add(bf1);
        faults.add(bf2);
        faults.add(bf3);
        assertThat(faults).hasSize(3);
        analysisContext.setFaults(faults);
    }

    @Test
    void testShortCircuitAnalysisResults() {
        Fault f1 = new BusFault("f1", "bus1");
        Fault f2 = new BusFault("f2", "bus2");

        LimitViolation lv1 = new LimitViolation("subj1", LimitViolationType.HIGH_VOLTAGE, 1.0, 2.0f, 3.0);
        LimitViolation lv2 = new LimitViolation("subj2", LimitViolationType.HIGH_VOLTAGE, 1.0, 2.0f, 3.0);
        LimitViolation lv3 = new LimitViolation("subj3", LimitViolationType.HIGH_VOLTAGE, 1.0, 2.0f, 3.0);
        LimitViolation lv4 = new LimitViolation("subj4", LimitViolationType.HIGH_VOLTAGE, 1.0, 2.0f, 3.0);
        LimitViolation lv5 = new LimitViolation("subj5", LimitViolationType.HIGH_VOLTAGE, 1.0, 2.0f, 3.0);

        MagnitudeFeederResult mfr1 = new MagnitudeFeederResult("connect1", 1.1);
        MagnitudeFeederResult mfr2 = new MagnitudeFeederResult("connect2", 1.2);

        FaultResult fr1 = new MagnitudeFaultResult(f1, 1.0, List.of(mfr1, mfr2), List.of(lv1, lv2), 5.0, FaultResult.Status.SUCCESS);
        FaultResult fr2 = new MagnitudeFaultResult(f2, 2.0, Collections.emptyList(), List.of(lv3, lv4, lv5), 6.0, FaultResult.Status.SUCCESS);

        ShortCircuitAnalysisResult fakeResults = new ShortCircuitAnalysisResult(List.of(fr1, fr2));

        List<Series> faultResultsSeries = Dataframes.createSeries(Dataframes.shortCircuitAnalysisFaultResultsMapper(), fakeResults);
        Assertions.assertThat(faultResultsSeries)
                .extracting(Series::getName)
                .containsExactly("id", "status", "shortCircuitPower", "timeConstant", "current", "voltage");
        Assertions.assertThat(faultResultsSeries.get(0).getStrings())
                .containsExactly("f1", "f2");
        Assertions.assertThat(faultResultsSeries.get(1).getStrings())
                .containsExactly("SUCCESS", "SUCCESS");
        Assertions.assertThat(faultResultsSeries.get(2).getDoubles())
                .containsExactly(1.0, 2.0);
        Assertions.assertThat(faultResultsSeries.get(3).getStrings())
                .containsExactly(null, null);
        Assertions.assertThat(faultResultsSeries.get(4).getDoubles())
                .containsExactly(5.0, 6.0);
        Assertions.assertThat(faultResultsSeries.get(5).getDoubles())
                .containsExactly(NaN, NaN);

        List<Series> feederResultsSeries = Dataframes.createSeries(Dataframes.shortCircuitAnalysisMagnitudeFeederResultsMapper(), fakeResults);
        Assertions.assertThat(feederResultsSeries)
                .extracting(Series::getName)
                .containsExactly("id", "connectable_id", "current");
        Assertions.assertThat(feederResultsSeries.get(0).getStrings())
                .containsExactly("f1", "f1");
        Assertions.assertThat(feederResultsSeries.get(1).getStrings())
                .containsExactly("connect1", "connect2");
        Assertions.assertThat(feederResultsSeries.get(2).getDoubles())
                .containsExactly(1.1, 1.2);

        List<Series> limitViolationsSeries = Dataframes.createSeries(Dataframes.shortCircuitAnalysisLimitViolationsResultsMapper(), fakeResults);
        Assertions.assertThat(limitViolationsSeries)
                .extracting(Series::getName)
                .containsExactly("id", "subject_id", "subject_name", "limit_type", "limit_name", "limit",
                        "acceptable_duration", "limit_reduction", "value", "side");
        Assertions.assertThat(limitViolationsSeries.get(0).getStrings())
                .containsExactly("f1", "f1", "f2", "f2", "f2");
        Assertions.assertThat(limitViolationsSeries.get(1).getStrings())
                .containsExactly("subj1", "subj2", "subj3", "subj4", "subj5");
    }
}
