/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.loadflow;

import com.powsybl.iidm.network.Branch;

import java.io.IOException;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
public class TwtValidationWriter extends DefaultInMemoryValidationWriter<TwtValidationWriter.ValidationData> {

    @Override
    public void write(String twtId, double error, double upIncrement, double downIncrement, double rho, double rhoPreviousStep, double rhoNextStep, int tapPosition, int lowTapPosition, int highTapPosition, double targetV, Branch.Side regulatedSide, double v, boolean connected, boolean mainComponent, boolean validated) throws IOException {
        list.add(new ValidationData(twtId, error, upIncrement, downIncrement, rho, rhoPreviousStep, rhoNextStep, tapPosition, lowTapPosition, highTapPosition, targetV, regulatedSide, v, connected, mainComponent, validated));
    }

    static class ValidationData {
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
        Branch.Side regulatedSide;
        double v;
        boolean connected;
        boolean mainComponent;
        boolean validated;

        ValidationData(String id, double error, double upIncrement, double downIncrement, double rho, double rhoPreviousStep, double rhoNextStep, int tapPosition, int lowTapPosition, int highTapPosition, double targetV, Branch.Side regulatedSide, double v, boolean connected, boolean mainComponent, boolean validated) {
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

        Branch.Side getRegulatedSide() {
            return regulatedSide;
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
}
