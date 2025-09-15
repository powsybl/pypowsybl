/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dataframe.dynamic.CategoryInformation;
import com.powsybl.dataframe.dynamic.CategoryAttributeUtils;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawo.builders.ModelInfo;
import com.powsybl.dynawo.models.generators.BaseGeneratorBuilder;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;

import java.util.Collection;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class BaseGeneratorAdder extends AbstractEquipmentAdder {

    private static final CategoryInformation CATEGORY_INFORMATION = new CategoryInformation(
            BaseGeneratorBuilder.CATEGORY,
            CategoryAttributeUtils.createFromMetadata(EQUIPMENT_METADATA));

    @Override
    public String getCategory() {
        return BaseGeneratorBuilder.CATEGORY;
    }

    @Override
    public CategoryInformation getCategoryInformation() {
        return CATEGORY_INFORMATION;
    }

    @Override
    public Collection<ModelInfo> getSupportedModels() {
        return BaseGeneratorBuilder.getSupportedModelInfos();
    }

    @Override
    protected DynamicModelSeries createDynamicModelSeries(UpdatingDataframe dataframe) {
        return new AbstractEquipmentSeries<Generator, BaseGeneratorBuilder>(dataframe) {

            @Override
            protected BaseGeneratorBuilder createBuilder(Network network, ReportNode reportNode) {
                return BaseGeneratorBuilder.of(network, reportNode);
            }

            @Override
            protected BaseGeneratorBuilder createBuilder(Network network, String modelName, ReportNode reportNode) {
                return BaseGeneratorBuilder.of(network, modelName, reportNode);
            }
        };
    }
}
