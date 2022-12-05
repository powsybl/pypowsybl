/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.SeriesMetadata;

import java.util.List;

/**
 * @author Coline PILOQUET <coline.piloquet at rte-france.com>
 */
public final class VoltageLevelTopologyCreationSeries {

    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.ints("low_busbar_index"),
            SeriesMetadata.ints("busbar_count"),
            SeriesMetadata.ints("low_section_index"),
            SeriesMetadata.ints("section_count"),
            SeriesMetadata.strings("busbar_section_prefix_id"),
            SeriesMetadata.strings("switch_prefix_id")
    );

    private VoltageLevelTopologyCreationSeries() {
    }

    public static List<SeriesMetadata> getSeriesMetadata() {
        return METADATA;
    }

}
