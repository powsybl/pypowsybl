/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.SeriesDataType;
import com.powsybl.iidm.network.EnergySource;
import com.powsybl.iidm.network.GeneratorAdder;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;

import java.util.Map;
import java.util.Optional;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
final class CreateEquipmentHelper {

    private static final Map<String, SeriesDataType> LOAD_MAPS = Map.of(
                "connectable_bus_id", SeriesDataType.STRING,
                "voltage_level_id", SeriesDataType.STRING
            );

    static void createElement(PyPowsyblApiHeader.ElementType elementType, Network network, String id,
                              Map<String, Double> doubleMap, Map<String, String> strMap, Map<String, Integer> intMap) {
        switch (elementType) {
            case LOAD:
                createLoad(network, id, doubleMap, strMap);
                break;
            case GENERATOR:
                createGenerator(network, id, doubleMap, strMap, intMap);
                break;
            default:
                throw new PowsyblException();
        }
    }

    private static void createLoad(Network network, String id, Map<String, Double> doubleMap, Map<String, String> strMap) {
        String vlId = strMap.get("voltage_level_id");
        VoltageLevel voltageLevel = network.getVoltageLevel(vlId);
        voltageLevel.newLoad()
                .setId(id)
                .setP0(orElseNan(doubleMap, "p0"))
                .setQ0(orElseNan(doubleMap, "q0"))
                .setConnectableBus(strMap.get("connectable_bus_id"))
                .setBus(strMap.get("bus_id"))
                .add();
    }

    private static void createGenerator(Network network, String id, Map<String, Double> doubleMap,
                                        Map<String, String> strMap, Map<String, Integer> intMap) {
        String vlId = strMap.get("voltage_level_id");
        VoltageLevel voltageLevel = network.getVoltageLevel(vlId);
        GeneratorAdder generatorAdder = voltageLevel.newGenerator()
                .setId(id)
                .setMaxP(orElseNan(doubleMap, "max_p"))
                .setMinP(orElseNan(doubleMap, "min_p"))
                .setTargetP(orElseNan(doubleMap, "target_p"))
                .setTargetQ(orElseNan(doubleMap, "target_q"))
                .setTargetV(orElseNan(doubleMap, "target_v"))
                .setRatedS(orElseNan(doubleMap, "rated_s"))
                .setConnectableBus(strMap.get("connectable_bus_id"))
                .setVoltageRegulatorOn(intMap.get("voltage_regulator_on") == 1)
                .setBus(strMap.get("bus_id"));
        Optional.ofNullable(strMap.get("energy_source")).ifPresentOrElse(v -> generatorAdder.setEnergySource(EnergySource.valueOf(v)), () -> { });
        generatorAdder.add();
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
