/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.extensions;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.AbstractSimpleAdder;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawo.extensions.api.generator.connection.GeneratorConnectionLevel;
import com.powsybl.dynawo.extensions.api.generator.connection.GeneratorConnectionLevelAdder;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class GeneratorConnectionLevelDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("level")
            );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class GeneratorConnectionLevelSeries {

        private final StringSeries id;
        private final StringSeries level;

        GeneratorConnectionLevelSeries(UpdatingDataframe dataframe) {
            this.id = dataframe.getStrings("id");
            this.level = dataframe.getStrings("level");
        }

        void create(Network network, int row) {
            String generatorId = this.id.get(row);
            Generator g = network.getGenerator(generatorId);
            if (g == null) {
                throw new PowsyblException("Invalid generator id : could not find " + generatorId);
            }
            var adder = g.newExtension(GeneratorConnectionLevelAdder.class);
            SeriesUtils.applyIfPresent(level, row, l -> adder.withLevel(GeneratorConnectionLevel.GeneratorConnectionLevelType.valueOf(l)));
            adder.add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        GeneratorConnectionLevelSeries series = new GeneratorConnectionLevelSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
