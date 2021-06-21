/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.dataframe.SeriesDataType;

import java.util.Map;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
public class EquipmentAdderFieldsHelper {

    private static final Map<String, SeriesDataType> LOAD_MAPS = Map.of(
                "connectable_bus_id", SeriesDataType.STRING,
                "voltage_level_id", SeriesDataType.STRING
            );

    static int getAdderSeriesType(PyPowsyblApiHeader.ElementType type, String fieldName) {
        SeriesDataType seriesDataType;
        switch (type) {
            case LOAD:
                seriesDataType = LOAD_MAPS.get(fieldName);
                break;
            default:
                throw new RuntimeException("Unexpected " + type + " adder.");
        }
        if (seriesDataType == null) {
            throw new RuntimeException("Field '" + fieldName + "' not found for " + type + " adder.");
        }
        return CDataframeHandler.convert(seriesDataType);
    }
}
