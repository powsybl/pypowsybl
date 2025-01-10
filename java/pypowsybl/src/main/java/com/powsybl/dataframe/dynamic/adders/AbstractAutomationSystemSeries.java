/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawo.models.automationsystems.AbstractAutomationSystemModelBuilder;

import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
abstract class AbstractAutomationSystemSeries<T extends AbstractAutomationSystemModelBuilder<T>> extends AbstractDynamicModelSeries<T> {

    AbstractAutomationSystemSeries(UpdatingDataframe dataframe) {
        super(dataframe);
    }

    protected void applyOnBuilder(int row, T builder) {
        applyIfPresent(dynamicModelIds, row, builder::dynamicModelId);
        applyIfPresent(parameterSetIds, row, builder::parameterSetId);
    }
}
