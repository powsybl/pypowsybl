package com.powsybl.dataframe.network.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.network.ExtensionInformation;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframeMapperBuilder;
import com.powsybl.dataframe.network.adders.NetworkElementAdder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.extensions.TwoWindingsTransformerPhaseAngleClock;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@AutoService(NetworkExtensionDataframeProvider.class)
public class TwoWindingsTransformerPhaseAngleClockDataframeProvider extends AbstractSingleDataframeNetworkExtension {

    @Override
    public String getExtensionName() {
        return TwoWindingsTransformerPhaseAngleClock.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(TwoWindingsTransformerPhaseAngleClock.NAME,
                "Provides phase angle clock information for two windings transformers",
                "index : id (str), phase_angle_clock (int)");
    }

    private Stream<TwoWindingsTransformerPhaseAngleClock> itemsStream(Network network) {
        return network.getTwoWindingsTransformerStream()
                .map(transformer -> (TwoWindingsTransformerPhaseAngleClock) transformer.getExtension(TwoWindingsTransformerPhaseAngleClock.class))
                .filter(Objects::nonNull);
    }

    private TwoWindingsTransformerPhaseAngleClock getOrThrow(Network network, String id) {
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(id);
        if (transformer == null) {
            throw new PowsyblException("Two windings transformer '" + id + "' does not exist.");
        }
        TwoWindingsTransformerPhaseAngleClock extension = transformer.getExtension(TwoWindingsTransformerPhaseAngleClock.class);
        if (extension == null) {
            throw new PowsyblException("Two windings transformer '" + id + "' has no phase angle clock extension");
        }
        return extension;
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", extension -> extension.getExtendable().getId())
                .ints("phase_angle_clock",
                        TwoWindingsTransformerPhaseAngleClock::getPhaseAngleClock,
                        TwoWindingsTransformerPhaseAngleClock::setPhaseAngleClock)
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream()
                .filter(Objects::nonNull)
                .map(network::getTwoWindingsTransformer)
                .filter(Objects::nonNull)
                .forEach(transformer -> transformer.removeExtension(TwoWindingsTransformerPhaseAngleClock.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new TwoWindingsTransformerPhaseAngleClockDataframeAdder();
    }
}
