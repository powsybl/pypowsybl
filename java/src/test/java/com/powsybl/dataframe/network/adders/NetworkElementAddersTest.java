/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe.network.adders;

import com.powsybl.dataframe.DataframeElementType;
import com.powsybl.dataframe.update.*;
import com.powsybl.iidm.network.LoadType;
import com.powsybl.iidm.network.ShuntCompensatorLinearModel;
import com.powsybl.iidm.network.ShuntCompensatorNonLinearModel;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.test.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.powsybl.iidm.network.ShuntCompensatorModelType.LINEAR;
import static com.powsybl.iidm.network.ShuntCompensatorModelType.NON_LINEAR;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 * @author Etienne Lesot <etienne.lesot at rte-france.com>
 * @author Sylvain Leclerc <sylvain.leclerc@rte-france.com>
 */
class NetworkElementAddersTest {

    @Test
    void twt2() {
        var network = EurostagTutorialExample1Factory.create();
        DefaultUpdatingDataframe dataframe = new DefaultUpdatingDataframe(1);
        addStringColumn(dataframe, "id", "test");
        addStringColumn(dataframe, "substation_id", "P1");
        addStringColumn(dataframe, "name", "l3");
        addStringColumn(dataframe, "bus1_id", "NGEN");
        addStringColumn(dataframe, "bus2_id", "NHV1");
        addStringColumn(dataframe, "voltage_level1_id", "VLGEN");
        addStringColumn(dataframe, "voltage_level2_id", "VLHV1");
        addStringColumn(dataframe, "connectable_bus1_id", "NGEN");
        addStringColumn(dataframe, "connectable_bus2_id", "NHV1");
        addDoubleColumn(dataframe, "rated_u1", 4.0);
        addDoubleColumn(dataframe, "rated_u2", 4.0);
        addDoubleColumn(dataframe, "rated_s", 4.0);
        addDoubleColumn(dataframe, "r", 4.0);
        addDoubleColumn(dataframe, "x", 4.0);
        addDoubleColumn(dataframe, "g", 4.0);
        addDoubleColumn(dataframe, "b", 4.0);
        NetworkElementAdders.addElements(DataframeElementType.TWO_WINDINGS_TRANSFORMER, network, singletonList(dataframe));
        assertEquals(3, network.getTwoWindingsTransformerCount());
    }

    private void addStringColumn(DefaultUpdatingDataframe dataframe, String column, String... value) {
        dataframe.addSeries(column, false, new TestStringSeries(value));
    }

    private void addDoubleColumn(DefaultUpdatingDataframe dataframe, String column, double... value) {
        dataframe.addSeries(column, false, new TestDoubleSeries(value));
    }

    private void addIntColumn(DefaultUpdatingDataframe dataframe, String column, int... value) {
        dataframe.addSeries(column, false, new TestIntSeries(value));
    }

    @Test
    void line() {
        var network = EurostagTutorialExample1Factory.create();
        var dataframe = new DefaultUpdatingDataframe(1);
        addDoubleColumn(dataframe, "r", 4.0);
        addDoubleColumn(dataframe, "x", 4.0);
        addDoubleColumn(dataframe, "g1", 4.0);
        addDoubleColumn(dataframe, "b1", 4.0);
        addDoubleColumn(dataframe, "g2", 4.0);
        addDoubleColumn(dataframe, "b2", 4.0);
        addStringColumn(dataframe, "id", "L3");
        addStringColumn(dataframe, "name", "l3");
        addStringColumn(dataframe, "bus1_id", "NHV1");
        addStringColumn(dataframe, "bus2_id", "NHV2");
        addStringColumn(dataframe, "voltage_level1_id", "VLHV1");
        addStringColumn(dataframe, "voltage_level2_id", "VLHV2");
        addStringColumn(dataframe, "connectable_bus1_id", "NHV1");
        addStringColumn(dataframe, "connectable_bus2_id", "NHV2");
        NetworkElementAdders.addElements(DataframeElementType.LINE, network, singletonList(dataframe));
        assertEquals(3, network.getLineCount());
    }

    @Test
    void lcc() {
        var network = HvdcTestNetwork.createLcc();
        var dataframe = new DefaultUpdatingDataframe(1);
        addStringColumn(dataframe, "id", "C3");
        addStringColumn(dataframe, "name", "name-c3");
        addStringColumn(dataframe, "connectable_bus_id", "B1");
        addStringColumn(dataframe, "bus_id", "B1");
        addStringColumn(dataframe, "voltage_level_id", "VL1");
        addDoubleColumn(dataframe, "loss_factor", 0.9d);
        addDoubleColumn(dataframe, "power_factor", 0.9d);
        NetworkElementAdders.addElements(DataframeElementType.LCC_CONVERTER_STATION, network, singletonList(dataframe));
        assertEquals(3, network.getLccConverterStationCount());
    }

