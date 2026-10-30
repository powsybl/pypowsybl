/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network;

import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.regulation.RegulationMode;
import com.powsybl.iidm.network.regulation.VoltageRegulation;
import com.powsybl.iidm.network.regulation.VoltageRegulationHolder;

import static com.powsybl.dataframe.network.PerUnitUtil.*;

/**
 * @author Hugo Kulesza {@literal <hugo.kulesza at rte-france.com>}
 */
public final class RegulationUtil {

    private RegulationUtil() {
    }

    static void setRegulating(VoltageRegulationHolder<?> holder, boolean regulating) {
        if (holder.getVoltageRegulation() != null) {
            holder.getVoltageRegulation().setRegulating(regulating);
        } else {
            holder.newVoltageRegulation().withRegulating(regulating).build();
        }
    }

    static RegulationMode getRegulationMode(VoltageRegulationHolder<?> holder) {
        return holder.getVoltageRegulation() != null ? holder.getVoltageRegulation().getMode() : null;
    }

    static void setRegulationMode(VoltageRegulationHolder<?> holder, RegulationMode mode) {
        if (holder.getVoltageRegulation() != null) {
            holder.getVoltageRegulation().setMode(mode);
        } else {
            holder.newVoltageRegulation().withMode(mode).withRegulating(false).build();
        }
    }

    static double getRegulationValue(VoltageRegulationHolder<?> holder, NetworkDataframeContext context) {
        return holder.getVoltageRegulation() != null ? perUnitWithMode(context, holder.getVoltageRegulation().getTargetValue(),
                holder.getVoltageRegulation().getMode(), holder.getRegulatingTerminal()) : Double.NaN;
    }

    static void setRegulationValue(VoltageRegulationHolder<?> holder, double value, NetworkDataframeContext context) {
        if (holder.getVoltageRegulation() != null) {
            double perUnitedValue = unPerUnitWithMode(context, value, holder.getVoltageRegulation().getMode(), holder.getRegulatingTerminal());

            holder.getVoltageRegulation().setTerminal(holder.getRegulatingTerminal(), perUnitedValue);
        } else {
            holder.newVoltageRegulation().withTargetValue(value)
                    .withRegulating(false).build();
        }
    }

    static void setRegulatingTerminal(VoltageRegulationHolder<?> holder, Terminal terminal) {
        boolean local = terminal.equals(holder.getTerminal());

        if (holder.getVoltageRegulation() != null) {
            VoltageRegulation regulation = holder.getVoltageRegulation();
            if (local) {
                regulation.setTerminal(null, Double.NaN);
            } else {
                holder.getVoltageRegulation().setTerminal(terminal, chooseValueToKeep(holder));
            }
        } else {
            if (!local) {
                holder.newVoltageRegulation().withTerminal(terminal).withRegulating(false).build();
            }
        }
    }

    private static double chooseValueToKeep(VoltageRegulationHolder<?> holder) {
        double valueToKeep = holder.getLocalTargetV();
        VoltageRegulation regulation = holder.getVoltageRegulation();
        double regulationValue = regulation.getTargetValue();
        if (Double.isNaN(regulationValue) && regulation.getMode() == RegulationMode.REACTIVE_POWER) {
            valueToKeep = holder.getLocalTargetQ();
        } else if (!Double.isNaN(regulationValue)) {
            valueToKeep = regulationValue;
        }
        return valueToKeep;
    }

    static double getTargetDeadband(VoltageRegulationHolder<?> holder, NetworkDataframeContext context) {
        return holder.getVoltageRegulation() != null ?
                perUnitV(context, holder.getVoltageRegulation().getTargetDeadband(), holder.getRegulatingTerminal()) : Double.NaN;
    }

    static void setTargetDeadband(VoltageRegulationHolder<?> holder, double deadband, NetworkDataframeContext context) {
        double unPerUnitDeadband = unPerUnitV(context, deadband, holder.getRegulatingTerminal());
        if (holder.getVoltageRegulation() != null) {
            holder.getVoltageRegulation().setTargetDeadband(unPerUnitDeadband);
        } else {
            holder.newVoltageRegulation().withTargetDeadband(unPerUnitDeadband).withRegulating(false).build();
        }
    }

    static double getSlope(VoltageRegulationHolder<?> holder, NetworkDataframeContext context) {
        return holder.getVoltageRegulation() != null ?
                perUnitV(context, holder.getVoltageRegulation().getSlope(), holder.getRegulatingTerminal()) : Double.NaN;
    }

    static void setSlope(VoltageRegulationHolder<?> holder, double slope, NetworkDataframeContext context) {
        if (holder.getVoltageRegulation() != null) {
            holder.getVoltageRegulation().setSlope(unPerUnitV(context, slope, holder.getRegulatingTerminal()));
        }
    }
}
