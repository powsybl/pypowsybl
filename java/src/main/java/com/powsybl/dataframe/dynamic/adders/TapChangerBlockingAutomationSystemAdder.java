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
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawo.builders.ModelInfo;
import com.powsybl.dynawo.models.automationsystems.TapChangerBlockingAutomationSystemBuilder;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.dynamic.PythonDynamicModelsSupplier;

import java.util.*;

import static com.powsybl.dataframe.dynamic.DynamicModelSeriesUtils.applyIfPresent;
import static com.powsybl.dataframe.dynamic.DynamicModelSeriesUtils.createIdMap;
import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class TapChangerBlockingAutomationSystemAdder implements DynamicMappingAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex(DYNAMIC_MODEL_ID),
            SeriesMetadata.strings(PARAMETER_SET_ID),
            SeriesMetadata.strings(MODEL_NAME),
            SeriesMetadata.strings(U_MEASUREMENTS));

    private static final List<SeriesMetadata> TRANSFORMER_METADATA = List.of(
            SeriesMetadata.stringIndex(DYNAMIC_MODEL_ID),
            SeriesMetadata.strings(TRANSFORMER_ID));

    private static final List<List<SeriesMetadata>> METADATA_LIST = List.of(METADATA, TRANSFORMER_METADATA);

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return METADATA_LIST;
    }

    @Override
    public Collection<ModelInfo> getSupportedModels() {
        return TapChangerBlockingAutomationSystemBuilder.getSupportedModelInfos();
    }

    @Override
    public void addElements(PythonDynamicModelsSupplier modelMapping, List<UpdatingDataframe> dataframes) {
        if (dataframes.size() != 2) {
            throw new IllegalArgumentException("Expected 2 dataframes: one for TCB, one for transformers.");
        }
        UpdatingDataframe dataframe = dataframes.get(0);
        UpdatingDataframe tfo_dataframe = dataframes.get(1);
        DynamicModelSeries series = new TapChangerBlockingSeries(dataframe, tfo_dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            modelMapping.addModel(series.getModelSupplier(row));
        }
    }

    private static class TapChangerBlockingSeries extends AbstractAutomationSystemSeries<TapChangerBlockingAutomationSystemBuilder> {

        private final StringSeries uMeasurements;
        private final Map<String, List<String>> transformers;

        TapChangerBlockingSeries(UpdatingDataframe tcbDataframe, UpdatingDataframe tfoDataframe) {
            super(tcbDataframe);
            this.uMeasurements = PersistentStringSeries.copyOf(tcbDataframe, U_MEASUREMENTS);
            this.transformers = createIdMap(tfoDataframe, DYNAMIC_MODEL_ID, TRANSFORMER_ID);
        }

        @Override
        protected void applyOnBuilder(int row, TapChangerBlockingAutomationSystemBuilder builder) {
            super.applyOnBuilder(row, builder);
            SeriesUtils.applyIfPresent(uMeasurements, row, builder::uMeasurements);
            String tcbId = dynamicModelIds.get(row);
            applyIfPresent(transformers, tcbId, builder::transformers);
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
}
