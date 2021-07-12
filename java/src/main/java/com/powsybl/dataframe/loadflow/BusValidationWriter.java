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
        list.add(new ValidationData(busId, incomingP));
    }

    public static class ValidationData {
        private final String id;
        private final double incomingP;

        ValidationData(String id, double incomingP) {
            this.id = id;
            this.incomingP = incomingP;
        }

        String getId() {
            return id;
        }

        double getIncomingP() {
            return incomingP;
        }
    }
}
