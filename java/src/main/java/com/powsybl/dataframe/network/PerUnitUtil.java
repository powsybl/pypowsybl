/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network;

import com.powsybl.iidm.network.Line;
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

    public static double perUnitI1(DataframeContext dataframeContext, Line line) {
        return perUnitI(dataframeContext, line.getTerminal1().getI(), line.getTerminal1().getVoltageLevel().getNominalV());
    }

    public static double perUnitI2(DataframeContext dataframeContext, Line line) {
        return perUnitI(dataframeContext, line.getTerminal1().getI(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double perUnitI(DataframeContext dataframeContext, double i, double nominalV) {
        return dataframeContext.isPerUnit() ? ((sqrt(3) * nominalV) / (dataframeContext.getNominalApparentPower() * pow(10, 3))) * i : i;
    }

    public static double perUnitGNotSameNominalVSide1(DataframeContext dataframeContext, Line line) {
        return perUnitGNotSameNominalV(dataframeContext, line.getG1(), line.getR(), line.getX(),
            line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double perUnitGNotSameNominalVSide2(DataframeContext dataframeContext, Line line) {
        return perUnitGNotSameNominalV(dataframeContext, line.getG2(), line.getR(), line.getX(),
            line.getTerminal2().getVoltageLevel().getNominalV(), line.getTerminal1().getVoltageLevel().getNominalV());
    }

    public static double perUnitGNotSameNominalV(DataframeContext dataframeContext, double g, double r, double x, double nominalV1, double nominalV2) {
        return dataframeContext.isPerUnit() ? (g * pow(nominalV1, 2) + (nominalV1 - nominalV2) * nominalV1 * computeY(r, x).getReal()) / dataframeContext.getNominalApparentPower() : g;
    }

    public static double unPerUnitGNotSameNominalVSide1(DataframeContext dataframeContext, Line line, double g) {
        return unPerUnitGNotSameNominalV(dataframeContext, g, line.getR(), line.getX(),
            line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitGNotSameNominalVSide2(DataframeContext dataframeContext, Line line, double g) {
        return unPerUnitGNotSameNominalV(dataframeContext, g, line.getR(), line.getX(),
            line.getTerminal2().getVoltageLevel().getNominalV(), line.getTerminal1().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitGNotSameNominalV(DataframeContext dataframeContext, double g, double r, double x, double nominalV1, double nominalV2) {
        return dataframeContext.isPerUnit() ? (g * dataframeContext.getNominalApparentPower() - (nominalV1 - nominalV2) * nominalV1 * computeY(r, x).getReal()) / pow(nominalV1, 2) : g;
    }

    public static double perUnitBNotSameNominalVSide1(DataframeContext dataframeContext, Line line) {
        return perUnitBNotSameNominalV(dataframeContext, line.getB1(), line.getR(), line.getX(),
            line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double perUnitBNotSameNominalVSide2(DataframeContext dataframeContext, Line line) {
        return perUnitBNotSameNominalV(dataframeContext, line.getB2(), line.getR(), line.getX(),
            line.getTerminal2().getVoltageLevel().getNominalV(), line.getTerminal1().getVoltageLevel().getNominalV());
    }

    public static double perUnitBNotSameNominalV(DataframeContext dataframeContext, double b, double r, double x, double nominalV1, double nominalV2) {
        return dataframeContext.isPerUnit() ? (b * pow(nominalV1, 2) + (nominalV1 - nominalV2) * nominalV1 * computeY(r, x).getImaginary()) / dataframeContext.getNominalApparentPower() : b;
    }

    public static double unPerUnitBNotSameNominalVSide2(DataframeContext dataframeContext, Line line, double b) {
        return unPerUnitBNotSameNominalV(dataframeContext, b, line.getR(), line.getX(),
            line.getTerminal2().getVoltageLevel().getNominalV(), line.getTerminal1().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitBNotSameNominalVSide1(DataframeContext dataframeContext, Line line, double b) {
        return unPerUnitBNotSameNominalV(dataframeContext, b, line.getR(), line.getX(),
            line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitBNotSameNominalV(DataframeContext dataframeContext, double b, double r, double x, double nominalV1, double nominalV2) {
        return dataframeContext.isPerUnit() ? (b * dataframeContext.getNominalApparentPower() - (nominalV1 - nominalV2) * nominalV1 * computeY(r, x).getImaginary()) / pow(nominalV1, 2) : b;
    }

    public static double perUnitR(DataframeContext dataframeContext, double r, double nominalV) {
        return dataframeContext.isPerUnit() ? dataframeContext.getNominalApparentPower() / pow(nominalV, 2) * r : r;
    }

    public static double perUnitRNotSameNominalV(DataframeContext dataframeContext, Line line) {
        return perUnitRXNotSameNominalV(dataframeContext, line.getR(), line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double perUnitXNotSameNominalV(DataframeContext dataframeContext, Line line) {
        return perUnitRXNotSameNominalV(dataframeContext, line.getX(), line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitRXNotSameNominalV(DataframeContext dataframeContext, Line line, double r) {
        return unPerUnitRXNotSameNominalV(dataframeContext, r, line.getTerminal1().getVoltageLevel().getNominalV(), line.getTerminal2().getVoltageLevel().getNominalV());
    }

    public static double unPerUnitRXNotSameNominalV(DataframeContext dataframeContext, double r, double nominalV1, double nominalV2) {
        return dataframeContext.isPerUnit() ? (nominalV1 * nominalV2) / dataframeContext.getNominalApparentPower() * r : r;
    }

    public static double perUnitRXNotSameNominalV(DataframeContext dataframeContext, double rx, double nominalV1, double nominalV2) {
        return dataframeContext.isPerUnit() ? dataframeContext.getNominalApparentPower() / (nominalV1 * nominalV2) * rx : rx;
    }

    public static double perUnitV(DataframeContext dataframeContext, double v, double nominalV) {
        return dataframeContext.isPerUnit() ? v / nominalV : v;
    }

    private static Complex computeY(double r, double x) {
        return Complex.valueOf(r, x).reciprocal();
    }
}
