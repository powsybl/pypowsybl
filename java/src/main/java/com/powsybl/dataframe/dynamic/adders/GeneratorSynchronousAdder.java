/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynamicsimulation.DynamicModel;
import com.powsybl.dynawaltz.models.generators.SynchronizedGeneratorBuilder;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.dynamic.PythonDynamicModelsSupplier;

import java.util.List;
import java.util.function.Function;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
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

        private final StringSeries staticIds;
        private final StringSeries parameterSetIds;
        private final StringSeries generatorLibs;

        GeneratorSynchronousSeries(UpdatingDataframe dataframe) {
            this.staticIds = dataframe.getStrings("static_id");
            this.parameterSetIds = dataframe.getStrings("parameter_set_id");
            this.generatorLibs = dataframe.getStrings("generator_lib");
        }

        public Function<Network, DynamicModel> getModelSupplier(int row) {
            return network -> {
                SynchronizedGeneratorBuilder builder = getBuilder(network, row);
                applyIfPresent(staticIds, row, builder::staticId);
                applyIfPresent(parameterSetIds, row, builder::parameterSetId);
                return builder.build();
            };
        }

        private SynchronizedGeneratorBuilder getBuilder(Network network, int row) {
            String lib = generatorLibs != null ? generatorLibs.get(row) : null;
            return lib != null ? SynchronizedGeneratorBuilder.of(network, lib)
                    : SynchronizedGeneratorBuilder.of(network);
        }
    }

    @Override
    public void addElements(PythonDynamicModelsSupplier modelMapping, UpdatingDataframe dataframe) {
        GeneratorSynchronousSeries series = new GeneratorSynchronousSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            modelMapping.addModel(series.getModelSupplier(row));
        }
    }

}
