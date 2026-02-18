/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
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
import com.powsybl.dynawo.models.generators.SignalNGeneratorBuilder;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;

import java.util.Collection;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class SignalNGeneratorAdder extends AbstractEquipmentAdder {

    private static final CategoryInformation CATEGORY_INFORMATION = new CategoryInformation(
            "SignalNGenerator",
            "Signal N generator",
            CategoryAttributeUtils.createFromMetadata(EQUIPMENT_METADATA));

    protected SignalNGeneratorAdder() {
        super(CATEGORY_INFORMATION);
    }

    @Override
    public Collection<ModelInfo> getSupportedModels() {
        return SignalNGeneratorBuilder.getSupportedModelInfos();
    }

    @Override
    protected DynamicModelSeries createDynamicModelSeries(UpdatingDataframe dataframe) {
        return new AbstractEquipmentSeries<Generator, SignalNGeneratorBuilder>(dataframe) {

            @Override
            protected SignalNGeneratorBuilder createBuilder(Network network, ReportNode reportNode) {
                return SignalNGeneratorBuilder.of(network, reportNode);
            }

            @Override
            protected SignalNGeneratorBuilder createBuilder(Network network, String modelName, ReportNode reportNode) {
                return SignalNGeneratorBuilder.of(network, modelName, reportNode);
            }
        };
    }
}
