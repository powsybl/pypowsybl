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
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawaltz.models.automationsystems.TapChangerAutomationSystemBuilder;
import com.powsybl.iidm.network.Network;

import java.util.List;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class TapChangerAutomationSystemAdder extends AbstractDynamicModelAdder {

    protected static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex(DYNAMIC_MODEL_ID),
            SeriesMetadata.strings(PARAMETER_SET_ID),
            SeriesMetadata.strings(MODEL_NAME),
            SeriesMetadata.strings(STATIC_ID),
            SeriesMetadata.ints(SIDE));

    @Override
    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    private static class TapChangerSeries extends AbstractAutomationSystemSeries<TapChangerAutomationSystemBuilder> {

        private final StringSeries staticIds;
        private final IntSeries sides;

        TapChangerSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.staticIds = dataframe.getStrings(STATIC_ID);
            this.sides = dataframe.getInts(SIDE);
        }

        @Override
        protected void applyOnBuilder(int row, TapChangerAutomationSystemBuilder builder) {
            super.applyOnBuilder(row, builder);
            applyIfPresent(staticIds, row, builder::staticId);
            // TODO handle TransformerSide enum on python side
            //applyIfPresent(sides, row, TransformerSide.class, builder::side);
        }

        @Override
        protected TapChangerAutomationSystemBuilder createBuilder(Network network, ReportNode reportNode) {
            return TapChangerAutomationSystemBuilder.of(network, reportNode);
        }

        @Override
        protected TapChangerAutomationSystemBuilder createBuilder(Network network, String modelName, ReportNode reportNode) {
            return TapChangerAutomationSystemBuilder.of(network, modelName, reportNode);
        }
    }

    @Override
    protected DynamicModelSeries createDynamicModelSeries(UpdatingDataframe dataframe) {
        return new TapChangerSeries(dataframe);
    }
}
