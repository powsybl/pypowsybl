/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.dataframe;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dataframe.update.UpdatingDataframe;
import com.powsybl.iidm.network.*;
import com.powsybl.python.CDataframeHandler;
import com.powsybl.python.PyPowsyblApiHeader;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Yichen TANG <yichen.tang at rte-france.com>
 */
public final class CreateEquipmentHelper {

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
    private static final String NODE = "node";
    private static final String NODE1 = "node1";
    private static final String NODE2 = "node2";

    private static final Map<String, SeriesDataType> INJECTION_MAPS = Map.of(
            CONNECTABLE_BUS_ID, SeriesDataType.STRING,
            NODE, SeriesDataType.INT);
    private static final Map<String, SeriesDataType> RATED_S_MAPS = Map.of(
            CONNECTABLE_BUS_ID, SeriesDataType.STRING,
            NODE, SeriesDataType.INT,
            "rated_s", SeriesDataType.DOUBLE);

    private static final Map<String, SeriesDataType> HVDC_MAPS = Map.of(
            NODE, SeriesDataType.INT,
            "loss_factor", SeriesDataType.DOUBLE,
            CONNECTABLE_BUS_ID, SeriesDataType.STRING);

    private static final Map<String, SeriesDataType> SVC_MAPS = Map.of(
            NODE, SeriesDataType.INT,
            "b_min", SeriesDataType.DOUBLE,
            "b_max", SeriesDataType.DOUBLE,
            CONNECTABLE_BUS_ID, SeriesDataType.STRING);

    private static final Map<String, SeriesDataType> LOAD_MAPS = INJECTION_MAPS;
    private static final Map<String, SeriesDataType> GEN_MAPS = RATED_S_MAPS;
    private static final Map<String, SeriesDataType> TWT_MAPS = Map.of(
            "node1", SeriesDataType.INT,
            "node2", SeriesDataType.INT,
            CONNECTABLE_BUS1_ID, SeriesDataType.STRING,
            CONNECTABLE_BUS2_ID, SeriesDataType.STRING,
            "rated_s", SeriesDataType.DOUBLE);
    private static final Map<String, SeriesDataType> BAT_MAPS = INJECTION_MAPS;
    private static final Map<String, SeriesDataType> BUSBAR_MAPS = Map.of(
            CONNECTABLE_BUS_ID, SeriesDataType.STRING, NODE, SeriesDataType.INT);
    private static final Map<String, SeriesDataType> SC_MAPS = Map.of(
            "node", SeriesDataType.INT,
            "g_per_section", SeriesDataType.DOUBLE,
            "b_per_section", SeriesDataType.DOUBLE,
            "max_section_count", SeriesDataType.INT);
    private static final Map<String, SeriesDataType> SWITCHS_MAPS = Map.of(
            "node1", SeriesDataType.INT,
            "node2", SeriesDataType.INT,
            "bus1_id", SeriesDataType.STRING,
            "bus2_id", SeriesDataType.STRING);
    private static final Map<String, SeriesDataType> BRANCH_MAPS = Map.of(
            "node1", SeriesDataType.INT,
            "node2", SeriesDataType.INT,
            CONNECTABLE_BUS1_ID, SeriesDataType.STRING,
            CONNECTABLE_BUS2_ID, SeriesDataType.STRING);
    private static final Map<String, SeriesDataType> RTC_MAPS = Map.of(
            "g", SeriesDataType.DOUBLE,
            "b", SeriesDataType.DOUBLE,
            "x", SeriesDataType.DOUBLE,
            "r", SeriesDataType.DOUBLE,
            "ratio", SeriesDataType.DOUBLE);
    private static final Map<String, SeriesDataType> PTC_MAPS = Map.of(
            "g", SeriesDataType.DOUBLE,
            "b", SeriesDataType.DOUBLE,
            "x", SeriesDataType.DOUBLE,
            "r", SeriesDataType.DOUBLE,
            "rho", SeriesDataType.DOUBLE,
            "alpha", SeriesDataType.DOUBLE);
    private static final Map<String, SeriesDataType> VL_MAPS = Map.of(
            "topology_kind", SeriesDataType.STRING);

