/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.extensions;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.AbstractSimpleAdder;
import com.powsybl.dataframe.network.adders.NetworkUtils;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.ThreeSides;
import com.powsybl.iidm.network.extensions.ReferenceTerminalsAdder;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public class ReferenceTerminalsDataframeAdder extends AbstractSimpleAdder {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("element_id"),
            SeriesMetadata.strings("side")
            );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    /**
     * The whole dataframe describes one extension, so every row is collected before the extension is created.
     */
    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        StringSeries elementIds = SeriesUtils.getRequiredStrings(dataframe, "element_id");
        StringSeries sides = dataframe.getStrings("side");

        Set<Terminal> terminals = new LinkedHashSet<>();
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            String elementId = elementIds.get(row);
            // an injection has a single terminal and its side is read back empty, only branches and three
            // windings transformers need one
            String side = sides == null ? null : sides.get(row);
            terminals.add(NetworkUtils.getTerminalOrThrow(network, elementId,
                    side == null || side.isEmpty() ? ThreeSides.ONE.toString() : side));
        }

        network.newExtension(ReferenceTerminalsAdder.class)
                .withTerminals(terminals)
                .add();
    }
}
