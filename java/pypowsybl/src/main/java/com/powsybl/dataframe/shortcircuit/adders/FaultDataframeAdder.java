/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.shortcircuit.adders;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.python.shortcircuit.ShortCircuitAnalysisContext;
import com.powsybl.shortcircuit.BranchFault;
import com.powsybl.shortcircuit.BusFault;
import com.powsybl.shortcircuit.Fault;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Christian Biasuzzi {@literal <christian.biasuzzi@soft.it>}
 */
public class FaultDataframeAdder implements ShortCircuitContextFaultAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
        SeriesMetadata.stringIndex("id"),
        SeriesMetadata.strings("element_id"),
        SeriesMetadata.doubles("r"),
        SeriesMetadata.doubles("x"),
        SeriesMetadata.doubles("proportional_location"),
        SeriesMetadata.strings("fault_type")
    );

    @Override
    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    private static final class FaultSeries {
        private final StringSeries faultId;
        private final StringSeries elementId;
        private final DoubleSeries r;
        private final DoubleSeries x;
        private final DoubleSeries proportionalLocation;
        private final StringSeries faultType;

        FaultSeries(UpdatingDataframe dataframe) {
            this.faultId = SeriesUtils.getRequiredStrings(dataframe, "id");
            this.elementId = SeriesUtils.getRequiredStrings(dataframe, "element_id");
            this.r = dataframe.getDoubles("r");
            this.x = dataframe.getDoubles("x");
            this.proportionalLocation = dataframe.getDoubles("proportionalLocation");
            this.faultType = SeriesUtils.getRequiredStrings(dataframe, "fault_type");
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

        public DoubleSeries getProportionalLocation() {
            return proportionalLocation;
        }

        public StringSeries getFaultType() {
            return faultType;
        }
    }

    @Override
    public void addElements(ShortCircuitAnalysisContext context, UpdatingDataframe dataframe) {
        List<Fault> faults = new ArrayList<>();
        if (dataframe.getRowCount() > 0) {
            FaultSeries series = new FaultSeries(dataframe);
            for (int row = 0; row < dataframe.getRowCount(); row++) {
                String faultId = series.getFaultId().get(row);
                String elementId = series.getElementId().get(row);
                double r = series.getR() != null ? series.getR().get(row) : 0;
                double x = series.getX() != null ? series.getX().get(row) : 0;
                double proportionalLocation = series.getProportionalLocation() != null ? series.getProportionalLocation().get(row) : 0;
                String faultType = series.getFaultType().get(row);
                if (faultType.equals("BUS_FAULT")) {
                    faults.add(new BusFault(faultId, elementId, r, x));
                } else if (faultType.equals("BRANCH_FAULT")) {
                    faults.add(new BranchFault(faultId, elementId, r, x, proportionalLocation));
                } else {
                    throw new PowsyblException("Fault type must be precised");
                }
            }
        }
        context.setFaults(faults);
    }

}
