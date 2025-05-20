/**
* Copyright (c) 2024, RTE (http://www.rte-france.com)
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/.
* SPDX-License-Identifier: MPL-2.0
*/
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.python.dynamic.PythonEventModelsSupplier;

import java.util.List;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public interface EventMappingAdder {

    /**
     * Get the list of metadata
     */
    List<SeriesMetadata> getMetadata();

    /**
     * Adds elements to the event model mapping, based on a list of dataframes.
     */
    void addElements(PythonEventModelsSupplier modelMapping, UpdatingDataframe dataframe);

}
