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
public class ShuntValidationWriter extends DefaultInMemoryValidationWriter<ShuntValidationWriter.ValidationData> {

    @Override
    public void write(String shuntId, double q, double expectedQ, double p, int currentSectionCount, int maximumSectionCount, double bPerSection, double v, boolean connected, double qMax, double nominalV, boolean mainComponent, boolean validated) throws IOException {
        list.add(new ValidationData(shuntId, q, expectedQ, p, currentSectionCount, maximumSectionCount, bPerSection, v, connected, qMax, nominalV, mainComponent, validated));
    }

    static class ValidationData {
        String shuntId;
        double q;
        double expectedQ;
        double p;
        int currentSectionCount;
        int maximumSectionCount;
        double bPerSection;
        double v;
        boolean connected;
        double qMax;
        double nominalV;
        boolean mainComponent;
        boolean validated;

        ValidationData(String shuntId, double q, double expectedQ, double p, int currentSectionCount, int maximumSectionCount, double bPerSection, double v, boolean connected, double qMax, double nominalV, boolean mainComponent, boolean validated) {
            this.shuntId = shuntId;
            this.q = q;
            this.expectedQ = expectedQ;
            this.p = p;
            this.currentSectionCount = currentSectionCount;
            this.maximumSectionCount = maximumSectionCount;
            this.bPerSection = bPerSection;
            this.v = v;
            this.connected = connected;
            this.qMax = qMax;
            this.nominalV = nominalV;
            this.mainComponent = mainComponent;
            this.validated = validated;
        }

        String getShuntId() {
            return shuntId;
        }

        double getQ() {
            return q;
        }

        double getExpectedQ() {
            return expectedQ;
        }

        double getP() {
            return p;
        }

        int getCurrentSectionCount() {
            return currentSectionCount;
        }

        int getMaximumSectionCount() {
            return maximumSectionCount;
        }

        double getbPerSection() {
            return bPerSection;
        }

        double getV() {
            return v;
        }

        boolean isConnected() {
            return connected;
        }

        double getqMax() {
            return qMax;
        }

        double getNominalV() {
            return nominalV;
        }

        boolean isMainComponent() {
            return mainComponent;
        }

        boolean isValidated() {
            return validated;
        }
    }
}
