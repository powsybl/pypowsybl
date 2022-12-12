/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.flow_decomposition;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class FlowDecompositionContext {
    private final List<String> precontingencyMonitoredElements = new ArrayList<>();

    public void addPrecontingencyMonitoredElements(List<String> elementIds) {
        precontingencyMonitoredElements.addAll(elementIds);
    }

    public List<String> getPrecontingencyMonitoredElements() {
        return precontingencyMonitoredElements;
    }
}
