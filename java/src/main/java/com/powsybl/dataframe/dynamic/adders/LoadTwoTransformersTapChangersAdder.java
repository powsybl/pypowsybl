/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawo.builders.ModelInfo;
import com.powsybl.dynawo.models.loads.LoadTwoTransformersTapChangersBuilder;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;

import java.util.Collection;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class LoadTwoTransformersTapChangersAdder extends AbstractEquipmentAdder {

    @Override
    public Collection<ModelInfo> getSupportedModels() {
        return LoadTwoTransformersTapChangersBuilder.getSupportedModelInfos();
    }

    @Override
    protected DynamicModelSeries createDynamicModelSeries(UpdatingDataframe dataframe) {
        return new AbstractEquipmentSeries<Load, LoadTwoTransformersTapChangersBuilder>(dataframe) {

            @Override
            protected LoadTwoTransformersTapChangersBuilder createBuilder(Network network, ReportNode reportNode) {
                return LoadTwoTransformersTapChangersBuilder.of(network, reportNode);
            }

            @Override
            protected LoadTwoTransformersTapChangersBuilder createBuilder(Network network, String modelName, ReportNode reportNode) {
                return LoadTwoTransformersTapChangersBuilder.of(network, modelName, reportNode);
            }
        };
    }
}