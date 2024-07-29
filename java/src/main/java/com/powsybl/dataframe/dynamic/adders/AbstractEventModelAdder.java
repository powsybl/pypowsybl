/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.python.dynamic.PythonEventModelsSupplier;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
abstract class AbstractEventModelAdder implements EventMappingAdder {

    @Override
    public void addElements(PythonEventModelsSupplier modelMapping, UpdatingDataframe dataframe) {
        EventModelSeries series = createEventModelSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            modelMapping.addModel(series.getModelSupplier(row));
        }
    }

    abstract protected EventModelSeries createEventModelSeries(UpdatingDataframe dataframe);
}
