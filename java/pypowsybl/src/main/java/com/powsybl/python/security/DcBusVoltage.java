/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.security;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Network;
import com.powsybl.security.results.BusResult;
import com.powsybl.security.results.NetworkResult;

/**
 * Nominal voltage of a bus monitored by a DC security analysis, which is its voltage magnitude in the DC approximation.
 *
 * <p>It is a snapshot taken when the analysis ends, as the result may be read after the network has been modified.
 * It only completes the bus results for which the provider reports an angle but no voltage magnitude.
 *
 * @author Samuel Monroe {@literal <smn at n-side.com>}
 */
public final class DcBusVoltage extends AbstractExtension<BusResult> {

    private final double voltage;

    private DcBusVoltage(double voltage) {
        this.voltage = voltage;
    }

    @Override
    public String getName() {
        return "pythonDcBusVoltage";
    }

    public static void attach(Network network, NetworkResult result) {
        result.getBusResults().stream()
                .filter(bus -> Double.isNaN(bus.getV()) && Double.isFinite(bus.getAngle()))
                .forEach(bus -> bus.addExtension(DcBusVoltage.class,
                        new DcBusVoltage(network.getVoltageLevel(bus.getVoltageLevelId()).getNominalV())));
    }

    public static double voltage(BusResult result) {
        DcBusVoltage extension = result.getExtension(DcBusVoltage.class);
        return extension == null ? result.getV() : extension.voltage;
    }
}
