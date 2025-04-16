/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.BaseDataframeMapperBuilder;
import com.powsybl.dataframe.network.ExtensionInformation;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframeMapperBuilder;
import com.powsybl.dataframe.network.adders.NetworkElementAdder;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.Coordinate;
import com.powsybl.iidm.network.extensions.LinePosition;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class LinePositionDataframeProvider extends AbstractSingleDataframeNetworkExtension {

    @Override
    public String getExtensionName() {
        return LinePosition.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(LinePosition.NAME,
                "Provides information about the line geographical coordinates",
                "index : id (str), num (int), latitude (float), longitude (float)");
    }

    record LineCoordinate(LinePosition linePosition, Integer num) {
    }

    private Stream<LineCoordinate> itemsStream(Network network) {
        return network.getLineStream()
                .map(g -> (LinePosition) g.getExtension(LinePosition.class))
                .filter(Objects::nonNull)
                .flatMap(lp -> IntStream.range(0, lp.getCoordinates().size()).mapToObj(num -> new LineCoordinate(lp, num)));
    }

    private static class LineCoordinateGetter implements BaseDataframeMapperBuilder.ItemGetter<Network, LineCoordinate> {

        @Override
        public LineCoordinate getItem(Network network, UpdatingDataframe updatingDataframe, int row) {
            String id = updatingDataframe.getStringValue("id", row).orElseThrow();
            Line l = network.getLine(id);
            if (l == null) {
                throw new PowsyblException("Line '" + id + "' not found");
            }
            LinePosition lp = l.getExtension(LinePosition.class);
            if (lp == null) {
                throw new PowsyblException("Line '" + id + "' has no LinePosition extension");
            }
            int num = updatingDataframe.getIntValue("id", row).orElseThrow();
            return new LineCoordinate(lp, num);
        }
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, new LineCoordinateGetter())
                .stringsIndex("id", lc -> ((Identifiable<?>) lc.linePosition().getExtendable()).getId())
                .intsIndex("num", lc -> lc.num)
                .doubles("latitude", lc -> ((Coordinate) lc.linePosition.getCoordinates().get(lc.num)).getLatitude())
                .doubles("longitude", lc -> ((Coordinate) lc.linePosition.getCoordinates().get(lc.num)).getLongitude())
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(network::getIdentifiable)
                .filter(Objects::nonNull)
                .forEach(g -> g.removeExtension(LinePosition.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new LinePositionDataframeAdder();
    }
}
