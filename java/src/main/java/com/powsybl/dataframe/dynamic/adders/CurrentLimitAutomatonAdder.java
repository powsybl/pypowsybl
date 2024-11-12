/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.dynamic.adders;

import java.util.List;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.Util;
import com.powsybl.python.dynamic.PythonDynamicModelsSupplier;

/**
 * @author Nicolas Pierre {@literal <nicolas.pierre@artelys.com>}
 */
public class CurrentLimitAutomatonAdder implements DynamicMappingAdder {
    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("static_id"),
            SeriesMetadata.strings("parameter_set_id"),
            SeriesMetadata.ints("branch_side"));

    @Override
    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    private static final class CurrentLimitAutomatonSeries {

        private final StringSeries staticId;
        private final StringSeries parameterSetId;
        private final IntSeries branchSide;

        CurrentLimitAutomatonSeries(UpdatingDataframe dataframe) {
            this.staticId = SeriesUtils.getRequiredStrings(dataframe, "static_id");
            this.parameterSetId = SeriesUtils.getRequiredStrings(dataframe, "parameter_set_id");
            this.branchSide = SeriesUtils.getRequiredInts(dataframe, "branch_side");
        }

        public StringSeries getStaticId() {
            return staticId;
        }

        public StringSeries getParameterSetId() {
            return parameterSetId;
        }

        public IntSeries getBranchSide() {
            return branchSide;
        }

    }

    @Override
    public void addElements(PythonDynamicModelsSupplier modelMapping, UpdatingDataframe dataframe) {
        CurrentLimitAutomatonSeries series = new CurrentLimitAutomatonSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            modelMapping.addCurrentLimitAutomaton(
                    series.getStaticId().get(row),
                    series.getParameterSetId().get(row),
                    Util.convert(PyPowsyblApiHeader.ThreeSideType.fromCValue(series.getBranchSide().get(row))).toTwoSides());
        }
    }

}
