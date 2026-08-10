/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.margincalculation.MarginCalculationParameters;
import com.powsybl.dynawo.margincalculation.MarginCalculationParameters.CalculationType;
import com.powsybl.dynawo.margincalculation.MarginCalculationParameters.LoadModelsRule;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.PyPowsyblApiHeader.MarginCalculationParametersPointer;

import java.util.Map;

import static com.powsybl.python.commons.PyPowsyblConfiguration.isReadConfig;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class MarginCalculationParametersCUtils {

    private MarginCalculationParametersCUtils() {
    }

    /**
     * Default margin calculation parameters, read from the platform config when config reading is enabled.
     */
    public static MarginCalculationParameters getDefaultMarginCalculationParameters() {
        return isReadConfig() ? MarginCalculationParameters.load()
                : MarginCalculationParameters.builder().setDynawoParameters(new DynawoSimulationParameters()).build();
    }

    public static MarginCalculationParameters createMarginCalculationParameters(MarginCalculationParametersPointer parametersPointer) {
        DynawoSimulationParameters dynawoParameters = isReadConfig() ? DynawoSimulationParameters.load() : new DynawoSimulationParameters();
        Map<String, String> providerParameters = getProviderParameters(parametersPointer);
        if (!providerParameters.isEmpty()) {
            dynawoParameters.update(providerParameters);
        }
        MarginCalculationParameters.Builder builder = MarginCalculationParameters.builder()
                .setStartTime(parametersPointer.getStartTime())
                .setStopTime(parametersPointer.getStopTime())
                .setMarginCalculationStartTime(parametersPointer.getMarginCalculationStartTime())
                .setLoadIncreaseStartTime(parametersPointer.getLoadIncreaseStartTime())
                .setLoadIncreaseStopTime(parametersPointer.getLoadIncreaseStopTime())
                .setContingenciesStartTime(parametersPointer.getContingenciesStartTime())
                .setCalculationType(toCalculationType(parametersPointer.getCalculationType()))
                .setAccuracy(parametersPointer.getAccuracy())
                .setLoadModelsRule(toLoadModelsRule(parametersPointer.getLoadModelsRule()))
                .setDynawoParameters(dynawoParameters);
        String debugDir = CTypeUtil.toString(parametersPointer.getDebugDir());
        if (!debugDir.isEmpty()) {
            builder.setDebugDir(debugDir);
        }
        return builder.build();
    }

    public static void copyToCMarginCalculationParameters(MarginCalculationParametersPointer cParameters) {
        MarginCalculationParameters defaultParameters = getDefaultMarginCalculationParameters();
        cParameters.setStartTime(defaultParameters.getStartTime());
        cParameters.setStopTime(defaultParameters.getStopTime());
        cParameters.setMarginCalculationStartTime(defaultParameters.getMarginCalculationStartTime());
        cParameters.setLoadIncreaseStartTime(defaultParameters.getLoadIncreaseStartTime());
        cParameters.setLoadIncreaseStopTime(defaultParameters.getLoadIncreaseStopTime());
        cParameters.setContingenciesStartTime(defaultParameters.getContingenciesStartTime());
        cParameters.setCalculationType(defaultParameters.getCalculationType().ordinal());
        cParameters.setAccuracy(defaultParameters.getAccuracy());
        cParameters.setLoadModelsRule(defaultParameters.getLoadModelsRule().ordinal());
        cParameters.setDebugDir(CTypeUtil.toCharPtr(defaultParameters.getDebugDir() == null ? "" : defaultParameters.getDebugDir()));
        cParameters.getProviderParameters().setProviderParametersKeysCount(0);
        cParameters.getProviderParameters().setProviderParametersValuesCount(0);
    }

    private static CalculationType toCalculationType(int value) {
        CalculationType[] values = CalculationType.values();
        if (value < 0 || value >= values.length) {
            throw new PowsyblException("Invalid margin calculation type: " + value);
        }
        return values[value];
    }

    private static LoadModelsRule toLoadModelsRule(int value) {
        LoadModelsRule[] values = LoadModelsRule.values();
        if (value < 0 || value >= values.length) {
            throw new PowsyblException("Invalid margin calculation load models rule: " + value);
        }
        return values[value];
    }

    private static Map<String, String> getProviderParameters(MarginCalculationParametersPointer parametersPointer) {
        return CTypeUtil.toStringMap(parametersPointer.getProviderParameters().getProviderParametersKeys(),
                parametersPointer.getProviderParameters().getProviderParametersKeysCount(),
                parametersPointer.getProviderParameters().getProviderParametersValues(),
                parametersPointer.getProviderParameters().getProviderParametersValuesCount());
    }
}
