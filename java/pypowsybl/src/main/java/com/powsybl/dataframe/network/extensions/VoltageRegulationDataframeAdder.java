/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
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
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.VoltageRegulationAdder;

import java.util.Collections;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class VoltageRegulationDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.booleans("voltage_regulator_on"),
            SeriesMetadata.doubles("target_v"),
            SeriesMetadata.strings("regulated_element_id")

    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class VoltageRegulationSerie {
        private final StringSeries id;
        private final IntSeries voltageRegulatorOn;
        private final DoubleSeries targetV;
        private final StringSeries regulatedElement;

        VoltageRegulationSerie(UpdatingDataframe dataframe) {
            this.id = dataframe.getStrings("id");
            this.voltageRegulatorOn = dataframe.getInts("voltage_regulator_on");
            this.targetV = dataframe.getDoubles("target_v");
            this.regulatedElement = dataframe.getStrings("regulated_element_id");
        }

        void create(Network network, int row) {
            String batteryId = this.id.get(row);
            Battery battery = network.getBattery(batteryId);
            if (battery == null) {
                throw new PowsyblException("Battery '" + batteryId + "' not found");
            }
            VoltageRegulationAdder adder = battery.newExtension(VoltageRegulationAdder.class);
            SeriesUtils.applyIfPresent(voltageRegulatorOn, row, value -> adder.withVoltageRegulatorOn(value != 0));
            SeriesUtils.applyIfPresent(targetV, row, adder::withTargetV);
            SeriesUtils.applyIfPresent(regulatedElement, row,
                    value -> adder.withRegulatingTerminal(getTerminal(network, value, batteryId)));
            adder.add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        VoltageRegulationSerie series = new VoltageRegulationSerie(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }

    protected static Terminal getTerminal(Network network, String regulatedElement, String id) {
        String targetElement = regulatedElement == null || regulatedElement.isEmpty() ? id : regulatedElement;
        Identifiable<?> identifiable = network.getIdentifiable(targetElement);
        if (identifiable instanceof Injection) {
            return ((Injection<?>) identifiable).getTerminal();
        } else {
            throw new UnsupportedOperationException("Cannot set regulated element to " + regulatedElement +
                    ": the regulated element may only be a busbar section or an injection.");
        }

    }
}
