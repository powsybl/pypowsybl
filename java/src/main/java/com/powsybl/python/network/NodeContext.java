/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.iidm.network.IdentifiableType;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public class NodeContext {
    private final int node;
    private final String connectableId;
    private final IdentifiableType connectableType;

    public NodeContext(int node, String connectableId, IdentifiableType connectableType) {
        this.node = node;
        this.connectableId = connectableId;
        this.connectableType = connectableType;
    }

    public int getNode() {
        return node;
    }

    public String getConnectableId() {
        return connectableId;
    }

    public IdentifiableType getConnectableType() {
        return connectableType;
    }
}
