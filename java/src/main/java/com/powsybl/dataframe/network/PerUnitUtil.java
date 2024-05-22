/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import org.apache.commons.math3.complex.Complex;

import static java.lang.Math.*;

/**
 * @author Etienne Lesot {@literal <etienne.lesot at rte-france.com>}
 */
public final class PerUnitUtil {

    private PerUnitUtil() {

    }

    public static double perUnitP(NetworkDataframeContext context, ThreeWindingsTransformer.Leg leg) {
        return perUnitPQ(context, leg.getTerminal().getP());
    }

    public static double perUnitQ(NetworkDataframeContext context, ThreeWindingsTransformer.Leg leg) {
        return perUnitPQ(context, leg.getTerminal().getQ());
    }

    public static double perUnitPQ(NetworkDataframeContext context, double p) {
        return context.isPerUnit() ? p / context.getNominalApparentPower() : p;
    }

    public static double unPerUnitPQ(NetworkDataframeContext context, double p) {
        return context.isPerUnit() ? p * context.getNominalApparentPower() : p;
    }

    public static double perUnitI(NetworkDataframeContext context, Terminal terminal) {
        return perUnitI(context, terminal.getI(), terminal.getVoltageLevel().getNominalV());
    }

    public static double perUnitI(NetworkDataframeContext context, double i, double nominalV) {
        return context.isPerUnit() ? ((sqrt(3) * nominalV) / (context.getNominalApparentPower() * pow(10, 3))) * i : i;
    }

