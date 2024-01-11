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
public class GeneratorSynchronousAdder implements DynamicMappingAdder {
    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("static_id"),
            SeriesMetadata.strings("parameter_set_id"),
            SeriesMetadata.strings("generator_lib"));

    @Override
    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    private static final class GeneratorSynchronousSeries {

        private final StringSeries staticId;
        private final StringSeries parameterSetId;
        private final StringSeries generatorLib;

        GeneratorSynchronousSeries(UpdatingDataframe dataframe) {
            this.staticId = SeriesUtils.getRequiredStrings(dataframe, "static_id");
            this.parameterSetId = SeriesUtils.getRequiredStrings(dataframe, "parameter_set_id");
            this.generatorLib = SeriesUtils.getRequiredStrings(dataframe, "generator_lib");
        }

        public StringSeries getStaticId() {
            return staticId;
        }

        public StringSeries getParameterSetId() {
            return parameterSetId;
        }

        public StringSeries getGeneratorLib() {
            return generatorLib;
        }

    }

    @Override
    public void addElements(PythonDynamicModelsSupplier modelMapping, UpdatingDataframe dataframe) {
        GeneratorSynchronousSeries series = new GeneratorSynchronousSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            modelMapping.addGeneratorSynchronous(series.getStaticId().get(row),
                    series.getParameterSetId().get(row),
                    series.getGeneratorLib().get(row));
        }
    }

}
