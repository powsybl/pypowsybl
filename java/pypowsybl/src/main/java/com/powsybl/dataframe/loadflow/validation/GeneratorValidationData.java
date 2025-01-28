package com.powsybl.dataframe.loadflow.validation;

/**
 * @author Yichen TANG {@literal <yichen.tang at rte-france.com>}
 */
public class GeneratorValidationData {
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

    public GeneratorValidationData(String id, double p, double q, double v, double targetP, double targetQ, double targetV, double expectedP, boolean connected, boolean voltageRegulatorOn, double minP, double maxP, double minQ, double maxQ, boolean mainComponent, boolean validated) {
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
