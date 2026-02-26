/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.dynamic.CategoryAttributeUtils;
import com.powsybl.dataframe.dynamic.EventInformation;
import com.powsybl.dataframe.dynamic.PersistentDoubleSeries;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawo.models.events.EventReferenceVoltageVariationBuilder;
import com.powsybl.iidm.network.Network;

import java.util.List;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class ReferenceVoltageVariationAdder extends AbstractEventModelAdder {

    protected static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex(STATIC_ID),
            SeriesMetadata.doubles(START_TIME),
            SeriesMetadata.doubles(DELTA_U));

    protected static final EventInformation INFORMATION = new EventInformation(
            EventReferenceVoltageVariationBuilder.getModelInfo(),
            CategoryAttributeUtils.createFromMetadata(METADATA)
    );

    @Override
    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    @Override
    public EventInformation getEventInformation() {
        return INFORMATION;
    }

    private static class ReferenceVoltageVariationSeries extends AbstractEventModelSeries<EventReferenceVoltageVariationBuilder> {

        private final DoubleSeries deltaUs;

        ReferenceVoltageVariationSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.deltaUs = PersistentDoubleSeries.copyOf(dataframe, DELTA_U);
        }

        @Override
        protected void applyOnBuilder(int row, EventReferenceVoltageVariationBuilder builder) {
            applyIfPresent(staticIds, row, builder::staticId);
            applyIfPresent(startTimes, row, builder::startTime);
            applyIfPresent(deltaUs, row, builder::deltaU);
        }

        @Override
        protected EventReferenceVoltageVariationBuilder createBuilder(Network network, ReportNode reportNode) {
            return EventReferenceVoltageVariationBuilder.of(network, reportNode);
        }
    }

    @Override
    protected EventModelSeries createEventModelSeries(UpdatingDataframe dataframe) {
        return new ReferenceVoltageVariationSeries(dataframe);
    }
}
