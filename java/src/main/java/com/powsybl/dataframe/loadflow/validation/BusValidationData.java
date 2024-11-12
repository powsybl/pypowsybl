package com.powsybl.dataframe.loadflow.validation;

/**
 * @author Yichen TANG {@literal <yichen.tang at rte-france.com>}
 */
public class BusValidationData {
    String id;
    double incomingP;
    double incomingQ;
    double loadP;
    double loadQ;
    double genP;
    double genQ;
    double batP;
    double batQ;
    double shuntP;
    double shuntQ;
    double svcP;
    double svcQ;
    double vscCSP;
    double vscCSQ;
    double lineP;
    double lineQ;
    double danglingLineP;
    double danglingLineQ;
    double twtP;
    double twtQ;
    double tltP;
    double tltQ;
    boolean mainComponent;
    boolean validated;

    public BusValidationData(String id, double incomingP, double incomingQ, double loadP, double loadQ, double genP, double genQ, double batP, double batQ, double shuntP, double shuntQ, double svcP, double svcQ, double vscCSP, double vscCSQ, double lineP, double lineQ, double danglingLineP, double danglingLineQ, double twtP, double twtQ, double tltP, double tltQ, boolean mainComponent, boolean validated) {
        this.id = id;
        this.incomingP = incomingP;
        this.incomingQ = incomingQ;
        this.loadP = loadP;
        this.loadQ = loadQ;
        this.genP = genP;
        this.genQ = genQ;
        this.batP = batP;
        this.batQ = batQ;
        this.shuntP = shuntP;
        this.shuntQ = shuntQ;
        this.svcP = svcP;
        this.svcQ = svcQ;
        this.vscCSP = vscCSP;
        this.vscCSQ = vscCSQ;
        this.lineP = lineP;
        this.lineQ = lineQ;
        this.danglingLineP = danglingLineP;
        this.danglingLineQ = danglingLineQ;
        this.twtP = twtP;
        this.twtQ = twtQ;
        this.tltP = tltP;
        this.tltQ = tltQ;
        this.mainComponent = mainComponent;
        this.validated = validated;
    }

    String getId() {
        return id;
    }

    double getIncomingP() {
        return incomingP;
    }

    double getIncomingQ() {
        return incomingQ;
    }

    double getLoadP() {
        return loadP;
    }

    double getLoadQ() {
        return loadQ;
    }

    double getGenP() {
        return genP;
    }

    double getGenQ() {
        return genQ;
    }

    double getBatP() {
        return batP;
    }

    double getBatQ() {
        return batQ;
    }

    double getShuntP() {
        return shuntP;
    }

    double getShuntQ() {
        return shuntQ;
    }

    double getSvcP() {
        return svcP;
    }

    double getSvcQ() {
        return svcQ;
    }

    double getVscCSP() {
        return vscCSP;
    }

    double getVscCSQ() {
        return vscCSQ;
    }

    double getLineP() {
        return lineP;
    }

    double getLineQ() {
        return lineQ;
    }

    double getDanglingLineP() {
        return danglingLineP;
    }

    double getDanglingLineQ() {
        return danglingLineQ;
    }

    double getTwtP() {
        return twtP;
    }

    double getTwtQ() {
        return twtQ;
    }

    double getTltP() {
        return tltP;
    }

    double getTltQ() {
        return tltQ;
    }

    boolean isMainComponent() {
        return mainComponent;
    }

    boolean isValidated() {
        return validated;
    }
}
