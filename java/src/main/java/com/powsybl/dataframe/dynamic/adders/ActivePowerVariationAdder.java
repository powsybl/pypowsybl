/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawaltz.models.events.EventActivePowerVariationBuilder;
import com.powsybl.iidm.network.Network;

import java.util.List;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class ActivePowerVariationAdder extends AbstractEventModelAdder {

    protected static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex(STATIC_ID),
            SeriesMetadata.stringIndex(START_TIME),
            SeriesMetadata.strings(DELTA_P));

    @Override
    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    private static class ActivePowerVariationSeries extends AbstractEventModelSeries<EventActivePowerVariationBuilder> {

        private final StringSeries staticIds;
        private final DoubleSeries startTimes;
        private final DoubleSeries deltaPs;

        ActivePowerVariationSeries(UpdatingDataframe dataframe) {
            this.staticIds = dataframe.getStrings(STATIC_ID);
            this.startTimes = dataframe.getDoubles(START_TIME);
            this.deltaPs = dataframe.getDoubles(DELTA_P);
        }

        @Override
        protected void applyOnBuilder(int row, EventActivePowerVariationBuilder builder) {
            applyIfPresent(staticIds, row, builder::staticId);
            applyIfPresent(startTimes, row, builder::startTime);
            applyIfPresent(deltaPs, row, builder::deltaP);
        }

        @Override
        protected EventActivePowerVariationBuilder createBuilder(Network network, ReportNode reportNode) {
            return EventActivePowerVariationBuilder.of(network, reportNode);
        }
    }

    @Override
    protected EventModelSeries createEventModelSeries(UpdatingDataframe dataframe) {
        return new ActivePowerVariationSeries(dataframe);
    }
}
