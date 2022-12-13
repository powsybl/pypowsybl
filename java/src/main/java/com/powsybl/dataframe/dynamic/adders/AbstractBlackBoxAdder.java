package com.powsybl.dataframe.dynamic.adders;

import java.util.List;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.python.dynamic.DynamicModelMapper;

public abstract class AbstractBlackBoxAdder implements DynamicMappingAdder {
    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("static_id"),
            SeriesMetadata.strings("parameter_set_id"));

    @Override
    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    private static final class BlackBoxSeries {

        private final StringSeries staticId;
        private final StringSeries parameterSetId;

        BlackBoxSeries(UpdatingDataframe dataframe) {
            this.staticId = SeriesUtils.getRequiredStrings(dataframe, "static_id");
            this.parameterSetId = SeriesUtils.getRequiredStrings(dataframe, "parameter_set_id");
        }

        public StringSeries getStaticId() {
            return staticId;
        }

        public StringSeries getParameterSetId() {
            return parameterSetId;
        }

    }

    @FunctionalInterface
    protected interface AddBlackBoxToModelMapping {
        void addToModel(DynamicModelMapper modelMapping, String staticId, String parameterSetId);
    }

    protected abstract AddBlackBoxToModelMapping getAddBlackBoxToModelMapping();

    @Override
    public void addElements(DynamicModelMapper modelMapping, UpdatingDataframe dataframe) {
        BlackBoxSeries series = new BlackBoxSeries(dataframe);
        AddBlackBoxToModelMapping adder = getAddBlackBoxToModelMapping();
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            adder.addToModel(modelMapping, series.getStaticId().get(row), series.getParameterSetId().get(row));
        }
    }

}
