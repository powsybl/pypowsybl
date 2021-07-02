/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.iidm.network.LoadType;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.test.*;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
class CreateEquipmentHelperTest {

    @Test
    void twt2() {
        var network = EurostagTutorialExample1Factory.create();
        Map<String, Double> doubleMap = Map.ofEntries(
                entry("rated_u1", 4.0),
                entry("rated_u2", 4.0),
                entry("rated_s", 4.0),
                entry("r", 4.0),
                entry("x", 4.0),
                entry("g", 4.0),
                entry("b", 4.0)
        );
        Map<String, String> strMap = Map.ofEntries(
                entry("id", "test"),
                entry("substation_id", "P1"),
                entry("name", "l3"),
                entry("bus1_id", "NGEN"),
                entry("bus2_id", "NHV1"),
                entry("voltage_level1_id", "VLGEN"),
                entry("voltage_level2_id", "VLHV1"),
                entry("connectable_bus1_id", "NGEN"),
                entry("connectable_bus2_id", "NHV1"));
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.TWO_WINDINGS_TRANSFORMER, network, doubleMap, strMap, Collections.emptyMap());
        assertEquals(3, network.getTwoWindingsTransformerCount());
    }

    @Test
    void line() {
        var network = EurostagTutorialExample1Factory.create();
        Map<String, Double> doubleMap = Map.ofEntries(
                entry("r", 4.0),
                entry("x", 4.0),
                entry("g1", 4.0),
                entry("b1", 4.0),
                entry("g2", 4.0),
                entry("b2", 4.0)
        );
        Map<String, String> strMap = Map.ofEntries(
                entry("id", "L3"),
                entry("name", "l3"),
                entry("bus1_id", "NHV1"),
                entry("bus2_id", "NHV2"),
                entry("voltage_level1_id", "VLHV1"),
                entry("voltage_level2_id", "VLHV2"),
                entry("connectable_bus1_id", "NHV1"),
                entry("connectable_bus2_id", "NHV2"));
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.LINE, network, doubleMap, strMap, Collections.emptyMap());
        assertEquals(3, network.getLineCount());
    }

    @Test
    void lcc() {
        var network = HvdcTestNetwork.createLcc();
        Map<String, String> strMap = Map.ofEntries(
                entry("id", "C3"),
                entry("name", "name-c3"),
                entry("connectable_bus_id", "B1"),
                entry("bus_id", "B1"),
                entry("voltage_level_id", "VL1"));
        Map<String, Double> doubleMap = Map.ofEntries(
                entry("loss_factor", 0.9d),
                entry("power_factor", 0.9d)
        );
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.LCC_CONVERTER_STATION, network, doubleMap, strMap, Collections.emptyMap());
        assertEquals(3, network.getLccConverterStationCount());
    }

    @Test
    void vsc() {
        var network = HvdcTestNetwork.createVsc();
        Map<String, String> strMap = Map.ofEntries(
                entry("id", "C3"),
                entry("name", "name-c3"),
                entry("connectable_bus_id", "B1"),
                entry("bus_id", "B1"),
                entry("voltage_level_id", "VL1"));
        Map<String, Double> doubleMap = Map.ofEntries(
                entry("voltage_setpoint", 0.6d),
                entry("reactive_power_setpoint", 1d),
                entry("loss_factor", 0.9d)
        );
        Map<String, Integer> intMap = Map.ofEntries(
                entry("voltage_regulator_on", 0)
        );
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.VSC_CONVERTER_STATION, network, doubleMap, strMap, intMap);
        assertEquals(3, network.getVscConverterStationCount());
    }

    @Test
    void danglingLine() {
        var network = DanglingLineNetworkFactory.create();
        Map<String, String> strMap = Map.ofEntries(
                entry("id", "dl2"),
                entry("name", "name-dl2"),
                entry("connectable_bus_id", "BUS"),
                entry("bus_id", "BUS"),
                entry("voltage_level_id", "VL"));
        Map<String, Double> doubleMap = Map.ofEntries(
                entry("r", 0.6d),
                entry("x", 1d),
                entry("g", Math.pow(10, -6)),
                entry("b", Math.pow(10, -6) * 4),
                entry("p0", 102d),
                entry("q0", 151d)
        );
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.DANGLING_LINE, network, doubleMap, strMap, Collections.emptyMap());
        assertEquals(2, network.getDanglingLineCount());
    }

    @Test
    void busbar() {
        var network = HvdcTestNetwork.createBase();
        Map<String, Integer> intMap = Map.of("node", 1);
        Map<String, String> strMap = Map.ofEntries(
                entry("id", "bs2"),
                entry("name", "name-bs2"),
                entry("voltage_level_id", "VL2"));
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.BUSBAR_SECTION, network, Collections.emptyMap(), strMap, intMap);
        assertEquals(2, network.getBusbarSectionCount());
    }

    @Test
    void load() {
        var network = EurostagTutorialExample1Factory.create();
        Map<String, Double> doubleMap = Map.ofEntries(
                entry("p0", 3.0d),
                entry("q0", 1.0d),
                entry("target_p", 4.0d),
                entry("target_v", 6.0d),
                entry("target_q", 7.0d),
                entry("rated_s", 5.0d));
        Map<String, String> strMap = Map.ofEntries(
                entry("id", "LOAD2"),
                entry("voltage_level_id", "VLLOAD"),
                entry("connectable_bus_id", "NLOAD"),
                entry("bus_id", "NLOAD"));
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.LOAD, network, doubleMap, strMap, Collections.emptyMap());
        assertEquals(2, network.getLoadCount());
        assertEquals(LoadType.UNDEFINED, network.getLoad("LOAD2").getLoadType());

        Map<String, String> map = new HashMap<>(strMap);
        map.put("type", LoadType.AUXILIARY.name());
        map.put("id", "LOAD3");
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.LOAD, network, doubleMap, map, Collections.emptyMap());
        assertEquals(LoadType.AUXILIARY, network.getLoad("LOAD3").getLoadType());
    }

    @Test
    void generator() {
        var network = EurostagTutorialExample1Factory.create();
        Map<String, Double> doubleMap = Map.ofEntries(
                entry("max_p", 3.0d),
                entry("min_p", 1.0d),
                entry("target_p", 4.0d),
                entry("target_v", 6.0d),
                entry("target_q", 7.0d),
                entry("rated_s", 5.0d));
        Map<String, String> strMap = Map.ofEntries(
                entry("id", "test"),
                entry("voltage_level_id", "VLGEN"),
                entry("connectable_bus_id", "NGEN"),
                entry("bus_id", "NGEN"));
        Map<String, Integer> intMap = Map.of("voltage_regulator_on", 1);
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.GENERATOR, network, doubleMap, strMap, intMap);
        assertEquals(2, network.getGeneratorCount());
    }

    @Test
    void shunt() {
        var network = ShuntTestCaseFactory.create();
        Map<String, Double> doubleMap = Map.ofEntries(
                entry("b", 1.0),
                entry("g", 2.0),
                entry("target_v", 30.0),
                entry("target_deadband", 4.0)
        );
        Map<String, Integer> ints = Map.ofEntries(
                entry("section_count", 4)
        );
        Map<String, String> strMap = Map.ofEntries(
                entry("id", "SHUNT2"),
                entry("voltage_level_id", "VL1"),
                entry("connectable_bus_id", "B1"),
                entry("bus_id", "B1"));
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.SHUNT_COMPENSATOR,
                network, doubleMap, strMap, ints);
        assertEquals(2, network.getShuntCompensatorCount());
    }

    @Test
    void svc() {
        var network = SvcTestCaseFactory.create();
        Map<String, Double> doubleMap = Map.ofEntries(
                entry("b_min", 0.0003),
                entry("b_max", 0.0009),
                entry("target_v", 30.0),
                entry("voltage_setpoint", 391.0)
        );
        Map<String, Integer> ints = Map.ofEntries(
                entry("section_count", 4)
        );
        var mode = StaticVarCompensator.RegulationMode.OFF;
        Map<String, String> strMap = Map.ofEntries(
                entry("id", "SVC"),
                entry("voltage_level_id", "VL2"),
                entry("connectable_bus_id", "B2"),
                entry("regulation_mode", mode.name()),
                entry("bus_id", "B2"));
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.STATIC_VAR_COMPENSATOR,
                network, doubleMap, strMap, ints);
        assertEquals(2, network.getStaticVarCompensatorCount());
        assertEquals(mode, network.getStaticVarCompensator("SVC").getRegulationMode());
    }
}
