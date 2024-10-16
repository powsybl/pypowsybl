/**
 * Copyright (c) 2020-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.dynamicsimulation.Curve;
import com.powsybl.dynamicsimulation.CurvesSupplier;
import com.powsybl.dynawaltz.curves.DynawoCurvesBuilder;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author Nicolas Pierre <nicolas.pierre@artelys.com>
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
//TODO handle static id
public class PythonCurveSupplier implements CurvesSupplier {

    private final List<BiConsumer<Consumer<Curve>, ReportNode>> curvesSupplierListRR = new ArrayList<>();

    public void addCurve(String dynamicId, String variable) {
        curvesSupplierListRR.add((c, r) -> new DynawoCurvesBuilder(r)
                .dynamicModelId(dynamicId)
                .variable(variable)
                .add(c));
    }

    public void addCurves(String dynamicId, List<String> variables) {
        curvesSupplierListRR.add((c, r) -> new DynawoCurvesBuilder(r)
                .dynamicModelId(dynamicId)
                .variables(variables)
                .add(c));
    }

    @Override
    public List<Curve> get(Network network, ReportNode reportNode) {
        List<Curve> curves = new ArrayList<>();
        curvesSupplierListRR.forEach(c -> c.accept(curves::add, reportNode));
        return curves.stream().filter(Objects::nonNull).toList();
    }
}
