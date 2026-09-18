/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.dynamic;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters;
import com.powsybl.security.dynamic.DynamicSecurityAnalysisParameters.ContingenciesParameters;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.PyPowsyblApiHeader.DynamicSecurityAnalysisParametersPointer;

import java.util.Map;

import static com.powsybl.python.commons.PyPowsyblConfiguration.isReadConfig;

/**
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
public final class DynamicSecurityAnalysisParametersCUtils {

    private DynamicSecurityAnalysisParametersCUtils() {
    }

    /**
     * Default dynamic security analysis parameters, read from the platform config when config reading is enabled.
     */
    public static DynamicSecurityAnalysisParameters getDefaultDynamicSecurityAnalysisParameters() {
        return isReadConfig() ? DynamicSecurityAnalysisParameters.load() : new DynamicSecurityAnalysisParameters();
    }

    public static DynamicSecurityAnalysisParameters createDynamicSecurityAnalysisParameters(DynamicSecurityAnalysisParametersPointer parametersPointer) {
        DynamicSimulationParameters dynamicSimulationParameters = DynamicSimulationParametersCUtils.createDynamicSimulationParameters(
                parametersPointer.getStartTime(), parametersPointer.getStopTime(), getProviderParameters(parametersPointer));
        DynamicSecurityAnalysisParameters parameters = getDefaultDynamicSecurityAnalysisParameters()
                .setDynamicSimulationParameters(dynamicSimulationParameters)
                .setDynamicContingenciesParameters(new ContingenciesParameters(parametersPointer.getContingenciesStartTime()));
        String debugDir = CTypeUtil.toString(parametersPointer.getDebugDir());
        if (!debugDir.isEmpty()) {
            parameters.setDebugDir(debugDir);
        }
        return parameters;
    }

    public static void copyToCDynamicSecurityAnalysisParameters(DynamicSecurityAnalysisParametersPointer cParameters) {
        DynamicSecurityAnalysisParameters defaultParameters = getDefaultDynamicSecurityAnalysisParameters();
        cParameters.setStartTime(defaultParameters.getDynamicSimulationParameters().getStartTime());
        cParameters.setStopTime(defaultParameters.getDynamicSimulationParameters().getStopTime());
        cParameters.setContingenciesStartTime(defaultParameters.getDynamicContingenciesParameters().getContingenciesStartTime());
        cParameters.setDebugDir(CTypeUtil.toCharPtr(defaultParameters.getDebugDir() == null ? "" : defaultParameters.getDebugDir()));
        cParameters.getProviderParameters().setProviderParametersKeysCount(0);
        cParameters.getProviderParameters().setProviderParametersValuesCount(0);
    }

    private static Map<String, String> getProviderParameters(DynamicSecurityAnalysisParametersPointer parametersPointer) {
        return CTypeUtil.toStringMap(parametersPointer.getProviderParameters().getProviderParametersKeys(),
                parametersPointer.getProviderParameters().getProviderParametersKeysCount(),
                parametersPointer.getProviderParameters().getProviderParametersValues(),
                parametersPointer.getProviderParameters().getProviderParametersValuesCount());
    }
}
