/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynamicsimulation.OutputVariable;
import com.powsybl.dynamicsimulation.OutputVariablesSupplier;
import com.powsybl.dynawo.outputvariables.DynawoOutputVariablesBuilder;
import com.powsybl.iidm.network.Network;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 */
public class CurveMappingSupplier implements OutputVariablesSupplier {

    private final List<OutputVariable> outputvariables;

    public CurveMappingSupplier() {
        outputvariables = new LinkedList<>();
    }

    public void addCurve(String dynamicId, String variable) {
        outputvariables.addAll(new DynawoOutputVariablesBuilder().dynamicModelId(dynamicId).variable(variable).build());
    }

    public void addCurves(String dynamicId, Collection<String> variablesCol) {
        for (String variable : variablesCol) {
            addCurve(dynamicId, variable);
        }
    }

    @Override
    public List<OutputVariable> get(Network network, ReportNode reportNode) {
        return get(network);
    }

    @Override
    public List<OutputVariable> get(Network network) {
        return outputvariables;
    }

}
