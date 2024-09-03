package com.powsybl.dataframe.network.extensions;

import com.powsybl.cgmes.extensions.CgmesMetadataModels;
import com.powsybl.cgmes.extensions.CgmesMetadataModelsAdder;
import com.powsybl.cgmes.model.CgmesMetadataModel;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.AbstractSimpleAdder;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CgmesMetadataModelDataframeAdder extends AbstractSimpleAdder {
    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("subset"),
            SeriesMetadata.strings("description"),
            SeriesMetadata.ints("version"),
            SeriesMetadata.strings("modeling_authority_set")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static class CgmesMetadataSeries {
        private final StringSeries id;
        private final StringSeries subset;
        private final StringSeries description;
        private final IntSeries version;
        private final StringSeries modeling_authority_set;
        private final Map<String, CgmesMetadataModel> models = new HashMap<>();

        private CgmesMetadataSeries(UpdatingDataframe dataframe) {
            this.id = dataframe.getStrings("id");
            this.subset = dataframe.getStrings("subset");
            this.description = dataframe.getStrings("description");
            this.version = dataframe.getInts("version");
            modeling_authority_set = dataframe.getStrings("modeling_authority_set");
        }

        void create(Network network, int row) {
            String elementId = this.id.get(row);
            CgmesMetadataModel cgmesMetadataModel = models.get(elementId);
            var adder = network.getExtension(CgmesMetadataModelsAdder.class);
            SeriesUtils.applyIfPresent(id, row, cgmesMetadataModel::setId);
            SeriesUtils.applyIfPresent(subset, row, type -> cgmesMetadataModel.setProfile(subset.toString()));
            SeriesUtils.applyIfPresent(description, row, side -> cgmesMetadataModel.setDescription(description.toString()));
            SeriesUtils.applyIfPresent(version, row, cgmesMetadataModel::setVersion);
            SeriesUtils.applyIfPresent(modeling_authority_set, row, cgmesMetadataModel::setModelingAuthoritySet);
            adder.add();
        }

    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        CgmesMetadataSeries series = new CgmesMetadataSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(network, row);
        }
    }
}
