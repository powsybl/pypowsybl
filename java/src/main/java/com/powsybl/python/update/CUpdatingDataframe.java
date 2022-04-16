/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.update;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;

import java.util.*;

/**
 * @author Etienne Lesot {@literal <etienne.lesot at rte-france.com>}
 */
public class CUpdatingDataframe implements UpdatingDataframe {
    private final int rowCount;
    private final Map<String, SeriesMetadata> seriesMetadata = new LinkedHashMap<>();
    private final Map<String, IntSeries> intSeries = new HashMap<>();
    private final Map<String, DoubleSeries> doubleSeries = new HashMap<>();
    private final Map<String, StringSeries> stringSeries = new HashMap<>();

    public CUpdatingDataframe(int rowCount) {
        this.rowCount = rowCount;
    }

    @Override
    public List<SeriesMetadata> getSeriesMetadata() {
        return new ArrayList<>(seriesMetadata.values());
    }

    @Override
    public DoubleSeries getDoubles(String column) {
        return doubleSeries.get(column);
    }

    @Override
    public IntSeries getInts(String column) {
        return intSeries.get(column);
    }

    @Override
    public StringSeries getStrings(String column) {
        return stringSeries.get(column);
    }

    public void addSeries(IntSeries series, SeriesMetadata seriesMetadata) {
        this.seriesMetadata.put(seriesMetadata.getName(), seriesMetadata);
        this.intSeries.put(seriesMetadata.getName(), series);
    }

    public void addSeries(DoubleSeries series, SeriesMetadata seriesMetadata) {
        this.seriesMetadata.put(seriesMetadata.getName(), seriesMetadata);
        this.doubleSeries.put(seriesMetadata.getName(), series);
    }

    public void addSeries(StringSeries series, SeriesMetadata seriesMetadata) {
        this.seriesMetadata.put(seriesMetadata.getName(), seriesMetadata);
        this.stringSeries.put(seriesMetadata.getName(), series);
    }

    @Override
    public int getRowCount() {
        return rowCount;
    }
}
