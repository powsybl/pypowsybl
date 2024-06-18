/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.loadflow;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.parameters.Parameter;
import com.powsybl.commons.parameters.ParameterType;
import com.powsybl.dataframe.DataframeMapper;
import com.powsybl.dataframe.DataframeMapperBuilder;
import com.powsybl.iidm.network.Country;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowProvider;
import com.powsybl.python.commons.CTypeUtil;
import com.powsybl.python.commons.PyPowsyblApiHeader.LoadFlowParametersPointer;
import com.powsybl.python.commons.PyPowsyblConfiguration;
import org.graalvm.nativeimage.UnmanagedMemory;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
public final class LoadFlowCUtils {

    static final DataframeMapper<LoadFlowProvider, Void> SPECIFIC_PARAMETERS_MAPPER = new DataframeMapperBuilder<LoadFlowProvider, Parameter, Void>()
            .itemsProvider(LoadFlowProvider::getSpecificParameters)
            .stringsIndex("name", Parameter::getName)
            .strings("description", Parameter::getDescription)
            .enums("type", ParameterType.class, Parameter::getType)
            .strings("default", p -> Objects.toString(p.getDefaultValue(), ""))
            .strings("possible_values", p -> p.getPossibleValues() == null ? "" : p.getPossibleValues().toString())
            .build();

    private LoadFlowCUtils() {
    }

    public static LoadFlowProvider getLoadFlowProvider(String name) {
        String actualName = name.isEmpty() ? PyPowsyblConfiguration.getDefaultLoadFlowProvider() : name;
        return LoadFlowProvider.findAll().stream()
                .filter(provider -> provider.getName().equals(actualName))
                .findFirst()
                .orElseThrow(() -> new PowsyblException("No loadflow provider for name '" + actualName + "'"));
    }

    private static Map<String, String> getSpecificParameters(LoadFlowParametersPointer loadFlowParametersPtr) {
        return CTypeUtil.toStringMap(loadFlowParametersPtr.getProviderParametersKeys(),
                loadFlowParametersPtr.getProviderParametersKeysCount(),
                loadFlowParametersPtr.getProviderParametersValues(),
                loadFlowParametersPtr.getProviderParametersValuesCount());
    }

    public static LoadFlowParameters createLoadFlowParameters() {
        return PyPowsyblConfiguration.isReadConfig() ? LoadFlowParameters.load() : new LoadFlowParameters();
    }

    public static LoadFlowParameters convertLoadFlowParameters(boolean dc,
                                                               LoadFlowParametersPointer loadFlowParametersPtr) {
        return createLoadFlowParameters()
                .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.values()[loadFlowParametersPtr.getVoltageInitMode()])
                .setTransformerVoltageControlOn(loadFlowParametersPtr.isTransformerVoltageControlOn())
                .setUseReactiveLimits(loadFlowParametersPtr.isUseReactiveLimits())
                .setPhaseShifterRegulationOn(loadFlowParametersPtr.isPhaseShifterRegulationOn())
                .setTwtSplitShuntAdmittance(loadFlowParametersPtr.isTwtSplitShuntAdmittance())
                .setShuntCompensatorVoltageControlOn(loadFlowParametersPtr.isShuntCompensatorVoltageControlOn())
                .setReadSlackBus(loadFlowParametersPtr.isReadSlackBus())
                .setWriteSlackBus(loadFlowParametersPtr.isWriteSlackBus())
                .setDistributedSlack(loadFlowParametersPtr.isDistributedSlack())
                .setDc(dc)
                .setBalanceType(LoadFlowParameters.BalanceType.values()[loadFlowParametersPtr.getBalanceType()])
                .setDcUseTransformerRatio(loadFlowParametersPtr.isDcUseTransformerRatio())
                .setCountriesToBalance(CTypeUtil.toStringList(loadFlowParametersPtr.getCountriesToBalance(), loadFlowParametersPtr.getCountriesToBalanceCount())
                        .stream().map(Country::valueOf).collect(Collectors.toSet()))
                .setConnectedComponentMode(LoadFlowParameters.ConnectedComponentMode.values()[loadFlowParametersPtr.getConnectedComponentMode()]);
    }

    /**
     * Creates loadflow parameters from its C representation, for the given loadflow provider.
     * The provider is used to instantiate implementation-specific parameters.
     */
    public static LoadFlowParameters createLoadFlowParameters(boolean dc, LoadFlowParametersPointer cParameters,
                                                              LoadFlowProvider provider) {
        LoadFlowParameters parameters = convertLoadFlowParameters(dc, cParameters);
        Map<String, String> specificParametersProperties = getSpecificParameters(cParameters);

        provider.loadSpecificParameters(specificParametersProperties).ifPresent(ext -> {
            // Dirty trick to get the class, and reload parameters if they exist.
            // TODO: SPI needs to be changed so that we don't need to read params to get the class
            Extension<LoadFlowParameters> configured = parameters.getExtension(ext.getClass());
            if (configured != null) {
                provider.updateSpecificParameters(configured, specificParametersProperties);
            } else {
                parameters.addExtension((Class) ext.getClass(), ext);
            }
        });
        return parameters;
    }

    public static LoadFlowParameters createLoadFlowParameters(boolean dc, LoadFlowParametersPointer cParameters,
                                                              String providerName) {
        return createLoadFlowParameters(dc, cParameters, getLoadFlowProvider(providerName));
    }

    /**
     * Frees inner memory, but not the pointer itself.
     */
    public static void freeLoadFlowParametersContent(LoadFlowParametersPointer parameters) {
        for (int i = 0; i < parameters.getCountriesToBalanceCount(); i++) {
            UnmanagedMemory.free(parameters.getCountriesToBalance().read(i));
        }
        UnmanagedMemory.free(parameters.getCountriesToBalance());
    }
}
