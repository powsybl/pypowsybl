/**
 * Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.dynamic;

import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynamicsimulation.CurvesSupplier;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynamicsimulation.DynamicSimulation;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationProvider;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.EventModelsSupplier;
import com.powsybl.iidm.network.Network;

public class DynamicSimulationContext {
    private static Logger log = LoggerFactory.getLogger(DynamicSimulationContext.class);

    public DynamicSimulationResult run(Network network,
            DynamicModelsSupplier dynamicModelsSupplier,
            EventModelsSupplier eventModelsSupplier,
            CurvesSupplier curvesSupplier,
            DynamicSimulationParameters parameters) {
        DynamicSimulationProvider provider = getDynamicProvider("");
        log.info("Running dynamic simulation with " + provider.getName());
        DynamicSimulation.Runner runner = new DynamicSimulation.Runner(provider);
        return runner.run(network,
                dynamicModelsSupplier,
                eventModelsSupplier,
                curvesSupplier,
                parameters);
    }

    /**
     * TODO do we need to keep interface with providers here or if it should be done
     * by the class {@link DynamicSimulation}
     *
     * @param name
     * @return
     */
    public static DynamicSimulationProvider getDynamicProvider(String name) {
        String actualName = (name == null || name.isEmpty()) ? "DynaWaltz" : name;
        return ServiceLoader.load(DynamicSimulationProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(provider -> provider.getName().equals(actualName))
                .findFirst()
                .orElseThrow(() -> new PowsyblException("No dynamicSimulation provider for name '" + actualName + "'"));
    }
}
