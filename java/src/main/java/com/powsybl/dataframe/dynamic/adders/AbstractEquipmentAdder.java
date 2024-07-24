/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.python.dynamic.PythonDynamicModelsSupplier;

import java.util.List;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
abstract class AbstractEquipmentAdder implements DynamicMappingAdder {

    protected static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex(STATIC_ID),
            SeriesMetadata.strings(PARAMETER_SET_ID),
            SeriesMetadata.strings(MODEL_NAME));

    @Override
    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    @Override
    public void addElements(PythonDynamicModelsSupplier modelMapping, UpdatingDataframe dataframe) {
        DynamicModelSeries series = createDynamicModelSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            modelMapping.addModel(series.getModelSupplier(row));
        }
    }

    abstract protected DynamicModelSeries createDynamicModelSeries(UpdatingDataframe dataframe);
}
