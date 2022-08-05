/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python.security;

import com.powsybl.commons.extensions.Extension;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblConfiguration;
import com.powsybl.python.loadflow.LoadFlowCUtils;
import com.powsybl.security.SecurityAnalysisParameters;
import com.powsybl.security.SecurityAnalysisProvider;

import java.util.Map;

/**
 * @author Etienne Lesot <etienne.lesot@rte-france.com>
 */
public final class SecurityAnalysisCUtils {

    private SecurityAnalysisCUtils() {
    }

    public static SecurityAnalysisParameters createSecurityAnalysisParameters() {
        return PyPowsyblConfiguration.isReadConfig() ? SecurityAnalysisParameters.load() : new SecurityAnalysisParameters();
    }

    public static SecurityAnalysisParameters createSecurityAnalysisParameters(boolean dc,
                                                                              PyPowsyblApiHeader.SecurityAnalysisParametersPointer securityAnalysisParametersPointer,
                                                                              SecurityAnalysisProvider provider) {
        SecurityAnalysisParameters parameters = createSecurityAnalysisParameters(securityAnalysisParametersPointer);
        parameters.setLoadFlowParameters(LoadFlowCUtils.createLoadFlowParameters(dc, securityAnalysisParametersPointer.getLoadFlowParameters(),
                LoadFlowCUtils.getLoadFlowProvider(provider.getLoadFlowProviderName().orElse(PyPowsyblConfiguration.getDefaultLoadFlowProvider()))));
        Map<String, String> specificParametersProperties = getSpecificParameters(securityAnalysisParametersPointer);
        provider.loadSpecificParameters(specificParametersProperties).ifPresent(ext -> {
            // Dirty trick to get the class, and reload parameters if they exist.
            // TODO: SPI needs to be changed so that we don't need to read params to get the class
            Extension<SecurityAnalysisParameters> configured = parameters.getExtension(ext.getClass());
            if (configured != null) {
                provider.updateSpecificParameters(configured, specificParametersProperties);
            } else {
                parameters.addExtension((Class) ext.getClass(), ext);
            }
        });
        return parameters;
    }

    private static SecurityAnalysisParameters createSecurityAnalysisParameters(PyPowsyblApiHeader.SecurityAnalysisParametersPointer securityAnalysisParametersPointer) {
        return createSecurityAnalysisParameters()
                .setIncreasedViolationsParameters(new SecurityAnalysisParameters.IncreasedViolationsParameters()
                        .setFlowProportionalThreshold(securityAnalysisParametersPointer.getFlowProportionalThreshold())
                        .setHighVoltageAbsoluteThreshold(securityAnalysisParametersPointer.getHighVoltageAbsoluteThreshold())
                        .setHighVoltageProportionalThreshold(securityAnalysisParametersPointer.getHighVoltageProportionalThreshold())
                        .setLowVoltageAbsoluteThreshold(securityAnalysisParametersPointer.getLowVoltageAbsoluteThreshold())
                        .setLowVoltageProportionalThreshold(securityAnalysisParametersPointer.getLowVoltageProportionalThreshold()));
    }

    private static Map<String, String> getSpecificParameters(PyPowsyblApiHeader.SecurityAnalysisParametersPointer securityAnalysisParametersPointer) {
        return CTypeUtil.toStringMap(securityAnalysisParametersPointer.getProviderParametersKeys(),
                securityAnalysisParametersPointer.getProviderParametersKeysCount(),
                securityAnalysisParametersPointer.getProviderParametersValues(),
                securityAnalysisParametersPointer.getProviderParametersValuesCount());
    }
}
