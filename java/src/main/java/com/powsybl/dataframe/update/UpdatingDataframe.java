/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.update;

import com.powsybl.dataframe.SeriesMetadata;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * @author Etienne Lesot {@literal <etienne.lesot at rte-france.com>}
 */
public interface UpdatingDataframe {

    static void applyIfPresent(IntSeries series, int index, IntConsumer consumer) {
        if (series != null) {
            consumer.accept(series.get(index));
        }
    }

    static void applyIfPresent(DoubleSeries series, int index, DoubleConsumer consumer) {
        if (series != null) {
            consumer.accept(series.get(index));
        }
    }

    static void applyIfPresent(StringSeries series, int index, Consumer<String> consumer) {
        if (series != null) {
            consumer.accept(series.get(index));
        }
    }

    List<SeriesMetadata> getSeriesMetadata();

    DoubleSeries getDoubles(String column);

    IntSeries getInts(String column);

    StringSeries getStrings(String column);

    default OptionalDouble getDoubleValue(String column, int row) {
        DoubleSeries series = getDoubles(column);
        return series != null ? OptionalDouble.of(series.get(row)) : OptionalDouble.empty();
    }

    default Optional<String> getStringValue(String column, int row) {
        StringSeries series = getStrings(column);
        return series != null ? Optional.of(series.get(row)) : Optional.empty();
    }

    default OptionalInt getIntValue(String column, int row) {
        IntSeries series = getInts(column);
        return series != null ? OptionalInt.of(series.get(row)) : OptionalInt.empty();
    }

    int getRowCount();
}
