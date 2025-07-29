/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.extensions;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.AbstractSimpleAdder;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.extensions.Coordinate;
import com.powsybl.iidm.network.extensions.SubstationPositionAdder;

import java.util.Collections;
import java.util.List;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class SubstationPositionDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.doubles("latitude"),
            SeriesMetadata.doubles("longitude")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class SubstationPositionSeries {

        private final StringSeries id;
        private final DoubleSeries latitude;
        private final DoubleSeries longitude;

        SubstationPositionSeries(UpdatingDataframe dataframe) {
            this.id = dataframe.getStrings("id");
            this.latitude = dataframe.getDoubles("latitude");
            this.longitude = dataframe.getDoubles("longitude");
        }

        void create(Network network, int row) {
            String id = this.id.get(row);
            Substation s = network.getSubstation(id);
            if (s == null) {
                throw new PowsyblException("Substation '" + id + "' not found");
            }
            var adder = s.newExtension(SubstationPositionAdder.class);
            if (latitude != null && longitude != null) {
                adder.withCoordinate(new Coordinate(latitude.get(row), longitude.get(row)));
            }
            adder.add();
        }
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        SubstationPositionSeries series = new SubstationPositionSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
