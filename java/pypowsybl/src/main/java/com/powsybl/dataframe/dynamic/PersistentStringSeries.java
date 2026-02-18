/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic;

import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public final class PersistentStringSeries implements StringSeries {

    private final List<String> values;

    public static PersistentStringSeries copyOf(UpdatingDataframe dataframe, String columnName) {
        StringSeries series = dataframe.getStrings(columnName);
        if (series == null) {
            return null;
        }
        int rowCount = dataframe.getRowCount();
        List<String> values = new ArrayList<>(rowCount);
        for (int row = 0; row < rowCount; row++) {
            values.add(series.get(row));
        }
        return new PersistentStringSeries(values);
    }

    private PersistentStringSeries(List<String> values) {
        this.values = values;
    }

    @Override
    public String get(int index) {
        return values.get(index);
    }
}
