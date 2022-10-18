package com.powsybl.python.dynamic;

import com.powsybl.dynamicsimulation.CurvesSupplier;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynamicsimulation.DynamicSimulation;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.EventModelsSupplier;
import com.powsybl.iidm.network.Network;

public class DynamicSimulationContext {
    public DynamicSimulationResult run(Network network,
            DynamicModelsSupplier dynamicModelsSupplier,
            EventModelsSupplier eventModelsSupplier,
            CurvesSupplier curvesSupplier,
            DynamicSimulationParameters parameters) {
        return DynamicSimulation.run(network, dynamicModelsSupplier);
    }
}
