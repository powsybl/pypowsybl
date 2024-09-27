/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.iidm.network.Switch;

import java.util.Objects;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public class NodeBreakerViewSwitchContext {
    private final Switch switchContext;
    private final int node1;
    private final int node2;

    public NodeBreakerViewSwitchContext(Switch switchContext, int node1, int node2) {
        this.switchContext = Objects.requireNonNull(switchContext);
        this.node1 = node1;
        this.node2 = node2;
    }

    public Switch getSwitchContext() {
        return switchContext;
    }

    public int getNode1() {
        return node1;
    }

    public int getNode2() {
        return node2;
    }
}
