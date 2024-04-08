/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.modifications;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.NetworkElementAdders;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.Network;
import com.powsybl.python.commons.PyPowsyblApiHeader;

import java.util.ArrayList;
import java.util.List;

import static com.powsybl.python.commons.Util.convert;

/**
 * @author Coline Piloquet <coline.piloquet at rte-france.com>
 */
public class CreateFeederBay implements NetworkModification {

    private static final List<SeriesMetadata> ADDITIONNAL_METADATA = List.of(
            SeriesMetadata.strings("bus_or_busbar_section_id"),
            SeriesMetadata.ints("position_order"),
            SeriesMetadata.strings("direction"),
            SeriesMetadata.strings("feeder_type")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata(DataframeElementType elementType) {
        List<List<SeriesMetadata>> feederMetadata = NetworkElementAdders.getAdder(elementType).getMetadata();
        List<SeriesMetadata> firstMetadata = new ArrayList<>(feederMetadata.get(0));
        firstMetadata.addAll(ADDITIONNAL_METADATA);
        List<List<SeriesMetadata>> metadata = new ArrayList<>();
        metadata.add(firstMetadata);
        for (int i = 1; i < feederMetadata.size(); i++) {
            metadata.add(feederMetadata.get(i));
        }
        return metadata;
    }

    @Override
    public void applyModification(Network network, List<UpdatingDataframe> dataframes, boolean throwException, ReportNode reportNode) {
        PyPowsyblApiHeader.ElementType elementType = PyPowsyblApiHeader.ElementType.valueOf(dataframes.get(0).getStrings("feeder_type").get(0));
        DataframeElementType type = convert(elementType);
        NetworkElementAdders.addElementsWithBay(type, network, dataframes, throwException, reportNode);
    }
}