    @Test
    void vsc() {
        var network = HvdcTestNetwork.createVsc();
        var dataframe = new DefaultUpdatingDataframe(1);
        addStringColumn(dataframe, "id", "C3");
        addStringColumn(dataframe, "name", "name-c3");
        addStringColumn(dataframe, "connectable_bus_id", "B1");
        addStringColumn(dataframe, "bus_id", "B1");
        addStringColumn(dataframe, "voltage_level_id", "VL1");
        addDoubleColumn(dataframe, "target_v", 0.6d);
        addDoubleColumn(dataframe, "target_q", 1d);
        addDoubleColumn(dataframe, "loss_factor", 0.9d);
        addIntColumn(dataframe, "voltage_regulator_on", 0);
        NetworkElementAdders.addElements(DataframeElementType.VSC_CONVERTER_STATION, network, singletonList(dataframe));
        assertEquals(3, network.getVscConverterStationCount());
    }

    @Test
    void danglingLine() {
        var network = DanglingLineNetworkFactory.create();
        var dataframe = new DefaultUpdatingDataframe(1);
        addStringColumn(dataframe, "id", "dl2");
        addStringColumn(dataframe, "name", "name-dl2");
        addStringColumn(dataframe, "connectable_bus_id", "BUS");
        addStringColumn(dataframe, "bus_id", "BUS");
        addStringColumn(dataframe, "voltage_level_id", "VL");
        addDoubleColumn(dataframe, "r", 0.6d);
        addDoubleColumn(dataframe, "x", 1d);
        addDoubleColumn(dataframe, "g", Math.pow(10, -6));
        addDoubleColumn(dataframe, "b", Math.pow(10, -6) * 4);
        addDoubleColumn(dataframe, "p0", 102d);
        addDoubleColumn(dataframe, "q0", 151d);
        NetworkElementAdders.addElements(DataframeElementType.DANGLING_LINE, network, singletonList(dataframe));
        assertEquals(2, network.getDanglingLineCount());
    }

    @Test
    void busbar() {
        var network = HvdcTestNetwork.createBase();
        var dataframe = new DefaultUpdatingDataframe(1);
        addStringColumn(dataframe, "id", "bs2");
        addStringColumn(dataframe, "name", "name-bs2");
        addStringColumn(dataframe, "voltage_level_id", "VL2");
        addIntColumn(dataframe, "node", 1);
        NetworkElementAdders.addElements(DataframeElementType.BUSBAR_SECTION, network, singletonList(dataframe));
        assertEquals(2, network.getBusbarSectionCount());
    }

    @Test
    void load() {
        var network = EurostagTutorialExample1Factory.create();
        var dataframe = new DefaultUpdatingDataframe(1);
        addStringColumn(dataframe, "id", "LOAD2");
        addStringColumn(dataframe, "voltage_level_id", "VLLOAD");
        addStringColumn(dataframe, "connectable_bus_id", "NLOAD");
        addStringColumn(dataframe, "bus_id", "NLOAD");
        addDoubleColumn(dataframe, "p0", 3.0d);
        addDoubleColumn(dataframe, "q0", 1.0d);
        addDoubleColumn(dataframe, "target_p", 4.0d);
        addDoubleColumn(dataframe, "target_v", 6.0d);
        addDoubleColumn(dataframe, "target_q", 7.0d);
        addDoubleColumn(dataframe, "rated_s", 5.0d);
        NetworkElementAdders.addElements(DataframeElementType.LOAD, network, singletonList(dataframe));
        assertEquals(2, network.getLoadCount());
        assertEquals(LoadType.UNDEFINED, network.getLoad("LOAD2").getLoadType());

        addStringColumn(dataframe, "type", LoadType.AUXILIARY.name());
        addStringColumn(dataframe, "id", "LOAD3");
        NetworkElementAdders.addElements(DataframeElementType.LOAD, network, singletonList(dataframe));
        assertEquals(LoadType.AUXILIARY, network.getLoad("LOAD3").getLoadType());
    }

    @Test
    void generator() {
        var network = EurostagTutorialExample1Factory.create();
        var dataframe = new DefaultUpdatingDataframe(1);
        addDoubleColumn(dataframe, "max_p", 3.0d);
        addDoubleColumn(dataframe, "min_p", 1.0d);
        addDoubleColumn(dataframe, "target_p", 4.0d);
        addDoubleColumn(dataframe, "target_v", 6.0d);
        addDoubleColumn(dataframe, "target_q", 7.0d);
        addDoubleColumn(dataframe, "rated_s", 5.0d);
        addStringColumn(dataframe, "id", "test");
        addStringColumn(dataframe, "voltage_level_id", "VLGEN");
        addStringColumn(dataframe, "connectable_bus_id", "NGEN");
        addStringColumn(dataframe, "bus_id", "NGEN");
        addIntColumn(dataframe, "voltage_regulator_on", 1);
        NetworkElementAdders.addElements(DataframeElementType.GENERATOR, network, singletonList(dataframe));
        assertEquals(2, network.getGeneratorCount());
    }

