package com.powsybl.dataframe.loadflow.validation;

/**
 * @author Yichen TANG {@literal <yichen.tang at rte-france.com>}
 */
class ShuntValidationData {
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

    ShuntValidationData(String shuntId, double q, double expectedQ, double p, int currentSectionCount, int maximumSectionCount, double bPerSection, double v, boolean connected, double qMax, double nominalV, boolean mainComponent, boolean validated) {
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
