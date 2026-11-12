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
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawo.models.events.EventCloseSwitchBuilder;
import com.powsybl.iidm.network.Network;

import java.util.List;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.START_TIME;
import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.STATIC_ID;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class CloseSwitchAdder extends AbstractEventModelAdder {

    protected static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex(STATIC_ID),
            SeriesMetadata.doubles(START_TIME));

    protected static final EventInformation INFORMATION = new EventInformation(
            EventCloseSwitchBuilder.getModelInfo(),
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

    private static class CloseSwitchSeries extends AbstractEventModelSeries<EventCloseSwitchBuilder> {

        CloseSwitchSeries(UpdatingDataframe dataframe) {
            super(dataframe);
        }

        @Override
        protected void applyOnBuilder(int row, EventCloseSwitchBuilder builder) {
            applyIfPresent(staticIds, row, builder::staticId);
            applyIfPresent(startTimes, row, builder::startTime);
        }

        @Override
        protected EventCloseSwitchBuilder createBuilder(Network network, ReportNode reportNode) {
            return EventCloseSwitchBuilder.of(network, reportNode);
        }
    }

    @Override
    protected EventModelSeries createEventModelSeries(UpdatingDataframe dataframe) {
        return new CloseSwitchSeries(dataframe);
    }
}
