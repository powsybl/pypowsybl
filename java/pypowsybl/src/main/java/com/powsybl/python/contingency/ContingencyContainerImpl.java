/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.contingency;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.*;
import com.powsybl.contingency.contingency.list.ContingencyList;
import com.powsybl.iidm.network.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class ContingencyContainerImpl implements ContingencyContainer {

    private final Map<String, List<String>> elementIdsByContingencyId = new HashMap<>();
    private Path pathToContingencyJsonFile = null;

    @Override
    public void addContingency(String contingencyId, List<String> elementIds) {
        elementIdsByContingencyId.put(contingencyId, elementIds);
    }

    @Override
    public void addContingencyFromJsonFile(Path pathToJsonFile) {
        pathToContingencyJsonFile = pathToJsonFile;
    }

    private static ContingencyElement createContingencyElement(Network network, String elementId) {
        Identifiable<?> identifiable = network.getIdentifiable(elementId);
        if (identifiable == null) {
            throw new PowsyblException("Element '" + elementId + "' not found");
        }
        if (identifiable instanceof Line) {
            return new LineContingency(elementId);
        } else if (identifiable instanceof TwoWindingsTransformer) {
            return new TwoWindingsTransformerContingency(elementId);
        } else if (identifiable instanceof HvdcLine) {
            return new HvdcLineContingency(elementId);
        } else if (identifiable instanceof BusbarSection) {
            return new BusbarSectionContingency(elementId);
        } else if (identifiable instanceof Generator) {
            return new GeneratorContingency(elementId);
        } else if (identifiable instanceof DanglingLine) {
            return new DanglingLineContingency(elementId);
        } else if (identifiable instanceof StaticVarCompensator) {
            return new StaticVarCompensatorContingency(elementId);
        } else if (identifiable instanceof ShuntCompensator) {
            return new ShuntCompensatorContingency(elementId);
        } else if (identifiable instanceof ThreeWindingsTransformer) {
            return new ThreeWindingsTransformerContingency(elementId);
        } else if (identifiable instanceof Load) {
            return new LoadContingency(elementId);
        } else if (identifiable instanceof Battery) {
            return new BatteryContingency(elementId);
        } else if (identifiable instanceof Switch) {
            return new SwitchContingency(elementId);
        } else if (identifiable instanceof TieLine) {
            return new TieLineContingency(elementId);
        } else {
            throw new PowsyblException("Element type not supported: " + identifiable.getClass().getSimpleName());
        }
    }

    protected List<Contingency> createContingencies(Network network) {
        List<Contingency> contingencies = new ArrayList<>(elementIdsByContingencyId.size());

        if (pathToContingencyJsonFile != null) {
            if (Files.exists(pathToContingencyJsonFile)) {
                ContingencyList contingenciesList;
                contingenciesList = ContingencyList.load(pathToContingencyJsonFile);

                for (Contingency contingency : contingenciesList.getContingencies(network)) {
                    contingencies.add(new Contingency(contingency.getId(), contingency.getElements()));
                }
            } else {
                throw new PowsyblException("File not found: " + pathToContingencyJsonFile);
            }

        }

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
