/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.loadflow;

import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
public final class Validations {

    public static DataframeMapper<DefaultInMemoryValidationWriter<BusValidationWriter.ValidationData>> busValidationsMapper() {
        return new DataframeMapperBuilder<DefaultInMemoryValidationWriter<BusValidationWriter.ValidationData>, BusValidationWriter.ValidationData>()
                .itemsProvider(DefaultInMemoryValidationWriter::getList)
                .stringsIndex("id", BusValidationWriter.ValidationData::getId)
                .doubles("incoming_p", BusValidationWriter.ValidationData::getIncomingP)
                .build();
    }

    public static DataframeMapper<DefaultInMemoryValidationWriter<GeneratorValidationWriter.ValidationData>> generatorValidationsMapper() {
        return new DataframeMapperBuilder<DefaultInMemoryValidationWriter<GeneratorValidationWriter.ValidationData>, GeneratorValidationWriter.ValidationData>()
                .itemsProvider(DefaultInMemoryValidationWriter::getList)
                .stringsIndex("id", GeneratorValidationWriter.ValidationData::getId)
                .doubles("p", GeneratorValidationWriter.ValidationData::getP)
                .build();
    }

    private Validations() {
    }
}
