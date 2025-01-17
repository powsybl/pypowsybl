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
import com.powsybl.dataframe.dynamic.PersistentStringSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawo.builders.ModelInfo;
import com.powsybl.dynawo.models.automationsystems.overloadmanagments.DynamicOverloadManagementSystemBuilder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class DynamicOverloadManagementSystemAdder extends AbstractSimpleDynamicModelAdder {

    protected static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex(DYNAMIC_MODEL_ID),
            SeriesMetadata.strings(PARAMETER_SET_ID),
            SeriesMetadata.strings(MODEL_NAME),
            SeriesMetadata.strings(CONTROLLED_BRANCH),
            SeriesMetadata.strings(I_MEASUREMENT),
            SeriesMetadata.strings(I_MEASUREMENT_SIDE));

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public Collection<ModelInfo> getSupportedModels() {
        return DynamicOverloadManagementSystemBuilder.getSupportedModelInfos();
    }

    private static class OverloadManagementSystemSeries extends AbstractAutomationSystemSeries<DynamicOverloadManagementSystemBuilder> {

        private final StringSeries controlledBranch;
        private final StringSeries iMeasurement;
        private final StringSeries iMeasurementSide;

        OverloadManagementSystemSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.controlledBranch = PersistentStringSeries.copyOf(dataframe, CONTROLLED_BRANCH);
            this.iMeasurement = PersistentStringSeries.copyOf(dataframe, I_MEASUREMENT);
            this.iMeasurementSide = PersistentStringSeries.copyOf(dataframe, I_MEASUREMENT_SIDE);
        }

        @Override
        protected void applyOnBuilder(int row, DynamicOverloadManagementSystemBuilder builder) {
            super.applyOnBuilder(row, builder);
            applyIfPresent(controlledBranch, row, builder::controlledBranch);
            applyIfPresent(iMeasurement, row, builder::iMeasurement);
            applyIfPresent(iMeasurementSide, row, TwoSides.class, builder::iMeasurementSide);
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
