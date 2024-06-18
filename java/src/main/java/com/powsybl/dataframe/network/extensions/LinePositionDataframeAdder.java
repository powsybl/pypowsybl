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
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.Coordinate;
import com.powsybl.iidm.network.extensions.LinePositionAdder;

import java.util.*;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class LinePositionDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.intIndex("num"),
            SeriesMetadata.doubles("latitude"),
            SeriesMetadata.doubles("longitude")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static Map<String, List<Coordinate>> readCoordinates(UpdatingDataframe dataframe) {
        StringSeries idCol = dataframe.getStrings("id");
        IntSeries numCol = dataframe.getInts("num");
        DoubleSeries latitudeCol = dataframe.getDoubles("latitude");
        DoubleSeries longitudeCol = dataframe.getDoubles("longitude");
        Map<String, List<Coordinate>> coordinatesByLineId = new HashMap<>();
        if (numCol != null && latitudeCol != null && longitudeCol != null) {
            for (int row = 0; row < dataframe.getRowCount(); row++) {
                String id = idCol.get(row);
                int num = numCol.get(row);
                double latitude = latitudeCol.get(row);
                double longitude = longitudeCol.get(row);
                List<Coordinate> coordinates = coordinatesByLineId.computeIfAbsent(id, k -> new ArrayList<>());
                // ensure list can store coordinate at num index
                if (num > coordinates.size()) {
                    for (int i = 0; i < num - coordinates.size(); i++) {
                        coordinates.add(null);
                    }
                }
                coordinates.set(num, new Coordinate(latitude, longitude));
            }
        }
        return coordinatesByLineId;
    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        Map<String, List<Coordinate>> coordinatesByLineId = readCoordinates(dataframe);
        for (var e : coordinatesByLineId.entrySet()) {
            String id = e.getKey();
            List<Coordinate> coordinates = e.getValue();
            Line l = network.getLine(id);
            if (l == null) {
                throw new PowsyblException("Line '" + id + "' not found");
            }
            // check there is no hole in the coordinate list
            for (int num = 0; num < coordinates.size(); num++) {
                Coordinate coordinate = coordinates.get(num);
                if (coordinate == null) {
                    throw new PowsyblException("Missing coordinate at " + num + " for line '" + id + "'");
                }
            }
            l.newExtension(LinePositionAdder.class)
                    .withCoordinates(coordinates)
                    .add();
        }
    }
}
