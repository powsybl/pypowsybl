package com.powsybl.dataframe.network;

import org.apache.commons.math3.complex.Complex;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public final class PerUnitUtil {

    private PerUnitUtil() {

    }

    public static double perUnitPQ(boolean perUnit, double p, double sn) {
        return perUnit ? p / sn : p;
    }

    public static double perUnitI(boolean perUnit, double i, double sn, double nominalV) {
        return perUnit ? ((sqrt(3) * nominalV) / (sn * pow(10, 3))) * i : i;
    }

    public static double perUnitG(boolean perUnit, double g, double sn, double nominalV) {
        return perUnit ? pow(nominalV, 2) / sn * g : g;
    }

    public static double perUnitGNotSameNominalV(boolean perUnit, double g, double r, double x, double sn, double nominalV1, double nominalV2) {
        return perUnit ? (g * pow(nominalV1, 2) + (nominalV1 - nominalV2) * nominalV1 * computeY(r, x).getReal()) / sn : g;
    }

    public static double perUnitBNotSameNominalV(boolean perUnit, double b, double r, double x, double sn, double nominalV1, double nominalV2) {
        return perUnit ? (b * pow(nominalV1, 2) + (nominalV1 - nominalV2) * nominalV1 * computeY(r, x).getImaginary()) / sn : b;
    }

    public static double perUnitR(boolean perUnit, double r, double sn, double nominalV) {
        return perUnit ? sn / pow(nominalV, 2) * r : r;
    }

    public static double perUnitRXNotSameNominalV(boolean perUnit, double r, double sn, double nominalV1, double nominalV2) {
        return perUnit ? sn / (nominalV1 * nominalV2) * r : r;
    }

    public static double perUnitV(boolean perUnit, double v, double nominalV) {
        return perUnit ? v / nominalV : v;
    }

    private static Complex computeY(double r, double x) {
        return Complex.valueOf(r, x).reciprocal();
    }
}
