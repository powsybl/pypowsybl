/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.shortcircuit.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.python.shortcircuit.ShortCircuitAnalysisContext;
import com.powsybl.shortcircuit.BusFault;
import com.powsybl.shortcircuit.Fault;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
public class BusFaultDataframeAdder implements ShortCircuitContextFaultAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("element_id"),
            SeriesMetadata.doubles("r"),
            SeriesMetadata.doubles("x")
    );

    @Override
    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    private static final class BusFaultSeries {
        private final StringSeries faultId;
        private final StringSeries elementId;
        private final DoubleSeries r;
        private final DoubleSeries x;

        BusFaultSeries(UpdatingDataframe dataframe) {
            this.faultId = SeriesUtils.getRequiredStrings(dataframe, "id");
            this.elementId = SeriesUtils.getRequiredStrings(dataframe, "element_id");
            this.r = dataframe.getDoubles("r");
            this.x = dataframe.getDoubles("x");
        }

        public StringSeries getFaultId() {
            return faultId;
        }

        public StringSeries getElementId() {
            return elementId;
        }

        public DoubleSeries getR() {
            return r;
        }

        public DoubleSeries getX() {
            return x;
        }
    }

    @Override
    public void addElements(ShortCircuitAnalysisContext context, UpdatingDataframe dataframe) {
        List<Fault> faults = new ArrayList<>();
        if (dataframe.getRowCount() > 0) {
            BusFaultSeries series = new BusFaultSeries(dataframe);
            for (int row = 0; row < dataframe.getRowCount(); row++) {
                String faultId = series.getFaultId().get(row);
                String elementId = series.getElementId().get(row);
                double r = series.getR() != null ? series.getR().get(row) : Double.NaN;
                double x = series.getX() != null ? series.getX().get(row) : Double.NaN;
                faults.add(r == Double.NaN || x == Double.NaN ?
                        new BusFault(faultId, elementId) :
                        new BusFault(faultId, elementId, r, x));
            }
        }
        context.setFaults(faults);
    }

}
