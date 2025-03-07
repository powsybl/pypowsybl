/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.PyPowsyblApiHeader.DynamicSimulationParametersPointer;

import java.util.Map;

import static com.powsybl.python.commons.PyPowsyblConfiguration.isReadConfig;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public final class DynamicSimulationParametersCUtils {

    private DynamicSimulationParametersCUtils() {
    }

    public static DynamicSimulationParameters createDynamicSimulationParameters() {
        return isReadConfig() ? DynamicSimulationParameters.load() : new DynamicSimulationParameters();
    }

    //TODO check how to handle parameterSet
    public static DynamicSimulationParameters createDynamicSimulationParameters(DynamicSimulationParametersPointer parametersPointer) {
        DynamicSimulationParameters parameters = createDynamicSimulationParameters()
                .setStartTime(parametersPointer.getStartTime())
                .setStopTime(parametersPointer.getStopTime());
        DynawoSimulationParameters.load(parameters).update(getSpecificParameters(parametersPointer));
        return parameters;
    }

    public static void copyToCDynamicSimulationParameters(DynamicSimulationParametersPointer cParameters) {
        copyToCDynamicSimulationParameters(createDynamicSimulationParameters(), cParameters);
    }

    public static void copyToCDynamicSimulationParameters(DynamicSimulationParameters parameters, DynamicSimulationParametersPointer cParameters) {
        cParameters.setStartTime(parameters.getStartTime());
        cParameters.setStopTime(parameters.getStopTime());
        cParameters.setProviderParametersValuesCount(0);
        cParameters.setProviderParametersKeysCount(0);
    }

    private static Map<String, String> getSpecificParameters(DynamicSimulationParametersPointer parametersPointer) {
        return CTypeUtil.toStringMap(parametersPointer.getProviderParametersKeys(),
                parametersPointer.getProviderParametersKeysCount(),
                parametersPointer.getProviderParametersValues(),
                parametersPointer.getProviderParametersValuesCount());
    }
}
