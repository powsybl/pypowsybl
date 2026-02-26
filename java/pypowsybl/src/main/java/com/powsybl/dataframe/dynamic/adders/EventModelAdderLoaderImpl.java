/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import com.google.auto.service.AutoService;

import java.util.List;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
@AutoService(EventModelAdderLoader.class)
public class EventModelAdderLoaderImpl implements EventModelAdderLoader {

    private static final List<EventMappingAdder> BASE_MODEL_ADDERS = List.of(
            new DisconnectAdder(),
            new NodeFaultAdder(),
            new ActivePowerVariationAdder(),
            new ReactivePowerVariationAdder(),
            new ReferenceVoltageVariationAdder());

    @Override
    public List<EventMappingAdder> getEventModelAdders() {
        return BASE_MODEL_ADDERS;
    }
}
