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

        public double getIncomingQ() {
            return incomingQ;
        }

        public double getLoadP() {
            return loadP;
        }

        public double getLoadQ() {
            return loadQ;
        }

        public double getGenP() {
            return genP;
        }

        public double getGenQ() {
            return genQ;
        }

        public double getBatP() {
            return batP;
        }

        public double getBatQ() {
            return batQ;
        }

        public double getShuntP() {
            return shuntP;
        }

        public double getShuntQ() {
            return shuntQ;
        }

        public double getSvcP() {
            return svcP;
        }

        public double getSvcQ() {
            return svcQ;
        }

        public double getVscCSP() {
            return vscCSP;
        }

        public double getVscCSQ() {
            return vscCSQ;
        }

        public double getLineP() {
            return lineP;
        }

        public double getLineQ() {
            return lineQ;
        }

        public double getDanglingLineP() {
            return danglingLineP;
        }

        public double getDanglingLineQ() {
            return danglingLineQ;
        }

        public double getTwtP() {
            return twtP;
        }

        public double getTwtQ() {
            return twtQ;
        }

        public double getTltP() {
            return tltP;
        }

        public double getTltQ() {
            return tltQ;
        }

        public boolean isMainComponent() {
            return mainComponent;
        }

        public boolean isValidated() {
            return validated;
        }
    }
}
