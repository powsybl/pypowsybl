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
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawaltz.models.automationsystems.overloadmanagments.DynamicOverloadManagementSystemBuilder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.python.commons.PyPowsyblApiHeader.ThreeSideType;
import com.powsybl.python.commons.Util;

import java.util.List;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class OverloadManagementSystemAdder extends AbstractDynamicModelAdder {

    protected static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex(DYNAMIC_MODEL_ID),
            SeriesMetadata.strings(PARAMETER_SET_ID),
            SeriesMetadata.strings(MODEL_NAME),
            SeriesMetadata.strings(CONTROLLED_BRANCH),
            SeriesMetadata.strings(I_MEASUREMENT),
            SeriesMetadata.strings(I_MEASUREMENT_SIDE));

    @Override
    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    private static class OverloadManagementSystemSeries extends AbstractAutomationSystemSeries<DynamicOverloadManagementSystemBuilder> {

        private final StringSeries controlledBranch;
        private final StringSeries iMeasurement;
        private final StringSeries iMeasurementSide;

        OverloadManagementSystemSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.controlledBranch = dataframe.getStrings(CONTROLLED_BRANCH);
            this.iMeasurement = dataframe.getStrings(I_MEASUREMENT);
            this.iMeasurementSide = dataframe.getStrings(I_MEASUREMENT_SIDE);
        }

        @Override
        protected void applyOnBuilder(int row, DynamicOverloadManagementSystemBuilder builder) {
            super.applyOnBuilder(row, builder);
            applyIfPresent(controlledBranch, row, builder::controlledBranch);
            applyIfPresent(iMeasurement, row, builder::iMeasurement);
            applyIfPresent(iMeasurementSide, row, ThreeSideType.class, Util::convertToTwoSides, builder::iMeasurementSide);
        }

        @Override
        protected DynamicOverloadManagementSystemBuilder createBuilder(Network network, ReportNode reportNode) {
            return DynamicOverloadManagementSystemBuilder.of(network, reportNode);
        }

        @Override
        protected DynamicOverloadManagementSystemBuilder createBuilder(Network network, String modelName, ReportNode reportNode) {
            return DynamicOverloadManagementSystemBuilder.of(network, modelName, reportNode);
        }
    }

    @Override
    protected DynamicModelSeries createDynamicModelSeries(UpdatingDataframe dataframe) {
        return new OverloadManagementSystemSeries(dataframe);
    }
}
