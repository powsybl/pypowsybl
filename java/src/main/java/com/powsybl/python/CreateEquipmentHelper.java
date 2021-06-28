/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesDataType;
import com.powsybl.iidm.network.*;

import java.util.Map;
import java.util.Optional;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
final class CreateEquipmentHelper {

    private static final String CONNECTABLE_BUS_ID = "connectable_bus_id";
    private static final String VOLTAGE_LEVEL_ID = "voltage_level_id";

    private static final Map<String, SeriesDataType> INJECTION_MAPS = Map.of(
            CONNECTABLE_BUS_ID, SeriesDataType.STRING
    );
    private static final Map<String, SeriesDataType> LOAD_MAPS = INJECTION_MAPS;
    private static final Map<String, SeriesDataType> GEN_MAPS = INJECTION_MAPS;
    private static final Map<String, SeriesDataType> BAT_MAPS = INJECTION_MAPS;

    static void createElement(PyPowsyblApiHeader.ElementType elementType, Network network,                              Map<String, Double> doubleMap, Map<String, String> strMap, Map<String, Integer> intMap) {
        switch (elementType) {
            case LOAD:
                createLoad(network, doubleMap, strMap);
                break;
            case GENERATOR:
                createGenerator(network, doubleMap, strMap, intMap);
                break;
            case BUSBAR_SECTION:
                createBusbar(network, doubleMap, strMap, intMap);
                break;
            case BATTERY:
                createBat(network, doubleMap, strMap, intMap);
                break;
            case SHUNT_COMPENSATOR:
                createShunt(network, doubleMap, strMap, intMap);
                break;
            case STATIC_VAR_COMPENSATOR:
                createSVC(network, doubleMap, strMap, intMap);
                break;
            default:
                throw new PowsyblException();
        }
    }

    private static void createLoad(Network network, Map<String, Double> doubleMap, Map<String, String> strMap) {
        LoadAdder loadAdder = network.getVoltageLevel(strMap.get(VOLTAGE_LEVEL_ID)).newLoad();
        Optional.ofNullable(strMap.get("type")).ifPresent(t -> loadAdder.setLoadType(LoadType.valueOf(t)));
        createInjection(loadAdder, strMap);
        loadAdder.setP0(orElseNan(doubleMap, "p0"))
                .setQ0(orElseNan(doubleMap, "q0"))
                .add();
    }

    private static void createBusbar(Network network, Map<String, Double> doubleMap, Map<String, String> strMap, Map<String, Integer> intMap) {
        BusbarSectionAdder adder = network.getVoltageLevel(strMap.get(VOLTAGE_LEVEL_ID)).getNodeBreakerView()
                .newBusbarSection();
        createIdentifiable(adder, strMap);
        adder.setNode(intMap.get("node"));
        adder.add();
    }

    private static void createGenerator(Network network, Map<String, Double> doubleMap,
                                        Map<String, String> strMap, Map<String, Integer> intMap) {
        GeneratorAdder generatorAdder = network.getVoltageLevel(strMap.get(VOLTAGE_LEVEL_ID)).newGenerator();
        createInjection(generatorAdder, strMap);
        generatorAdder.setMaxP(orElseNan(doubleMap, "max_p"))
                .setMinP(orElseNan(doubleMap, "min_p"))
                .setTargetP(orElseNan(doubleMap, "target_p"))
                .setTargetQ(orElseNan(doubleMap, "target_q"))
                .setTargetV(orElseNan(doubleMap, "target_v"))
                .setRatedS(orElseNan(doubleMap, "rated_s"))
                .setVoltageRegulatorOn(intMap.get("voltage_regulator_on") == 1);
        Optional.ofNullable(strMap.get("energy_source")).ifPresent(v -> generatorAdder.setEnergySource(EnergySource.valueOf(v)));
        generatorAdder.add();
    }

    private static void createBat(Network network, Map<String, Double> doubleMap,
                                  Map<String, String> strMap, Map<String, Integer> intMap) {
        BatteryAdder batteryAdder = network.getVoltageLevel(strMap.get(VOLTAGE_LEVEL_ID))
                .newBattery();
        createInjection(batteryAdder, strMap);
        batteryAdder.setMaxP(orElseNan(doubleMap, "max_p"))
                .setMinP(orElseNan(doubleMap, "min_p"))
                .setP0(orElseNan(doubleMap, "p0"))
                .setQ0(orElseNan(doubleMap, "q0"))
                .add();
    }

    private static void createShunt(Network network, Map<String, Double> doubleMap,
                                    Map<String, String> strMap, Map<String, Integer> intMap) {
        ShuntCompensatorAdder adder = network.getVoltageLevel(strMap.get(VOLTAGE_LEVEL_ID))
                .newShuntCompensator();
        createInjection(adder, strMap);
        adder.setSectionCount(intMap.get("section_count"))
                .setTargetDeadband(orElseNan(doubleMap, "target_deadband"))
                .setTargetV(orElseNan(doubleMap, "target_v"));
        adder.add();
    }

    private static void createSVC(Network network, Map<String, Double> doubleMap,
                                  Map<String, String> strMap, Map<String, Integer> intMap) {
        StaticVarCompensatorAdder adder = network.getVoltageLevel(strMap.get(VOLTAGE_LEVEL_ID))
                .newStaticVarCompensator();
        createInjection(adder, strMap);
        adder.setBmax(orElseNan(doubleMap, "b_max"))
                .setBmin(orElseNan(doubleMap, "b_min"))
                .setRegulationMode(StaticVarCompensator.RegulationMode.valueOf(strMap.get("regulation_mode")))
                .setVoltageSetpoint(orElseNan(doubleMap, "voltage_setpoint"))
                .setReactivePowerSetpoint(orElseNan(doubleMap, "reactive_power_setpoint"))
                .add();
    }

    private static void createIdentifiable(IdentifiableAdder adder, Map<String, String> strMap) {
        adder.setId(strMap.get("id"));
        Optional.ofNullable(strMap.get("name")).ifPresent(adder::setName);
    }

    private static void createInjection(InjectionAdder adder, Map<String, String> strMap) {
        createIdentifiable(adder, strMap);
        adder.setConnectableBus(strMap.get(CONNECTABLE_BUS_ID))
                .setBus(strMap.get("bus_id"));
    }

    private static double orElseNan(Map<String, Double> doubleMap, String field) {
        return Optional.ofNullable(doubleMap.get(field)).orElse(Double.NaN);
    }

    static int getAdderSeriesType(PyPowsyblApiHeader.ElementType type, String fieldName) {
        SeriesDataType seriesDataType;
        switch (type) {
            case LOAD:
                seriesDataType = LOAD_MAPS.get(fieldName);
                break;
            case GENERATOR:
                seriesDataType = GEN_MAPS.get(fieldName);
                break;
            case BATTERY:
                seriesDataType = BAT_MAPS.get(fieldName);
                break;
            default:
                throw new RuntimeException("Unexpected " + type + " adder.");
        }
        if (seriesDataType == null) {
            throw new RuntimeException("Field '" + fieldName + "' not found for " + type + " adder.");
        }
        return CDataframeHandler.convert(seriesDataType);
    }

    private CreateEquipmentHelper() {
    }
}
