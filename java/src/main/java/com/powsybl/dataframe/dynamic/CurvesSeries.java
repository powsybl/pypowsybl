package com.powsybl.dataframe.dynamic;

import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.timeseries.DoublePoint;
import com.powsybl.timeseries.TimeSeries;

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
