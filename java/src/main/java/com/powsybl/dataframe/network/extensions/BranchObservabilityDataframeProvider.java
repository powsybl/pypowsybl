/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.extensions;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
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
 * @author Etienne Lesot <etienne.lesot@rte-france.com>
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class BranchObservabilityDataframeProvider implements NetworkExtensionDataframeProvider {

    private Stream<BranchObservability> itemsStream(Network network) {
        return network.getBranchStream().filter(Objects::nonNull)
                .map(branch -> (BranchObservability) branch.getExtension(BranchObservability.class))
                .filter(Objects::nonNull);
    }

    private BranchObservability getOrThrow(Network network, String id) {
        Branch branch = network.getBranch(id);
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
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", branchObservability -> ((Branch) branchObservability.getExtendable()).getId())
                .booleans("observable", BranchObservability::isObservable)
                .doubles("p1_standard_deviation", branchObservability -> branchObservability.getQualityP1() != null ? branchObservability.getQualityP1().getStandardDeviation() : null,
                    (branchObservability, standardDeviation) -> branchObservability.getQualityP1().setStandardDeviation(standardDeviation))
                .booleans("p1_redundant", branchObservability -> branchObservability.getQualityP1() != null && branchObservability.getQualityP1().isRedundant(),
                    (branchObservability, redundant) -> branchObservability.getQualityP1().setRedundant(redundant))
                .doubles("p2_standard_deviation", branchObservability -> branchObservability.getQualityP2() != null ? branchObservability.getQualityP2().getStandardDeviation() : null,
                    (branchObservability, standardDeviation) -> branchObservability.getQualityP2().setStandardDeviation(standardDeviation))
                .booleans("p2_redundant", branchObservability -> branchObservability.getQualityP2() != null && branchObservability.getQualityP2().isRedundant(),
                    (branchObservability, redundant) -> branchObservability.getQualityP2().setRedundant(redundant))
                .doubles("q1_standard_deviation", branchObservability -> branchObservability.getQualityQ1() != null ? branchObservability.getQualityQ1().getStandardDeviation() : null,
                    (branchObservability, standardDeviation) -> branchObservability.getQualityQ1().setStandardDeviation(standardDeviation))
                .booleans("q1_redundant", branchObservability -> branchObservability.getQualityQ1() != null && branchObservability.getQualityQ1().isRedundant(),
                    (branchObservability, redundant) -> branchObservability.getQualityQ1().setRedundant(redundant))
                .doubles("q2_standard_deviation", branchObservability -> branchObservability.getQualityQ2() != null ? branchObservability.getQualityQ2().getStandardDeviation() : null,
                    (branchObservability, standardDeviation) -> branchObservability.getQualityQ2().setStandardDeviation(standardDeviation))
                .booleans("q2_redundant", branchObservability -> branchObservability.getQualityQ2() != null && branchObservability.getQualityQ2().isRedundant(),
                    (branchObservability, redundant) -> branchObservability.getQualityQ2().setRedundant(redundant))
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
