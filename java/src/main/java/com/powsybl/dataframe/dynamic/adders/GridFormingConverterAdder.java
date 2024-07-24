/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawaltz.models.generators.GridFormingConverterBuilder;
import com.powsybl.iidm.network.Network;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class GridFormingConverterAdder extends AbstractEquipmentAdder {

    @Override
    protected DynamicModelSeries createDynamicModelSeries(UpdatingDataframe dataframe) {
        return new AbstractEquipmentSeries<GridFormingConverterBuilder>(dataframe) {

            @Override
            protected GridFormingConverterBuilder createBuilder(Network network, ReportNode reportNode) {
                return GridFormingConverterBuilder.of(network, reportNode);
            }

            @Override
            protected GridFormingConverterBuilder createBuilder(Network network, String modelName, ReportNode reportNode) {
                return GridFormingConverterBuilder.of(network, modelName, reportNode);
            }
        };
    }
}
