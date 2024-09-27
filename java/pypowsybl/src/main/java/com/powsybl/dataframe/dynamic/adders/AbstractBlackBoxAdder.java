/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import java.util.List;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.python.dynamic.PythonDynamicModelsSupplier;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 */
public abstract class AbstractBlackBoxAdder implements DynamicMappingAdder {
    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("static_id"),
            SeriesMetadata.strings("parameter_set_id"));

    @Override
    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    private static final class BlackBoxSeries {

        private final StringSeries staticId;
        private final StringSeries parameterSetId;

        BlackBoxSeries(UpdatingDataframe dataframe) {
            this.staticId = SeriesUtils.getRequiredStrings(dataframe, "static_id");
            this.parameterSetId = SeriesUtils.getRequiredStrings(dataframe, "parameter_set_id");
        }

        public StringSeries getStaticId() {
            return staticId;
        }

        public StringSeries getParameterSetId() {
            return parameterSetId;
        }

    }

    @FunctionalInterface
    protected interface AddBlackBoxToModelMapping {
        void addToModel(PythonDynamicModelsSupplier modelMapping, String staticId, String parameterSetId);
    }

    protected abstract AddBlackBoxToModelMapping getAddBlackBoxToModelMapping();

    @Override
    public void addElements(PythonDynamicModelsSupplier modelMapping, UpdatingDataframe dataframe) {
        BlackBoxSeries series = new BlackBoxSeries(dataframe);
        AddBlackBoxToModelMapping adder = getAddBlackBoxToModelMapping();
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            adder.addToModel(modelMapping, series.getStaticId().get(row), series.getParameterSetId().get(row));
        }
    }

}