    @Disabled
    @Test
    void linearShunt() {
        var network = ShuntTestCaseFactory.create();
        var shuntDataframe = new DefaultUpdatingDataframe(1);
        addStringColumn(shuntDataframe, "id", "SHUNT2");
        addDoubleColumn(shuntDataframe, "b", 1.0);
        addDoubleColumn(shuntDataframe, "g", 2.0);
        addDoubleColumn(shuntDataframe, "target_v", 30.0);
        addDoubleColumn(shuntDataframe, "target_deadband", 4.0);
        addStringColumn(shuntDataframe, "voltage_level_id", "VL1");
        addStringColumn(shuntDataframe, "connectable_bus_id", "B1");
        addStringColumn(shuntDataframe, "bus_id", "B1");
        addStringColumn(shuntDataframe, "model_type", "LINEAR");
        addIntColumn(shuntDataframe, "section_count", 1);
        var sectionDataframe = new DefaultUpdatingDataframe(1);
        addStringColumn(sectionDataframe, "id", "SHUNT2");
        addIntColumn(sectionDataframe, "max_section_count", 1);
        addDoubleColumn(sectionDataframe, "b_per_section", 0.1);
        addDoubleColumn(sectionDataframe, "g_per_section", 0.2);

        List<UpdatingDataframe> dataframes = new ArrayList<>();
        dataframes.add(shuntDataframe);
        dataframes.add(sectionDataframe);
        NetworkElementAdders.addElements(DataframeElementType.SHUNT_COMPENSATOR, network, dataframes);
        assertEquals(2, network.getShuntCompensatorCount());
        assertEquals(LINEAR, network.getShuntCompensator("SHUNT2").getModelType());
        assertEquals(0.1, ((ShuntCompensatorLinearModel) network.getShuntCompensator("SHUNT2").getModel()).getBPerSection());
    }

    @Disabled
    @Test
    void nonLinearShunt() {
        var network = ShuntTestCaseFactory.create();
        var shuntDataframe = new DefaultUpdatingDataframe(1);
        addStringColumn(shuntDataframe, "id", "SHUNT2");
        addDoubleColumn(shuntDataframe, "b", 1.0);
        addDoubleColumn(shuntDataframe, "g", 2.0);
        addDoubleColumn(shuntDataframe, "target_v", 30.0);
        addDoubleColumn(shuntDataframe, "target_deadband", 4.0);
        addStringColumn(shuntDataframe, "voltage_level_id", "VL1");
        addStringColumn(shuntDataframe, "connectable_bus_id", "B1");
        addStringColumn(shuntDataframe, "bus_id", "B1");
        addStringColumn(shuntDataframe, "model_type", "NON_LINEAR");
        addIntColumn(shuntDataframe, "section_count", 2);
        var sectionDataframe = new DefaultUpdatingDataframe(2);
        addStringColumn(sectionDataframe, "id", "SHUNT2", "SHUNT2");
        addDoubleColumn(sectionDataframe, "g", 0.1, 0.3);
        addDoubleColumn(sectionDataframe, "b", 0.1, 0.3);
        List<UpdatingDataframe> dataframes = new ArrayList<>();
        dataframes.add(shuntDataframe);
        dataframes.add(sectionDataframe);
        NetworkElementAdders.addElements(DataframeElementType.SHUNT_COMPENSATOR, network, dataframes);
        assertEquals(2, network.getShuntCompensatorCount());
        assertEquals(NON_LINEAR, network.getShuntCompensator("SHUNT2").getModelType());
        assertEquals(2, ((ShuntCompensatorNonLinearModel) network.getShuntCompensator("SHUNT2").getModel()).getAllSections().size());
        assertEquals(0.3, ((ShuntCompensatorNonLinearModel) network.getShuntCompensator("SHUNT2").getModel()).getAllSections().get(1).getB());

    }

    @Test
    void svc() {
        var network = SvcTestCaseFactory.create();
        var mode = StaticVarCompensator.RegulationMode.OFF;
        var dataframe = new DefaultUpdatingDataframe(1);
        addDoubleColumn(dataframe, "b_min", 0.0003);
        addDoubleColumn(dataframe, "b_max", 0.0009);
        addDoubleColumn(dataframe, "target_v", 30.0);
        addDoubleColumn(dataframe, "voltage_setpoint", 391.0);
        addStringColumn(dataframe, "id", "SVC");
        addStringColumn(dataframe, "voltage_level_id", "VL2");
        addStringColumn(dataframe, "connectable_bus_id", "B2");
        addStringColumn(dataframe, "regulation_mode", mode.name());
        addStringColumn(dataframe, "bus_id", "B2");
        addIntColumn(dataframe, "section_count", 4);
        NetworkElementAdders.addElements(DataframeElementType.STATIC_VAR_COMPENSATOR, network, singletonList(dataframe));
        assertEquals(2, network.getStaticVarCompensatorCount());
        assertEquals(mode, network.getStaticVarCompensator("SVC").getRegulationMode());
    }

}
