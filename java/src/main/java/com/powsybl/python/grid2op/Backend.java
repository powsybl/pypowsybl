/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.grid2op;

import com.powsybl.iidm.network.*;
import com.powsybl.python.commons.PyPowsyblApiHeader;
import com.powsybl.python.commons.PyPowsyblApiHeader.ArrayPointer;
import com.powsybl.python.commons.Util;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CIntPointer;

import java.io.Closeable;
import java.util.*;

import static com.powsybl.python.commons.PyPowsyblApiHeader.allocArrayPointer;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class Backend implements Closeable {

    private final Network network;

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
    private final ArrayPointer<CIntPointer> shuntBusGlobalNum;

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
    private final int[] branchBusGlobalNum1;
    private final int[] branchBusGlobalNum2;

    private int[] loadTopoVectPosition;
    private int[] generatorTopoVectPosition;
    private int[] branchTopoVectPosition1;
    private int[] branchTopoVectPosition2;
    private ArrayPointer<CIntPointer> topoVect;

    public Backend(Network network) {
        this.network = Objects.requireNonNull(network);

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
        shuntBusGlobalNum = createIntArrayPointer(shunts.size());
        for (int i = 0; i < shunts.size(); i++) {
            ShuntCompensator shunt = shunts.get(i);
            shuntToVoltageLevelNum.getPtr().write(i, voltageLevelIdToNum.get(shunt.getTerminal().getVoltageLevel().getId()));
            Bus bus = shunt.getTerminal().getBusBreakerView().getBus();
            shuntBusGlobalNum.getPtr().write(i, bus == null ? -1 : busIdToGlobalNum.get(bus.getId()));
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
        }

        computeBigTopo();

        updateBuses();
        updateLoads();
        updateGenerators();
        updateShunts();
        updateBranches();
        updateTopoVect();
    }

    static int localToGlobalBusNum(int voltageLevelCount, int voltageLevelNum, int localNum) {
        return voltageLevelNum + voltageLevelCount * (localNum - 1);
    }

    private int localToGlobalBusNum(int voltageLevelNum, int localNum) {
        return localToGlobalBusNum(voltageLevels.size(), voltageLevelNum, localNum);
    }

    static int globalToLocalBusNum(int voltageLevelCount, int globalNum) {
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

    public void updateBuses() {
        for (int i = 0; i < buses.length; i++) {
            Bus bus = buses[i];
            if (bus != null) {
                busV[i] = fixNan(bus.getV());
            }
        }
    }

    public void updateLoads() {
        for (int i = 0; i < loads.size(); i++) {
            Load load = loads.get(i);
            Terminal terminal = load.getTerminal();
            loadP.getPtr().write(i, fixNan(terminal.getP()));
            loadQ.getPtr().write(i, fixNan(terminal.getQ()));
            loadV.getPtr().write(i, busV[loadBusGlobalNum[i]]);
        }
    }

    public void updateGenerators() {
        for (int i = 0; i < generators.size(); i++) {
            Generator generator = generators.get(i);
            Terminal terminal = generator.getTerminal();
            generatorP.getPtr().write(i, fixNan(terminal.getP()));
            generatorQ.getPtr().write(i, fixNan(terminal.getQ()));
            generatorV.getPtr().write(i, busV[generatorBusGlobalNum[i]]);
        }
    }

    public void updateShunts() {
        for (int i = 0; i < shunts.size(); i++) {
            ShuntCompensator shunt = shunts.get(i);
            Terminal terminal = shunt.getTerminal();
            shuntP.getPtr().write(i, fixNan(terminal.getP()));
            shuntQ.getPtr().write(i, fixNan(terminal.getQ()));
            shuntV.getPtr().write(i, busV[shuntBusGlobalNum.getPtr().read(i)]);
        }
    }

    public void updateBranches() {
        for (int i = 0; i < branches.size(); i++) {
            Branch<?> branch = branches.get(i);
            Terminal terminal1 = branch.getTerminal1();
            Terminal terminal2 = branch.getTerminal2();
            branchP1.getPtr().write(i, fixNan(terminal1.getP()));
            branchP2.getPtr().write(i, fixNan(terminal2.getP()));
            branchQ1.getPtr().write(i, fixNan(terminal1.getQ()));
            branchQ2.getPtr().write(i, fixNan(terminal2.getQ()));
            branchV1.getPtr().write(i, busV[branchBusGlobalNum1[i]]);
            branchV2.getPtr().write(i, busV[branchBusGlobalNum2[i]]);
            branchI1.getPtr().write(i, fixNan(terminal1.getI()));
            branchI2.getPtr().write(i, fixNan(terminal2.getI()));
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
                    }
                }
            }
            case UPDATE_LOAD_Q -> {
                for (int i = 0; i < loads.size(); i++) {
                    if (changedPtr.read(i) == 1) {
                        Load load = loads.get(i);
                        load.setQ0(valuePtr.read(i));
                        loadP.getPtr().write(i, load.getQ0());
                    }
                }
            }
            case UPDATE_GENERATOR_P -> {
                for (int i = 0; i < generators.size(); i++) {
                    if (changedPtr.read(i) == 1) {
                        Generator generator = generators.get(i);
                        generator.setTargetP(valuePtr.read(i));
                        generatorP.getPtr().write(i, generator.getTargetP());
                    }
                }
            }
            case UPDATE_GENERATOR_V -> {
                for (int i = 0; i < generators.size(); i++) {
                    if (changedPtr.read(i) == 1) {
                        Generator generator = generators.get(i);
                        generator.setTargetV(valuePtr.read(i));
                        generatorV.getPtr().write(i, generator.getTargetV());
                    }
                }
            }
        }
    }

    private void changeTopo(int i, Terminal t, CIntPointer valuePtr, int[] xBusGlobalNum, ArrayPointer<CIntPointer> xToVoltageLevelNum,
                            int[] xTopoVectPosition) {
        int localBusNum = valuePtr.read(i);
        if (localBusNum == -1) {
            t.disconnect();
            xBusGlobalNum[i] = -1;
        } else {
            int globalBusNum = localToGlobalBusNum(xToVoltageLevelNum.getPtr().read(i), localBusNum);
            if (globalBusNum != xBusGlobalNum[i]) {
                String newBusId = buses[globalBusNum].getId();
                t.getBusBreakerView().setConnectableBus(newBusId);
                t.connect();
                xBusGlobalNum[i] = globalBusNum;
            }
        }
        // update topo vect
        topoVect.getPtr().write(xTopoVectPosition[i], localBusNum);
    }

    public void updateIntegerValue(Grid2opCFunctions.Grid2opUpdateIntegerValueType valueType, CIntPointer valuePtr, CIntPointer changedPtr) {
        switch (Objects.requireNonNull(valueType)) {
            case UPDATE_LOAD_BUS -> {
                for (int i = 0; i < loads.size(); i++) {
                    if (changedPtr.read(i) == 1) {
                        Load load = loads.get(i);
                        changeTopo(i, load.getTerminal(), valuePtr, loadBusGlobalNum, loadToVoltageLevelNum, loadTopoVectPosition);
                    }
                }
            }
            case UPDATE_GENERATOR_BUS -> {
                for (int i = 0; i < generators.size(); i++) {
                    if (changedPtr.read(i) == 1) {
                        Generator generator = generators.get(i);
                        changeTopo(i, generator.getTerminal(), valuePtr, generatorBusGlobalNum, generatorToVoltageLevelNum, generatorTopoVectPosition);
                    }
                }
            }
            case UPDATE_BRANCH_BUS1 -> {
                for (int i = 0; i < branches.size(); i++) {
                    if (changedPtr.read(i) == 1) {
                        Branch<?> branch = branches.get(i);
                        changeTopo(i, branch.getTerminal1(), valuePtr, branchBusGlobalNum1, branchToVoltageLevelNum1, branchTopoVectPosition1);
                    }
                }
            }
            case UPDATE_BRANCH_BUS2 -> {
                for (int i = 0; i < branches.size(); i++) {
                    if (changedPtr.read(i) == 1) {
                        Branch<?> branch = branches.get(i);
                        changeTopo(i, branch.getTerminal2(), valuePtr, branchBusGlobalNum2, branchToVoltageLevelNum2, branchTopoVectPosition2);
                    }
                }
            }
        }
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
        PyPowsyblApiHeader.freeArrayPointer(shuntBusGlobalNum);

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
