/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.gridpy;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
abstract class AbstractContingencyContainer implements ContingencyContainer {

    private final Map<String, List<String>> elementIdsByContingencyId = new HashMap<>();

    @Override
    public void addContingency(String contingencyId, List<String> elementIds) {
        elementIdsByContingencyId.put(contingencyId, elementIds);
    }

    private static ContingencyElement createContingencyElement(Network network, String elementId) {
        Identifiable<?> identifiable = network.getIdentifiable(elementId);
        if (identifiable == null) {
            throw new PowsyblException("Element '" + elementId + "' not found");
        }
        if (identifiable instanceof Branch) {
            return new BranchContingency(elementId);
        } else {
            throw new PowsyblException("Element type not supported: " + identifiable.getClass().getSimpleName());
        }
    }

    protected List<Contingency> createContingencies(Network network) {
        List<Contingency> contingencies = new ArrayList<>(elementIdsByContingencyId.size());
        for (Map.Entry<String, List<String>> e : elementIdsByContingencyId.entrySet()) {
            String contingencyId = e.getKey();
            List<String> elementIds = e.getValue();
            List<ContingencyElement> elements = elementIds.stream()
                    .map(elementId -> createContingencyElement(network, elementId))
                    .collect(Collectors.toList());
            contingencies.add(new Contingency(contingencyId, elements));
        }
        return contingencies;
    }
}
