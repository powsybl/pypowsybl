package com.powsybl.dataframe.dynamic.adders;

import java.util.List;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.Util;
import com.powsybl.python.dynamic.DynamicModelMapper;

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
    public void addElements(DynamicModelMapper modelMapping, UpdatingDataframe dataframe) {
        CurrentLimitAutomatonSeries series = new CurrentLimitAutomatonSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            modelMapping.addCurrentLimitAutomaton(
                    series.getStaticId().get(row),
                    series.getParameterSetId().get(row),
                    Util.convert(PyPowsyblApiHeader.BranchSide.fromCValue(series.getBranchSide().get(row))));
        }
    }

}
