/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.iidm.import_.Importer;
import com.powsybl.iidm.parameters.Parameter;
import com.powsybl.iidm.parameters.ParameterType;
import com.powsybl.python.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.PyPowsyblApiHeader.SeriesPointer;

import java.util.Objects;

/**
 * Mappers to dataframes.
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public final class Dataframes {

    private static final DataframeMapper<Importer> PARAMETERS_MAPPER = new DataframeMapperBuilder<Importer, Parameter>()
        .itemsProvider(Importer::getParameters)
        .stringsIndex("name", Parameter::getName)
        .strings("description", Parameter::getDescription)
        .enums("type", ParameterType.class, Parameter::getType)
        .strings("default", p -> Objects.toString(p.getDefaultValue(), ""))
        .build();

    private Dataframes() {
    }

    /**
     * Maps an object to a C struct using the provided mapper.
     */
    public static <T> ArrayPointer<SeriesPointer> createCDataframe(DataframeMapper<T> mapper, T object) {
        CDataframeHandler handler = new CDataframeHandler();
        mapper.createDataframe(object, handler);
        return handler.getDataframePtr();
    }

    /**
     * A mapper which maps an importer to a dataframe containing its parameters.
     */
    public static DataframeMapper<Importer> parametersMapper() {
        return PARAMETERS_MAPPER;
    }
}
