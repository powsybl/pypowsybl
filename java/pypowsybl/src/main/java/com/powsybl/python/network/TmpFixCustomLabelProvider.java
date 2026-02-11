/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.network;

import com.powsybl.nad.model.BranchEdge;
import com.powsybl.nad.svg.CustomLabelProvider;
import com.powsybl.nad.svg.EdgeInfo;
import com.powsybl.nad.svg.VoltageLevelLegend;

import java.util.Map;
import java.util.Optional;

/**
 * Temporarily overrides bugged method from powsybl-diagram. To remove in further releases
 *
 * @author Hugo Kulesza {@literal <hugo.kulesza at rte-france.com>}
 */
public class TmpFixCustomLabelProvider extends CustomLabelProvider {
    public TmpFixCustomLabelProvider(Map<String, BranchLabels> branchLabels, Map<String, ThreeWtLabels> threeWtLabels, Map<String, InjectionLabels> injectionLabels, Map<String, VoltageLevelLegend> vlLegends) {
        super(branchLabels, threeWtLabels, injectionLabels, vlLegends);
    }

    @Override
    public Optional<EdgeInfo> getBranchEdgeInfo(String branchId, String branchType) {
        Optional<EdgeInfo> edgeInfo = super.getBranchEdgeInfo(branchId, BranchEdge.Side.ONE, branchType);
        if (edgeInfo.isPresent()) {
            return super.getBranchEdgeInfo(branchId, branchType);
        }
        return Optional.empty();
    }
}
