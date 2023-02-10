/**
 * Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic;

import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.timeseries.DoublePoint;
import com.powsybl.timeseries.TimeSeries;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 */
public final class CurvesSeries {

    private CurvesSeries() {
    }

    public static DataframeMapper<TimeSeries<DoublePoint, ?>> curvesDataFrameMapper(String colName) {
        DataframeMapperBuilder<TimeSeries<DoublePoint, ?>, DoublePoint> df = new DataframeMapperBuilder<>();
        df.itemsStreamProvider(TimeSeries::stream)
                .intsIndex("timestamp", pt -> (int) (pt.getTime() % Integer.MAX_VALUE))
                .doubles(colName, DoublePoint::getValue);
        return df.build();
    }
}
