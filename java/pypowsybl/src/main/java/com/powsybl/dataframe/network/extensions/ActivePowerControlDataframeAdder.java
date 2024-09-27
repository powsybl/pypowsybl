/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.extensions;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.AbstractSimpleAdder;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.ActivePowerControlAdder;

import java.util.Collections;
import java.util.List;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
public class ActivePowerControlDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.doubles("droop"),
            SeriesMetadata.booleans("participate"),
            SeriesMetadata.doubles("participation_factor"),
            SeriesMetadata.doubles("max_target_p"),
            SeriesMetadata.doubles("min_target_p")
            );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class ActivePowerControlSeries {

        private final StringSeries id;
        private final DoubleSeries droop;
        private final IntSeries participate;
        private final DoubleSeries participationFactor;
        private final DoubleSeries maxTargetP;
        private final DoubleSeries minTargetP;

        ActivePowerControlSeries(UpdatingDataframe dataframe) {
            this.id = dataframe.getStrings("id");
            this.droop = dataframe.getDoubles("droop");
            this.participate = dataframe.getInts("participate");
            this.participationFactor = dataframe.getDoubles("participation_factor");
            this.maxTargetP = dataframe.getDoubles("max_target_p");
            this.minTargetP = dataframe.getDoubles("min_target_p");
        }

        void create(Network network, int row) {
            String generatorId = this.id.get(row);
            Generator g = network.getGenerator(generatorId);
            if (g == null) {
                throw new PowsyblException("Invalid generator id : could not find " + generatorId);
            }
            var adder = g.newExtension(ActivePowerControlAdder.class);
            SeriesUtils.applyIfPresent(droop, row, adder::withDroop);
            SeriesUtils.applyBooleanIfPresent(participate, row, adder::withParticipate);
            SeriesUtils.applyIfPresent(participationFactor, row, adder::withParticipationFactor);
            SeriesUtils.applyIfPresent(maxTargetP, row, adder::withMaxTargetP);
            SeriesUtils.applyIfPresent(minTargetP, row, adder::withMinTargetP);
            adder.add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        ActivePowerControlSeries series = new ActivePowerControlSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
