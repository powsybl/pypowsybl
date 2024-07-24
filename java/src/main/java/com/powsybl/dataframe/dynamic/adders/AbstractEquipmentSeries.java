/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynamicsimulation.DynamicModel;
import com.powsybl.dynawaltz.builders.EquipmentModelBuilder;
import com.powsybl.iidm.network.Network;

import java.util.function.BiFunction;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
abstract class AbstractEquipmentSeries<T extends EquipmentModelBuilder<T>> implements DynamicModelSeries {

    protected final StringSeries staticIds;
    protected final StringSeries parameterSetIds;
    protected final StringSeries modelsNames;

    AbstractEquipmentSeries(UpdatingDataframe dataframe) {
        this.staticIds = dataframe.getStrings(STATIC_ID);
        this.parameterSetIds = dataframe.getStrings(PARAMETER_SET_ID);
        this.modelsNames = dataframe.getStrings(MODEL_NAME);
    }

    @Override
    public BiFunction<Network, ReportNode, DynamicModel> getModelSupplier(int row) {
        return (network, reportNode) -> {
            T builder = getBuilder(network, reportNode, row);
            if (builder == null) {
                return null;
            }
            applyIfPresent(staticIds, row, builder::staticId);
            applyIfPresent(parameterSetIds, row, builder::parameterSetId);
            return builder.build();
        };
    }

    protected T getBuilder(Network network, ReportNode reportNode, int row) {
        String modelName = modelsNames != null ? modelsNames.get(row) : null;
        return modelName == null || modelName.isEmpty() ? createBuilder(network, reportNode)
                : createBuilder(network, modelName, reportNode);
    }

    protected abstract T createBuilder(Network network, ReportNode reportNode);

    protected abstract T createBuilder(Network network, String modelName, ReportNode reportNode);
}
