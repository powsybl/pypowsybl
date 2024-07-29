/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynamicsimulation.EventModel;
import com.powsybl.dynamicsimulation.EventModelsSupplier;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public class PythonEventModelsSupplier implements EventModelsSupplier {

    private final List<BiFunction<Network, ReportNode, EventModel>> eventSupplierList = new ArrayList<>();

    @Override
    public List<EventModel> get(Network network, ReportNode reportNode) {
        return eventSupplierList.stream().map(f -> f.apply(network, reportNode)).filter(Objects::nonNull).toList();
    }

    public void addModel(BiFunction<Network, ReportNode, EventModel> modelFunction) {
        eventSupplierList.add(modelFunction);
    }
}
