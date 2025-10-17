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
import com.powsybl.dynawo.models.hvdc.HvdcPBuilder;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;

import java.util.Collection;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class HvdcPAdder extends AbstractEquipmentAdder {

    private static final CategoryInformation CATEGORY_INFORMATION = new CategoryInformation(
            "HvdcP",
            "HVDC P",
            CategoryAttributeUtils.createFromMetadata(EQUIPMENT_METADATA));

    @Override
    public String getCategory() {
        return CATEGORY_INFORMATION.name();
    }

    @Override
    public CategoryInformation getCategoryInformation() {
        return CATEGORY_INFORMATION;
    }

    @Override
    public Collection<ModelInfo> getSupportedModels() {
        return HvdcPBuilder.getSupportedModelInfos();
    }

    @Override
    protected DynamicModelSeries createDynamicModelSeries(UpdatingDataframe dataframe) {
        return new AbstractEquipmentSeries<HvdcLine, HvdcPBuilder>(dataframe) {

            @Override
            protected HvdcPBuilder createBuilder(Network network, ReportNode reportNode) {
                return HvdcPBuilder.of(network, reportNode);
            }

            @Override
            protected HvdcPBuilder createBuilder(Network network, String modelName, ReportNode reportNode) {
                return HvdcPBuilder.of(network, modelName, reportNode);
            }
        };
    }
}