    public static void createElement(PyPowsyblApiHeader.ElementType elementType, Network network,
                                     List<UpdatingDataframe> dataframes) {
        for (int indexElement = 0; indexElement < dataframes.get(0).getLineCount(); indexElement++) {
            switch (elementType) {
                case LOAD:
                    createLoad(network, dataframes.get(0), indexElement);
                    break;
                case GENERATOR:
                    createGenerator(network, dataframes.get(0), indexElement);
                    break;
                case BUSBAR_SECTION:
                    createBusbar(network, dataframes.get(0), indexElement);
                    break;
                case BATTERY:
                    createBatteries(network, dataframes.get(0), indexElement);
                    break;
                case DANGLING_LINE:
                    createDanglingLine(network, dataframes.get(0), indexElement);
                    break;
                case SHUNT_COMPENSATOR:
                    createShunt(network, dataframes.get(0), dataframes.get(1), indexElement);
                    break;
                case STATIC_VAR_COMPENSATOR:
                    createSVC(network, dataframes.get(0), indexElement);
                    break;
                case VSC_CONVERTER_STATION:
                    createVSC(network, dataframes.get(0), indexElement);
                    break;
                case LCC_CONVERTER_STATION:
                    createLCC(network, dataframes.get(0), indexElement);
                    break;
                case LINE:
                    createLine(network, dataframes.get(0), indexElement);
                    break;
                case TWO_WINDINGS_TRANSFORMER:
                    createTwt2(network, dataframes.get(0), indexElement);
                    break;
                case SWITCH:
                    createSwitches(network, dataframes.get(0), indexElement);
                    break;
                case BUS:
                    createBuses(network, dataframes.get(0), indexElement);
                    break;
                case RATIO_TAP_CHANGER:
                    createRatioTapChangers(network, dataframes.get(0), dataframes.get(1), indexElement);
                    break;
                case PHASE_TAP_CHANGER:
                    createPhaseTapChangers(network, dataframes.get(0), dataframes.get(1), indexElement);
                    break;
                case VOLTAGE_LEVEL:
                    createVoltageLevels(network, dataframes.get(0), indexElement);
                    break;
                default:
                    throw new PowsyblException();
            }
        }
    }

    private static void createVoltageLevels(Network network, UpdatingDataframe dataframe, int indexElement) {
        VoltageLevelAdder adder = network.getSubstation(dataframe.getStringValue("substation_id", indexElement)
                .orElseThrow(() -> new PowsyblException("substation_id is missing"))).newVoltageLevel();
        createIdentifiable(adder, dataframe, indexElement);
        dataframe.getDoubleValue("high_voltage_limit", indexElement).ifPresent(adder::setHighVoltageLimit);
        dataframe.getDoubleValue("low_voltage_limit", indexElement).ifPresent(adder::setLowVoltageLimit);
        dataframe.getDoubleValue("nominal_v", indexElement).ifPresent(adder::setNominalV);
        dataframe.getStringValue("topology_kind", indexElement).ifPresent(adder::setTopologyKind);
        adder.add();
    }

    private static void createPhaseTapChangers(Network network, UpdatingDataframe phasesDataframe, UpdatingDataframe stepsDataframe, int indexElement) {
        String transfomerId = phasesDataframe.getStringValue("id", indexElement)
                .orElseThrow(() -> new PowsyblException("id is missing"));
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(transfomerId);
        PhaseTapChangerAdder adder = transformer.newPhaseTapChanger();
        phasesDataframe.getDoubleValue("target_deadband", indexElement).ifPresent(adder::setTargetDeadband);
        phasesDataframe.getStringValue("regulation_mode", indexElement)
                .ifPresent(rm -> adder.setRegulationMode(PhaseTapChanger.RegulationMode.valueOf(rm)));
        phasesDataframe.getIntValue("low_tap", indexElement).ifPresent(adder::setLowTapPosition);
        for (int sectionIndex = 0; sectionIndex < stepsDataframe.getLineCount(); sectionIndex++) {
            String transformerStepId = stepsDataframe.getStringValue("id", sectionIndex).orElse(null);
            if (transfomerId.equals(transformerStepId)) {
                PhaseTapChangerAdder.StepAdder stepAdder = adder.beginStep();
                stepsDataframe.getDoubleValue("b", indexElement).ifPresent(stepAdder::setB);
                stepsDataframe.getDoubleValue("g", indexElement).ifPresent(stepAdder::setG);
                stepsDataframe.getDoubleValue("r", indexElement).ifPresent(stepAdder::setR);
                stepsDataframe.getDoubleValue("x", indexElement).ifPresent(stepAdder::setX);
                stepsDataframe.getDoubleValue("rho", indexElement).ifPresent(stepAdder::setRho);
                stepsDataframe.getDoubleValue("alpha", indexElement).ifPresent(stepAdder::setAlpha);
                stepAdder.endStep();
            }
        }
        phasesDataframe.getIntValue("tap", indexElement).ifPresent(adder::setTapPosition);
        adder.add();
    }

