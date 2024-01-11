/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.powsybl.commons.reporter.Reporter;
import com.powsybl.dynamicsimulation.Curve;
import com.powsybl.dynamicsimulation.CurvesSupplier;
import com.powsybl.dynawaltz.DynaWaltzCurve;
import com.powsybl.iidm.network.Network;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 */
public class CurveMappingSupplier implements CurvesSupplier {

    private List<Supplier<DynaWaltzCurve>> curvesSupplierList;

    public CurveMappingSupplier() {
        curvesSupplierList = new LinkedList<>();
    }

    public void addCurve(String dynamicId, String variable) {
        curvesSupplierList.add(() -> new DynaWaltzCurve(dynamicId, variable));
    }

    public void addCurves(String dynamicId, Collection<String> variablesCol) {
        for (String variable : variablesCol) {
            addCurve(dynamicId, variable);
        }
    }

    @Override
    public List<Curve> get(Network network, Reporter reporter) {
        return get(network);
    }

    @Override
    public List<Curve> get(Network network) {
        return curvesSupplierList.stream().map(Supplier::get).collect(Collectors.toList());
    }

}
