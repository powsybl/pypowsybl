/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.loadflow;

import java.io.IOException;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
public class BranchValidationWriter extends DefaultInMemoryValidationWriter<BranchValidationWriter.ValidationData> {

    @Override
    public void write(String branchId, double p1, double p1Calc, double q1, double q1Calc, double p2, double p2Calc, double q2, double q2Calc, double r, double x, double g1, double g2, double b1, double b2, double rho1, double rho2, double alpha1, double alpha2, double u1, double u2, double theta1, double theta2, double z, double y, double ksi, int phaseAngleClock, boolean connected1, boolean connected2, boolean mainComponent1, boolean mainComponent2, boolean validated) throws IOException {
        list.add(new ValidationData(branchId, p1, p1Calc, q1, q1Calc, p2, p2Calc, q2, q2Calc, r, x, g1, g2, b1, b2, rho1, rho2, alpha1, alpha2, u1, u2, theta1, theta2, z, y, ksi, phaseAngleClock, connected1, connected2, mainComponent1, mainComponent2, validated));
    }

    static class ValidationData {
        String branchId;
        double p1;
        double p1Calc;
        double q1;
        double q1Calc;
        double p2;
        double p2Calc;
        double q2;
        double q2Calc;
        double r;
        double x;
        double g1;
        double g2;
        double b1;
        double b2;
        double rho1;
        double rho2;
        double alpha1;
        double alpha2;
        double u1;
        double u2;
        double theta1;
        double theta2;
        double z;
        double y;
        double ksi;
        int phaseAngleClock;
        boolean connected1;
        boolean connected2;
        boolean mainComponent1;
        boolean mainComponent2;
        boolean validated;

        public ValidationData(String branchId, double p1, double p1Calc, double q1, double q1Calc, double p2, double p2Calc, double q2, double q2Calc, double r, double x, double g1, double g2, double b1, double b2, double rho1, double rho2, double alpha1, double alpha2, double u1, double u2, double theta1, double theta2, double z, double y, double ksi, int phaseAngleClock, boolean connected1, boolean connected2, boolean mainComponent1, boolean mainComponent2, boolean validated) {
            this.branchId = branchId;
            this.p1 = p1;
            this.p1Calc = p1Calc;
            this.q1 = q1;
            this.q1Calc = q1Calc;
            this.p2 = p2;
            this.p2Calc = p2Calc;
            this.q2 = q2;
            this.q2Calc = q2Calc;
            this.r = r;
            this.x = x;
            this.g1 = g1;
            this.g2 = g2;
            this.b1 = b1;
            this.b2 = b2;
            this.rho1 = rho1;
            this.rho2 = rho2;
            this.alpha1 = alpha1;
            this.alpha2 = alpha2;
            this.u1 = u1;
            this.u2 = u2;
            this.theta1 = theta1;
            this.theta2 = theta2;
            this.z = z;
            this.y = y;
            this.ksi = ksi;
            this.phaseAngleClock = phaseAngleClock;
            this.connected1 = connected1;
            this.connected2 = connected2;
            this.mainComponent1 = mainComponent1;
            this.mainComponent2 = mainComponent2;
            this.validated = validated;
        }

        public String getBranchId() {
            return branchId;
        }

        public double getP1() {
            return p1;
        }

        public double getP1Calc() {
            return p1Calc;
        }

        public double getQ1() {
            return q1;
        }

        public double getQ1Calc() {
            return q1Calc;
        }

        public double getP2() {
            return p2;
        }

        public double getP2Calc() {
            return p2Calc;
        }

        public double getQ2() {
            return q2;
        }

        public double getQ2Calc() {
            return q2Calc;
        }

        public double getR() {
            return r;
        }

        public double getX() {
            return x;
        }

        public double getG1() {
            return g1;
        }

        public double getG2() {
            return g2;
        }

        public double getB1() {
            return b1;
        }

        public double getB2() {
            return b2;
        }

        public double getRho1() {
            return rho1;
        }

        public double getRho2() {
            return rho2;
        }

        public double getAlpha1() {
            return alpha1;
        }

        public double getAlpha2() {
            return alpha2;
        }

        public double getU1() {
            return u1;
        }

        public double getU2() {
            return u2;
        }

        public double getTheta1() {
            return theta1;
        }

        public double getTheta2() {
            return theta2;
        }

        public double getZ() {
            return z;
        }

        public double getY() {
            return y;
        }

        public double getKsi() {
            return ksi;
        }

        public int getPhaseAngleClock() {
            return phaseAngleClock;
        }

        public boolean isConnected1() {
            return connected1;
        }

        public boolean isConnected2() {
            return connected2;
        }

        public boolean isMainComponent1() {
            return mainComponent1;
        }

        public boolean isMainComponent2() {
            return mainComponent2;
        }

        public boolean isValidated() {
            return validated;
        }
    }
}