    private static void createRatioTapChangers(Network network, UpdatingDataframe ratiosDataframe, UpdatingDataframe stepsDataframe, int indexElement) {
        String transfomerId = ratiosDataframe.getStringValue("id", indexElement)
                .orElseThrow(() -> new PowsyblException("id is missing"));
        TwoWindingsTransformer transformer = network.getTwoWindingsTransformer(transfomerId);
        RatioTapChangerAdder adder = transformer.newRatioTapChanger();
        ratiosDataframe.getDoubleValue("target_deadband", indexElement).ifPresent(adder::setTargetDeadband);
        ratiosDataframe.getDoubleValue("target_v", indexElement).ifPresent(adder::setTargetV);
        ratiosDataframe.getIntValue("on_load", indexElement).ifPresent(onLoad -> adder.setLoadTapChangingCapabilities(onLoad == 1));
        ratiosDataframe.getIntValue("low_tap", indexElement).ifPresent(adder::setLowTapPosition);
        for (int sectionIndex = 0; sectionIndex < stepsDataframe.getLineCount(); sectionIndex++) {
            String transformerStepId = stepsDataframe.getStringValue("id", sectionIndex).orElse(null);
            if (transfomerId.equals(transformerStepId)) {
                RatioTapChangerAdder.StepAdder stepAdder = adder.beginStep();
                stepsDataframe.getDoubleValue("b", indexElement).ifPresent(stepAdder::setB);
                stepsDataframe.getDoubleValue("g", indexElement).ifPresent(stepAdder::setG);
                stepsDataframe.getDoubleValue("r", indexElement).ifPresent(stepAdder::setR);
                stepsDataframe.getDoubleValue("x", indexElement).ifPresent(stepAdder::setX);
                stepsDataframe.getDoubleValue("rho", indexElement).ifPresent(stepAdder::setRho);
                stepAdder.endStep();
            }
        }
        ratiosDataframe.getIntValue("tap", indexElement).ifPresent(adder::setTapPosition);
        adder.add();
    }

