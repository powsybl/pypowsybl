/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.powsybl.dataframe.SeriesMetadata;

import java.util.Collections;
import java.util.List;

import static com.powsybl.dataframe.dynamic.adders.DynamicModelDataframeConstants.*;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
abstract class AbstractEquipmentAdder extends AbstractSimpleDynamicModelAdder {

    protected static final List<SeriesMetadata> EQUIPMENT_METADATA = List.of(
            SeriesMetadata.stringIndex(STATIC_ID),
            SeriesMetadata.strings(PARAMETER_SET_ID),
            SeriesMetadata.strings(DYNAMIC_MODEL_ID),
            SeriesMetadata.strings(MODEL_NAME));

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(EQUIPMENT_METADATA);
    }
}
