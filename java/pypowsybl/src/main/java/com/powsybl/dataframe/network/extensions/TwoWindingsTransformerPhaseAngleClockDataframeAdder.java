/**
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
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.extensions.TwoWindingsTransformerPhaseAngleClockAdder;

/**
 * @author Nico Westerbeck {@literal <nico.westerbeck@50hertz.com>}
 */
public class TwoWindingsTransformerPhaseAngleClockDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.ints("phase_angle_clock")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static final class PhaseAngleClockSeries {

        private final StringSeries id;
        private final IntSeries phaseAngleClock;

        private PhaseAngleClockSeries(UpdatingDataframe dataframe) {
            this.id = SeriesUtils.getRequiredStrings(dataframe, "id");
            this.phaseAngleClock = SeriesUtils.getRequiredInts(dataframe, "phase_angle_clock");
        }

        private void create(Network network, int row) {
            String transformerId = id.get(row);
            TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(transformerId);
            if (transformer == null) {
                throw new PowsyblException("Two windings transformer '" + transformerId + "' does not exist.");
            }
            transformer.newExtension(TwoWindingsTransformerPhaseAngleClockAdder.class)
                    .withPhaseAngleClock(phaseAngleClock.get(row))
                    .add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        PhaseAngleClockSeries series = new PhaseAngleClockSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
