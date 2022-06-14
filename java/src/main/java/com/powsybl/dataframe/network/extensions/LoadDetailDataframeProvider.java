/**
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
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
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.LoadDetail;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Etienne Lesot <etienne.lesot@rte-france.com>
 */
@AutoService(NetworkExtensionDataframeProvider.class)
public class LoadDetailDataframeProvider implements NetworkExtensionDataframeProvider {

    @Override
    public String getExtensionName() {
        return LoadDetail.NAME;
    }

    private Stream<LoadDetail> itemsStream(Network network) {
        return network.getLoadStream()
                .map(l -> (LoadDetail) l.getExtension(LoadDetail.class))
                .filter(Objects::nonNull);
    }

    private LoadDetail getOrThrow(Network network, String id) {
        Load load = network.getLoad(id);
        if (load == null) {
            throw new PowsyblException("Load '" + id + "' not found");
        }
        LoadDetail loadDetail = load.getExtension(LoadDetail.class);
        if (loadDetail == null) {
            throw new PowsyblException("Load '" + id + "' has no LoadDetail extension");
        }
        return loadDetail;
    }

    @Override
    public NetworkDataframeMapper createMapper() {
        return NetworkDataframeMapperBuilder.ofStream(this::itemsStream, this::getOrThrow)
                .stringsIndex("id", ext -> ext.getExtendable().getId())
                .doubles("fixed_active_power", LoadDetail::getFixedActivePower, LoadDetail::setFixedActivePower)
                .doubles("variable_active_power", LoadDetail::getVariableActivePower, LoadDetail::setVariableActivePower)
                .doubles("fixed_reactive_power", LoadDetail::getFixedReactivePower, LoadDetail::setFixedReactivePower)
                .doubles("variable_reactive_power", LoadDetail::getVariableReactivePower, LoadDetail::setVariableReactivePower)
                .build();
    }

    @Override
    public void removeExtensions(Network network, List<String> ids) {
        ids.stream().filter(Objects::nonNull)
                .map(network::getLoad)
                .filter(Objects::nonNull)
                .forEach(l -> l.removeExtension(LoadDetail.class));
    }

    @Override
    public NetworkElementAdder createAdder() {
        return new LoadDetailDataframeAdder();
    }
}
