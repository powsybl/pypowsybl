/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.loadflow;

import com.powsybl.iidm.network.util.TwtData;

import java.io.IOException;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
public class Twt3wValidationWriter extends DefaultInMemoryValidationWriter<Twt3wValidationWriter.ValidationData> {

    @Override
    public void write(String twtId, TwtData twtData, boolean validated) throws IOException {
        list.add(new ValidationData(twtId, twtData, validated));
    }

    static class ValidationData {
        String id;
        TwtData twtData;
        boolean validated;

        ValidationData(String id, TwtData twtData, boolean validated) {
            this.id = id;
            this.twtData = twtData;
            this.validated = validated;
        }

        String getId() {
            return id;
        }

        TwtData getTwtData() {
            return twtData;
        }

        boolean isValidated() {
            return validated;
        }
    }
}
