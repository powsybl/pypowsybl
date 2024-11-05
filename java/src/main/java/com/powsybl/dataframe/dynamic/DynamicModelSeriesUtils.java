/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public final class DynamicModelSeriesUtils {

    private DynamicModelSeriesUtils(){
    }

    public static Map<String, List<String>> createIdMap(UpdatingDataframe dataframe, String indexColumn, String idColumn) {
        StringSeries joinIds = dataframe.getStrings(indexColumn);
        if (joinIds == null) {
            throw new PowsyblException("Join dataframe: %s column is not set".formatted(indexColumn));
        }
        StringSeries ids = dataframe.getStrings(idColumn);
        if (ids == null) {
            throw new PowsyblException("Join dataframe: %s column is not set".formatted(idColumn));
        }
        Map<String, List<String>> idMap = new HashMap<>();
        for (int index = 0; index < dataframe.getRowCount(); index++) {
            String joinId = joinIds.get(index);
            idMap.computeIfAbsent(joinId, k -> new ArrayList<>())
                    .add(ids.get(index));
        }
        return idMap;
    }

    public static void applyIfPresent(Map<String, List<String>> idMap, String id, Consumer<List<String>> consumer) {
        List<String> idList = idMap.get(id);
        if (idList != null) {
            consumer.accept(idList);
        }
    }
}
