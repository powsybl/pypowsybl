/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.shortcircuit;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblConfiguration;
import com.powsybl.shortcircuit.InitialVoltageProfileMode;
import com.powsybl.shortcircuit.ShortCircuitAnalysisProvider;
import com.powsybl.shortcircuit.ShortCircuitParameters;
import com.powsybl.shortcircuit.StudyType;

import java.util.Map;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@soft.it>
 */
public final class ShortCircuitAnalysisCUtils {

    private ShortCircuitAnalysisCUtils() {
    }

    public static ShortCircuitAnalysisProvider getShortCircuitAnalysisProvider(String name) {
        String actualName = name.isEmpty() ? PyPowsyblConfiguration.getDefaultShortCircuitAnalysisProvider() : name;
        return ShortCircuitAnalysisProvider.findAll().stream()
                .filter(provider -> provider.getName().equals(actualName))
                .findFirst()
                .orElseThrow(() -> new PowsyblException("No short-circuit analysis provider for name '" + actualName + "'"));
    }

    public static ShortCircuitParameters createShortCircuitAnalysisParameters() {
        return PyPowsyblConfiguration.isReadConfig() ? ShortCircuitParameters.load() : new ShortCircuitParameters();
    }

    public static ShortCircuitParameters createShortCircuitAnalysisParameters(PyPowsyblApiHeader.ShortCircuitAnalysisParametersPointer shortCircuitAnalysisParametersPointer,
                                                                              ShortCircuitAnalysisProvider provider) {
        ShortCircuitParameters parameters = createShortCircuitAnalysisParameters(shortCircuitAnalysisParametersPointer);
        Map<String, String> specificParametersProperties = getSpecificParameters(shortCircuitAnalysisParametersPointer);
        provider.loadSpecificParameters(specificParametersProperties).ifPresent(ext -> {
            // Dirty trick to get the class, and reload parameters if they exist.
            Extension<ShortCircuitParameters> configured = parameters.getExtension(ext.getClass());
            if (configured != null) {
                provider.updateSpecificParameters(configured, specificParametersProperties);
            } else {
                parameters.addExtension((Class) ext.getClass(), ext);
            }
        });
        return parameters;
    }

    private static ShortCircuitParameters createShortCircuitAnalysisParameters(PyPowsyblApiHeader.ShortCircuitAnalysisParametersPointer shortCircuitAnalysisParametersPointer) {
        return createShortCircuitAnalysisParameters().setWithLimitViolations(shortCircuitAnalysisParametersPointer.isWithLimitViolations())
                .setWithVoltageResult(shortCircuitAnalysisParametersPointer.isWithVoltageResult())
                .setWithFeederResult(shortCircuitAnalysisParametersPointer.isWithFeederResult())
                .setWithFortescueResult(shortCircuitAnalysisParametersPointer.isWithFortescueResult())
                .setWithLimitViolations(shortCircuitAnalysisParametersPointer.isWithLimitViolations())
                .setMinVoltageDropProportionalThreshold(shortCircuitAnalysisParametersPointer.getMinVoltageDropProportionalThreshold())
                .setInitialVoltageProfileMode(InitialVoltageProfileMode.values()[shortCircuitAnalysisParametersPointer.getInitialVoltageProfileMode()])
                .setStudyType(StudyType.values()[shortCircuitAnalysisParametersPointer.getStudyType()]);
    }

    private static Map<String, String> getSpecificParameters(PyPowsyblApiHeader.ShortCircuitAnalysisParametersPointer shortCircuitAnalysisParametersPointer) {
        return CTypeUtil.toStringMap(shortCircuitAnalysisParametersPointer.getProviderParametersKeys(),
                shortCircuitAnalysisParametersPointer.getProviderParametersKeysCount(),
                shortCircuitAnalysisParametersPointer.getProviderParametersValues(),
                shortCircuitAnalysisParametersPointer.getProviderParametersValuesCount());
    }
}
