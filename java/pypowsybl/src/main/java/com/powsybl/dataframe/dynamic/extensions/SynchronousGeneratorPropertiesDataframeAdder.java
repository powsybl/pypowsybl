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
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawo.extensions.api.generator.RpclType;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties;
import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorPropertiesAdder;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class SynchronousGeneratorPropertiesDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("numberOfWindings"),
            SeriesMetadata.strings("governor"),
            SeriesMetadata.strings("voltageRegulator"),
            SeriesMetadata.strings("pss"),
            SeriesMetadata.booleans("auxiliaries"),
            SeriesMetadata.booleans("internalTransformer"),
            SeriesMetadata.strings("rpcl"),
            SeriesMetadata.strings("uva"),
            SeriesMetadata.booleans("aggregated"),
            SeriesMetadata.booleans("qlim")
            );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class SynchronousGeneratorPropertiesSeries {

        private final StringSeries id;
        private final StringSeries numberOfWindings;
        private final StringSeries governor;
        private final StringSeries voltageRegulator;
        private final StringSeries pss;
        private final IntSeries auxiliaries;
        private final IntSeries internalTransformer;
        private final StringSeries rpcl;
        private final StringSeries uva;
        private final IntSeries aggregated;
        private final IntSeries qlim;

        SynchronousGeneratorPropertiesSeries(UpdatingDataframe dataframe) {
            this.id = dataframe.getStrings("id");
            this.numberOfWindings = dataframe.getStrings("numberOfWindings");
            this.governor = dataframe.getStrings("governor");
            this.voltageRegulator = dataframe.getStrings("voltageRegulator");
            this.pss = dataframe.getStrings("pss");
            this.auxiliaries = dataframe.getInts("auxiliaries");
            this.internalTransformer = dataframe.getInts("internalTransformer");
            this.rpcl = dataframe.getStrings("rpcl");
            this.uva = dataframe.getStrings("uva");
            this.aggregated = dataframe.getInts("aggregated");
            this.qlim = dataframe.getInts("qlim");
        }

        void create(Network network, int row) {
            String generatorId = this.id.get(row);
            Generator g = network.getGenerator(generatorId);
            if (g == null) {
                throw new PowsyblException("Invalid generator id : could not find " + generatorId);
            }
            var adder = g.newExtension(SynchronousGeneratorPropertiesAdder.class);
            SeriesUtils.applyIfPresent(numberOfWindings, row, w -> adder.withNumberOfWindings(SynchronousGeneratorProperties.Windings.valueOf(w)));
            SeriesUtils.applyIfPresent(governor, row, adder::withGovernor);
            SeriesUtils.applyIfPresent(voltageRegulator, row, adder::withVoltageRegulator);
            SeriesUtils.applyIfPresent(pss, row, adder::withPss);
            SeriesUtils.applyBooleanIfPresent(auxiliaries, row, adder::withAuxiliaries);
            SeriesUtils.applyBooleanIfPresent(internalTransformer, row, adder::withInternalTransformer);
            SeriesUtils.applyIfPresent(rpcl, row, r -> adder.withRpcl(RpclType.valueOf(r)));
            SeriesUtils.applyIfPresent(uva, row, u -> adder.withUva(SynchronousGeneratorProperties.Uva.valueOf(u)));
            SeriesUtils.applyBooleanIfPresent(aggregated, row, adder::withAggregated);
            SeriesUtils.applyBooleanIfPresent(qlim, row, adder::withQlim);
            adder.add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        SynchronousGeneratorPropertiesSeries series = new SynchronousGeneratorPropertiesSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
