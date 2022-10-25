package com.powsybl.python.dynamic;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.powsybl.dynamicsimulation.Curve;
import com.powsybl.dynamicsimulation.CurvesSupplier;
import com.powsybl.dynawaltz.DynaWaltzCurve;
import com.powsybl.iidm.network.Network;

public class TimeseriesSupplier implements CurvesSupplier {

    private List<Supplier<Curve>> curvesSupplierList;

    public TimeseriesSupplier() {
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
    public List<Curve> get(Network network) {
        return curvesSupplierList.stream().map(supplier -> supplier.get()).collect(Collectors.toList());
    }

}
