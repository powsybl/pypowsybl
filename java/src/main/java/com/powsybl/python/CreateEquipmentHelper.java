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

    private static final String BUS_ID = "bus_id";
    private static final String BUS1_ID = "bus1_id";
    private static final String BUS2_ID = "bus2_id";
    private static final String CONNECTABLE_BUS_ID = "connectable_bus_id";
    private static final String CONNECTABLE_BUS1_ID = "connectable_bus1_id";
    private static final String CONNECTABLE_BUS2_ID = "connectable_bus2_id";
    private static final String SUBSTATION_ID = "substation_id";
    private static final String VOLTAGE_LEVEL_ID = "voltage_level_id";
    private static final String VOLTAGE_LEVEL1_ID = "voltage_level1_id";
    private static final String VOLTAGE_LEVEL2_ID = "voltage_level2_id";
    private static final String NODE1 = "node1";
    private static final String NODE2 = "node2";

    private static final Map<String, SeriesDataType> INJECTION_MAPS = Map.of(
            CONNECTABLE_BUS_ID, SeriesDataType.STRING
    );
    private static final Map<String, SeriesDataType> LOAD_MAPS = INJECTION_MAPS;
    private static final Map<String, SeriesDataType> GEN_MAPS = INJECTION_MAPS;
    private static final Map<String, SeriesDataType> BAT_MAPS = INJECTION_MAPS;

    static void createElement(PyPowsyblApiHeader.ElementType elementType, Network network,
                              Map<String, Double> doubleMap, Map<String, String> strMap, Map<String, Integer> intMap) {
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
            case DANGLING_LINE:
                createDanglingLine(network, doubleMap, strMap, intMap);
                break;
            case SHUNT_COMPENSATOR:
                createShunt(network, doubleMap, strMap, intMap);
                break;
            case STATIC_VAR_COMPENSATOR:
                createSVC(network, doubleMap, strMap, intMap);
                break;
            case VSC_CONVERTER_STATION:
                createVSC(network, doubleMap, strMap, intMap);
                break;
            case LCC_CONVERTER_STATION:
                createLCC(network, doubleMap, strMap, intMap);
                break;
            case LINE:
                createLine(network, doubleMap, strMap, intMap);
                break;
            case TWO_WINDINGS_TRANSFORMER:
                createTwt2(network, doubleMap, strMap, intMap);
                break;
            default:
                throw new PowsyblException();
        }
    }

    private static void createTwt2(Network network, Map<String, Double> doubleMap, Map<String, String> strMap, Map<String, Integer> intMap) {
        var adder = network.getSubstation(strMap.get(SUBSTATION_ID)).newTwoWindingsTransformer();
        createBranch(adder, doubleMap, strMap, intMap);
        adder.setRatedU1(orElseFloatNan(doubleMap, "rated_u1"))
                .setRatedU2(orElseFloatNan(doubleMap, "rated_u1"))
                .setRatedS(orElseFloatNan(doubleMap, "rated_s"))
                .setB(orElseNan(doubleMap, "b"))
                .setG(orElseNan(doubleMap, "g"))
                .setR(orElseNan(doubleMap, "r"))
                .setX(orElseNan(doubleMap, "x"));
        adder.add();
    }

    private static void createLine(Network network, Map<String, Double> doubleMap, Map<String, String> strMap, Map<String, Integer> intMap) {
        LineAdder lineAdder = network.newLine();
        createBranch(lineAdder, doubleMap, strMap, intMap);
        lineAdder.setB1(orElseNan(doubleMap, "b1"))
                .setB2(orElseNan(doubleMap, "b2"))
                .setG1(orElseNan(doubleMap, "g1"))
                .setG2(orElseNan(doubleMap, "g2"))
                .setR(orElseNan(doubleMap, "r"))
                .setX(orElseNan(doubleMap, "x"));
        lineAdder.add();
    }

    private static void createBranch(BranchAdder adder, Map<String, Double> doubleMap, Map<String, String> strMap, Map<String, Integer> intMap) {
        createIdentifiable(adder, strMap);
        adder.setBus1(strMap.get(BUS1_ID))
                .setBus2(strMap.get(BUS2_ID))
                .setVoltageLevel1(strMap.get(VOLTAGE_LEVEL1_ID))
                .setVoltageLevel2(strMap.get(VOLTAGE_LEVEL2_ID));
        if (strMap.containsKey(CONNECTABLE_BUS1_ID)) {
            adder.setConnectableBus1(strMap.get(CONNECTABLE_BUS1_ID))
                    .setConnectableBus2(strMap.get(CONNECTABLE_BUS2_ID));
        } else if (strMap.containsKey(NODE1)) {
            adder.setNode1(intMap.get(NODE1))
                    .setNode2(intMap.get(NODE2));
        } else {
            throw new PowsyblException("Connectable bus or node should be set");
        }
    }

    private static void createLCC(Network network, Map<String, Double> doubleMap, Map<String, String> strMap, Map<String, Integer> intMap) {
        LccConverterStationAdder adder = network.getVoltageLevel(strMap.get(VOLTAGE_LEVEL_ID)).newLccConverterStation();
        createHvdc(adder, doubleMap, strMap, intMap);
        adder.setPowerFactor(orElseFloatNan(doubleMap, "power_factor"));
        adder.add();
    }

    private static void createHvdc(HvdcConverterStationAdder adder, Map<String, Double> doubleMap, Map<String, String> strMap, Map<String, Integer> intMap) {
        createInjection(adder, strMap);
        adder.setLossFactor(orElseFloatNan(doubleMap, "loss_factor"));
    }

    private static void createVSC(Network network, Map<String, Double> doubleMap, Map<String, String> strMap, Map<String, Integer> intMap) {
        VscConverterStationAdder adder = network.getVoltageLevel(strMap.get(VOLTAGE_LEVEL_ID)).newVscConverterStation();
        createHvdc(adder, doubleMap, strMap, intMap);
        adder.setVoltageSetpoint(orElseNan(doubleMap, "voltage_setpoint"))
                .setReactivePowerSetpoint(orElseNan(doubleMap, "reactive_power_setpoint"))
                // TODO NPL
                .setVoltageRegulatorOn(intMap.get("voltage_regulator_on") == 1);
        adder.add();
    }

    private static void createDanglingLine(Network network, Map<String, Double> doubleMap, Map<String, String> strMap, Map<String, Integer> intMap) {
        DanglingLineAdder adder = network.getVoltageLevel(strMap.get(VOLTAGE_LEVEL_ID)).newDanglingLine();
        createInjection(adder, strMap);
        adder.setP0(orElseNan(doubleMap, "p0"))
                .setQ0(orElseNan(doubleMap, "q0"))
                .setR(orElseNan(doubleMap, "r"))
                .setX(orElseNan(doubleMap, "x"))
                .setG(orElseNan(doubleMap, "g"))
                .setB(orElseNan(doubleMap, "b"));
        adder.add();
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

    private static float orElseFloatNan(Map<String, Double> doubleMap, String field) {
        return Optional.ofNullable(doubleMap.get(field))
                .map((Double::floatValue)).orElse(Float.NaN);
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
