package com.powsybl.python;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 */
public class NodeContext {
    final int node;
    final String connectableId;

    public NodeContext(int node, @Nullable String connectableId) {
        this.node = Objects.requireNonNull(node);
        this.connectableId = connectableId;
    }

    public int getNode() {
        return node;
    }

    public String getConnectableId() {
        return connectableId;
    }
}
