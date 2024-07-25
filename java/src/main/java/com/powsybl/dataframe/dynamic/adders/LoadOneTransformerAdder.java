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
import com.powsybl.dynawaltz.models.loads.LoadOneTransformerBuilder;
import com.powsybl.iidm.network.Network;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class LoadOneTransformerAdder extends AbstractEquipmentAdder {

    @Override
    protected DynamicModelSeries createDynamicModelSeries(UpdatingDataframe dataframe) {
        return new AbstractEquipmentSeries<LoadOneTransformerBuilder>(dataframe) {

            @Override
            protected LoadOneTransformerBuilder createBuilder(Network network, ReportNode reportNode) {
                return LoadOneTransformerBuilder.of(network, reportNode);
            }

            @Override
            protected LoadOneTransformerBuilder createBuilder(Network network, String modelName, ReportNode reportNode) {
                return LoadOneTransformerBuilder.of(network, modelName, reportNode);
            }
        };
    }
}
