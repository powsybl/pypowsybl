/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic;

import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public final class PersistentDoubleSeries implements DoubleSeries {

    private final List<Double> values;

    public static PersistentDoubleSeries copyOf(UpdatingDataframe dataframe, String columnName) {
        DoubleSeries series = dataframe.getDoubles(columnName);
        if (series == null) {
            return null;
        }
        int rowCount = dataframe.getRowCount();
        List<Double> values = new ArrayList<>(rowCount);
        for (int row = 0; row < rowCount; row++) {
            values.add(series.get(row));
        }
        return new PersistentDoubleSeries(values);
    }

    private PersistentDoubleSeries(List<Double> values) {
        this.values = values;
    }

    @Override
    public double get(int index) {
        return values.get(index);
    }
}
