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
import com.powsybl.dynawaltz.models.automationsystems.TapChangerBlockingAutomationSystemBuilder;
import com.powsybl.iidm.network.Network;

import java.util.List;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class TapChangerBlockingAutomationSystemAdder extends AbstractDynamicModelAdder {

    protected static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex(DYNAMIC_MODEL_ID),
            SeriesMetadata.strings(PARAMETER_SET_ID),
            SeriesMetadata.strings(MODEL_NAME),
            SeriesMetadata.strings(U_MEASUREMENTS),
            SeriesMetadata.strings(TRANSFORMERS));

    @Override
    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    private static class TapChangerBlockingSeries extends AbstractAutomationSystemSeries<TapChangerBlockingAutomationSystemBuilder> {

        private final StringSeries uMeasurements;
        private final StringSeries transformers;

        TapChangerBlockingSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.uMeasurements = dataframe.getStrings(U_MEASUREMENTS);
            this.transformers = dataframe.getStrings(TRANSFORMERS);
        }

        @Override
        protected void applyOnBuilder(int row, TapChangerBlockingAutomationSystemBuilder builder) {
            super.applyOnBuilder(row, builder);
            applyIfPresent(uMeasurements, row, builder::uMeasurements);
            applyIfPresent(transformers, row, builder::transformers);
        }

        @Override
        protected TapChangerBlockingAutomationSystemBuilder createBuilder(Network network, ReportNode reportNode) {
            return TapChangerBlockingAutomationSystemBuilder.of(network, reportNode);
        }

        @Override
        protected TapChangerBlockingAutomationSystemBuilder createBuilder(Network network, String modelName, ReportNode reportNode) {
            return TapChangerBlockingAutomationSystemBuilder.of(network, modelName, reportNode);
        }
    }

    @Override
    protected DynamicModelSeries createDynamicModelSeries(UpdatingDataframe dataframe) {
        return new TapChangerBlockingSeries(dataframe);
    }
}
