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
import com.powsybl.dynawo.builders.ModelInfo;
import com.powsybl.dynawo.models.automationsystems.TapChangerBlockingAutomationSystemBuilder;
import com.powsybl.iidm.network.Network;

import java.util.Collection;
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
            //TODO add meta data U_MEASUREMENTS_ALT handling 2D list for alternative bus name
            SeriesMetadata.strings(TRANSFORMERS));

    @Override
    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    @Override
    public Collection<ModelInfo> getSupportedModels() {
        return TapChangerBlockingAutomationSystemBuilder.getSupportedModelInfos();
    }

    private static class TapChangerBlockingSeries extends AbstractAutomationSystemSeries<TapChangerBlockingAutomationSystemBuilder> {

        // TODO handle list
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
