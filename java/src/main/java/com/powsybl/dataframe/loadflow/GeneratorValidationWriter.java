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
public class GeneratorValidationWriter extends DefaultInMemoryValidationWriter<GeneratorValidationWriter.ValidationData> {

    @Override
    public void write(String generatorId, double p, double q, double v, double targetP, double targetQ, double targetV, double expectedP, boolean connected, boolean voltageRegulatorOn, double minP, double maxP, double minQ, double maxQ, boolean mainComponent, boolean validated) throws IOException {
        list.add(new ValidationData(generatorId, p));
    }

    public static class ValidationData {
        String id;
        double p;

        ValidationData(String id, double p) {
            this.id = id;
            this.p = p;
        }

        public String getId() {
            return id;
        }

        double getP() {
            return p;
        }
    }
}
