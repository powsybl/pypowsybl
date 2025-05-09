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
import com.powsybl.dataframe.network.adders.FeederBaysLineSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.modification.topology.CreateBranchFeederBaysBuilder;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Coline Piloquet {@literal <coline.piloquet at rte-france.com>}
 */
public class CreateLineFeeder implements NetworkModification {

    @Override
    public List<List<SeriesMetadata>> getMetadata(DataframeElementType elementType) {
        return Collections.singletonList(FeederBaysLineSeries.getSeriesMetadata());
    }

    @Override
    public void applyModification(Network network, List<UpdatingDataframe> dataframes, boolean throwException, ReportNode reportNode) {
        if (dataframes.size() != 1) {
            throw new IllegalArgumentException("Expected only one input dataframe");
        }
        for (int i = 0; i < dataframes.get(0).getRowCount(); i++) {
            FeederBaysLineSeries fbLineSeries = new FeederBaysLineSeries();
            Optional<CreateBranchFeederBaysBuilder> builder = fbLineSeries.createBuilder(network, dataframes.get(0), i, throwException);
            if (builder.isPresent()) {
                com.powsybl.iidm.modification.NetworkModification modification = builder.get().build();
                modification.apply(network, throwException, reportNode == null ? ReportNode.NO_OP : reportNode);
            }
        }
    }
}
