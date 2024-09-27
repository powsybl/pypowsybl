/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.sensitivity;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblConfiguration;
import com.powsybl.python.loadflow.LoadFlowCUtils;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityAnalysisProvider;

import java.util.Map;

/**
 * @author Etienne Lesot <etienne.lesot@rte-france.com>
 */
public final class SensitivityAnalysisCUtils {

    private SensitivityAnalysisCUtils() {
    }

    public static SensitivityAnalysisProvider getSensitivityAnalysisProvider(String name) {
        String actualName = name.isEmpty() ? PyPowsyblConfiguration.getDefaultSensitivityAnalysisProvider() : name;
        return SensitivityAnalysisProvider.findAll().stream()
                .filter(provider -> provider.getName().equals(actualName))
                .findFirst()
                .orElseThrow(() -> new PowsyblException("No sensitivity analysis provider for name '" + actualName + "'"));
    }

    public static SensitivityAnalysisParameters createSensitivityAnalysisParameters() {
        return PyPowsyblConfiguration.isReadConfig() ? SensitivityAnalysisParameters.load() : new SensitivityAnalysisParameters();
    }

    public static SensitivityAnalysisParameters createSensitivityAnalysisParameters(boolean dc, PyPowsyblApiHeader.SensitivityAnalysisParametersPointer sensitivityAnalysisParametersPointer,
                                                                              SensitivityAnalysisProvider provider) {
        SensitivityAnalysisParameters parameters = createSensitivityAnalysisParameters();
        parameters.setLoadFlowParameters(LoadFlowCUtils.createLoadFlowParameters(dc, sensitivityAnalysisParametersPointer.getLoadFlowParameters(),
                LoadFlowCUtils.getLoadFlowProvider(provider.getLoadFlowProviderName().orElse(PyPowsyblConfiguration.getDefaultLoadFlowProvider()))));
        Map<String, String> specificParametersProperties = getSpecificParameters(sensitivityAnalysisParametersPointer);

        provider.loadSpecificParameters(specificParametersProperties).ifPresent(ext -> {
            // Dirty trick to get the class, and reload parameters if they exist.
            // TODO: SPI needs to be changed so that we don't need to read params to get the class
            Extension<SensitivityAnalysisParameters> configured = parameters.getExtension(ext.getClass());
            if (configured != null) {
                provider.updateSpecificParameters(configured, specificParametersProperties);
            } else {
                parameters.addExtension((Class) ext.getClass(), ext);
            }
        });
        return parameters;
    }

    private static Map<String, String> getSpecificParameters(PyPowsyblApiHeader.SensitivityAnalysisParametersPointer sensitivityAnalysisParametersPointer) {
        return CTypeUtil.toStringMap(sensitivityAnalysisParametersPointer.getProviderParametersKeys(),
                sensitivityAnalysisParametersPointer.getProviderParametersKeysCount(),
                sensitivityAnalysisParametersPointer.getProviderParametersValues(),
                sensitivityAnalysisParametersPointer.getProviderParametersValuesCount());
    }
}
