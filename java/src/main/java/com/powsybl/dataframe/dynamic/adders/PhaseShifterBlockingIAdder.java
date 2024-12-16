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
import com.powsybl.dynawo.models.automationsystems.phaseshifters.PhaseShifterBlockingIAutomationSystemBuilder;
import com.powsybl.iidm.network.Network;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class PhaseShifterBlockingIAdder extends AbstractSimpleDynamicModelAdder {

    protected static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex(DYNAMIC_MODEL_ID),
            SeriesMetadata.strings(PARAMETER_SET_ID),
            SeriesMetadata.strings(MODEL_NAME),
            SeriesMetadata.strings(PHASE_SHIFTER_ID));

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    @Override
    public Collection<ModelInfo> getSupportedModels() {
        return PhaseShifterBlockingIAutomationSystemBuilder.getSupportedModelInfos();
    }

    private static class PhaseShifterBlockingISeries extends AbstractAutomationSystemSeries<PhaseShifterBlockingIAutomationSystemBuilder> {

        private final StringSeries phaseShifterId;

        PhaseShifterBlockingISeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.phaseShifterId = PersistentStringSeries.copyOf(dataframe, PHASE_SHIFTER_ID);
        }

        @Override
        protected void applyOnBuilder(int row, PhaseShifterBlockingIAutomationSystemBuilder builder) {
            super.applyOnBuilder(row, builder);
            applyIfPresent(phaseShifterId, row, builder::phaseShifterId);
        }

        @Override
        protected PhaseShifterBlockingIAutomationSystemBuilder createBuilder(Network network, ReportNode reportNode) {
            return PhaseShifterBlockingIAutomationSystemBuilder.of(network, reportNode);
        }

        @Override
        protected PhaseShifterBlockingIAutomationSystemBuilder createBuilder(Network network, String modelName, ReportNode reportNode) {
            return PhaseShifterBlockingIAutomationSystemBuilder.of(network, modelName, reportNode);
        }
    }

    @Override
    protected DynamicModelSeries createDynamicModelSeries(UpdatingDataframe dataframe) {
        return new PhaseShifterBlockingISeries(dataframe);
    }
}
