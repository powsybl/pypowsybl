package com.powsybl.dataframe.loadflow.validation;

import com.powsybl.iidm.network.TwoSides;

import java.util.Optional;

/**
 * @author Yichen TANG {@literal <yichen.tang at rte-france.com>}
 */
class TwtValidationData {
    String id;
    double error;
    double upIncrement;
    double downIncrement;
    double rho;
    double rhoPreviousStep;
    double rhoNextStep;
    int tapPosition;
    int lowTapPosition;
    int highTapPosition;
    double targetV;
    TwoSides regulatedSide;
    double v;
    boolean connected;
    boolean mainComponent;
    boolean validated;

    TwtValidationData(String id, double error, double upIncrement, double downIncrement, double rho, double rhoPreviousStep,
                      double rhoNextStep, int tapPosition, int lowTapPosition, int highTapPosition, double targetV,
                      TwoSides regulatedSide, double v, boolean connected, boolean mainComponent, boolean validated) {
        this.id = id;
        this.error = error;
        this.upIncrement = upIncrement;
        this.downIncrement = downIncrement;
        this.rho = rho;
        this.rhoPreviousStep = rhoPreviousStep;
        this.rhoNextStep = rhoNextStep;
        this.tapPosition = tapPosition;
        this.lowTapPosition = lowTapPosition;
        this.highTapPosition = highTapPosition;
        this.targetV = targetV;
        this.regulatedSide = regulatedSide;
        this.v = v;
        this.connected = connected;
        this.mainComponent = mainComponent;
        this.validated = validated;
    }

    String getId() {
        return id;
    }

    double getError() {
        return error;
    }

    double getUpIncrement() {
        return upIncrement;
    }

    double getDownIncrement() {
        return downIncrement;
    }

    double getRho() {
        return rho;
    }

    double getRhoPreviousStep() {
        return rhoPreviousStep;
    }

    double getRhoNextStep() {
        return rhoNextStep;
    }

    int getTapPosition() {
        return tapPosition;
    }

    int getLowTapPosition() {
        return lowTapPosition;
    }

    int getHighTapPosition() {
        return highTapPosition;
    }

    double getTargetV() {
        return targetV;
    }

    Optional<TwoSides> getRegulatedSide() {
        return Optional.ofNullable(regulatedSide);
    }

    double getV() {
        return v;
    }

    boolean isConnected() {
        return connected;
    }

    boolean isMainComponent() {
        return mainComponent;
    }

    boolean isValidated() {
        return validated;
    }
}
