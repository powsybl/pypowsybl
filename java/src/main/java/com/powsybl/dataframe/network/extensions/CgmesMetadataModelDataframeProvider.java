package com.powsybl.dataframe.network.extensions;

import com.google.auto.service.AutoService;

import com.powsybl.cgmes.model.CgmesMetadataModel;
import com.powsybl.dataframe.network.ExtensionInformation;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframeMapperBuilder;
import com.powsybl.dataframe.network.adders.NetworkElementAdder;
import com.powsybl.iidm.network.Network;
import com.powsybl.cgmes.extensions.CgmesMetadataModels;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Naledi El Cheikh <naledi.elcheikh@rte-france.com>
 */

@AutoService(NetworkExtensionDataframeProvider.class)
public class CgmesMetadataModelDataframeProvider extends AbstractSingleDataframeNetworkExtension {
    @Override
    public String getExtensionName() {
        return CgmesMetadataModels.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(CgmesMetadataModels.NAME, "Provides information about CGMES metadata models",
                "index : id (str), cgmes_subset (str), description (str), " +
                        "version (int), modeling_authority_set (str), profiles (str), dependent_on (str), supersedes (str) ");
    }

    private Stream<CgmesMetadataModel> itemsStream(Network network) {
        if (network.getExtension(CgmesMetadataModels.class) == null) {
            return Stream.empty();
        }
        return network.getExtension(CgmesMetadataModels.class).getModels().stream();
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream)
                .stringsIndex("id", CgmesMetadataModel::getId)
                .strings("cgmes_subset", cgmesMetadataModel -> String.valueOf(cgmesMetadataModel.getSubset()))
                .strings("description", CgmesMetadataModel::getDescription)
                .ints("version", CgmesMetadataModel::getVersion)
                .strings("modeling_authority_set", CgmesMetadataModel::getModelingAuthoritySet)
                .strings("profiles", cgmesMetadataModel -> String.join(",", cgmesMetadataModel.getProfiles()))
                .strings("dependent_on", cgmesMetadataModel -> String.join(",", cgmesMetadataModel.getDependentOn()))
                .strings("supersedes", cgmesMetadataModel -> String.join(",", cgmesMetadataModel.getSupersedes()))
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        throw new UnsupportedOperationException("Cannot remove CGMES metadata models");
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new CgmesMetadataModelDataframeAdder();
    }
}
