/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.iidm.network.Network;
import com.powsybl.nad.model.VoltageLevelNode;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.DefaultLabelProvider;

import java.util.List;

/**
 * @author Florian Dupuy {@literal <florian.dupuy at rte-france.com>}
 */
public class CustomLabelProvider extends DefaultLabelProvider {
    public CustomLabelProvider(Network network, SvgParameters svgParameters) {
        super(network, svgParameters);
    }

    @Override
    public List<String> getVoltageLevelDetails(VoltageLevelNode vlNode) {
        DiagramCFunctions.VoltageLevelDetailsProvider vlDetailProvider = (DiagramCFunctions.VoltageLevelDetailsProvider) DiagramCFunctions.voltageLevelDetailsProvider;
        String details = vlDetailProvider.get(vlNode.getEquipmentId());
        return List.of(details);
    }
}
