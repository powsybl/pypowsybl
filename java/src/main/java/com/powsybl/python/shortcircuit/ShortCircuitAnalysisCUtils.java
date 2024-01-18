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
import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.dataframe.update.DoubleSeries;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblConfiguration;
import com.powsybl.python.network.Dataframes;
import com.powsybl.shortcircuit.FortescueValue;
import com.powsybl.shortcircuit.ShortCircuitAnalysisProvider;
import com.powsybl.shortcircuit.ShortCircuitParameters;
import com.powsybl.shortcircuit.StudyType;
import org.apache.commons.lang3.tuple.Pair;
import org.graalvm.nativeimage.c.function.CEntryPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.powsybl.python.network.NetworkCFunctions.createDataframe;
import static java.lang.Integer.MIN_VALUE;

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

    @CEntryPoint(name = "convertFortescueValueToThreePhase")
    public static PyPowsyblApiHeader.DataframeArrayPointer convertFortescueValueToThreePhase(PyPowsyblApiHeader.DataframePointer dataframePointer) {
        UpdatingDataframe df = createDataframe(dataframePointer);

        //TODO: check that names are the same as in the dataframes with results
        DoubleSeries positiveMagnitudeSeries = df.getDoubles("positive_magnitude");
        DoubleSeries zeroMagnitudeSeries = df.getDoubles("zero_magnitude");
        DoubleSeries negativeMagnitudeSeries = df.getDoubles("negative_magnitude");
        DoubleSeries positiveAngleSeries = df.getDoubles("positive_angle");
        DoubleSeries zeroAngleSeries = df.getDoubles("zero_angle");
        DoubleSeries negativeAngleSeries = df.getDoubles("negative_angle");

        List<FortescueValue.ThreePhaseValue> threePhaseValues = new ArrayList<>();
        for (int i = 0; i < df.getRowCount(); i++) {
            FortescueValue value = new FortescueValue(positiveMagnitudeSeries.get(i), zeroMagnitudeSeries.get(i), negativeMagnitudeSeries.get(i),
                    positiveAngleSeries.get(i), zeroAngleSeries.get(i), negativeAngleSeries.get(i));
            threePhaseValues.add(value.toThreePhaseValue());
        }
        // create dataframe with three phase value
        // return SeriesArray?

    }

    private static DataframeMapper<List<FortescueValue.ThreePhaseValue>> threePhaseValueDataframeMapper() {
        return new DataframeMapperBuilder<List<FortescueValue.ThreePhaseValue>, FortescueValue.ThreePhaseValue>()
                .itemsProvider()
                .doubles("magnitude_a", FortescueValue.ThreePhaseValue::getMagnitudeA)
                .doubles("magnitude_b", FortescueValue.ThreePhaseValue::getMagnitudeB)
                .doubles("magnitude_c", FortescueValue.ThreePhaseValue::getMagnitudeC)
                .doubles("angle_a", FortescueValue.ThreePhaseValue::getAngleA)
                .doubles("angle_b", FortescueValue.ThreePhaseValue::getAngleB)
                .doubles("angle_c", FortescueValue.ThreePhaseValue::getAngleC)
                .build();
    }

    //TODO: convertThreePhaseToFortescueValue method?

    private static ShortCircuitParameters createShortCircuitAnalysisParameters(PyPowsyblApiHeader.ShortCircuitAnalysisParametersPointer shortCircuitAnalysisParametersPointer) {
        return createShortCircuitAnalysisParameters().setWithLimitViolations(shortCircuitAnalysisParametersPointer.isWithLimitViolations())
                .setWithVoltageResult(shortCircuitAnalysisParametersPointer.isWithVoltageResult())
                .setWithFeederResult(shortCircuitAnalysisParametersPointer.isWithFeederResult())
                .setWithFortescueResult(shortCircuitAnalysisParametersPointer.isWithFortescueResult())
                .setWithLimitViolations(shortCircuitAnalysisParametersPointer.isWithLimitViolations())
                .setMinVoltageDropProportionalThreshold(shortCircuitAnalysisParametersPointer.getMinVoltageDropProportionalThreshold())
                .setStudyType(StudyType.values()[shortCircuitAnalysisParametersPointer.getStudyType()]);
    }

    private static Map<String, String> getSpecificParameters(PyPowsyblApiHeader.ShortCircuitAnalysisParametersPointer shortCircuitAnalysisParametersPointer) {
        return CTypeUtil.toStringMap(shortCircuitAnalysisParametersPointer.getProviderParametersKeys(),
                shortCircuitAnalysisParametersPointer.getProviderParametersKeysCount(),
                shortCircuitAnalysisParametersPointer.getProviderParametersValues(),
                shortCircuitAnalysisParametersPointer.getProviderParametersValuesCount());
    }
}
