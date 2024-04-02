/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network;

import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Terminal;
import org.apache.commons.math3.complex.Complex;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * @author Etienne Lesot {@literal <etienne.lesot at rte-france.com>}
 */
public final class PerUnitUtil {

    private PerUnitUtil() {

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

    public static double unPerUnitB(DataframeContext dataframeContext, double b, double r, double x, double nominalV1, double nominalV2) {
        return dataframeContext.isPerUnit() ? (b * dataframeContext.getNominalApparentPower() - (nominalV1 - nominalV2) * nominalV1 * computeY(r, x).getImaginary()) / pow(nominalV1, 2) : b;
    }

    public static double perUnitR(DataframeContext dataframeContext, Line line) {
        return perUnitRX(dataframeContext, line.getR(), line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double perUnitX(DataframeContext dataframeContext, Line line) {
        return perUnitRX(dataframeContext, line.getX(), line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitRX(DataframeContext dataframeContext, Line line, double r) {
        return unPerUnitRX(dataframeContext, r, line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitRX(DataframeContext dataframeContext, double r, double nominalV1, double nominalV2) {
        return dataframeContext.isPerUnit() ? (nominalV1 * nominalV2) / dataframeContext.getNominalApparentPower() * r : r;
    }

    public static double perUnitRX(DataframeContext dataframeContext, double rx, double nominalV1, double nominalV2) {
        return dataframeContext.isPerUnit() ? dataframeContext.getNominalApparentPower() / (nominalV1 * nominalV2) * rx : rx;
    }

    public static double unPerUnitV(DataframeContext dataframeContext, double v, Terminal terminal) {
        return unPerUnitV(dataframeContext, v, terminal.getVoltageLevel().getNominalV());
    }

    public static double unPerUnitV(DataframeContext dataframeContext, double v, double nominalV) {
        return dataframeContext.isPerUnit() ? v * nominalV : v;
    }

    public static double perUnitV(DataframeContext dataframeContext, double v, Terminal terminal) {
        return perUnitV(dataframeContext, v, terminal.getVoltageLevel().getNominalV());
    }

    public static double perUnitV(DataframeContext dataframeContext, double v, double nominalV) {
        return dataframeContext.isPerUnit() ? v / nominalV : v;
    }

    private static Complex computeY(double r, double x) {
        return Complex.valueOf(r, x).reciprocal();
    }
}
