/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawaltz.builders.EquipmentModelBuilder;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
abstract class AbstractEquipmentSeries<T extends EquipmentModelBuilder<T>> extends AbstractDynamicModelSeries<T> {

    protected final StringSeries staticIds;

    AbstractEquipmentSeries(UpdatingDataframe dataframe) {
        super(dataframe);
        this.staticIds = dataframe.getStrings(STATIC_ID);
    }

    protected void applyOnBuilder(int row, T builder) {
        applyIfPresent(staticIds, row, builder::staticId);
        applyIfPresent(parameterSetIds, row, builder::parameterSetId);
    }
}