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
public class GeneratorValidationWriter extends DefaultInMemoryValidationWriter<GeneratorValidationWriter.ValidationData> {

    @Override
    public void write(String id, double p, double q, double v, double targetP, double targetQ, double targetV, double expectedP, boolean connected, boolean voltageRegulatorOn, double minP, double maxP, double minQ, double maxQ, boolean mainComponent, boolean validated) throws IOException {
        list.add(new ValidationData(id, p, q, v, targetP, targetQ, targetV, expectedP, connected, voltageRegulatorOn, minP, maxP, minQ, maxQ, mainComponent, validated));
    }

    public static class ValidationData {
        String id;
        double p;
        double q;
        double v;
        double targetP;
        double targetQ;
        double targetV;
        double expectedP;
        boolean connected;
        boolean voltageRegulatorOn;
        double minP;
        double maxP;
        double minQ;
        double maxQ;
        boolean mainComponent;
        boolean validated;

        public ValidationData(String id, double p, double q, double v, double targetP, double targetQ, double targetV, double expectedP, boolean connected, boolean voltageRegulatorOn, double minP, double maxP, double minQ, double maxQ, boolean mainComponent, boolean validated) {
            this.id = id;
            this.p = p;
            this.q = q;
            this.v = v;
            this.targetP = targetP;
            this.targetQ = targetQ;
            this.targetV = targetV;
            this.expectedP = expectedP;
            this.connected = connected;
            this.voltageRegulatorOn = voltageRegulatorOn;
            this.minP = minP;
            this.maxP = maxP;
            this.minQ = minQ;
            this.maxQ = maxQ;
            this.mainComponent = mainComponent;
            this.validated = validated;
        }

        String getId() {
            return id;
        }

        double getP() {
            return p;
        }

        double getQ() {
            return q;
        }

        double getV() {
            return v;
        }

        double getTargetP() {
            return targetP;
        }

        double getTargetQ() {
            return targetQ;
        }

        double getTargetV() {
            return targetV;
        }

        double getExpectedP() {
            return expectedP;
        }

        boolean isConnected() {
            return connected;
        }

        boolean isVoltageRegulatorOn() {
            return voltageRegulatorOn;
        }

        double getMinP() {
            return minP;
        }

        double getMaxP() {
            return maxP;
        }

        double getMinQ() {
            return minQ;
        }

        double getMaxQ() {
            return maxQ;
        }

        boolean isMainComponent() {
            return mainComponent;
        }

        boolean isValidated() {
            return validated;
        }
    }
}
