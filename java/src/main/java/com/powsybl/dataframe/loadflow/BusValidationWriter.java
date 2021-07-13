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
public class BusValidationWriter extends DefaultInMemoryValidationWriter<BusValidationWriter.ValidationData> {

    @Override
    public void write(String busId, double incomingP, double incomingQ, double loadP, double loadQ, double genP, double genQ, double batP, double batQ, double shuntP, double shuntQ, double svcP, double svcQ, double vscCSP, double vscCSQ, double lineP, double lineQ, double danglingLineP, double danglingLineQ, double twtP, double twtQ, double tltP, double tltQ, boolean mainComponent, boolean validated) throws IOException {
        list.add(new ValidationData(busId, incomingP, incomingQ, loadP, loadQ, genP, genQ, batP, batQ, shuntP, shuntQ, svcP, svcQ, vscCSP, vscCSQ, lineP, lineQ, danglingLineP, danglingLineQ, twtP, twtQ, tltP, tltQ, mainComponent, validated));
    }

    public static class ValidationData {
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

        public ValidationData(String id, double incomingP, double incomingQ, double loadP, double loadQ, double genP, double genQ, double batP, double batQ, double shuntP, double shuntQ, double svcP, double svcQ, double vscCSP, double vscCSQ, double lineP, double lineQ, double danglingLineP, double danglingLineQ, double twtP, double twtQ, double tltP, double tltQ, boolean mainComponent, boolean validated) {
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
}
