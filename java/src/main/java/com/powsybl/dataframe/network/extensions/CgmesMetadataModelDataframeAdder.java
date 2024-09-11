package com.powsybl.dataframe.network.extensions;

import com.powsybl.cgmes.extensions.CgmesMetadataModelsAdder;
import com.powsybl.cgmes.model.CgmesSubset;
import com.powsybl.dataframe.SeriesMetadata;
import com.powsybl.dataframe.network.adders.AbstractSimpleAdder;
import com.powsybl.dataframe.network.adders.SeriesUtils;
import com.powsybl.dataframe.update.IntSeries;
import com.powsybl.dataframe.update.StringSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;

import java.util.*;

/**
 * @author Naledi El Cheikh <naledi.elcheikh@rte-france.com>
 */

public class CgmesMetadataModelDataframeAdder extends AbstractSimpleAdder {
    private static final List<SeriesMetadata> METADATA = List.of(
            SeriesMetadata.stringIndex("id"),
            SeriesMetadata.strings("subset"),
            SeriesMetadata.strings("description"),
            SeriesMetadata.ints("version"),
            SeriesMetadata.strings("modelingAuthoritySet"),
            SeriesMetadata.strings("profiles"),
            SeriesMetadata.strings("dependentOn"),
            SeriesMetadata.strings("supersedes")
    );

    @Override
    public List<List<SeriesMetadata>> getMetadata() {
        return Collections.singletonList(METADATA);
    }

    private static final class CgmesMetadataSeries {
        private final StringSeries id;
        private final StringSeries subset;
        private final StringSeries description;
        private final IntSeries version;
        private final StringSeries modelingAuthoritySet;
        private final StringSeries profiles;
        private final StringSeries dependentOn;
        private final StringSeries supersedes;

        private CgmesMetadataSeries(UpdatingDataframe dataframe) {
            this.id = dataframe.getStrings("id");
            this.subset = dataframe.getStrings("subset");
            this.description = dataframe.getStrings("description");
            this.version = dataframe.getInts("version");
            this.modelingAuthoritySet = dataframe.getStrings("modelingAuthoritySet");
            this.profiles = dataframe.getStrings("profiles");
            dependentOn = dataframe.getStrings("dependentOn");
            this.supersedes = dataframe.getStrings("supersedes");
        }

        void create(int row, CgmesMetadataModelsAdder adder) {
            CgmesMetadataModelsAdder.ModelAdder modelAdder = adder.newModel();
            SeriesUtils.applyIfPresent(id, row, modelAdder::setId);
            SeriesUtils.applyIfPresent(subset, row, subset -> modelAdder.setSubset(CgmesSubset.valueOf(subset)));
            SeriesUtils.applyIfPresent(description, row, modelAdder::setDescription);
            SeriesUtils.applyIfPresent(version, row, modelAdder::setVersion);
            SeriesUtils.applyIfPresent(modelingAuthoritySet, row, modelAdder::setModelingAuthoritySet);
            SeriesUtils.applyIfPresent(profiles, row, modelAdder::addProfile);
            SeriesUtils.applyIfPresent(dependentOn, row, modelAdder::addDependentOn);
            SeriesUtils.applyIfPresent(supersedes, row, modelAdder::addSupersedes);
            modelAdder.add();
        }

    }

    @Override
    public void addElements(Network network, UpdatingDataframe dataframe) {
        CgmesMetadataModelsAdder adder = network.newExtension(CgmesMetadataModelsAdder.class);
        CgmesMetadataSeries series = new CgmesMetadataSeries(dataframe);
        for (int row = 0; row < dataframe.getRowCount(); row++) {
            series.create(row, adder);
        }
        adder.add();
    }
}
