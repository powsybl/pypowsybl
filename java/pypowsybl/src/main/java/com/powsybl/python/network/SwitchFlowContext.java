package com.powsybl.python.network;

import java.util.Optional;

/**
 * Flow through one switch as computed by {@link com.powsybl.iidm.network.util.SwitchesFlow}, together with the
 * parallel switch indicator: {@code firstParallelSwitchId} is the id of one other closed switch connecting the same
 * two nodes or buses, or empty if there is none.
 */
public record SwitchFlowContext(String id, double p, double q, Optional<String> firstParallelSwitchId) {
}
