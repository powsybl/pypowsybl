/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.loadflow;

import com.powsybl.iidm.network.StaticVarCompensator;

import java.io.IOException;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
public class SvcValidationWriter extends DefaultInMemoryValidationWriter<SvcValidationWriter.ValidationData> {

    @Override
    public void write(String svcId, double p, double q, double vControlled, double vController, double nominalVcontroller, double reactivePowerSetpoint, double voltageSetpoint, boolean connected, StaticVarCompensator.RegulationMode regulationMode, double bMin, double bMax, boolean mainComponent, boolean validated) throws IOException {
        list.add(new ValidationData(svcId, p, q, vControlled, vController, nominalVcontroller, reactivePowerSetpoint, voltageSetpoint, connected, regulationMode, bMin, bMax, mainComponent, validated));
    }

    static class ValidationData {
        String svcId;
        double p;
        double q;
        double vControlled;
        double vController;
        double nominalVcontroller;
        double reactivePowerSetpoint;
        double voltageSetpoint;
        boolean connected;
        StaticVarCompensator.RegulationMode regulationMode;
        double bMin;
        double bMax;
        boolean mainComponent;
        boolean validated;

        ValidationData(String svcId, double p, double q, double vControlled, double vController, double nominalVcontroller, double reactivePowerSetpoint, double voltageSetpoint, boolean connected, StaticVarCompensator.RegulationMode regulationMode, double bMin, double bMax, boolean mainComponent, boolean validated) {
            this.svcId = svcId;
            this.p = p;
            this.q = q;
            this.vControlled = vControlled;
            this.vController = vController;
            this.nominalVcontroller = nominalVcontroller;
            this.reactivePowerSetpoint = reactivePowerSetpoint;
            this.voltageSetpoint = voltageSetpoint;
            this.connected = connected;
            this.regulationMode = regulationMode;
            this.bMin = bMin;
            this.bMax = bMax;
            this.mainComponent = mainComponent;
            this.validated = validated;
        }

        String getSvcId() {
            return svcId;
        }

        double getP() {
            return p;
        }

        double getQ() {
            return q;
        }

        double getvControlled() {
            return vControlled;
        }

        double getvController() {
            return vController;
        }

        double getNominalVcontroller() {
            return nominalVcontroller;
        }

        double getReactivePowerSetpoint() {
            return reactivePowerSetpoint;
        }

        double getVoltageSetpoint() {
            return voltageSetpoint;
        }

        boolean isConnected() {
            return connected;
        }

        StaticVarCompensator.RegulationMode getRegulationMode() {
            return regulationMode;
        }

        double getbMin() {
            return bMin;
        }

        double getbMax() {
            return bMax;
        }

        boolean isMainComponent() {
            return mainComponent;
        }

        boolean isValidated() {
            return validated;
        }
    }
}
