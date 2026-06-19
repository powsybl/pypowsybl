/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic;

import com.powsybl.dynawo.builders.ModelInfo;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public record EventInformation(ModelInfo modelInfo, String attribute) {

    public String name() {
        return modelInfo().name();
    }

    public String description() {
        return modelInfo().doc();
    }
}
