/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.dynawaltz.models.hvdc.AbstractHvdcBuilder;
import com.powsybl.python.commons.PyPowsyblApiHeader.ThreeSideType;
import com.powsybl.python.commons.Util;

import java.util.ArrayList;
import java.util.List;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.DANGLING_SIDE;
import static com.powsybl.dataframe.network.adders.SeriesUtils.applyIfPresent;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
abstract class AbstractHvdcAdder extends AbstractEquipmentAdder {

    protected static final List<SeriesMetadata> HVDC_METADATA;

    static {
        List<SeriesMetadata> tmp = new ArrayList<>(EQUIPMENT_METADATA);
        tmp.add(SeriesMetadata.ints(DANGLING_SIDE));
        HVDC_METADATA = List.copyOf(tmp);
    }

    @Override
    public List<SeriesMetadata> getMetadata() {
        return HVDC_METADATA;
    }

    protected abstract static class AbstractHvdcSeries<T extends AbstractHvdcBuilder<T>> extends AbstractEquipmentSeries<T> {

        private final StringSeries danglingSides;

        AbstractHvdcSeries(UpdatingDataframe dataframe) {
            super(dataframe);
            this.danglingSides = dataframe.getStrings(DANGLING_SIDE);
        }

        @Override
        protected void applyOnBuilder(int row, T builder) {
            super.applyOnBuilder(row, builder);
            applyIfPresent(danglingSides, row, ThreeSideType.class, Util::convertToTwoSides, builder::dangling);
        }
    }
}
