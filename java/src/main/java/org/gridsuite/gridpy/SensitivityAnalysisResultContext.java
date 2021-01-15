/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.gridpy;

import com.powsybl.sensitivity.SensitivityValue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class SensitivityAnalysisResultContext {

    private final int rowCount;

    private final int columnCount;

    private final Collection<SensitivityValue> sensitivityValues;

    private final Map<String, List<SensitivityValue>> sensitivityValuesByContingencyId;

    SensitivityAnalysisResultContext(int rowCount, int columnCount, Collection<SensitivityValue> sensitivityValues,
                                     Map<String, List<SensitivityValue>> sensitivityValuesByContingencyId) {
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        this.sensitivityValues = sensitivityValues;
        this.sensitivityValuesByContingencyId = sensitivityValuesByContingencyId;
    }

    int getRowCount() {
        return rowCount;
    }

    int getColumnCount() {
        return columnCount;
    }

    Collection<SensitivityValue> getSensitivityValues(String contingencyId) {
        return contingencyId.isEmpty() ? sensitivityValues : sensitivityValuesByContingencyId.get(contingencyId);
    }
}
