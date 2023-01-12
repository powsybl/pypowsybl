package com.powsybl.dataframe.network.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.network.ExtensionInformation;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframeMapperBuilder;
import com.powsybl.dataframe.network.adders.NetworkElementAdder;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.CoordinatedReactiveControl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@AutoService(NetworkExtensionDataframeProvider.class)
public class CoordinatedReactiveControlDataframeProvider implements NetworkExtensionDataframeProvider {
    @Override
    public String getExtensionName() {
        return CoordinatedReactiveControl.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(CoordinatedReactiveControl.NAME, "it allow to specify the percent of " +
                "the coordinated reactive control that comes from a generator",
                "index : generator_id (str), q_percent (float)");
    }

    private Stream<CoordinatedReactiveControl> itemsStream(Network network) {
        return network.getGeneratorStream().filter(Objects::nonNull)
                .map(generator -> (CoordinatedReactiveControl) generator.getExtension(CoordinatedReactiveControl.class))
                .filter(Objects::nonNull);
    }

    private CoordinatedReactiveControl getOrThrow(Network network, String id) {
        Generator generator = network.getGenerator(id);
        if (generator == null) {
            throw new PowsyblException("Invalid generator id : could not find " + id);
        }
        return generator.getExtension(CoordinatedReactiveControl.class);
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("generator_id", coordinatedReactiveControl -> coordinatedReactiveControl.getExtendable().getId())
                .doubles("q_percent", CoordinatedReactiveControl::getQPercent, CoordinatedReactiveControl::setQPercent)
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(network::getGenerator)
                .filter(Objects::nonNull)
                .forEach(generator -> generator.removeExtension(CoordinatedReactiveControl.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new CoordinatedReactiveControlDataframeAdder();
    }
}