    private static void createBuses(Network network, UpdatingDataframe dataframe, int indexElement) {
        VoltageLevel vl = network.getVoltageLevel(dataframe.getStringValue(VOLTAGE_LEVEL_ID, indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing")));
        System.out.println("test");
        BusAdder adder = vl.getBusBreakerView().newBus();
        createIdentifiable(adder, dataframe, indexElement);
        adder.add();
    }

    private static void createSwitches(Network network, UpdatingDataframe dataframe, int indexElement) {
        VoltageLevel vl = network.getVoltageLevel(dataframe.getStringValue(VOLTAGE_LEVEL_ID, indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing")));
        TopologyKind kind = vl.getTopologyKind();
        if (kind == TopologyKind.NODE_BREAKER) {
            VoltageLevel.NodeBreakerView.SwitchAdder adder = vl.getNodeBreakerView().newSwitch();
            createIdentifiable(adder, dataframe, indexElement);
            dataframe.getStringValue("kind", indexElement).ifPresent(adder::setKind);
            dataframe.getIntValue("node1", indexElement).ifPresent(adder::setNode1);
            dataframe.getIntValue("node2", indexElement).ifPresent(adder::setNode2);
            dataframe.getIntValue("open", indexElement).ifPresent(open -> adder.setOpen(open == 1));
            dataframe.getIntValue("retained", indexElement).ifPresent(retained -> adder.setRetained(retained == 1));
            adder.add();
        } else if (kind == TopologyKind.BUS_BREAKER) {
            VoltageLevel.BusBreakerView.SwitchAdder adder = vl.getBusBreakerView().newSwitch();
            createIdentifiable(adder, dataframe, indexElement);
            dataframe.getStringValue("bus1_id", indexElement).ifPresent(adder::setBus1);
            dataframe.getStringValue("bus2_id", indexElement).ifPresent(adder::setBus2);
            dataframe.getIntValue("open", indexElement).ifPresent(open -> adder.setOpen(open == 1));
            dataframe.getIntValue("fictitious", indexElement).ifPresent(fictitious -> adder.setFictitious(fictitious == 1));
            adder.add();
        }
    }

    private static void createTwt2(Network network, UpdatingDataframe dataframe, int indexElement) {
        String voltageLevel1 = dataframe.getStringValue(VOLTAGE_LEVEL1_ID, indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level1_id is missing"));
        String voltageLevel2 = dataframe.getStringValue(VOLTAGE_LEVEL2_ID, indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level2_id is missing"));
        AtomicReference<Substation> substation = new AtomicReference<>();
        network.getSubstationStream().forEach(substation1 -> {
            if (substation1.getVoltageLevelStream().anyMatch(voltageLevel -> voltageLevel.getId().equals(voltageLevel1))
                    && substation1.getVoltageLevelStream().anyMatch(voltageLevel -> voltageLevel.getId().equals(voltageLevel2))) {
                substation.set(substation1);
            }
        });
        if (substation.get() == null) {
            throw new PowsyblException("both voltage ids must be on the same substation");
        }
        var adder = substation.get().newTwoWindingsTransformer();

        createBranch(adder, dataframe, indexElement);
        dataframe.getDoubleValue("rated_u1", indexElement).ifPresent(adder::setRatedU1);
        dataframe.getDoubleValue("rated_u2", indexElement).ifPresent(adder::setRatedU2);
        dataframe.getDoubleValue("rated_s", indexElement).ifPresent(adder::setRatedS);
        dataframe.getDoubleValue("b", indexElement).ifPresent(adder::setB);
        dataframe.getDoubleValue("g", indexElement).ifPresent(adder::setG);
        dataframe.getDoubleValue("r", indexElement).ifPresent(adder::setR);
        dataframe.getDoubleValue("x", indexElement).ifPresent(adder::setX);
        adder.add();
    }

    private static void createLine(Network network, UpdatingDataframe dataframe, int indexElement) {
        LineAdder lineAdder = network.newLine();
        createBranch(lineAdder, dataframe, indexElement);
        dataframe.getDoubleValue("b1", indexElement).ifPresent(lineAdder::setB1);
        dataframe.getDoubleValue("b2", indexElement).ifPresent(lineAdder::setB2);
        dataframe.getDoubleValue("g1", indexElement).ifPresent(lineAdder::setG1);
        dataframe.getDoubleValue("g2", indexElement).ifPresent(lineAdder::setG2);
        dataframe.getDoubleValue("r", indexElement).ifPresent(lineAdder::setR);
        dataframe.getDoubleValue("x", indexElement).ifPresent(lineAdder::setX);
        lineAdder.add();
    }

    private static void createBranch(BranchAdder adder, UpdatingDataframe dataframe, int indexElement) {
        createIdentifiable(adder, dataframe, indexElement);
        dataframe.getStringValue(BUS1_ID, indexElement).ifPresent(adder::setBus1);
        dataframe.getStringValue(BUS2_ID, indexElement).ifPresent(adder::setBus2);
        dataframe.getStringValue(VOLTAGE_LEVEL1_ID, indexElement).ifPresent(adder::setVoltageLevel1);
        dataframe.getStringValue(VOLTAGE_LEVEL2_ID, indexElement).ifPresent(adder::setVoltageLevel2);
        dataframe.getStringValue(CONNECTABLE_BUS1_ID, indexElement).ifPresent(adder::setConnectableBus1);
        dataframe.getStringValue(CONNECTABLE_BUS2_ID, indexElement).ifPresent(adder::setConnectableBus2);
        dataframe.getIntValue(NODE1, indexElement).ifPresent(adder::setNode1);
        dataframe.getIntValue(NODE2, indexElement).ifPresent(adder::setNode2);
    }

    private static void createLCC(Network network, UpdatingDataframe dataframe, int indexElement) {
        LccConverterStationAdder adder = network.getVoltageLevel(dataframe.getStringValue(VOLTAGE_LEVEL_ID, indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing")))
                .newLccConverterStation();
        createHvdc(adder, dataframe, indexElement);
        dataframe.getDoubleValue("power_factor", indexElement).ifPresent(pf -> adder.setPowerFactor((float) pf));
        adder.add();
    }

    private static void createHvdc(HvdcConverterStationAdder adder, UpdatingDataframe dataframe, int indexElement) {
        createInjection(adder, dataframe, indexElement);
        dataframe.getDoubleValue("loss_factor", indexElement).ifPresent(lf -> adder.setLossFactor((float) lf));
    }

    private static void createVSC(Network network, UpdatingDataframe dataframe, int indexElement) {
        VscConverterStationAdder adder = network.getVoltageLevel(dataframe.getStringValue(VOLTAGE_LEVEL_ID, indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing"))).newVscConverterStation();
        createHvdc(adder, dataframe, indexElement);
        dataframe.getDoubleValue("voltage_setpoint", indexElement).ifPresent(adder::setVoltageSetpoint);
        dataframe.getDoubleValue("reactive_power_setpoint", indexElement).ifPresent(adder::setReactivePowerSetpoint);
        adder.setVoltageRegulatorOn(dataframe.getIntValue("voltage_regulator_on", indexElement).orElse(0) == 1);
        adder.add();
    }

    private static void createDanglingLine(Network network, UpdatingDataframe dataframe, int indexElement) {
        DanglingLineAdder adder = network.getVoltageLevel(dataframe.getStringValue(VOLTAGE_LEVEL_ID, indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing"))).newDanglingLine();
        createInjection(adder, dataframe, indexElement);
        dataframe.getDoubleValue("p0", indexElement).ifPresent(adder::setP0);
        dataframe.getDoubleValue("q0", indexElement).ifPresent(adder::setQ0);
        dataframe.getDoubleValue("r", indexElement).ifPresent(adder::setR);
        dataframe.getDoubleValue("x", indexElement).ifPresent(adder::setX);
        dataframe.getDoubleValue("g", indexElement).ifPresent(adder::setG);
        dataframe.getDoubleValue("b", indexElement).ifPresent(adder::setB);
        adder.add();
    }

    private static void createLoad(Network network, UpdatingDataframe dataframe, int indexElement) {
        LoadAdder loadAdder = network.getVoltageLevel(dataframe.getStringValue(VOLTAGE_LEVEL_ID, indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing"))).newLoad();
        dataframe.getStringValue("type", indexElement)
                .ifPresent(t -> loadAdder.setLoadType(LoadType.valueOf(t)));
        createInjection(loadAdder, dataframe, indexElement);
        dataframe.getDoubleValue("p0", indexElement).ifPresent(loadAdder::setP0);
        dataframe.getDoubleValue("q0", indexElement).ifPresent(loadAdder::setQ0);
        loadAdder.add();
    }

    private static void createBusbar(Network network, UpdatingDataframe dataframe, int indexElement) {
        BusbarSectionAdder adder = network.getVoltageLevel(dataframe.getStringValue(VOLTAGE_LEVEL_ID, indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing")))
                .getNodeBreakerView()
                .newBusbarSection();
        createIdentifiable(adder, dataframe, indexElement);
        dataframe.getIntValue(NODE, indexElement).ifPresent(adder::setNode);
        adder.add();
    }

    private static void createGenerator(Network network, UpdatingDataframe dataframe, int indexElement) {
        GeneratorAdder generatorAdder = network.getVoltageLevel(dataframe.getStringValue(VOLTAGE_LEVEL_ID, indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing")))
                .newGenerator();
        createInjection(generatorAdder, dataframe, indexElement);
        dataframe.getDoubleValue("max_p", indexElement).ifPresent(generatorAdder::setMaxP);
        dataframe.getDoubleValue("min_p", indexElement).ifPresent(generatorAdder::setMinP);
        dataframe.getDoubleValue("target_p", indexElement).ifPresent(generatorAdder::setTargetP);
        dataframe.getDoubleValue("target_q", indexElement).ifPresent(generatorAdder::setTargetQ);
        dataframe.getDoubleValue("target_v", indexElement).ifPresent(generatorAdder::setTargetV);
        dataframe.getDoubleValue("rated_s", indexElement).ifPresent(generatorAdder::setRatedS);
        generatorAdder.setVoltageRegulatorOn(dataframe.getIntValue("voltage_regulator_on", indexElement).orElse(0) == 1);
        dataframe.getStringValue("energy_source", indexElement)
                .ifPresent(v -> generatorAdder.setEnergySource(EnergySource.valueOf(v)));
        generatorAdder.add();
    }

    private static void createBatteries(Network network, UpdatingDataframe dataframe, int indexElement) {
        BatteryAdder batteryAdder = network.getVoltageLevel(dataframe.getStringValue(VOLTAGE_LEVEL_ID, indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing")))
                .newBattery();
        createInjection(batteryAdder, dataframe, indexElement);
        dataframe.getDoubleValue("max_p", indexElement).ifPresent(batteryAdder::setMaxP);
        dataframe.getDoubleValue("min_p", indexElement).ifPresent(batteryAdder::setMinP);
        dataframe.getDoubleValue("p0", indexElement).ifPresent(batteryAdder::setP0);
        dataframe.getDoubleValue("q0", indexElement).ifPresent(batteryAdder::setQ0);
        batteryAdder.add();
    }

    private static void createShunt(Network network, UpdatingDataframe shuntDataframe,
                                    UpdatingDataframe sectionDataframe, int indexElement) {
        String voltageLevelId = shuntDataframe.getStringValue(VOLTAGE_LEVEL_ID, indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing"));
        if (shuntDataframe.getStringValue("id", indexElement).isEmpty()) {
            throw new PowsyblException("id must be defined for a linear shunt");
        }
        String shuntId = shuntDataframe.getStringValue("id", indexElement).get();
        ShuntCompensatorAdder adder = network.getVoltageLevel(voltageLevelId)
                .newShuntCompensator();
        createInjection(adder, shuntDataframe, indexElement);
        shuntDataframe.getIntValue("section_count", indexElement).ifPresent(adder::setSectionCount);
        shuntDataframe.getDoubleValue("target_deadband", indexElement).ifPresent(adder::setTargetDeadband);
        shuntDataframe.getDoubleValue("target_v", indexElement).ifPresent(adder::setTargetV);

        if (shuntDataframe.getStringValue("model_type", indexElement).isEmpty()) {
            throw new PowsyblException("model_type must be defined for a linear shunt");
        }
        if (shuntDataframe.getStringValue("model_type", indexElement).get().equals("LINEAR")) {
            ShuntCompensatorLinearModelAdder linearModelAdder = adder.newLinearModel();
            int index = sectionDataframe.getIndex("id", shuntId);
            if (index == -1) {
                throw new PowsyblException("one section must be defined for a linear shunt");
            }
            sectionDataframe.getDoubleValue("b_per_section", index).ifPresent(linearModelAdder::setBPerSection);
            sectionDataframe.getDoubleValue("g_per_section", index).ifPresent(linearModelAdder::setGPerSection);
            sectionDataframe.getIntValue("max_section_count", index).ifPresent(linearModelAdder::setMaximumSectionCount);
            linearModelAdder.add();

        } else if (shuntDataframe.getStringValue("model_type", indexElement).get().equals("NON_LINEAR")) {
            ShuntCompensatorNonLinearModelAdder nonLinearAdder = adder.newNonLinearModel();
            int sectionNumber = 0;
            for (int sectionIndex = 0; sectionIndex < sectionDataframe.getLineCount(); sectionIndex++) {
                String id = sectionDataframe.getStringValue("id", sectionIndex).orElse(null);
                if (shuntId.equals(id)) {
                    sectionNumber++;
                    ShuntCompensatorNonLinearModelAdder.SectionAdder section = nonLinearAdder.beginSection();
                    sectionDataframe.getDoubleValue("g", sectionIndex).ifPresent(section::setG);
                    sectionDataframe.getDoubleValue("b", sectionIndex).ifPresent(section::setB);
                    section.endSection();
                }
            }
            if (sectionNumber == 0) {
                throw new PowsyblException("at least one section must be defined for a shunt");
            }
            nonLinearAdder.add();

        } else {
            throw new PowsyblException("shunt model type non valid");
        }
        adder.add();
    }

    private static void createSVC(Network network, UpdatingDataframe dataframe, int indexElement) {
        StaticVarCompensatorAdder adder = network.getVoltageLevel(dataframe.getStringValue(VOLTAGE_LEVEL_ID, indexElement)
                .orElseThrow(() -> new PowsyblException("voltage_level_id is missing")))
                .newStaticVarCompensator();
        createInjection(adder, dataframe, indexElement);
        dataframe.getDoubleValue("b_max", indexElement).ifPresent(adder::setBmax);
        dataframe.getDoubleValue("b_min", indexElement).ifPresent(adder::setBmin);
        dataframe.getStringValue("regulation_mode", indexElement)
                .ifPresent(rm -> adder.setRegulationMode(StaticVarCompensator.RegulationMode.valueOf(rm)));
        dataframe.getDoubleValue("voltage_setpoint", indexElement).ifPresent(adder::setVoltageSetpoint);
        dataframe.getDoubleValue("reactive_power_setpoint", indexElement).ifPresent(adder::setReactivePowerSetpoint);
        adder.add();
    }

    private static void createIdentifiable(IdentifiableAdder adder, UpdatingDataframe dataframe, int indexElement) {
        dataframe.getStringValue("id", indexElement).ifPresent(adder::setId);
        dataframe.getStringValue("name", indexElement).ifPresent(adder::setName);
    }

    private static void createInjection(InjectionAdder adder, UpdatingDataframe dataframe, int indexElement) {
        createIdentifiable(adder, dataframe, indexElement);
        dataframe.getStringValue(CONNECTABLE_BUS_ID, indexElement).ifPresent(adder::setConnectableBus);
        dataframe.getStringValue(BUS_ID, indexElement).ifPresent(adder::setBus);
        dataframe.getIntValue(NODE, indexElement).ifPresent(adder::setNode);
    }

    public static int getAdderSeriesType(PyPowsyblApiHeader.ElementType type, String fieldName) {
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
            case BUSBAR_SECTION:
                seriesDataType = BUSBAR_MAPS.get(fieldName);
                break;
            case TWO_WINDINGS_TRANSFORMER:
                seriesDataType = TWT_MAPS.get(fieldName);
                break;
            case SHUNT_COMPENSATOR:
                seriesDataType = SC_MAPS.get(fieldName);
                break;
            case SWITCH:
                seriesDataType = SWITCHS_MAPS.get(fieldName);
                break;
            case LINE:
                seriesDataType = BRANCH_MAPS.get(fieldName);
                break;
            case VSC_CONVERTER_STATION:
                seriesDataType = HVDC_MAPS.get(fieldName);
                break;
            case STATIC_VAR_COMPENSATOR:
                seriesDataType = SVC_MAPS.get(fieldName);
                break;
            case RATIO_TAP_CHANGER:
                seriesDataType = RTC_MAPS.get(fieldName);
                break;
            case PHASE_TAP_CHANGER:
                seriesDataType = PTC_MAPS.get(fieldName);
                break;
            case VOLTAGE_LEVEL:
                seriesDataType = VL_MAPS.get(fieldName);
                break;
            default:
                throw new RuntimeException("Unexpected " + type + " adder.");
        }
        if (seriesDataType == null) {
            throw new RuntimeException("Field '" + fieldName + "' not found for " + type + ".");
        }
        return CDataframeHandler.convert(seriesDataType);
    }

    private CreateEquipmentHelper() {
    }
}
