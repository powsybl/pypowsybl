/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.flow_decomposition;

import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.flow_decomposition.xnec_provider.XnecProvider5percPtdf;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderAllBranches;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderInterconnection;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderUnion;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
final class FlowDecompositionContext {
    private final XnecProviderByIds.Builder xnecProviderByIdsBuilder = XnecProviderByIds.builder();
    private final List<XnecProvider> additionalXnecProviderList = new ArrayList<>();

    public XnecProviderByIds.Builder getXnecProviderByIdsBuilder() {
        return xnecProviderByIdsBuilder;
    }

    public void addAdditionalXnecProviderList(DefaultXnecProvider defaultXnecProvider) {
        additionalXnecProviderList.add(getXnecProvider(defaultXnecProvider));
    }

    private static XnecProvider getXnecProvider(DefaultXnecProvider defaultXnecProvider) {
        return switch (defaultXnecProvider) {
            case GT_5_PERC_ZONE_TO_ZONE_PTDF -> new XnecProvider5percPtdf();
            case ALL_BRANCHES -> new XnecProviderAllBranches();
            case INTERCONNECTIONS -> new XnecProviderInterconnection();
        };
    }

    XnecProvider getXnecProvider() {
        List<XnecProvider> xnecProviderList = new ArrayList<>(additionalXnecProviderList);
        xnecProviderList.add(xnecProviderByIdsBuilder.build());
        return new XnecProviderUnion(xnecProviderList);
    }

    enum DefaultXnecProvider {
        GT_5_PERC_ZONE_TO_ZONE_PTDF,
        ALL_BRANCHES,
        INTERCONNECTIONS
    }
}
