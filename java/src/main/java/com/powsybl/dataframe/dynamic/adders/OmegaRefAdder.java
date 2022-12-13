package com.powsybl.dataframe.dynamic.adders;

import java.util.Collections;
import java.util.List;

import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.python.dynamic.DynamicModelMapper;

public class OmegaRefAdder implements DynamicMappingAdder {
    private static final List<SeriesMetadata> METADATA = Collections.singletonList(
            SeriesMetadata.stringIndex("generator_id"));

    @Override
    public List<SeriesMetadata> getMetadata() {
        return METADATA;
    }

    @Override
    public void addElements(DynamicModelMapper modelMapping, UpdatingDataframe dataframe) {
        StringSeries genIdSeries = SeriesUtils.getRequiredStrings(dataframe, "generator_id");
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            modelMapping.addOmegaRef(genIdSeries.get(row));
        }
    }

}
