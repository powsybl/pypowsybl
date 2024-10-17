/**
 * Copyright (c) 2024, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
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
import com.powsybl.dataframe.network.adders.NetworkUtils;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.ReferencePriorities;
import com.powsybl.iidm.network.extensions.ReferencePriority;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Damien Jeandemange <damien.jeandemange@artelys.com>
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class ReferencePrioritiesDataframeProvider extends AbstractSingleDataframeNetworkExtension {

    @Override
    public String getExtensionName() {
        return ReferencePriorities.NAME;
    }

    @Override
    public ExtensionInformation getExtensionInformation() {
        return new ExtensionInformation(ReferencePriorities.NAME,
                "Defines the angle reference generator, busbar section or load of a power flow calculation, " +
                        "i.e. which bus will be used with a zero-voltage angle.",
                "index : id (str), " +
                        "priority (int)");
    }

    private Stream<ReferencePriorities> itemsStream(Network network) {
        return Stream.of(network.getGenerators(), network.getBusbarSections(), network.getLoads())
                .flatMap(i -> StreamSupport.stream(i.spliterator(), false))
                .map(g -> (ReferencePriorities) g.getExtension(ReferencePriorities.class))
                .filter(Objects::nonNull);
    }

    private ReferencePriorities getOrThrow(Network network, String id) {
        Identifiable<?> identifiable = NetworkUtils.getGenOrLoadOrBusbarSectionOrThrow(network, id);
        ReferencePriorities referencePriorities = identifiable.getExtension(ReferencePriorities.class);
        if (referencePriorities == null) {
            throw new PowsyblException("Injection '" + id + "' has no ReferencePriorities extension");
        }
        return referencePriorities;
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", ext -> ((Identifiable<?>) ext.getExtendable()).getId())
                .ints("priority",
                        rp -> ((List<ReferencePriority>) rp.getReferencePriorities()).get(0).getPriority(),
                        (rp, priority) -> ReferencePriority.set((Injection) (((List<ReferencePriority>) rp.getReferencePriorities()).get(0).getTerminal().getConnectable()), priority))
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(network::getIdentifiable)
                .filter(Objects::nonNull)
                .forEach(i -> i.removeExtension(ReferencePriorities.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new ReferencePrioritiesDataframeAdder();
    }
}
