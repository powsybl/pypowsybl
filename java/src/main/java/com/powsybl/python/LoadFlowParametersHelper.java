/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.commons.config.ModuleConfig;
import com.powsybl.commons.config.ModuleConfigRepository;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.config.StackedModuleConfigRepository;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.iidm.network.Country;
import com.powsybl.loadflow.LoadFlowParameters;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
public final class LoadFlowParametersHelper {

    static LoadFlowParameters createLoadFlowParameters(boolean dc, PyPowsyblApiHeader.LoadFlowParametersPointer pointer, PlatformConfig platformConfig) {
        CCharPointerPointer keys = pointer.getOtherKeys();
        CCharPointerPointer values = pointer.getOtherValues();
        int size = pointer.getOtherKeysCount();
        Map<String, String> map = mergeListsToMap(keys, values, size);
        List<String> countries = CTypeUtil.toStringList(pointer.getCountriesToBalance(), pointer.getCountriesToBalanceCount());
        return createLoadFlowParameters(dc, map, countries, pointer, platformConfig);
    }

    static LoadFlowParameters createLoadFlowParameters(boolean dc,
                                                       Map<String, String> extensionConfigMap,
                                                       List<String> countries,
                                                       PyPowsyblApiHeader.LoadFlowParametersPointer loadFlowParametersPtr, PlatformConfig platformConfig) {
        var params = new LoadFlowParameters()
                .setVoltageInitMode(LoadFlowParameters.VoltageInitMode.values()[loadFlowParametersPtr.getVoltageInitMode()])
                .setTransformerVoltageControlOn(loadFlowParametersPtr.isTransformerVoltageControlOn())
                .setNoGeneratorReactiveLimits(loadFlowParametersPtr.isNoGeneratorReactiveLimits())
                .setPhaseShifterRegulationOn(loadFlowParametersPtr.isPhaseShifterRegulationOn())
                .setTwtSplitShuntAdmittance(loadFlowParametersPtr.isTwtSplitShuntAdmittance())
                .setSimulShunt(loadFlowParametersPtr.isSimulShunt())
                .setReadSlackBus(loadFlowParametersPtr.isReadSlackBus())
                .setWriteSlackBus(loadFlowParametersPtr.isWriteSlackBus())
                .setDistributedSlack(loadFlowParametersPtr.isDistributedSlack())
                .setDc(dc)
                .setBalanceType(LoadFlowParameters.BalanceType.values()[loadFlowParametersPtr.getBalanceType()])
                .setDcUseTransformerRatio(loadFlowParametersPtr.isDcUseTransformerRatio())
                .setCountriesToBalance(countries.stream().map(Country::valueOf).collect(Collectors.toSet()))
                .setConnectedComponentMode(LoadFlowParameters.ConnectedComponentMode.values()[loadFlowParametersPtr.getConnectedComponentMode()]);
        // add ext
        ModuleConfigRepository others = new MapModuleConfigRepository(extensionConfigMap);
        ModuleConfigRepository repository = loadFlowParametersPtr.isReadConfig() ?
                new StackedModuleConfigRepository(others, new PlatformConfigBasedRepository(platformConfig)) : others;
        PlatformConfig merged = new PlatformConfig(repository, platformConfig.getConfigDir());
        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load(merged);
        Collection<Extension<LoadFlowParameters>> extensions = loadFlowParameters.getExtensions();
        for (Extension<LoadFlowParameters> ext : extensions) {
            Class<? extends Extension> aClass = ext.getClass();
            ext.setExtendable(null);
            params.addExtension((Class) aClass, ext);
        }
        return params;
    }

    private LoadFlowParametersHelper() {
    }

    public static class PlatformConfigBasedRepository implements ModuleConfigRepository {

        private final PlatformConfig platformConfig;

        public PlatformConfigBasedRepository(PlatformConfig platformConfig) {
            this.platformConfig = Objects.requireNonNull(platformConfig);
        }

        @Override
        public boolean moduleExists(String s) {
            return platformConfig.moduleExists(s);
        }

        @Override
        public Optional<ModuleConfig> getModuleConfig(String s) {
            return platformConfig.getOptionalModuleConfig(s);
        }
    }

    private static Map<String, String> mergeListsToMap(CCharPointerPointer keysPtrPtr, CCharPointerPointer valuesPtrPtr, int mapSize) {
        List<String> parameterNames = CTypeUtil.toStringList(keysPtrPtr, mapSize);
        List<String> parameterValues = CTypeUtil.toStringList(valuesPtrPtr, mapSize);
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < mapSize; i++) {
            map.put(parameterNames.get(i), parameterValues.get(i));
        }
        return map;
    }
}
