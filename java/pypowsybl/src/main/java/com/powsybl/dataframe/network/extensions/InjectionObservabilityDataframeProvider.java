/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dataframe.network.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.network.ExtensionInformation;
import com.powsybl.dataframe.network.NetworkDataframeMapper;
import com.powsybl.dataframe.network.NetworkDataframeMapperBuilder;
import com.powsybl.dataframe.network.adders.NetworkElementAdder;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.InjectionObservability;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Etienne Lesot {@literal <etienne.lesot@rte-france.com>}
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class InjectionObservabilityDataframeProvider extends AbstractSingleDataframeNetworkExtension {

    private Stream<InjectionObservability> itemsStream(Network network) {
        return network.getIdentifiables().stream().filter(Objects::nonNull)
                .filter(identifiable -> identifiable instanceof Injection)
                .map(inj -> (InjectionObservability) inj.getExtension(InjectionObservability.class))
                .filter(Objects::nonNull);
    }

    private InjectionObservability getOrThrow(Network network, String id) {
        Identifiable identifiable = network.getIdentifiable(id);
        if (identifiable == null) {
            throw new PowsyblException("Invalid injection id : could not find " + id);
        }
        if (!(identifiable instanceof Injection)) {
            throw new PowsyblException(id + " is not an injection");
        }
        Injection injection = (Injection) identifiable;
        return (InjectionObservability) injection.getExtension(InjectionObservability.class);
    }

    @Override
    public String getExtensionName() {
        return InjectionObservability.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(InjectionObservability.NAME, "Provides information about the observability of a injection",
                "index : id (str), observable (bool), p_standard_deviation (float), p_redundant (bool), " +
                        "q_standard_deviation (float), q_redundant (bool), v_standard_deviation (float), v_redundant (bool)");
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", injectionObservability -> ((Injection) injectionObservability.getExtendable()).getId())
                .booleans("observable", InjectionObservability::isObservable)
                .doubles("p_standard_deviation", (injectionObservability, context) -> injectionObservability.getQualityP().isPresent() ?
                                injectionObservability.getNullableQualityP().getStandardDeviation() : Double.NaN,
                    (injectionObservability, standardDeviation, context) -> {
                        if (injectionObservability.getQualityP().isPresent()) {
                            injectionObservability.getNullableQualityP().setStandardDeviation(standardDeviation);
                        } else {
                            injectionObservability.setQualityP(standardDeviation);
                        }
                    })
                .booleans("p_redundant", injectionObservability -> injectionObservability.getQualityP().isPresent() &&
                                (boolean) injectionObservability.getNullableQualityP().isRedundant().orElse(false),
                    (injectionObservability, redundant) -> injectionObservability.getNullableQualityP().setRedundant(redundant))
                .booleans("p_redundant_null", injectionObservability -> injectionObservability.getQualityP().isEmpty() ||
                        injectionObservability.getNullableQualityP().isRedundant().isEmpty())
                .doubles("q_standard_deviation", (injectionObservability, context) -> injectionObservability.getQualityQ().isPresent() ?
                                injectionObservability.getNullableQualityQ().getStandardDeviation() : Double.NaN,
                    (injectionObservability, standardDeviation, context) -> {
                        if (injectionObservability.getQualityQ().isPresent()) {
                            injectionObservability.getNullableQualityQ().setStandardDeviation(standardDeviation);
                        } else {
                            injectionObservability.setQualityQ(standardDeviation);
                        }
                    })
                .booleans("q_redundant", injectionObservability -> injectionObservability.getQualityQ().isPresent() &&
                                (boolean) injectionObservability.getNullableQualityQ().isRedundant().orElse(false),
                    (injectionObservability, redundant) -> injectionObservability.getNullableQualityQ().setRedundant(redundant))
                .booleans("q_redundant_null", injectionObservability -> injectionObservability.getQualityQ().isEmpty() ||
                        injectionObservability.getNullableQualityQ().isRedundant().isEmpty())
                .doubles("v_standard_deviation", (injectionObservability, context) -> injectionObservability.getQualityV().isPresent() ?
                                injectionObservability.getNullableQualityV().getStandardDeviation() : Double.NaN,
                    (injectionObservability, standardDeviation, context) -> {
                        if (injectionObservability.getQualityV().isPresent()) {
                            injectionObservability.getNullableQualityV().setStandardDeviation(standardDeviation);
                        } else {
                            injectionObservability.setQualityV(standardDeviation);
                        }
                    })
                .booleans("v_redundant", injectionObservability -> injectionObservability.getQualityV().isPresent() &&
                                (boolean) injectionObservability.getNullableQualityV().isRedundant().orElse(false),
                    (injectionObservability, redundant) -> injectionObservability.getNullableQualityV().setRedundant(redundant))
                .booleans("v_redundant_null", injectionObservability -> injectionObservability.getQualityV().isEmpty() ||
                        injectionObservability.getNullableQualityV().isRedundant().isEmpty())
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(network::getIdentifiable)
                .filter(Objects::nonNull)
                .filter(identifiable -> identifiable instanceof Injection)
                .forEach(inj -> inj.removeExtension(InjectionObservability.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new InjectionObservabilityDataframeAdder();
    }
}
