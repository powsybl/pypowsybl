/**
 * Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic;

import java.util.Collection;

import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.dynawaltz.dynamicmodels.BlackBoxModel;
import com.powsybl.python.commons.Util;

public final class DynamicMappingsDataframeHandle {

    private DynamicMappingsDataframeHandle() {
    }

    public static DataframeMapper<Collection<BlackBoxModel>> dynamicMappingsDataFrameMapper() {
        DataframeMapperBuilder<Collection<BlackBoxModel>, BlackBoxModel> df = new DataframeMapperBuilder<>();
        df.itemsStreamProvider(Collection::stream)
                .intsIndex("id", model -> (model.getStaticId() + model.getLib()).hashCode())
                .strings("static_id", BlackBoxModel::getStaticId)
                .strings("parameter_set_id", BlackBoxModel::getStaticId)
                .ints("mapping_type", model -> Util.getEnumValue(model.getLib()).getCValue());
        return df.build();
    }
}
