package com.powsybl.dataframe.loadflow.validation;

import com.powsybl.iidm.network.StaticVarCompensator;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
class SvcValidationData {
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

    SvcValidationData(String svcId, double p, double q, double vControlled, double vController, double nominalVcontroller, double reactivePowerSetpoint, double voltageSetpoint, boolean connected, StaticVarCompensator.RegulationMode regulationMode, double bMin, double bMax, boolean mainComponent, boolean validated) {
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
