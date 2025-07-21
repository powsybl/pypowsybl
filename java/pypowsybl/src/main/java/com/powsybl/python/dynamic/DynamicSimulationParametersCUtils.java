/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.parameters.Parameter;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationProvider;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.PyPowsyblApiHeader.DynamicSimulationParametersPointer;

import java.util.List;
import java.util.Map;

import static com.powsybl.python.commons.PyPowsyblConfiguration.getDefaultDynamicSimulationProvider;
import static com.powsybl.python.commons.PyPowsyblConfiguration.isReadConfig;

/**
 * @author Laurent Issertial {@literal <laurent.issertial at rte-france.com>}
 */
public final class DynamicSimulationParametersCUtils {

    private DynamicSimulationParametersCUtils() {
    }

    public static DynamicSimulationProvider getDynamicSimulationProvider() {
        return DynamicSimulationProvider.findAll().stream()
                .filter(provider -> provider.getName().equals(getDefaultDynamicSimulationProvider()))
                .findFirst()
                .orElseThrow(() -> new PowsyblException("No dynamic simulation provider for name '" + getDefaultDynamicSimulationProvider() + "'"));
    }

    public static DynamicSimulationParameters createDynamicSimulationParameters() {
        return isReadConfig() ? DynamicSimulationParameters.load() : new DynamicSimulationParameters();
    }

    public static List<Parameter> getSpecificParametersInfo() {
        return getDynamicSimulationProvider().getSpecificParameters();
    }

    public static DynamicSimulationParameters createDynamicSimulationParameters(DynamicSimulationParametersPointer parametersPointer) {
        DynamicSimulationParameters parameters = createDynamicSimulationParameters()
                .setStartTime(parametersPointer.getStartTime())
                .setStopTime(parametersPointer.getStopTime());
        DynawoSimulationParameters specificParameters = createSpecificDynamicSimulationParameters(parameters);
        Map<String, String> specificParametersMap = getSpecificParameters(parametersPointer);
        if (!specificParametersMap.isEmpty()) {
            specificParameters.update(specificParametersMap);
        }
        return parameters;
    }

    public static void copyToCDynamicSimulationParameters(DynamicSimulationParametersPointer cParameters) {
        copyToCDynamicSimulationParameters(createDynamicSimulationParameters(), cParameters);
    }

    public static void copyToCDynamicSimulationParameters(DynamicSimulationParameters parameters, DynamicSimulationParametersPointer cParameters) {
        cParameters.setStartTime(parameters.getStartTime());
        cParameters.setStopTime(parameters.getStopTime());
        cParameters.getProviderParameters().setProviderParametersValuesCount(0);
        cParameters.getProviderParameters().setProviderParametersKeysCount(0);
    }

    private static DynawoSimulationParameters createSpecificDynamicSimulationParameters(DynamicSimulationParameters parameters) {
        DynawoSimulationParameters specificParameters = parameters.getExtension(DynawoSimulationParameters.class);
        if (specificParameters == null) {
            specificParameters = isReadConfig() ? DynawoSimulationParameters.load() : new DynawoSimulationParameters();
            parameters.addExtension(DynawoSimulationParameters.class, specificParameters);
        }
        return specificParameters;
    }

    private static Map<String, String> getSpecificParameters(DynamicSimulationParametersPointer parametersPointer) {
        return CTypeUtil.toStringMap(parametersPointer.getProviderParameters().getProviderParametersKeys(),
                parametersPointer.getProviderParameters().getProviderParametersKeysCount(),
                parametersPointer.getProviderParameters().getProviderParametersValues(),
                parametersPointer.getProviderParameters().getProviderParametersValuesCount());
    }
}
