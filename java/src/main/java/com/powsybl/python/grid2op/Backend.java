/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.grid2op;

import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowProvider;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.commons.Util;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.*;

import static com.powsybl.python.commons.PyPowsyblApiHeader.allocArrayPointer;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class Backend implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Backend.class);

    private final Network network;
    private final boolean considerOpenBranchReactiveFlow;

    private final List<VoltageLevel> voltageLevels;
    private final ArrayPointer<CCharPointerPointer> voltageLevelName;

    private final Bus[] buses;
    private final double[] busV;

    private final List<Load> loads;
    private final ArrayPointer<CCharPointerPointer> loadName;
    private final ArrayPointer<CIntPointer> loadToVoltageLevelNum;
    private final ArrayPointer<CDoublePointer> loadP;
    private final ArrayPointer<CDoublePointer> loadQ;
    private final ArrayPointer<CDoublePointer> loadV;
    private final int[] loadBusGlobalNum;

    private final List<Generator> generators;
    private final ArrayPointer<CCharPointerPointer> generatorName;
    private final ArrayPointer<CIntPointer> generatorToVoltageLevelNum;
    private final ArrayPointer<CDoublePointer> generatorP;
    private final ArrayPointer<CDoublePointer> generatorQ;
    private final ArrayPointer<CDoublePointer> generatorV;
    private final int[] generatorBusGlobalNum;

    private final List<ShuntCompensator> shunts;
    private final ArrayPointer<CCharPointerPointer> shuntName;
    private final ArrayPointer<CIntPointer> shuntToVoltageLevelNum;
    private final ArrayPointer<CDoublePointer> shuntP;
    private final ArrayPointer<CDoublePointer> shuntQ;
    private final ArrayPointer<CDoublePointer> shuntV;
    private final int[] shuntBusGlobalNum;
    private final ArrayPointer<CIntPointer> shuntBusLocalNum;

    private final List<Battery> batteries = Collections.emptyList();

    private final List<Branch> branches;
    private final ArrayPointer<CCharPointerPointer> branchName;
    private final ArrayPointer<CIntPointer> branchToVoltageLevelNum1;
    private final ArrayPointer<CIntPointer> branchToVoltageLevelNum2;
    private final ArrayPointer<CDoublePointer> branchP1;
    private final ArrayPointer<CDoublePointer> branchP2;
    private final ArrayPointer<CDoublePointer> branchQ1;
    private final ArrayPointer<CDoublePointer> branchQ2;
    private final ArrayPointer<CDoublePointer> branchV1;
    private final ArrayPointer<CDoublePointer> branchV2;
    private final ArrayPointer<CDoublePointer> branchI1;
    private final ArrayPointer<CDoublePointer> branchI2;
    private final ArrayPointer<CDoublePointer> branchPermanentLimitA;
    private final int[] branchBusGlobalNum1;
    private final int[] branchBusGlobalNum2;

    private int[] loadTopoVectPosition;
    private int[] generatorTopoVectPosition;
    private int[] branchTopoVectPosition1;
    private int[] branchTopoVectPosition2;
    private ArrayPointer<CIntPointer> topoVect;

    private final LoadFlowProvider loadFlowProvider = LoadFlowProvider.findAll().stream()
            .filter(p -> p.getName().equals("OpenLoadFlow"))
            .findFirst()
            .orElseThrow();
    private final LoadFlow.Runner loadFlowRunner = new LoadFlow.Runner(loadFlowProvider);

    public Backend(Network network, boolean considerOpenBranchReactiveFlow, int busesPerVoltageLevel, boolean connectAllElementsToFirstBus) {
        this.network = Objects.requireNonNull(network);
        this.considerOpenBranchReactiveFlow = considerOpenBranchReactiveFlow;

        // reset switch retain
        for (Switch s : network.getSwitches()) {
            s.setRetained(false);
        }

        // waiting for switch action, we convert all voltage levels to bus/breaker topo
        for (VoltageLevel voltageLevel : network.getVoltageLevels()) {
            voltageLevel.convertToTopology(TopologyKind.BUS_BREAKER);
        }

        // create missing buses to get a constant number of buses per voltage level
        int maxBusCount = network.getVoltageLevelStream()
                .map(vl -> vl.getBusBreakerView().getBusCount())
                .max(Integer::compareTo)
                .orElseThrow();
        if (busesPerVoltageLevel > maxBusCount) {
            maxBusCount = busesPerVoltageLevel;
        }
        for (VoltageLevel voltageLevel : network.getVoltageLevels()) {
            int localBusCount = voltageLevel.getBusBreakerView().getBusCount();
            if (localBusCount < maxBusCount) {
                for (int i = localBusCount; i < maxBusCount; i++) {
                    voltageLevel.getBusBreakerView().newBus()
                            .setId(voltageLevel.getId() + "_extra_busbar_" + i)
                            .add();
                }
            }
        }

        // voltage levels
        voltageLevels = network.getVoltageLevelStream().toList();
        voltageLevelName = Util.createCharPtrArray(voltageLevels.stream().map(Identifiable::getId).toList());
        Map<String, Integer> voltageLevelIdToNum = new HashMap<>(voltageLevels.size());
        for (int i = 0; i < voltageLevels.size(); i++) {
            VoltageLevel voltageLevel = voltageLevels.get(i);
            voltageLevelIdToNum.put(voltageLevel.getId(), i);
        }

        // buses
        int busCount = network.getBusBreakerView().getBusCount();
        buses = new Bus[busCount];
        busV = new double[busCount];
        Map<String, Integer> busIdToGlobalNum = new HashMap<>(busCount);
        for (int voltageLevelNum = 0; voltageLevelNum < voltageLevels.size(); voltageLevelNum++) {
            VoltageLevel voltageLevel = voltageLevels.get(voltageLevelNum);
            List<Bus> localBuses = voltageLevel.getBusBreakerView().getBusStream().toList();
            for (int i = 0; i < localBuses.size(); i++) {
                Bus localBus = localBuses.get(i);
                int localNum = i + 1;
                int globalNum = localToGlobalBusNum(voltageLevelNum, localNum);
                busIdToGlobalNum.put(localBus.getId(), globalNum);
                buses[globalNum] = localBus;
            }
        }

        // loads
        loads = network.getLoadStream().toList();
        loadName = Util.createCharPtrArray(loads.stream().map(Identifiable::getId).toList());
        loadToVoltageLevelNum = createIntArrayPointer(loads.size());
        loadP = createDoubleArrayPointer(loads.size());
        loadQ = createDoubleArrayPointer(loads.size());
        loadV = createDoubleArrayPointer(loads.size());
        loadBusGlobalNum = new int[loads.size()];
        for (int i = 0; i < loads.size(); i++) {
            Load load = loads.get(i);
            loadToVoltageLevelNum.getPtr().write(i, voltageLevelIdToNum.get(load.getTerminal().getVoltageLevel().getId()));
            Bus bus = load.getTerminal().getBusBreakerView().getBus();
            loadBusGlobalNum[i] = bus == null ? -1 : busIdToGlobalNum.get(bus.getId());
        }

        // generators
        generators = network.getGeneratorStream().toList();
        generatorName = Util.createCharPtrArray(generators.stream().map(Identifiable::getId).toList());
        generatorToVoltageLevelNum = createIntArrayPointer(generators.size());
        generatorP = createDoubleArrayPointer(generators.size());
        generatorQ = createDoubleArrayPointer(generators.size());
        generatorV = createDoubleArrayPointer(generators.size());
        generatorBusGlobalNum = new int[generators.size()];
        for (int i = 0; i < generators.size(); i++) {
            Generator generator = generators.get(i);
            generatorToVoltageLevelNum.getPtr().write(i, voltageLevelIdToNum.get(generator.getTerminal().getVoltageLevel().getId()));
            Bus bus = generator.getTerminal().getBusBreakerView().getBus();
            generatorBusGlobalNum[i] = bus == null ? -1 : busIdToGlobalNum.get(bus.getId());
        }

        // shunts
        shunts = network.getShuntCompensatorStream().toList();
        shuntName = Util.createCharPtrArray(shunts.stream().map(Identifiable::getId).toList());
        shuntToVoltageLevelNum = createIntArrayPointer(shunts.size());
        shuntP = createDoubleArrayPointer(shunts.size());
        shuntQ = createDoubleArrayPointer(shunts.size());
        shuntV = createDoubleArrayPointer(shunts.size());
        shuntBusGlobalNum = new int[shunts.size()];
        shuntBusLocalNum = createIntArrayPointer(shunts.size());
        for (int i = 0; i < shunts.size(); i++) {
            ShuntCompensator shunt = shunts.get(i);
            shuntToVoltageLevelNum.getPtr().write(i, voltageLevelIdToNum.get(shunt.getTerminal().getVoltageLevel().getId()));
            Bus bus = shunt.getTerminal().getBusBreakerView().getBus();
            shuntBusGlobalNum[i] = bus == null ? -1 : busIdToGlobalNum.get(bus.getId());
            shuntBusLocalNum.getPtr().write(i, globalToLocalBusNum(shuntBusGlobalNum[i]));
        }

        // branches
        branches = network.getBranchStream().toList();
        branchName = Util.createCharPtrArray(branches.stream().map(Identifiable::getId).toList());
        branchToVoltageLevelNum1 = createIntArrayPointer(branches.size());
        branchToVoltageLevelNum2 = createIntArrayPointer(branches.size());
        branchP1 = createDoubleArrayPointer(branches.size());
        branchP2 = createDoubleArrayPointer(branches.size());
        branchQ1 = createDoubleArrayPointer(branches.size());
        branchQ2 = createDoubleArrayPointer(branches.size());
        branchV1 = createDoubleArrayPointer(branches.size());
        branchV2 = createDoubleArrayPointer(branches.size());
        branchI1 = createDoubleArrayPointer(branches.size());
        branchI2 = createDoubleArrayPointer(branches.size());
        branchPermanentLimitA = createDoubleArrayPointer(branches.size());
        branchBusGlobalNum1 = new int[branches.size()];
        branchBusGlobalNum2 = new int[branches.size()];
        for (int i = 0; i < branches.size(); i++) {
            Branch<?> branch = branches.get(i);
            branchToVoltageLevelNum1.getPtr().write(i, voltageLevelIdToNum.get(branch.getTerminal1().getVoltageLevel().getId()));
            branchToVoltageLevelNum2.getPtr().write(i, voltageLevelIdToNum.get(branch.getTerminal2().getVoltageLevel().getId()));
            Bus bus1 = branch.getTerminal1().getBusBreakerView().getBus();
            Bus bus2 = branch.getTerminal2().getBusBreakerView().getBus();
            branchBusGlobalNum1[i] = bus1 == null ? -1 : busIdToGlobalNum.get(bus1.getId());
            branchBusGlobalNum2[i] = bus2 == null ? -1 : busIdToGlobalNum.get(bus2.getId());
            branchPermanentLimitA.getPtr().write(i, branch.getCurrentLimits1().map(LoadingLimits::getPermanentLimit)
                    .or(() -> branch.getCurrentLimits2().map(LoadingLimits::getPermanentLimit))
                    .orElse(999999.0));
        }

        computeBigTopo();

        updateState();
        updateTopoVect();

        if (connectAllElementsToFirstBus) {
            LOGGER.debug("Connect all elements to first bus of voltage level");
            for (int i = 0; i < loads.size(); i++) {
                Load load = loads.get(i);
                changeTopo(getLoadTopoLabel(load), i, load.getTerminal(), 1, loadBusGlobalNum, loadToVoltageLevelNum, loadTopoVectPosition);
            }
            for (int i = 0; i < generators.size(); i++) {
                Generator generator = generators.get(i);
                changeTopo(getGeneratorTopoLabel(generator), i, generator.getTerminal(), 1, generatorBusGlobalNum, generatorToVoltageLevelNum, generatorTopoVectPosition);
            }
            for (int i = 0; i < shunts.size(); i++) {
                ShuntCompensator shunt = shunts.get(i);
                changeTopo(getShuntTopoLabel(shunt), i, shunt.getTerminal(), 1, shuntBusGlobalNum, shuntToVoltageLevelNum);
            }
            for (int i = 0; i < branches.size(); i++) {
                Branch<?> branch = branches.get(i);
                changeTopo(getBranch1TopoLabel(branch), i, branch.getTerminal1(), 1, branchBusGlobalNum1, branchToVoltageLevelNum1, branchTopoVectPosition1);
                changeTopo(getBranch2TopoLabel(branch), i, branch.getTerminal2(), 1, branchBusGlobalNum2, branchToVoltageLevelNum2, branchTopoVectPosition2);
            }
        }
    }

    static int localToGlobalBusNum(int voltageLevelCount, int voltageLevelNum, int localNum) {
        if (localNum == -1) {
            return -1;
        }
        return voltageLevelNum + voltageLevelCount * (localNum - 1);
    }

    private int localToGlobalBusNum(int voltageLevelNum, int localNum) {
        return localToGlobalBusNum(voltageLevels.size(), voltageLevelNum, localNum);
    }

    static int globalToLocalBusNum(int voltageLevelCount, int globalNum) {
        if (globalNum == -1) {
            return -1;
        }
        return globalNum / voltageLevelCount + 1;
    }

    private int globalToLocalBusNum(int globalNum) {
        return globalToLocalBusNum(voltageLevels.size(), globalNum);
    }

    private int[] computeVoltageLevelPosition(int[] nextVoltageLevelPosition, ArrayPointer<CIntPointer> voltageLevelNum) {
        int[] voltageLevelPosition = new int[voltageLevelNum.getLength()];
        for (int i = 0; i < voltageLevelNum.getLength(); i++) {
            voltageLevelPosition[i] = nextVoltageLevelPosition[voltageLevelNum.getPtr().read(i)]++;
        }
        return voltageLevelPosition;
    }

    private void computeBigTopo() {
        // find a position inside a voltage level for each of the element
        int[] nextVoltageLevelPosition = new int[voltageLevels.size()]; // next position inside a voltage level
        int[] loadToVoltageLevelPosition = computeVoltageLevelPosition(nextVoltageLevelPosition, loadToVoltageLevelNum);
        int[] generatorToVoltageLevelPosition = computeVoltageLevelPosition(nextVoltageLevelPosition, generatorToVoltageLevelNum);
        int[] lineToVoltageLevelPosition1 = computeVoltageLevelPosition(nextVoltageLevelPosition, branchToVoltageLevelNum1);
        int[] lineToVoltageLevelPosition2 = computeVoltageLevelPosition(nextVoltageLevelPosition, branchToVoltageLevelNum2);
//        int[] batteryToVoltageLevelPosition = computeVoltageLevelPosition(nextVoltageLevelPosition, batteryToVoltageLevelNum);

        // now find a position inside the topo vect for each of the element
        loadTopoVectPosition = computeTopoVectPosition(loadToVoltageLevelNum, loadToVoltageLevelPosition, nextVoltageLevelPosition);
        generatorTopoVectPosition = computeTopoVectPosition(generatorToVoltageLevelNum, generatorToVoltageLevelPosition, nextVoltageLevelPosition);
        branchTopoVectPosition1 = computeTopoVectPosition(branchToVoltageLevelNum1, lineToVoltageLevelPosition1, nextVoltageLevelPosition);
        branchTopoVectPosition2 = computeTopoVectPosition(branchToVoltageLevelNum2, lineToVoltageLevelPosition2, nextVoltageLevelPosition);
//        int[] batteryTopoVectPosition = computeTopoVectPosition(batteryToVoltageLevelNum, nextVoltageLevelPosition);

        int topoVectSize = 2 * branches.size() + loads.size() + generators.size() + batteries.size();
        topoVect = createIntArrayPointer(topoVectSize);
    }

    private static int[] computeTopoVectPosition(ArrayPointer<CIntPointer> toVoltageLevelNum,
                                                 int[] toVoltageLevelPosition,
                                                 int[] maxVoltageLevelPosition) {
        int[] res = new int[toVoltageLevelNum.getLength()];
        for (int i = 0; i < toVoltageLevelNum.getLength(); i++) {
            int voltageLevelNum = toVoltageLevelNum.getPtr().read(i);
            int objBefore = Arrays.stream(maxVoltageLevelPosition, 0, voltageLevelNum).sum();
            res[i] = objBefore + toVoltageLevelPosition[i];
        }
        return res;
    }

    private void updateTopoVect(int[] topoVectPosition, int[] busGlobalNum) {
        for (int i = 0; i < topoVectPosition.length; i++) {
            topoVect.getPtr().write(topoVectPosition[i], globalToLocalBusNum(busGlobalNum[i]));
        }
    }

    public void updateTopoVect() {
        updateTopoVect(loadTopoVectPosition, loadBusGlobalNum);
        updateTopoVect(generatorTopoVectPosition, generatorBusGlobalNum);
        updateTopoVect(branchTopoVectPosition1, branchBusGlobalNum1);
        updateTopoVect(branchTopoVectPosition2, branchBusGlobalNum2);
    }

    private static ArrayPointer<CDoublePointer> createDoubleArrayPointer(int length) {
        return allocArrayPointer(UnmanagedMemory.calloc(length * SizeOf.get(CDoublePointer.class)), length);
    }

    private static ArrayPointer<CIntPointer> createIntArrayPointer(int length) {
        return allocArrayPointer(UnmanagedMemory.calloc(length * SizeOf.get(CIntPointer.class)), length);
    }

    private static double fixNan(double f) {
        return Double.isNaN(f) ? 0 : f;
    }

    public void updateState() {
        updateBuses();
        updateLoads();
        updateGenerators();
        updateShunts();
        updateBranches();
    }

    private void updateBuses() {
        for (int i = 0; i < buses.length; i++) {
            Bus bus = buses[i];
            if (bus != null) {
                busV[i] = fixNan(bus.getV());
            }
        }
    }

    private double getV(int i, int[] xBusGlobalNum) {
        int globalNum = xBusGlobalNum[i];
        if (globalNum == -1) {
            return 0.0;
        }
        return busV[globalNum];
    }

    private double getP(Terminal t, int i, int[] xBusGlobalNum) {
        int globalNum = xBusGlobalNum[i];
        if (globalNum == -1) {
            return 0;
        }
        return fixNan(t.getP());
    }

    private double getQ(Terminal t, int i, int[] xBusGlobalNum) {
        int globalNum = xBusGlobalNum[i];
        if (!considerOpenBranchReactiveFlow && globalNum == -1) {
            return 0;
        }
        return fixNan(t.getQ());
    }

    private double getI(Terminal t, int i, int[] xBusGlobalNum) {
        int globalNum = xBusGlobalNum[i];
        if (!considerOpenBranchReactiveFlow && globalNum == -1) {
            return 0;
        }
        return fixNan(t.getI());
    }

    private void updateLoads() {
        for (int i = 0; i < loads.size(); i++) {
            Load load = loads.get(i);
            Terminal terminal = load.getTerminal();
            loadP.getPtr().write(i, getP(terminal, i, loadBusGlobalNum));
            loadQ.getPtr().write(i, getQ(terminal, i, loadBusGlobalNum));
            loadV.getPtr().write(i, getV(i, loadBusGlobalNum));
        }
    }

    private void updateGenerators() {
        for (int i = 0; i < generators.size(); i++) {
            Generator generator = generators.get(i);
            Terminal terminal = generator.getTerminal();
            generatorP.getPtr().write(i, -getP(terminal, i, generatorBusGlobalNum));
            generatorQ.getPtr().write(i, -getQ(terminal, i, generatorBusGlobalNum));
            generatorV.getPtr().write(i, getV(i, generatorBusGlobalNum));
        }
    }

    private void updateShunts() {
        for (int i = 0; i < shunts.size(); i++) {
            ShuntCompensator shunt = shunts.get(i);
            Terminal terminal = shunt.getTerminal();
            shuntP.getPtr().write(i, getP(terminal, i, shuntBusGlobalNum));
            shuntQ.getPtr().write(i, getQ(terminal, i, shuntBusGlobalNum));
            shuntV.getPtr().write(i, getV(i, shuntBusGlobalNum));
        }
    }

    private void updateBranches() {
        for (int i = 0; i < branches.size(); i++) {
            Branch<?> branch = branches.get(i);
            Terminal terminal1 = branch.getTerminal1();
            Terminal terminal2 = branch.getTerminal2();
            branchP1.getPtr().write(i, getP(terminal1, i, branchBusGlobalNum1));
            branchP2.getPtr().write(i, getP(terminal2, i, branchBusGlobalNum2));
            branchQ1.getPtr().write(i, getQ(terminal1, i, branchBusGlobalNum1));
            branchQ2.getPtr().write(i, getQ(terminal2, i, branchBusGlobalNum2));
            branchV1.getPtr().write(i, getV(i, branchBusGlobalNum1));
            branchV2.getPtr().write(i, getV(i, branchBusGlobalNum2));
            branchI1.getPtr().write(i, getI(terminal1, i, branchBusGlobalNum1));
            branchI2.getPtr().write(i, getI(terminal2, i, branchBusGlobalNum2));
        }
    }

    public ArrayPointer<CCharPointerPointer> getStringValue(Grid2opCFunctions.Grid2opStringValueType valueType) {
        return switch (Objects.requireNonNull(valueType)) {
            case VOLTAGE_LEVEL_NAME -> voltageLevelName;
            case LOAD_NAME -> loadName;
            case GENERATOR_NAME -> generatorName;
            case SHUNT_NAME -> shuntName;
            case BRANCH_NAME -> branchName;
        };
    }

    public ArrayPointer<CIntPointer> getIntegerValue(Grid2opCFunctions.Grid2opIntegerValueType valueType) {
        return switch (Objects.requireNonNull(valueType)) {
            case LOAD_VOLTAGE_LEVEL_NUM -> loadToVoltageLevelNum;
            case GENERATOR_VOLTAGE_LEVEL_NUM -> generatorToVoltageLevelNum;
            case SHUNT_VOLTAGE_LEVEL_NUM -> shuntToVoltageLevelNum;
            case BRANCH_VOLTAGE_LEVEL_NUM_1 -> branchToVoltageLevelNum1;
            case BRANCH_VOLTAGE_LEVEL_NUM_2 -> branchToVoltageLevelNum2;
            case SHUNT_LOCAL_BUS -> shuntBusLocalNum;
            case TOPO_VECT -> topoVect;
        };
    }

    public ArrayPointer<CDoublePointer> getDoubleValue(Grid2opCFunctions.Grid2opDoubleValueType valueType) {
        return switch (Objects.requireNonNull(valueType)) {
            case LOAD_P -> loadP;
            case LOAD_Q -> loadQ;
            case LOAD_V -> loadV;
            case GENERATOR_P -> generatorP;
            case GENERATOR_Q -> generatorQ;
            case GENERATOR_V -> generatorV;
            case SHUNT_P -> shuntP;
            case SHUNT_Q -> shuntQ;
            case SHUNT_V -> shuntV;
            case BRANCH_P1 -> branchP1;
            case BRANCH_P2 -> branchP2;
            case BRANCH_Q1 -> branchQ1;
            case BRANCH_Q2 -> branchQ2;
            case BRANCH_V1 -> branchV1;
            case BRANCH_V2 -> branchV2;
            case BRANCH_I1 -> branchI1;
            case BRANCH_I2 -> branchI2;
            case BRANCH_PERMANENT_LIMIT_A -> branchPermanentLimitA;
        };
    }

    public void updateDoubleValue(Grid2opCFunctions.Grid2opUpdateDoubleValueType valueType, CDoublePointer valuePtr, CIntPointer changedPtr) {
        switch (Objects.requireNonNull(valueType)) {
            case UPDATE_LOAD_P -> {
                for (int i = 0; i < loads.size(); i++) {
                    if (changedPtr.read(i) == 1) {
                        Load load = loads.get(i);
                        load.setP0(valuePtr.read(i));
                        loadP.getPtr().write(i, load.getP0());
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Update load '{}' p0 {}", load.getId(), load.getP0());
                        }
                    }
                }
            }
            case UPDATE_LOAD_Q -> {
                for (int i = 0; i < loads.size(); i++) {
                    if (changedPtr.read(i) == 1) {
                        Load load = loads.get(i);
                        load.setQ0(valuePtr.read(i));
                        loadP.getPtr().write(i, load.getQ0());
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Update load '{}' q0 {}", load.getId(), load.getP0());
                        }
                    }
                }
            }
            case UPDATE_GENERATOR_P -> {
                for (int i = 0; i < generators.size(); i++) {
                    if (changedPtr.read(i) == 1) {
                        Generator generator = generators.get(i);
                        generator.setTargetP(valuePtr.read(i));
                        generatorP.getPtr().write(i, generator.getTargetP());
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Update generator '{}' target p {}", generator.getId(), generator.getTargetP());
                        }
                    }
                }
            }
            case UPDATE_GENERATOR_V -> {
                for (int i = 0; i < generators.size(); i++) {
                    if (changedPtr.read(i) == 1) {
                        Generator generator = generators.get(i);
                        generator.setTargetV(valuePtr.read(i));
                        generatorV.getPtr().write(i, generator.getTargetV());
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Update generator '{}' target v {}", generator.getId(), generator.getTargetV());
                        }
                    }
                }
            }
        }
    }

    private int changeTopo(String label, int i, Terminal t, CIntPointer valuePtr, int[] xBusGlobalNum, ArrayPointer<CIntPointer> xToVoltageLevelNum) {
        int localBusNum = valuePtr.read(i);
        changeTopo(label, i, t, localBusNum, xBusGlobalNum, xToVoltageLevelNum);
        return localBusNum;
    }

    private void changeTopo(String label, int i, Terminal t, int localBusNum, int[] xBusGlobalNum, ArrayPointer<CIntPointer> xToVoltageLevelNum) {
        int oldGlobalBusNum = xBusGlobalNum[i];
        if (localBusNum == -1) {
            if (oldGlobalBusNum != -1) {
                if (LOGGER.isTraceEnabled()) {
                    int oldLocalBusNum = globalToLocalBusNum(oldGlobalBusNum);
                    LOGGER.trace("Disconnect {} from bus {}", label, oldLocalBusNum);
                }
                t.disconnect();
                xBusGlobalNum[i] = -1;
            }
        } else {
            int globalBusNum = localToGlobalBusNum(xToVoltageLevelNum.getPtr().read(i), localBusNum);

            if (globalBusNum != oldGlobalBusNum) {
                if (LOGGER.isTraceEnabled()) {
                    if (oldGlobalBusNum != -1) {
                        int oldLocalBusNum = globalToLocalBusNum(oldGlobalBusNum);
                        LOGGER.trace("Connect {} from bus {} to bus {}", label, oldLocalBusNum, localBusNum);
                    } else {
                        LOGGER.trace("Connect {} to bus {}", label, localBusNum);
                    }
                }
                String newBusId = buses[globalBusNum].getId();
                t.getBusBreakerView().setConnectableBus(newBusId);
                t.connect();
                xBusGlobalNum[i] = globalBusNum;
            }
        }
    }

    private void changeTopo(String label, int i, Terminal t, int localBusNum, int[] xBusGlobalNum, ArrayPointer<CIntPointer> xToVoltageLevelNum,
                            int[] xTopoVectPosition) {
        changeTopo(label, i, t, localBusNum, xBusGlobalNum, xToVoltageLevelNum);
        // update topo vect
        topoVect.getPtr().write(xTopoVectPosition[i], localBusNum);
    }

    private void changeTopo(String label, int i, Terminal t, CIntPointer valuePtr, int[] xBusGlobalNum, ArrayPointer<CIntPointer> xToVoltageLevelNum,
                            int[] xTopoVectPosition) {
        int localBusNum = changeTopo(label, i, t, valuePtr, xBusGlobalNum, xToVoltageLevelNum);
        // update topo vect
        topoVect.getPtr().write(xTopoVectPosition[i], localBusNum);
    }

    private static String getLoadTopoLabel(Load load) {
        return "load '" + load.getId() + "'";
    }

    private static String getGeneratorTopoLabel(Generator generator) {
        return "generator '" + generator.getId() + "'";
    }

    private static String getShuntTopoLabel(ShuntCompensator shunt) {
        return "shunt '" + shunt.getId() + "'";
    }

    private static String getBranch1TopoLabel(Branch<?> branch) {
        return "branch side 1 '" + branch.getId() + "'";
    }

    private static String getBranch2TopoLabel(Branch<?> branch) {
        return "branch side 2 '" + branch.getId() + "'";
    }

    public void updateIntegerValue(Grid2opCFunctions.Grid2opUpdateIntegerValueType valueType, CIntPointer valuePtr, CIntPointer changedPtr) {
        switch (Objects.requireNonNull(valueType)) {
            case UPDATE_LOAD_BUS -> {
                for (int i = 0; i < loads.size(); i++) {
                    if (changedPtr.read(i) == 1) {
                        Load load = loads.get(i);
                        changeTopo(getLoadTopoLabel(load), i, load.getTerminal(), valuePtr, loadBusGlobalNum, loadToVoltageLevelNum, loadTopoVectPosition);
                    }
                }
            }
            case UPDATE_GENERATOR_BUS -> {
                for (int i = 0; i < generators.size(); i++) {
                    if (changedPtr.read(i) == 1) {
                        Generator generator = generators.get(i);
                        changeTopo(getGeneratorTopoLabel(generator), i, generator.getTerminal(), valuePtr, generatorBusGlobalNum, generatorToVoltageLevelNum, generatorTopoVectPosition);
                    }
                }
            }
            case UPDATE_SHUNT_BUS -> {
                for (int i = 0; i < shunts.size(); i++) {
                    if (changedPtr.read(i) == 1) {
                        ShuntCompensator shunt = shunts.get(i);
                        int localBusNum = changeTopo(getShuntTopoLabel(shunt), i, shunt.getTerminal(), valuePtr, shuntBusGlobalNum, shuntToVoltageLevelNum);
                        shuntBusLocalNum.getPtr().write(i, localBusNum);
                    }
                }
            }
            case UPDATE_BRANCH_BUS1 -> {
                for (int i = 0; i < branches.size(); i++) {
                    if (changedPtr.read(i) == 1) {
                        Branch<?> branch = branches.get(i);
                        changeTopo(getBranch1TopoLabel(branch), i, branch.getTerminal1(), valuePtr, branchBusGlobalNum1, branchToVoltageLevelNum1, branchTopoVectPosition1);
                    }
                }
            }
            case UPDATE_BRANCH_BUS2 -> {
                for (int i = 0; i < branches.size(); i++) {
                    if (changedPtr.read(i) == 1) {
                        Branch<?> branch = branches.get(i);
                        changeTopo(getBranch2TopoLabel(branch), i, branch.getTerminal2(), valuePtr, branchBusGlobalNum2, branchToVoltageLevelNum2, branchTopoVectPosition2);
                    }
                }
            }
        }
    }

    public LoadFlowProvider getLoadFlowProvider() {
        return loadFlowProvider;
    }

    private static <T extends Injection<T>> boolean checkIsolatedAndDisconnectedInjections(List<T> injections, boolean checkConnection) {
        for (T injection : injections) {
            Bus bus = injection.getTerminal().getBusView().getBus();
            if (checkConnection && bus == null) {
                return true;
            }
            if (bus != null && !bus.isInMainSynchronousComponent()) {
                return true;
            }
        }
        return false;
    }

    public boolean checkIsolatedAndDisconnectedInjections() {
        if (checkIsolatedAndDisconnectedInjections(loads, true)) {
            return true;
        }
        if (checkIsolatedAndDisconnectedInjections(generators, true)) {
            return true;
        }
        if (checkIsolatedAndDisconnectedInjections(shunts, false)) {
            return true;
        }
        return false;
    }

    public LoadFlowResult runLoadFlow(LoadFlowParameters parameters) {
        checkIsolatedAndDisconnectedInjections();
        LoadFlowResult result = loadFlowRunner.run(network, parameters);
        updateState();
        return result;
    }

    @Override
    public void close() {
        Util.freeCharPtrArray(voltageLevelName);

        Util.freeCharPtrArray(loadName);
        PyPowsyblApiHeader.freeArrayPointer(loadToVoltageLevelNum);
        PyPowsyblApiHeader.freeArrayPointer(loadP);
        PyPowsyblApiHeader.freeArrayPointer(loadQ);
        PyPowsyblApiHeader.freeArrayPointer(loadV);

        Util.freeCharPtrArray(generatorName);
        PyPowsyblApiHeader.freeArrayPointer(generatorToVoltageLevelNum);
        PyPowsyblApiHeader.freeArrayPointer(generatorP);
        PyPowsyblApiHeader.freeArrayPointer(generatorQ);
        PyPowsyblApiHeader.freeArrayPointer(generatorV);

        Util.freeCharPtrArray(shuntName);
        PyPowsyblApiHeader.freeArrayPointer(shuntToVoltageLevelNum);
        PyPowsyblApiHeader.freeArrayPointer(shuntP);
        PyPowsyblApiHeader.freeArrayPointer(shuntQ);
        PyPowsyblApiHeader.freeArrayPointer(shuntV);
        PyPowsyblApiHeader.freeArrayPointer(shuntBusLocalNum);

        Util.freeCharPtrArray(branchName);
        PyPowsyblApiHeader.freeArrayPointer(branchToVoltageLevelNum1);
        PyPowsyblApiHeader.freeArrayPointer(branchToVoltageLevelNum2);
        PyPowsyblApiHeader.freeArrayPointer(branchP1);
        PyPowsyblApiHeader.freeArrayPointer(branchP2);
        PyPowsyblApiHeader.freeArrayPointer(branchQ1);
        PyPowsyblApiHeader.freeArrayPointer(branchQ2);
        PyPowsyblApiHeader.freeArrayPointer(branchV1);
        PyPowsyblApiHeader.freeArrayPointer(branchV2);
        PyPowsyblApiHeader.freeArrayPointer(branchI1);
        PyPowsyblApiHeader.freeArrayPointer(branchI2);

        PyPowsyblApiHeader.freeArrayPointer(topoVect);
    }
}
