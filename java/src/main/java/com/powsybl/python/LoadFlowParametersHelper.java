/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.commons.config.*;
import com.powsybl.loadflow.LoadFlowParameters;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;

import java.nio.file.FileSystems;
import java.util.*;

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
                                                       PyPowsyblApiHeader.LoadFlowParametersPointer pointer, PlatformConfig platformConfig) {
        PlatformConfigBasedRepository platformRepo = new PlatformConfigBasedRepository(platformConfig);
        InMemoryPlatformConfig ptrConfig = new InMemoryPlatformConfig(FileSystems.getDefault());
        PlatformConfigBasedRepository ptrRepo = new PlatformConfigBasedRepository(ptrConfig);
        MapModuleConfig ptrMapModuleConfig = ptrConfig.createModuleConfig("load-flow-default-parameters");
        if (pointer.getVoltageInitMode() != LoadFlowParameters.DEFAULT_VOLTAGE_INIT_MODE.ordinal()) {
            ptrMapModuleConfig.setStringProperty("voltageInitMode", LoadFlowParameters.VoltageInitMode.values()[pointer.getVoltageInitMode()].name());
        }
        if (pointer.isTransformerVoltageControlOn() != LoadFlowParameters.DEFAULT_TRANSFORMER_VOLTAGE_CONTROL_ON) {
            ptrMapModuleConfig.setStringProperty("transformerVoltageControlOn", String.valueOf(pointer.isTransformerVoltageControlOn()));
        }
        if (pointer.isNoGeneratorReactiveLimits() != LoadFlowParameters.DEFAULT_NO_GENERATOR_REACTIVE_LIMITS) {
            ptrMapModuleConfig.setStringProperty("noGeneratorReactiveLimits", String.valueOf(pointer.isNoGeneratorReactiveLimits()));
        }
        if (pointer.isPhaseShifterRegulationOn() != LoadFlowParameters.DEFAULT_PHASE_SHIFTER_REGULATION_ON) {
            ptrMapModuleConfig.setStringProperty("phaseShifterRegulationOn", String.valueOf(pointer.isPhaseShifterRegulationOn()));
        }
        if (pointer.isTwtSplitShuntAdmittance() != LoadFlowParameters.DEFAULT_TWT_SPLIT_SHUNT_ADMITTANCE) {
            ptrMapModuleConfig.setStringProperty("twtSplitShuntAdmittance", String.valueOf(pointer.isTwtSplitShuntAdmittance()));
        }
        if (pointer.isSimulShunt() != LoadFlowParameters.DEFAULT_SIMUL_SHUNT) {
            ptrMapModuleConfig.setStringProperty("simulShunt", String.valueOf(pointer.isSimulShunt()));
        }
        if (pointer.isReadSlackBus() != LoadFlowParameters.DEFAULT_READ_SLACK_BUS) {
            ptrMapModuleConfig.setStringProperty("readSlackBus", String.valueOf(pointer.isReadSlackBus()));
        }
        if (pointer.isWriteSlackBus() != LoadFlowParameters.DEFAULT_WRITE_SLACK_BUS) {
            ptrMapModuleConfig.setStringProperty("writeSlackBus", String.valueOf(pointer.isWriteSlackBus()));
        }
        if (pointer.isDistributedSlack() != LoadFlowParameters.DEFAULT_DISTRIBUTED_SLACK) {
            ptrMapModuleConfig.setStringProperty("distributedSlack", String.valueOf(pointer.isDistributedSlack()));
        }
        if (pointer.getBalanceType() != LoadFlowParameters.DEFAULT_BALANCE_TYPE.ordinal()) {
            ptrMapModuleConfig.setStringProperty("balanceType", LoadFlowParameters.BalanceType.values()[pointer.getBalanceType()].name());
        }
        if (pointer.isDcUseTransformerRatio() != LoadFlowParameters.DEFAULT_DC_USE_TRANSFORMER_RATIO_DEFAULT) {
            ptrMapModuleConfig.setStringProperty("dcUseTransformerRatio", String.valueOf(pointer.isDcUseTransformerRatio()));
        }
        if (pointer.isDistributedSlack() != LoadFlowParameters.DEFAULT_DISTRIBUTED_SLACK) {
            ptrMapModuleConfig.setStringProperty("distributedSlack", String.valueOf(pointer.isDistributedSlack()));
        }
        if (!countries.isEmpty()) {
            ptrMapModuleConfig.setStringListProperty("countriesToBalance", countries);
        }
        if (pointer.getConnectedComponentMode() != LoadFlowParameters.DEFAULT_CONNECTED_COMPONENT_MODE.ordinal()) {
            ptrMapModuleConfig.setStringProperty("connectedComponentMode", LoadFlowParameters.ConnectedComponentMode.values()[pointer.getConnectedComponentMode()].name());
        }
        ModuleConfigRepository others = new MapModuleConfigRepository(extensionConfigMap);
        StackedModuleConfigRepository stacked = new StackedModuleConfigRepository(others, ptrRepo, platformRepo);
        PlatformConfig merged = new PlatformConfig(stacked, platformConfig.getConfigDir());
        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load(merged);
        if (LoadFlowParameters.DEFAULT_BALANCE_TYPE != LoadFlowParameters.BalanceType.values()[pointer.getBalanceType()]) {
            loadFlowParameters.setBalanceType(LoadFlowParameters.BalanceType.values()[pointer.getBalanceType()]);
        }
        loadFlowParameters.setDc(dc);
        return loadFlowParameters;
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

}
