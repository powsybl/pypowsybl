/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic;

import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.dynawo.contingency.results.ScenarioResult;
import com.powsybl.dynawo.margincalculation.results.LoadIncreaseResult;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Dataframe mappers for the results of a margin calculation.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class MarginCalculationDataframeMappersUtils {

    private MarginCalculationDataframeMappersUtils() {
    }

    public static DataframeMapper<List<LoadIncreaseResult>, Void> loadIncreaseResultsDataFrameMapper() {
        AtomicInteger index = new AtomicInteger();
        return new DataframeMapperBuilder<List<LoadIncreaseResult>, LoadIncreaseResult, Void>()
                .itemsProvider(Function.identity())
                .intsIndex("index", r -> index.getAndIncrement())
                .doubles("load_level", LoadIncreaseResult::loadLevel)
                .strings("status", r -> r.status().name())
                .build();
    }

    public static DataframeMapper<List<LoadIncreaseResult>, Void> scenarioResultsDataFrameMapper() {
        AtomicInteger index = new AtomicInteger();
        return new DataframeMapperBuilder<List<LoadIncreaseResult>, Pair<Double, ScenarioResult>, Void>()
                .itemsStreamProvider(results -> results.stream()
                        .flatMap(r -> r.scenarioResults().stream().map(s -> Pair.of(r.loadLevel(), s))))
                .intsIndex("index", p -> index.getAndIncrement())
                .doubles("load_level", Pair::getLeft)
                .strings("id", p -> p.getRight().id())
                .strings("status", p -> p.getRight().status().name())
                .build();
    }
}
