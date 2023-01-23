/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.modifications;

import com.powsybl.commons.reporter.ReporterModel;
import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.FeederBaysLineSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.modification.topology.CreateBranchFeederBaysBuilder;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;

/**
 * @author Coline Piloquet <coline.piloquet at rte-france.com>
 */
public class CreateLineFeeder implements NetworkModification {

    @Override
    public List<List<SeriesMetadata>> getMetadata(DataframeElementType elementType) {
        return Collections.singletonList(FeederBaysLineSeries.getSeriesMetadata());
    }

    @Override
    public void applyModification(Network network, List<UpdatingDataframe> dataframe, boolean throwException, ReporterModel reporter) {
        FeederBaysLineSeries fbLineSeries = new FeederBaysLineSeries();
        CreateBranchFeederBaysBuilder builder = fbLineSeries.createBuilder(network, dataframe.get(0));
        com.powsybl.iidm.modification.NetworkModification modification = builder.build();
        modification.apply(network);
    }
}
