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
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.BranchObservability;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Etienne Lesot {@literal <etienne.lesot@rte-france.com>}
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class BranchObservabilityDataframeProvider extends AbstractSingleDataframeNetworkExtension {

    private Stream<BranchObservability> itemsStream(Network network) {
        return network.getBranchStream().filter(Objects::nonNull)
                .map(branch -> (BranchObservability) branch.getExtension(BranchObservability.class))
                .filter(Objects::nonNull);
    }

    private BranchObservability<?> getOrThrow(Network network, String id) {
        Branch<?> branch = network.getBranch(id);
        if (branch == null) {
            throw new PowsyblException("Invalid branch id : could not find " + id);
        }
        return (BranchObservability) branch.getExtension(BranchObservability.class);
    }

    @Override
    public String getExtensionName() {
        return BranchObservability.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(BranchObservability.NAME,
                "Provides information about the observability of a branch",
                "index : id (str), observable (bool), p1_standard_deviation (float), p1_redundant (bool), p2_standard_deviation (float), p2_redundant (bool), "
                        + "q1_standard_deviation (float), q1_redundant (bool), q2_standard_deviation (float), q2_redundant (bool)");
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", branchObservability -> ((Branch<?>) branchObservability.getExtendable()).getId())
                .booleans("observable", BranchObservability::isObservable)
                .doubles("p1_standard_deviation", (branchObservability, context) -> branchObservability.getQualityP1().isPresent() ?
                                branchObservability.getNullableQualityP1().getStandardDeviation() : Double.NaN,
                    (branchObservability, standardDeviation, context) -> branchObservability.getNullableQualityP1().setStandardDeviation(standardDeviation))
                .booleans("p1_redundant", branchObservability -> branchObservability.getQualityP1().isPresent() &&
                                (boolean) branchObservability.getNullableQualityQ1().isRedundant().orElse(false),
                    (branchObservability, redundant) -> branchObservability.getNullableQualityQ1().setRedundant(redundant))
                .booleans("p1_redundant_null", branchObservability -> branchObservability.getQualityP1().isEmpty() || branchObservability.getNullableQualityQ1().isRedundant().isEmpty())
                .doubles("p2_standard_deviation", (branchObservability, context) -> branchObservability.getQualityP2().isPresent() ?
                                branchObservability.getNullableQualityP2().getStandardDeviation() : Double.NaN,
                    (branchObservability, standardDeviation, context) -> branchObservability.getNullableQualityP2().setStandardDeviation(standardDeviation))
                .booleans("p2_redundant", branchObservability -> branchObservability.getQualityP2().isPresent() &&
                                (boolean) branchObservability.getNullableQualityP2().isRedundant().orElse(false),
                    (branchObservability, redundant) -> branchObservability.getNullableQualityP2().setRedundant(redundant))
                .booleans("p2_redundant_null", branchObservability -> branchObservability.getQualityP2().isEmpty() ||
                        branchObservability.getNullableQualityP2().isRedundant().isEmpty())
                .doubles("q1_standard_deviation", (branchObservability, context) -> branchObservability.getQualityQ1().isPresent() ?
                                branchObservability.getNullableQualityQ1().getStandardDeviation() : Double.NaN,
                    (branchObservability, standardDeviation, context) -> branchObservability.getNullableQualityQ1().setStandardDeviation(standardDeviation))
                .booleans("q1_redundant", branchObservability -> branchObservability.getQualityQ1().isPresent() &&
                                (boolean) branchObservability.getNullableQualityQ1().isRedundant().orElse(false),
                    (branchObservability, redundant) -> branchObservability.getNullableQualityQ1().setRedundant(redundant))
                .booleans("q1_redundant_null", branchObservability -> branchObservability.getQualityQ1().isEmpty() ||
                        branchObservability.getNullableQualityQ1().isRedundant().isEmpty())
                .doubles("q2_standard_deviation", (branchObservability, context) -> branchObservability.getQualityQ2().isPresent() ?
                                branchObservability.getNullableQualityQ2().getStandardDeviation() : Double.NaN,
                    (branchObservability, standardDeviation, context) ->
                            branchObservability.getNullableQualityQ2().setStandardDeviation(standardDeviation))
                .booleans("q2_redundant", branchObservability -> branchObservability.getQualityQ2().isPresent() &&
                                (boolean) branchObservability.getNullableQualityQ2().isRedundant().orElse(false),
                    (branchObservability, redundant) -> branchObservability.getNullableQualityQ2().setRedundant(redundant))
                .booleans("q2_redundant_null", branchObservability -> branchObservability.getQualityQ2().isEmpty() ||
                        branchObservability.getNullableQualityQ2().isRedundant().isEmpty())
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(network::getBranch)
                .filter(Objects::nonNull)
                .forEach(branch -> branch.removeExtension(BranchObservability.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new BranchObservabilityDataframeAdder();
    }
}
