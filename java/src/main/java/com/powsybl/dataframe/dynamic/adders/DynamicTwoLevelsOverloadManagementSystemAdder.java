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
import com.powsybl.dynawaltz.models.automationsystems.overloadmanagments.DynamicTwoLevelsOverloadManagementSystemBuilder;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.Util;

import java.util.List;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class DynamicTwoLevelsOverloadManagementSystemAdder extends AbstractDynamicModelAdder {

    protected static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex(DYNAMIC_MODEL_ID),
            SeriesMetadata.strings(PARAMETER_SET_ID),
            SeriesMetadata.strings(MODEL_NAME),
            SeriesMetadata.strings(CONTROLLED_BRANCH),
            SeriesMetadata.strings(I_MEASUREMENT_1),
            SeriesMetadata.ints(I_MEASUREMENT_1_SIDE),
            SeriesMetadata.strings(I_MEASUREMENT_2),
            SeriesMetadata.ints(I_MEASUREMENT_2_SIDE));

    @Override
    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    private static class OverloadManagementSystemSeries extends AbstractAutomationSystemSeries<DynamicTwoLevelsOverloadManagementSystemBuilder> {

        private final StringSeries controlledBranch;
        private final StringSeries iMeasurement1;
        private final IntSeries iMeasurement1Side;
        private final StringSeries iMeasurement2;
        private final IntSeries iMeasurement2Side;

        OverloadManagementSystemSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.controlledBranch = dataframe.getStrings(CONTROLLED_BRANCH);
            this.iMeasurement1 = dataframe.getStrings(I_MEASUREMENT_1);
            this.iMeasurement1Side = dataframe.getInts(I_MEASUREMENT_1_SIDE);
            this.iMeasurement2 = dataframe.getStrings(I_MEASUREMENT_2);
            this.iMeasurement2Side = dataframe.getInts(I_MEASUREMENT_2_SIDE);
        }

        @Override
        protected void applyOnBuilder(int row, DynamicTwoLevelsOverloadManagementSystemBuilder builder) {
            super.applyOnBuilder(row, builder);
            applyIfPresent(controlledBranch, row, builder::controlledBranch);
            applyIfPresent(iMeasurement1, row, builder::iMeasurement1);
            applyIfPresent(iMeasurement1Side, row, Util::convertToTwoSides, builder::iMeasurement1Side);
            applyIfPresent(iMeasurement2, row, builder::iMeasurement2);
            applyIfPresent(iMeasurement2Side, row, Util::convertToTwoSides, builder::iMeasurement2Side);
        }

        @Override
        protected DynamicTwoLevelsOverloadManagementSystemBuilder createBuilder(Network network, ReportNode reportNode) {
            return DynamicTwoLevelsOverloadManagementSystemBuilder.of(network, reportNode);
        }

        @Override
        protected DynamicTwoLevelsOverloadManagementSystemBuilder createBuilder(Network network, String modelName, ReportNode reportNode) {
            return DynamicTwoLevelsOverloadManagementSystemBuilder.of(network, modelName, reportNode);
        }
    }

    @Override
    protected DynamicModelSeries createDynamicModelSeries(UpdatingDataframe dataframe) {
        return new OverloadManagementSystemSeries(dataframe);
    }
}
