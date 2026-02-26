/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.dynamic.CategoryAttributeUtils;
import com.powsybl.dataframe.dynamic.CategoryInformation;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawo.builders.ModelInfo;
import com.powsybl.dynawo.models.shunts.BaseShuntBuilder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;

import java.util.Collection;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class ShuntAdder extends AbstractEquipmentAdder {

    private static final CategoryInformation CATEGORY_INFORMATION = new CategoryInformation(
            "Shunt",
            "Shunt compensator",
            CategoryAttributeUtils.createFromMetadata(EQUIPMENT_METADATA));

    protected ShuntAdder() {
        super(CATEGORY_INFORMATION);
    }

    @Override
    public Collection<ModelInfo> getSupportedModels() {
        return BaseShuntBuilder.getSupportedModelInfos();
    }

    @Override
    protected DynamicModelSeries createDynamicModelSeries(UpdatingDataframe dataframe) {
        return new AbstractEquipmentSeries<ShuntCompensator, BaseShuntBuilder>(dataframe) {

            @Override
            protected BaseShuntBuilder createBuilder(Network network, ReportNode reportNode) {
                return BaseShuntBuilder.of(network, reportNode);
            }

            @Override
            protected BaseShuntBuilder createBuilder(Network network, String modelName, ReportNode reportNode) {
                return BaseShuntBuilder.of(network, modelName, reportNode);
            }
        };
    }
}
