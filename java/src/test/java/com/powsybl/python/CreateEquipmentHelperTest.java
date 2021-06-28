/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.python;

import com.powsybl.iidm.network.LoadType;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.HvdcTestNetwork;
import com.powsybl.iidm.network.test.ShuntTestCaseFactory;
import com.powsybl.iidm.network.test.SvcTestCaseFactory;
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
