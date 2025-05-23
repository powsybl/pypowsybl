/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.dynamic.PersistentStringSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynamicsimulation.DynamicModel;
import com.powsybl.dynawo.builders.ModelBuilder;
import com.powsybl.iidm.network.Network;

import java.util.function.BiFunction;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
abstract class AbstractDynamicModelSeries<T extends ModelBuilder<DynamicModel>> implements DynamicModelSeries {

    protected final StringSeries parameterSetIds;
    protected final StringSeries modelsNames;

    AbstractDynamicModelSeries(UpdatingDataframe dataframe) {
        this.parameterSetIds = PersistentStringSeries.copyOf(dataframe, PARAMETER_SET_ID);
        this.modelsNames = PersistentStringSeries.copyOf(dataframe, MODEL_NAME);
    }

    @Override
    public BiFunction<Network, ReportNode, DynamicModel> getModelSupplier(int row) {
        return (network, reportNode) -> {
            T builder = getBuilder(network, reportNode, row);
            if (builder == null) {
                return null;
            }
            applyOnBuilder(row, builder);
            return builder.build();
        };
    }

    protected abstract void applyOnBuilder(int row, T builder);

    protected T getBuilder(Network network, ReportNode reportNode, int row) {
        String modelName = modelsNames != null ? modelsNames.get(row) : null;
        return modelName == null || modelName.isEmpty() ? createBuilder(network, reportNode)
                : createBuilder(network, modelName, reportNode);
    }

    protected abstract T createBuilder(Network network, ReportNode reportNode);

    protected abstract T createBuilder(Network network, String modelName, ReportNode reportNode);
}
