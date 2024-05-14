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

    public static double perUnitP(DataframeContext dataframeContext, ThreeWindingsTransformer.Leg leg) {
        return perUnitPQ(dataframeContext, leg.getTerminal().getP());
    }

    public static double perUnitQ(DataframeContext dataframeContext, ThreeWindingsTransformer.Leg leg) {
        return perUnitPQ(dataframeContext, leg.getTerminal().getQ());
    }

    public static double perUnitPQ(DataframeContext dataframeContext, double p) {
        return dataframeContext.isPerUnit() ? p / dataframeContext.getNominalApparentPower() : p;
    }

    public static double unPerUnitPQ(DataframeContext dataframeContext, double p) {
        return dataframeContext.isPerUnit() ? p * dataframeContext.getNominalApparentPower() : p;
    }

    public static double perUnitI(DataframeContext dataframeContext, Terminal terminal) {
        return perUnitI(dataframeContext, terminal.getI(), terminal.getVoltageLevel().getNominalV());
    }

    public static double perUnitI(DataframeContext dataframeContext, double i, double nominalV) {
        return dataframeContext.isPerUnit() ? ((sqrt(3) * nominalV) / (dataframeContext.getNominalApparentPower() * pow(10, 3))) * i : i;
    }

    public static double perUnitGSide1(DataframeContext dataframeContext, Line line) {
        return perUnitG(dataframeContext, line.getG1(), line.getR(), line.getX(),
            line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double perUnitGSide2(DataframeContext dataframeContext, Line line) {
        return perUnitG(dataframeContext, line.getG2(), line.getR(), line.getX(),
            line.getTerminal2().getVoltageLevel().getNominalV(), line.getTerminal1().getVoltageLevel().getNominalV());
    }

    public static double perUnitG(DataframeContext dataframeContext, DanglingLine dl) {
        return perUnitG(dataframeContext, dl.getG(), dl.getR(), dl.getX(),
            dl.getTerminal().getVoltageLevel().getNominalV(), dl.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double perUnitG(DataframeContext dataframeContext, ShuntCompensator shuntCompensator) {
        return perUnitBG(dataframeContext, shuntCompensator.getG(), shuntCompensator.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double perUnitG(DataframeContext dataframeContext, ShuntCompensatorNonLinearModel.Section section, ShuntCompensator shuntCompensator) {
        return perUnitBG(dataframeContext, section.getG(), shuntCompensator.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double perUnitBG(DataframeContext dataframeContext, double bg, ThreeWindingsTransformer twt) {
        return perUnitBG(dataframeContext, bg, twt.getRatedU0());
    }

    public static double perUnitBG(DataframeContext dataframeContext, double bg, double nominalV) {
        return dataframeContext.isPerUnit() ? bg * pow(nominalV, 2) / dataframeContext.getNominalApparentPower() : bg;
    }

    public static double perUnitG(DataframeContext dataframeContext, double g, double r, double x, double nominalV1, double nominalV2) {
        return dataframeContext.isPerUnit() ? (g * pow(nominalV1, 2) + (nominalV1 - nominalV2) * nominalV1 * computeY(r, x).getReal()) / dataframeContext.getNominalApparentPower() : g;
    }

    public static double unPerUnitGSide1(DataframeContext dataframeContext, Line line, double g) {
        return unPerUnitG(dataframeContext, g, line.getR(), line.getX(),
            line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitGSide2(DataframeContext dataframeContext, Line line, double g) {
        return unPerUnitG(dataframeContext, g, line.getR(), line.getX(),
            line.getTerminal2().getVoltageLevel().getNominalV(), line.getTerminal1().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitBG(DataframeContext dataframeContext, TwoWindingsTransformer twt, double g) {
        return unPerUnitBG(dataframeContext, g, twt.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitG(DataframeContext dataframeContext, DanglingLine danglingLine, double g) {
        return unPerUnitG(dataframeContext, g, danglingLine.getR(), danglingLine.getX(),
            danglingLine.getTerminal().getVoltageLevel().getNominalV(), danglingLine.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitBG(DataframeContext dataframeContext, ShuntCompensator shuntCompensator, double bg) {
        return unPerUnitBG(dataframeContext, bg, shuntCompensator.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitBG(DataframeContext dataframeContext, ThreeWindingsTransformer twt, double bg) {
        return unPerUnitBG(dataframeContext, bg, twt.getRatedU0());
    }

    public static double unPerUnitBG(DataframeContext dataframeContext, double bg, double nominalV) {
        return dataframeContext.isPerUnit() ? bg * dataframeContext.getNominalApparentPower() / pow(nominalV, 2) : bg;
    }

    public static double unPerUnitG(DataframeContext dataframeContext, double g, double r, double x, double nominalV1, double nominalV2) {
        return dataframeContext.isPerUnit() ? (g * dataframeContext.getNominalApparentPower() - (nominalV1 - nominalV2) * nominalV1 * computeY(r, x).getReal()) / pow(nominalV1, 2) : g;
    }

    public static double perUnitBSide1(DataframeContext dataframeContext, Line line) {
        return perUnitB(dataframeContext, line.getB1(), line.getR(), line.getX(),
            line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double perUnitBSide2(DataframeContext dataframeContext, Line line) {
        return perUnitB(dataframeContext, line.getB2(), line.getR(), line.getX(),
            line.getTerminal2().getVoltageLevel().getNominalV(), line.getTerminal1().getVoltageLevel().getNominalV());
    }

    public static double perUnitBG(DataframeContext dataframeContext, TwoWindingsTransformer twt, double bg) {
        return perUnitBG(dataframeContext, bg, twt.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double perUnitB(DataframeContext dataframeContext, DanglingLine dl) {
        return perUnitBG(dataframeContext, dl.getB(), dl.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double perUnitB(DataframeContext dataframeContext, ShuntCompensatorNonLinearModel.Section section, ShuntCompensator shuntCompensator) {
        return perUnitBG(dataframeContext, section.getB(), shuntCompensator.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double perUnitB(DataframeContext dataframeContext, ShuntCompensator shuntCompensator) {
        return perUnitBG(dataframeContext, shuntCompensator.getB(), shuntCompensator.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double perUnitB(DataframeContext dataframeContext, double b, double r, double x, double nominalV1, double nominalV2) {
        return dataframeContext.isPerUnit() ? (b * pow(nominalV1, 2) + (nominalV1 - nominalV2) * nominalV1 * computeY(r, x).getImaginary()) / dataframeContext.getNominalApparentPower() : b;
    }

    public static double unPerUnitBSide2(DataframeContext dataframeContext, Line line, double b) {
        return unPerUnitB(dataframeContext, b, line.getR(), line.getX(),
            line.getTerminal2().getVoltageLevel().getNominalV(), line.getTerminal1().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitBSide1(DataframeContext dataframeContext, Line line, double b) {
        return unPerUnitB(dataframeContext, b, line.getR(), line.getX(),
            line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitB(DataframeContext dataframeContext, DanglingLine danglingLine, double b) {
        return unPerUnitBG(dataframeContext, b, danglingLine.getTerminal().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitB(DataframeContext dataframeContext, double b, double r, double x, double nominalV1, double nominalV2) {
        return dataframeContext.isPerUnit() ? (b * dataframeContext.getNominalApparentPower() - (nominalV1 - nominalV2) * nominalV1 * computeY(r, x).getImaginary()) / pow(nominalV1, 2) : b;
    }

    public static double perUnitR(DataframeContext dataframeContext, Line line) {
        return perUnitRX(dataframeContext, line.getR(), line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double perUnitRX(DataframeContext dataframeContext, double rx, Terminal terminal) {
        return perUnitRX(dataframeContext, rx, terminal.getVoltageLevel().getNominalV(), terminal.getVoltageLevel().getNominalV());
    }

    public static double perUnitX(DataframeContext dataframeContext, Line line) {
        return perUnitRX(dataframeContext, line.getX(), line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitRX(DataframeContext dataframeContext, Line line, double r) {
        return unPerUnitRX(dataframeContext, r, line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitRX(DataframeContext dataframeContext, Terminal terminal, double r) {
        return unPerUnitRX(dataframeContext, r, terminal.getVoltageLevel().getNominalV(), terminal.getVoltageLevel().getNominalV());
    }

    public static double unPerUnitRX(DataframeContext dataframeContext, TwoWindingsTransformer twt, double rx) {
        return unPerUnitRX(dataframeContext, rx, twt.getTerminal2().getVoltageLevel().getNominalV(), twt.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitRX(DataframeContext dataframeContext, ThreeWindingsTransformer twt, double rx) {
        return unPerUnitRX(dataframeContext, rx, twt.getRatedU0(), twt.getRatedU0());
    }

    public static double unPerUnitRX(DataframeContext dataframeContext, double r, double nominalV1, double nominalV2) {
        return dataframeContext.isPerUnit() ? (nominalV1 * nominalV2) / dataframeContext.getNominalApparentPower() * r : r;
    }

    public static double perUnitRX(DataframeContext dataframeContext, double rx, TwoWindingsTransformer twt) {
        return perUnitRX(dataframeContext, rx, twt.getTerminal2().getVoltageLevel().getNominalV(), twt.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double perUnitRX(DataframeContext dataframeContext, double rx, ThreeWindingsTransformer twt) {
        return perUnitRX(dataframeContext, rx, twt.getRatedU0(), twt.getRatedU0());
    }

    public static double perUnitRX(DataframeContext dataframeContext, double rx, double nominalV1, double nominalV2) {
        return dataframeContext.isPerUnit() ? dataframeContext.getNominalApparentPower() / (nominalV1 * nominalV2) * rx : rx;
    }

    public static double unPerUnitV(DataframeContext dataframeContext, double v, Bus bus) {
        return unPerUnitV(dataframeContext, v, bus.getVoltageLevel().getNominalV());
    }

    public static double unPerUnitTargetV(DataframeContext dataframeContext, double v, Terminal distantTerminal, Terminal localTerminal) {
        if (distantTerminal != null) {
            return unPerUnitV(dataframeContext, v, distantTerminal);
        } else {
            return unPerUnitV(dataframeContext, v, localTerminal);
        }
    }

    public static double unPerUnitV(DataframeContext dataframeContext, double v, Terminal terminal) {
        if (terminal == null) {
            if (dataframeContext.isPerUnit()) {
                throw new PowsyblException("terminal not found for un per unit V");
            } else {
                return v;
            }
        }
        return unPerUnitV(dataframeContext, v, terminal.getVoltageLevel().getNominalV());
    }

    public static double unPerUnitV(DataframeContext dataframeContext, double v, ThreeWindingsTransformer.Leg leg) {
        return unPerUnitV(dataframeContext, v, leg.getTerminal());
    }

    public static double unPerUnitV(DataframeContext dataframeContext, double v, double nominalV) {
        return dataframeContext.isPerUnit() ? v * nominalV : v;
    }

    public static double perUnitV(DataframeContext dataframeContext, ThreeWindingsTransformer.Leg leg) {
        return perUnitV(dataframeContext, leg.getRatedU(), leg.getTerminal());
    }

    public static double perUnitTargetV(DataframeContext dataframeContext, double v, Terminal distantTerminal, Terminal localTerminal) {
        if (distantTerminal != null) {
            return perUnitV(dataframeContext, v, distantTerminal);
        } else {
            return perUnitV(dataframeContext, v, localTerminal);
        }
    }

    public static double perUnitV(DataframeContext dataframeContext, double v, Terminal terminal) {
        if (terminal == null) {
            if (dataframeContext.isPerUnit()) {
                throw new PowsyblException("terminal not found for per unit V");
            } else {
                return v;
            }
        }
        return perUnitV(dataframeContext, v, terminal.getVoltageLevel().getNominalV());
    }

    public static double perUnitV(DataframeContext dataframeContext, double v, Bus bus) {
        return perUnitV(dataframeContext, v, bus.getVoltageLevel().getNominalV());
    }

    public static double perUnitV(DataframeContext dataframeContext, double v, double nominalV) {
        return dataframeContext.isPerUnit() ? v / nominalV : v;
    }

    public static double perUnitRho(DataframeContext dataframeContext, TwoWindingsTransformer twt, double rho) {
        return dataframeContext.isPerUnit() ? rho * twt.getTerminal1().getVoltageLevel().getNominalV() / twt.getTerminal2().getVoltageLevel().getNominalV() : rho;
    }

    public static double unPerUnitRho(DataframeContext dataframeContext, TwoWindingsTransformer twt, double rho) {
        return dataframeContext.isPerUnit() ? rho * twt.getTerminal2().getVoltageLevel().getNominalV() / twt.getTerminal1().getVoltageLevel().getNominalV() : rho;
    }

    public static double perUnitAngle(DataframeContext dataframeContext, double angle) {
        return dataframeContext.isPerUnit() ? toRadians(angle) : angle;
    }

    public static double unPerUnitAngle(DataframeContext dataframeContext, double angle) {
        return dataframeContext.isPerUnit() ? toDegrees(angle) : angle;
    }

    private static Complex computeY(double r, double x) {
        return Complex.valueOf(r, x).reciprocal();
    }
}
