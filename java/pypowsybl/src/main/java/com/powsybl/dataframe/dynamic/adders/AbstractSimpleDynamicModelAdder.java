/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.dynamic.CategoryAttributeUtils;
import com.powsybl.dataframe.dynamic.CategoryInformation;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.python.dynamic.PythonDynamicModelsSupplier;

import java.util.List;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
abstract class AbstractSimpleDynamicModelAdder implements DynamicMappingAdder {

    private final List<List<SeriesMetadata>> metadata;
    private final CategoryInformation categoryInformation;

    protected AbstractSimpleDynamicModelAdder(List<List<SeriesMetadata>> metadata, String name, String description) {
        this.metadata = metadata;
        this.categoryInformation = new CategoryInformation(name, description, CategoryAttributeUtils.createFromMetadata(metadata.getFirst()));
    }

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return metadata;
    }

    @Override
    public CategoryInformation getCategoryInformation() {
        return categoryInformation;
    }

    @Override
    public String getCategory() {
        return categoryInformation.name();
    }

    @Override
    public void addElements(PythonDynamicModelsSupplier modelMapping, List<UpdatingDataframe> dataframes) {
        if (dataframes.size() != 1) {
            throw new IllegalArgumentException("Expected only one input dataframe");
        }
        UpdatingDataframe dataframe = dataframes.get(0);
        DynamicModelSeries series = createDynamicModelSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            modelMapping.addModel(series.getModelSupplier(row));
        }
    }

    protected abstract DynamicModelSeries createDynamicModelSeries(UpdatingDataframe dataframe);
}
