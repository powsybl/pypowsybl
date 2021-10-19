/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe;

import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.LoadType;
import com.powsybl.iidm.network.ShuntCompensatorLinearModel;
import com.powsybl.iidm.network.ShuntCompensatorNonLinearModel;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.test.*;
import com.powsybl.python.PyPowsyblApiHeader;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.iidm.network.ShuntCompensatorModelType.LINEAR;
import static com.powsybl.iidm.network.ShuntCompensatorModelType.NON_LINEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
class CreateEquipmentHelperTest {

    @Test
    void twt2() {
        var network = EurostagTutorialExample1Factory.create();
        TestDataframe dataframe = new TestDataframe(1);
        addSingletonStringColumnAndValue(dataframe, "id", "test");
        addSingletonStringColumnAndValue(dataframe, "substation_id", "P1");
        addSingletonStringColumnAndValue(dataframe, "name", "l3");
        addSingletonStringColumnAndValue(dataframe, "bus1_id", "NGEN");
        addSingletonStringColumnAndValue(dataframe, "bus2_id", "NHV1");
        addSingletonStringColumnAndValue(dataframe, "voltage_level1_id", "VLGEN");
        addSingletonStringColumnAndValue(dataframe, "voltage_level2_id", "VLHV1");
        addSingletonStringColumnAndValue(dataframe, "connectable_bus1_id", "NGEN");
        addSingletonStringColumnAndValue(dataframe, "connectable_bus2_id", "NHV1");
        addSingletonDoubleColumnAndValue(dataframe, "rated_u1", 4.0);
        addSingletonDoubleColumnAndValue(dataframe, "rated_u2", 4.0);
        addSingletonDoubleColumnAndValue(dataframe, "rated_s", 4.0);
        addSingletonDoubleColumnAndValue(dataframe, "r", 4.0);
        addSingletonDoubleColumnAndValue(dataframe, "x", 4.0);
        addSingletonDoubleColumnAndValue(dataframe, "g", 4.0);
        addSingletonDoubleColumnAndValue(dataframe, "b", 4.0);
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.TWO_WINDINGS_TRANSFORMER, network, Collections.singletonList(dataframe));
        assertEquals(3, network.getTwoWindingsTransformerCount());
    }

    private void addSingletonStringColumnAndValue(TestDataframe dataframe, String column, String... value) {
        dataframe.addColumnName(column, SeriesDataType.STRING, false);
        dataframe.addSeries(new TestDataframe.TestStringSeries(Arrays.asList(value), column));
    }

    private void addSingletonDoubleColumnAndValue(TestDataframe dataframe, String column, double... value) {
        dataframe.addColumnName(column, SeriesDataType.DOUBLE, false);
        dataframe.addSeries(new TestDataframe.TestDoubleSeries(Arrays.stream(value).boxed().collect(Collectors.toList()), column));
    }

    private void addSingletonIntColumnAndValue(TestDataframe dataframe, String column, int... value) {
        dataframe.addColumnName(column, SeriesDataType.DOUBLE, false);
        dataframe.addSeries(new TestDataframe.TestIntSeries(Arrays.stream(value).boxed().collect(Collectors.toList()), column));
    }

    @Test
    void line() {
        var network = EurostagTutorialExample1Factory.create();
        TestDataframe dataframe = new TestDataframe(1);
        addSingletonDoubleColumnAndValue(dataframe, "r", 4.0);
        addSingletonDoubleColumnAndValue(dataframe, "x", 4.0);
        addSingletonDoubleColumnAndValue(dataframe, "g1", 4.0);
        addSingletonDoubleColumnAndValue(dataframe, "b1", 4.0);
        addSingletonDoubleColumnAndValue(dataframe, "g2", 4.0);
        addSingletonDoubleColumnAndValue(dataframe, "b2", 4.0);
        addSingletonStringColumnAndValue(dataframe, "id", "L3");
        addSingletonStringColumnAndValue(dataframe, "name", "l3");
        addSingletonStringColumnAndValue(dataframe, "bus1_id", "NHV1");
        addSingletonStringColumnAndValue(dataframe, "bus2_id", "NHV2");
        addSingletonStringColumnAndValue(dataframe, "voltage_level1_id", "VLHV1");
        addSingletonStringColumnAndValue(dataframe, "voltage_level2_id", "VLHV2");
        addSingletonStringColumnAndValue(dataframe, "connectable_bus1_id", "NHV1");
        addSingletonStringColumnAndValue(dataframe, "connectable_bus2_id", "NHV2");
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.LINE, network, Collections.singletonList(dataframe));
        assertEquals(3, network.getLineCount());
    }

    @Test
    void lcc() {
        var network = HvdcTestNetwork.createLcc();
        TestDataframe dataframe = new TestDataframe(1);
        addSingletonStringColumnAndValue(dataframe, "id", "C3");
        addSingletonStringColumnAndValue(dataframe, "name", "name-c3");
        addSingletonStringColumnAndValue(dataframe, "connectable_bus_id", "B1");
        addSingletonStringColumnAndValue(dataframe, "bus_id", "B1");
        addSingletonStringColumnAndValue(dataframe, "voltage_level_id", "VL1");
        addSingletonDoubleColumnAndValue(dataframe, "loss_factor", 0.9d);
        addSingletonDoubleColumnAndValue(dataframe, "power_factor", 0.9d);
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.LCC_CONVERTER_STATION, network, Collections.singletonList(dataframe));
        assertEquals(3, network.getLccConverterStationCount());
    }

    @Test
    void vsc() {
        var network = HvdcTestNetwork.createVsc();
        TestDataframe dataframe = new TestDataframe(1);
        addSingletonStringColumnAndValue(dataframe, "id", "C3");
        addSingletonStringColumnAndValue(dataframe, "name", "name-c3");
        addSingletonStringColumnAndValue(dataframe, "connectable_bus_id", "B1");
        addSingletonStringColumnAndValue(dataframe, "bus_id", "B1");
        addSingletonStringColumnAndValue(dataframe, "voltage_level_id", "VL1");
        addSingletonDoubleColumnAndValue(dataframe, "voltage_setpoint", 0.6d);
        addSingletonDoubleColumnAndValue(dataframe, "reactive_power_setpoint", 1d);
        addSingletonDoubleColumnAndValue(dataframe, "loss_factor", 0.9d);
        addSingletonIntColumnAndValue(dataframe, "voltage_regulator_on", 0);
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.VSC_CONVERTER_STATION, network, Collections.singletonList(dataframe));
        assertEquals(3, network.getVscConverterStationCount());
    }

    @Test
    void danglingLine() {
        var network = DanglingLineNetworkFactory.create();
        TestDataframe dataframe = new TestDataframe(1);
        addSingletonStringColumnAndValue(dataframe, "id", "dl2");
        addSingletonStringColumnAndValue(dataframe, "name", "name-dl2");
        addSingletonStringColumnAndValue(dataframe, "connectable_bus_id", "BUS");
        addSingletonStringColumnAndValue(dataframe, "bus_id", "BUS");
        addSingletonStringColumnAndValue(dataframe, "voltage_level_id", "VL");
        addSingletonDoubleColumnAndValue(dataframe, "r", 0.6d);
        addSingletonDoubleColumnAndValue(dataframe, "x", 1d);
        addSingletonDoubleColumnAndValue(dataframe, "g", Math.pow(10, -6));
        addSingletonDoubleColumnAndValue(dataframe, "b", Math.pow(10, -6) * 4);
        addSingletonDoubleColumnAndValue(dataframe, "p0", 102d);
        addSingletonDoubleColumnAndValue(dataframe, "q0", 151d);
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.DANGLING_LINE, network, Collections.singletonList(dataframe));
        assertEquals(2, network.getDanglingLineCount());
    }

    @Test
    void busbar() {
        var network = HvdcTestNetwork.createBase();
        TestDataframe dataframe = new TestDataframe(1);
        addSingletonStringColumnAndValue(dataframe, "id", "bs2");
        addSingletonStringColumnAndValue(dataframe, "name", "name-bs2");
        addSingletonStringColumnAndValue(dataframe, "voltage_level_id", "VL2");
        addSingletonIntColumnAndValue(dataframe, "node", 1);
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.BUSBAR_SECTION, network, Collections.singletonList(dataframe));
        assertEquals(2, network.getBusbarSectionCount());
    }

    @Test
    void load() {
        var network = EurostagTutorialExample1Factory.create();
        TestDataframe dataframe = new TestDataframe(1);
        addSingletonStringColumnAndValue(dataframe, "id", "LOAD2");
        addSingletonStringColumnAndValue(dataframe, "voltage_level_id", "VLLOAD");
        addSingletonStringColumnAndValue(dataframe, "connectable_bus_id", "NLOAD");
        addSingletonStringColumnAndValue(dataframe, "bus_id", "NLOAD");
        addSingletonDoubleColumnAndValue(dataframe, "p0", 3.0d);
        addSingletonDoubleColumnAndValue(dataframe, "q0", 1.0d);
        addSingletonDoubleColumnAndValue(dataframe, "target_p", 4.0d);
        addSingletonDoubleColumnAndValue(dataframe, "target_v", 6.0d);
        addSingletonDoubleColumnAndValue(dataframe, "target_q", 7.0d);
        addSingletonDoubleColumnAndValue(dataframe, "rated_s", 5.0d);
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.LOAD, network, Collections.singletonList(dataframe));
        assertEquals(2, network.getLoadCount());
        assertEquals(LoadType.UNDEFINED, network.getLoad("LOAD2").getLoadType());

        addSingletonStringColumnAndValue(dataframe, "type", LoadType.AUXILIARY.name());
        addSingletonStringColumnAndValue(dataframe, "id", "LOAD3");
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.LOAD, network, Collections.singletonList(dataframe));
        assertEquals(LoadType.AUXILIARY, network.getLoad("LOAD3").getLoadType());
    }

    @Test
    void generator() {
        var network = EurostagTutorialExample1Factory.create();
        TestDataframe dataframe = new TestDataframe(1);
        addSingletonDoubleColumnAndValue(dataframe, "max_p", 3.0d);
        addSingletonDoubleColumnAndValue(dataframe, "min_p", 1.0d);
        addSingletonDoubleColumnAndValue(dataframe, "target_p", 4.0d);
        addSingletonDoubleColumnAndValue(dataframe, "target_v", 6.0d);
        addSingletonDoubleColumnAndValue(dataframe, "target_q", 7.0d);
        addSingletonDoubleColumnAndValue(dataframe, "rated_s", 5.0d);
        addSingletonStringColumnAndValue(dataframe, "id", "test");
        addSingletonStringColumnAndValue(dataframe, "voltage_level_id", "VLGEN");
        addSingletonStringColumnAndValue(dataframe, "connectable_bus_id", "NGEN");
        addSingletonStringColumnAndValue(dataframe, "bus_id", "NGEN");
        addSingletonIntColumnAndValue(dataframe, "voltage_regulator_on", 1);
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.GENERATOR, network, Collections.singletonList(dataframe));
        assertEquals(2, network.getGeneratorCount());
    }

    @Test
    void linearShunt() {
        var network = ShuntTestCaseFactory.create();
        TestDataframe shuntDataframe = new TestDataframe(1);
        addSingletonStringColumnAndValue(shuntDataframe, "id", "SHUNT2");
        addSingletonDoubleColumnAndValue(shuntDataframe, "b", 1.0);
        addSingletonDoubleColumnAndValue(shuntDataframe, "g", 2.0);
        addSingletonDoubleColumnAndValue(shuntDataframe, "target_v", 30.0);
        addSingletonDoubleColumnAndValue(shuntDataframe, "target_deadband", 4.0);
        addSingletonStringColumnAndValue(shuntDataframe, "voltage_level_id", "VL1");
        addSingletonStringColumnAndValue(shuntDataframe, "connectable_bus_id", "B1");
        addSingletonStringColumnAndValue(shuntDataframe, "bus_id", "B1");
        addSingletonStringColumnAndValue(shuntDataframe, "model_type", "LINEAR");
        addSingletonIntColumnAndValue(shuntDataframe, "section_count", 1);
        TestDataframe sectionDataframe = new TestDataframe(1);
        addSingletonStringColumnAndValue(sectionDataframe, "id", "SHUNT2");
        addSingletonIntColumnAndValue(sectionDataframe, "max_section_count", 1);
        addSingletonDoubleColumnAndValue(sectionDataframe, "b_per_section", 0.1);
        addSingletonDoubleColumnAndValue(sectionDataframe, "g_per_section", 0.2);
        List<UpdatingDataframe> dataframes = new ArrayList<>();
        dataframes.add(shuntDataframe);
        dataframes.add(sectionDataframe);
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.SHUNT_COMPENSATOR,
                network, dataframes);
        assertEquals(2, network.getShuntCompensatorCount());
        assertEquals(LINEAR, network.getShuntCompensator("SHUNT2").getModelType());
        assertEquals(0.1, ((ShuntCompensatorLinearModel) network.getShuntCompensator("SHUNT2").getModel()).getBPerSection());
    }

    @Test
    void nonLinearShunt() {
        var network = ShuntTestCaseFactory.create();
        TestDataframe shuntDataframe = new TestDataframe(1);
        addSingletonStringColumnAndValue(shuntDataframe, "id", "SHUNT2");
        addSingletonDoubleColumnAndValue(shuntDataframe, "b", 1.0);
        addSingletonDoubleColumnAndValue(shuntDataframe, "g", 2.0);
        addSingletonDoubleColumnAndValue(shuntDataframe, "target_v", 30.0);
        addSingletonDoubleColumnAndValue(shuntDataframe, "target_deadband", 4.0);
        addSingletonStringColumnAndValue(shuntDataframe, "voltage_level_id", "VL1");
        addSingletonStringColumnAndValue(shuntDataframe, "connectable_bus_id", "B1");
        addSingletonStringColumnAndValue(shuntDataframe, "bus_id", "B1");
        addSingletonStringColumnAndValue(shuntDataframe, "model_type", "NON_LINEAR");
        addSingletonIntColumnAndValue(shuntDataframe, "section_count", 2);
        TestDataframe sectionDataframe = new TestDataframe(2);
        addSingletonStringColumnAndValue(sectionDataframe, "id", "SHUNT2", "SHUNT2");
        addSingletonDoubleColumnAndValue(sectionDataframe, "g", 0.1, 0.3);
        addSingletonDoubleColumnAndValue(sectionDataframe, "b", 0.1, 0.3);
        List<UpdatingDataframe> dataframes = new ArrayList<>();
        dataframes.add(shuntDataframe);
        dataframes.add(sectionDataframe);
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.SHUNT_COMPENSATOR,
                network, dataframes);
        assertEquals(2, network.getShuntCompensatorCount());
        assertEquals(NON_LINEAR, network.getShuntCompensator("SHUNT2").getModelType());
        assertEquals(2, ((ShuntCompensatorNonLinearModel) network.getShuntCompensator("SHUNT2").getModel()).getAllSections().size());
        assertEquals(0.3, ((ShuntCompensatorNonLinearModel) network.getShuntCompensator("SHUNT2").getModel()).getAllSections().get(1).getB());

    }

    @Test
    void svc() {
        var network = SvcTestCaseFactory.create();
        var mode = StaticVarCompensator.RegulationMode.OFF;
        TestDataframe dataframe = new TestDataframe(1);
        addSingletonDoubleColumnAndValue(dataframe, "b_min", 0.0003);
        addSingletonDoubleColumnAndValue(dataframe, "b_max", 0.0009);
        addSingletonDoubleColumnAndValue(dataframe, "target_v", 30.0);
        addSingletonDoubleColumnAndValue(dataframe, "voltage_setpoint", 391.0);
        addSingletonStringColumnAndValue(dataframe, "id", "SVC");
        addSingletonStringColumnAndValue(dataframe, "voltage_level_id", "VL2");
        addSingletonStringColumnAndValue(dataframe, "connectable_bus_id", "B2");
        addSingletonStringColumnAndValue(dataframe, "regulation_mode", mode.name());
        addSingletonStringColumnAndValue(dataframe, "bus_id", "B2");
        addSingletonIntColumnAndValue(dataframe, "section_count", 4);
        CreateEquipmentHelper.createElement(PyPowsyblApiHeader.ElementType.STATIC_VAR_COMPENSATOR, network, Collections.singletonList(dataframe));
        assertEquals(2, network.getStaticVarCompensatorCount());
        assertEquals(mode, network.getStaticVarCompensator("SVC").getRegulationMode());
    }
}
