/**
* Copyright (c) 2022, RTE (http://www.rte-france.com)
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
* SPDX-License-Identifier: MPL-2.0
*/
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.python.dynamic.DynamicModelMapper;

import java.util.List;

/**
 * @author Nicolas PIERRE <nicolas.pierre@artelys.com>
 */
public interface DynamicMappingAdder {

    /**
     * Get the list of metadata
     */
    List<SeriesMetadata> getMetadata();

    /**
     * Adds elements to the dynamic model mapping, based on a list of dataframes.
     * The first dataframe is considered the "primary" dataframe, other dataframes
     * can provide additional data.
     */
    void addElements(DynamicModelMapper modelMapping, UpdatingDataframe dataframe);

}