    public static double perUnitGSide1(NetworkDataframeContext context, Line line) {
        return perUnitG(context, line.getG1(), line.getR(), line.getX(),
            line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double perUnitGSide2(NetworkDataframeContext context, Line line) {
        return perUnitG(context, line.getG2(), line.getR(), line.getX(),
            line.getTerminal2().getVoltageLevel().getNominalV(), line.getTerminal1().getVoltageLevel().getNominalV());
    }

    public static double perUnitG(NetworkDataframeContext context, DanglingLine dl) {
        return perUnitG(context, dl.getG(), dl.getR(), dl.getX(),
            dl.getTerminal().getVoltageLevel().getNominalV(), dl.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double perUnitG(NetworkDataframeContext context, ShuntCompensator shuntCompensator) {
        return perUnitBG(context, shuntCompensator.getG(), shuntCompensator.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double perUnitG(NetworkDataframeContext context, ShuntCompensatorNonLinearModel.Section section, ShuntCompensator shuntCompensator) {
        return perUnitBG(context, section.getG(), shuntCompensator.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double perUnitBG(NetworkDataframeContext context, double bg, ThreeWindingsTransformer twt) {
        return perUnitBG(context, bg, twt.getRatedU0());
    }

    public static double perUnitBG(NetworkDataframeContext context, double bg, double nominalV) {
        return context.isPerUnit() ? bg * pow(nominalV, 2) / context.getNominalApparentPower() : bg;
    }

    public static double perUnitG(NetworkDataframeContext context, double g, double r, double x, double nominalV1, double nominalV2) {
        return context.isPerUnit() ? (g * pow(nominalV1, 2) + (nominalV1 - nominalV2) * nominalV1 * computeY(r, x).getReal()) / context.getNominalApparentPower() : g;
    }

    public static double unPerUnitGSide1(NetworkDataframeContext context, Line line, double g) {
        return unPerUnitG(context, g, line.getR(), line.getX(),
            line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitGSide2(NetworkDataframeContext context, Line line, double g) {
        return unPerUnitG(context, g, line.getR(), line.getX(),
            line.getTerminal2().getVoltageLevel().getNominalV(), line.getTerminal1().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitBG(NetworkDataframeContext context, TwoWindingsTransformer twt, double g) {
        return unPerUnitBG(context, g, twt.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitG(NetworkDataframeContext context, DanglingLine danglingLine, double g) {
        return unPerUnitG(context, g, danglingLine.getR(), danglingLine.getX(),
            danglingLine.getTerminal().getVoltageLevel().getNominalV(), danglingLine.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitBG(NetworkDataframeContext context, ShuntCompensator shuntCompensator, double bg) {
        return unPerUnitBG(context, bg, shuntCompensator.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitBG(NetworkDataframeContext context, ThreeWindingsTransformer twt, double bg) {
        return unPerUnitBG(context, bg, twt.getRatedU0());
    }

    public static double unPerUnitBG(NetworkDataframeContext context, double bg, double nominalV) {
        return context.isPerUnit() ? bg * context.getNominalApparentPower() / pow(nominalV, 2) : bg;
    }

    public static double unPerUnitG(NetworkDataframeContext context, double g, double r, double x, double nominalV1, double nominalV2) {
        return context.isPerUnit() ? (g * context.getNominalApparentPower() - (nominalV1 - nominalV2) * nominalV1 * computeY(r, x).getReal()) / pow(nominalV1, 2) : g;
    }

    public static double perUnitBSide1(NetworkDataframeContext context, Line line) {
        return perUnitB(context, line.getB1(), line.getR(), line.getX(),
            line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double perUnitBSide2(NetworkDataframeContext context, Line line) {
        return perUnitB(context, line.getB2(), line.getR(), line.getX(),
            line.getTerminal2().getVoltageLevel().getNominalV(), line.getTerminal1().getVoltageLevel().getNominalV());
    }

    public static double perUnitBG(NetworkDataframeContext context, TwoWindingsTransformer twt, double bg) {
        return perUnitBG(context, bg, twt.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double perUnitB(NetworkDataframeContext context, DanglingLine dl) {
        return perUnitBG(context, dl.getB(), dl.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double perUnitB(NetworkDataframeContext context, ShuntCompensatorNonLinearModel.Section section, ShuntCompensator shuntCompensator) {
        return perUnitBG(context, section.getB(), shuntCompensator.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double perUnitB(NetworkDataframeContext context, ShuntCompensator shuntCompensator) {
        return perUnitBG(context, shuntCompensator.getB(), shuntCompensator.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double perUnitB(NetworkDataframeContext context, double b, double r, double x, double nominalV1, double nominalV2) {
        return context.isPerUnit() ? (b * pow(nominalV1, 2) + (nominalV1 - nominalV2) * nominalV1 * computeY(r, x).getImaginary()) / context.getNominalApparentPower() : b;
    }

    public static double unPerUnitBSide2(NetworkDataframeContext context, Line line, double b) {
        return unPerUnitB(context, b, line.getR(), line.getX(),
            line.getTerminal2().getVoltageLevel().getNominalV(), line.getTerminal1().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitBSide1(NetworkDataframeContext context, Line line, double b) {
        return unPerUnitB(context, b, line.getR(), line.getX(),
            line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitB(NetworkDataframeContext context, DanglingLine danglingLine, double b) {
        return unPerUnitBG(context, b, danglingLine.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitB(NetworkDataframeContext context, double b, double r, double x, double nominalV1, double nominalV2) {
        return context.isPerUnit() ? (b * context.getNominalApparentPower() - (nominalV1 - nominalV2) * nominalV1 * computeY(r, x).getImaginary()) / pow(nominalV1, 2) : b;
    }

    public static double perUnitR(NetworkDataframeContext context, Line line) {
        return perUnitRX(context, line.getR(), line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double perUnitRX(NetworkDataframeContext context, double rx, Terminal terminal) {
        return perUnitRX(context, rx, terminal.getVoltageLevel().getNominalV(), terminal.getVoltageLevel().getNominalV());
    }

    public static double perUnitX(NetworkDataframeContext context, Line line) {
        return perUnitRX(context, line.getX(), line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitRX(NetworkDataframeContext context, Line line, double r) {
        return unPerUnitRX(context, r, line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitRX(NetworkDataframeContext context, Terminal terminal, double r) {
        return unPerUnitRX(context, r, terminal.getVoltageLevel().getNominalV(), terminal.getVoltageLevel().getNominalV());
    }

    public static double unPerUnitRX(NetworkDataframeContext context, TwoWindingsTransformer twt, double rx) {
        return unPerUnitRX(context, rx, twt.getTerminal2().getVoltageLevel().getNominalV(), twt.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitRX(NetworkDataframeContext context, ThreeWindingsTransformer twt, double rx) {
        return unPerUnitRX(context, rx, twt.getRatedU0(), twt.getRatedU0());
    }

    public static double unPerUnitRX(NetworkDataframeContext context, double r, double nominalV1, double nominalV2) {
        return context.isPerUnit() ? (nominalV1 * nominalV2) / context.getNominalApparentPower() * r : r;
    }

    public static double perUnitRX(NetworkDataframeContext context, double rx, TwoWindingsTransformer twt) {
        return perUnitRX(context, rx, twt.getTerminal2().getVoltageLevel().getNominalV(), twt.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double perUnitRX(NetworkDataframeContext context, double rx, ThreeWindingsTransformer twt) {
        return perUnitRX(context, rx, twt.getRatedU0(), twt.getRatedU0());
    }

    public static double perUnitRX(NetworkDataframeContext context, double rx, double nominalV1, double nominalV2) {
        return context.isPerUnit() ? context.getNominalApparentPower() / (nominalV1 * nominalV2) * rx : rx;
    }

    public static double unPerUnitV(NetworkDataframeContext context, double v, Bus bus) {
        return unPerUnitV(context, v, bus.getVoltageLevel().getNominalV());
    }

    public static double unPerUnitTargetV(NetworkDataframeContext context, double v, Terminal distantTerminal, Terminal localTerminal) {
        if (distantTerminal != null) {
            return unPerUnitV(context, v, distantTerminal);
        } else {
            return unPerUnitV(context, v, localTerminal);
        }
    }

    public static double unPerUnitV(NetworkDataframeContext context, double v, Terminal terminal) {
        if (terminal == null) {
            if (context.isPerUnit()) {
                throw new PowsyblException("terminal not found for un per unit V");
            } else {
                return v;
            }
        }
        return unPerUnitV(context, v, terminal.getVoltageLevel().getNominalV());
    }

    public static double unPerUnitV(NetworkDataframeContext context, double v, ThreeWindingsTransformer.Leg leg) {
        return unPerUnitV(context, v, leg.getTerminal());
    }

    public static double unPerUnitV(NetworkDataframeContext context, double v, double nominalV) {
        return context.isPerUnit() ? v * nominalV : v;
    }

    public static double perUnitV(NetworkDataframeContext context, ThreeWindingsTransformer.Leg leg) {
        return perUnitV(context, leg.getRatedU(), leg.getTerminal());
    }

    public static double perUnitTargetV(NetworkDataframeContext context, double v, Terminal distantTerminal, Terminal localTerminal) {
        if (distantTerminal != null) {
            return perUnitV(context, v, distantTerminal);
        } else {
            return perUnitV(context, v, localTerminal);
        }
    }

    public static double perUnitV(NetworkDataframeContext context, double v, Terminal terminal) {
        if (terminal == null) {
            if (context.isPerUnit()) {
                throw new PowsyblException("terminal not found for per unit V");
            } else {
                return v;
            }
        }
        return perUnitV(context, v, terminal.getVoltageLevel().getNominalV());
    }

    public static double perUnitV(NetworkDataframeContext context, double v, Bus bus) {
        return perUnitV(context, v, bus.getVoltageLevel().getNominalV());
    }

    public static double perUnitV(NetworkDataframeContext context, double v, double nominalV) {
        return context.isPerUnit() ? v / nominalV : v;
    }

    public static double perUnitRho(NetworkDataframeContext context, TwoWindingsTransformer twt, double rho) {
        return context.isPerUnit() ? rho * twt.getTerminal1().getVoltageLevel().getNominalV() / twt.getTerminal2().getVoltageLevel().getNominalV() : rho;
    }

    public static double unPerUnitRho(NetworkDataframeContext context, TwoWindingsTransformer twt, double rho) {
        return context.isPerUnit() ? rho * twt.getTerminal2().getVoltageLevel().getNominalV() / twt.getTerminal1().getVoltageLevel().getNominalV() : rho;
    }

    public static double perUnitAngle(NetworkDataframeContext context, double angle) {
        return context.isPerUnit() ? toRadians(angle) : angle;
    }

    public static double unPerUnitAngle(NetworkDataframeContext context, double angle) {
        return context.isPerUnit() ? toDegrees(angle) : angle;
    }

    private static Complex computeY(double r, double x) {
        return Complex.valueOf(r, x).reciprocal();
    }
}
