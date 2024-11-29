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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.powsybl.python.commons.PyPowsyblApiHeader.allocArrayPointer;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
public class Backend implements Closeable {

    private final Network network;

    private final ArrayPointer<CCharPointerPointer> voltageLevelName;

    private final List<Bus> buses;
    private final double[] busV;
    private final int[] globalToLocalBusNum;

    private final List<Load> loads;
    private final ArrayPointer<CCharPointerPointer> loadName;
    private final ArrayPointer<CIntPointer> loadVoltageLevelNum;
    private final ArrayPointer<CDoublePointer> loadP;
    private final ArrayPointer<CDoublePointer> loadQ;
    private final ArrayPointer<CDoublePointer> loadV;
    private final int[] loadBusGlobalNum;
    private final CIntPointer loadBusLocalNum;

    private final List<Generator> generators;
    private final ArrayPointer<CCharPointerPointer> generatorName;
    private final ArrayPointer<CIntPointer> generatorVoltageLevelNum;
    private final ArrayPointer<CDoublePointer> generatorP;
    private final ArrayPointer<CDoublePointer> generatorQ;
    private final ArrayPointer<CDoublePointer> generatorV;
    private final int[] generatorBusGlobalNum;
    private final CIntPointer generatorBusLocalNum;

    private final List<ShuntCompensator> shunts;
    private final ArrayPointer<CCharPointerPointer> shuntName;
    private final ArrayPointer<CIntPointer> shuntVoltageLevelNum;
    private final ArrayPointer<CDoublePointer> shuntP;
    private final ArrayPointer<CDoublePointer> shuntQ;
    private final ArrayPointer<CDoublePointer> shuntV;
    private final int[] shuntBusGlobalNum;
    private final CIntPointer shuntBusLocalNum;

    private final List<Branch> branches;
    private final ArrayPointer<CCharPointerPointer> branchName;
    private final ArrayPointer<CIntPointer> branchVoltageLevelNum1;
    private final ArrayPointer<CIntPointer> branchVoltageLevelNum2;
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
    private final CIntPointer branchBusLocalNum1;
    private final CIntPointer branchBusLocalNum2;

    public Backend(Network network) {
        this.network = Objects.requireNonNull(network);

        // voltage levels
        List<VoltageLevel> voltageLevels = network.getVoltageLevelStream().toList();
        voltageLevelName = Util.createCharPtrArray(voltageLevels.stream().map(Identifiable::getId).toList());
        Map<String, Integer> voltageLevelNum = new HashMap<>(voltageLevels.size());
        for (int i = 0; i < voltageLevels.size(); i++) {
            VoltageLevel voltageLevel = voltageLevels.get(i);
            voltageLevelNum.put(voltageLevel.getId(), i);
        }

        // buses
        buses = network.getBusBreakerView().getBusStream().toList();
        busV = new double[buses.size()];
        Map<String, Integer> busIdToGlobalNum = new HashMap<>(buses.size());
        for (int i = 0; i < buses.size(); i++) {
            Bus bus = buses.get(i);
            busIdToGlobalNum.put(bus.getId(), i);
        }
        globalToLocalBusNum = new int[buses.size()];
        for (VoltageLevel voltageLevel : voltageLevels) {
            List<Bus> localBuses = voltageLevel.getBusBreakerView().getBusStream().toList();
            for (int localNum = 0; localNum < localBuses.size(); localNum++) {
                globalToLocalBusNum[busIdToGlobalNum.get(localBuses.get(localNum).getId())] = localNum + 1;
            }
        }

        // loads
        loads = network.getLoadStream().toList();
        loadName = Util.createCharPtrArray(loads.stream().map(Identifiable::getId).toList());
        loadVoltageLevelNum = allocArrayPointer(UnmanagedMemory.calloc(loads.size() * SizeOf.get(CIntPointer.class)), loads.size());
        loadP = allocArrayPointer(UnmanagedMemory.calloc(loads.size() * SizeOf.get(CDoublePointer.class)), loads.size());
        loadQ = allocArrayPointer(UnmanagedMemory.calloc(loads.size() * SizeOf.get(CDoublePointer.class)), loads.size());
        loadV = allocArrayPointer(UnmanagedMemory.calloc(loads.size() * SizeOf.get(CDoublePointer.class)), loads.size());
        loadBusGlobalNum = new int[loads.size()];
        loadBusLocalNum = UnmanagedMemory.calloc(loads.size() * SizeOf.get(CIntPointer.class));
        for (int i = 0; i < loads.size(); i++) {
            Load load = loads.get(i);
            loadVoltageLevelNum.getPtr().write(i, voltageLevelNum.get(load.getTerminal().getVoltageLevel().getId()));
            Bus bus = load.getTerminal().getBusBreakerView().getBus();
            loadBusGlobalNum[i] = bus == null ? -1 : busIdToGlobalNum.get(bus.getId());
            loadBusLocalNum.write(i, globalToLocalBusNum[loadBusGlobalNum[i]]);
        }

        // generators
        generators = network.getGeneratorStream().toList();
        generatorName = Util.createCharPtrArray(generators.stream().map(Identifiable::getId).toList());
        generatorVoltageLevelNum = allocArrayPointer(UnmanagedMemory.calloc(generators.size() * SizeOf.get(CIntPointer.class)), generators.size());
        generatorP = allocArrayPointer(UnmanagedMemory.calloc(generators.size() * SizeOf.get(CDoublePointer.class)), generators.size());
        generatorQ = allocArrayPointer(UnmanagedMemory.calloc(generators.size() * SizeOf.get(CDoublePointer.class)), generators.size());
        generatorV = allocArrayPointer(UnmanagedMemory.calloc(generators.size() * SizeOf.get(CDoublePointer.class)), generators.size());
        generatorBusGlobalNum = new int[generators.size()];
        generatorBusLocalNum = UnmanagedMemory.calloc(generators.size() * SizeOf.get(CIntPointer.class));
        for (int i = 0; i < generators.size(); i++) {
            Generator generator = generators.get(i);
            generatorVoltageLevelNum.getPtr().write(i, voltageLevelNum.get(generator.getTerminal().getVoltageLevel().getId()));
            Bus bus = generator.getTerminal().getBusBreakerView().getBus();
            generatorBusGlobalNum[i] = bus == null ? -1 : busIdToGlobalNum.get(bus.getId());
            generatorBusLocalNum.write(i, globalToLocalBusNum[generatorBusGlobalNum[i]]);
        }

        // shunts
        shunts = network.getShuntCompensatorStream().toList();
        shuntName = Util.createCharPtrArray(shunts.stream().map(Identifiable::getId).toList());
        shuntVoltageLevelNum = allocArrayPointer(UnmanagedMemory.calloc(shunts.size() * SizeOf.get(CIntPointer.class)), shunts.size());
        shuntP = allocArrayPointer(UnmanagedMemory.calloc(shunts.size() * SizeOf.get(CDoublePointer.class)), shunts.size());
        shuntQ = allocArrayPointer(UnmanagedMemory.calloc(shunts.size() * SizeOf.get(CDoublePointer.class)), shunts.size());
        shuntV = allocArrayPointer(UnmanagedMemory.calloc(shunts.size() * SizeOf.get(CDoublePointer.class)), shunts.size());
        shuntBusGlobalNum = new int[shunts.size()];
        shuntBusLocalNum = UnmanagedMemory.calloc(shunts.size() * SizeOf.get(CIntPointer.class));
        for (int i = 0; i < shunts.size(); i++) {
            ShuntCompensator shunt = shunts.get(i);
            shuntVoltageLevelNum.getPtr().write(i, voltageLevelNum.get(shunt.getTerminal().getVoltageLevel().getId()));
            Bus bus = shunt.getTerminal().getBusBreakerView().getBus();
            shuntBusGlobalNum[i] = bus == null ? -1 : busIdToGlobalNum.get(bus.getId());
            shuntBusLocalNum.write(i, globalToLocalBusNum[shuntBusGlobalNum[i]]);
        }

        // branches
        branches = network.getBranchStream().toList();
        branchName = Util.createCharPtrArray(branches.stream().map(Identifiable::getId).toList());
        branchVoltageLevelNum1 = allocArrayPointer(UnmanagedMemory.calloc(branches.size() * SizeOf.get(CIntPointer.class)), branches.size());
        branchVoltageLevelNum2 = allocArrayPointer(UnmanagedMemory.calloc(branches.size() * SizeOf.get(CIntPointer.class)), branches.size());
        branchP1 = allocArrayPointer(UnmanagedMemory.calloc(branches.size() * SizeOf.get(CDoublePointer.class)), branches.size());
        branchP2 = allocArrayPointer(UnmanagedMemory.calloc(branches.size() * SizeOf.get(CDoublePointer.class)), branches.size());
        branchQ1 = allocArrayPointer(UnmanagedMemory.calloc(branches.size() * SizeOf.get(CDoublePointer.class)), branches.size());
        branchQ2 = allocArrayPointer(UnmanagedMemory.calloc(branches.size() * SizeOf.get(CDoublePointer.class)), branches.size());
        branchV1 = allocArrayPointer(UnmanagedMemory.calloc(branches.size() * SizeOf.get(CDoublePointer.class)), branches.size());
        branchV2 = allocArrayPointer(UnmanagedMemory.calloc(branches.size() * SizeOf.get(CDoublePointer.class)), branches.size());
        branchI1 = allocArrayPointer(UnmanagedMemory.calloc(branches.size() * SizeOf.get(CDoublePointer.class)), branches.size());
        branchI2 = allocArrayPointer(UnmanagedMemory.calloc(branches.size() * SizeOf.get(CDoublePointer.class)), branches.size());
        branchBusGlobalNum1 = new int[branches.size()];
        branchBusGlobalNum2 = new int[branches.size()];
        branchBusLocalNum1 = UnmanagedMemory.calloc(branches.size() * SizeOf.get(CIntPointer.class));
        branchBusLocalNum2 = UnmanagedMemory.calloc(branches.size() * SizeOf.get(CIntPointer.class));
        for (int i = 0; i < branches.size(); i++) {
            Branch<?> branch = branches.get(i);
            branchVoltageLevelNum1.getPtr().write(i, voltageLevelNum.get(branch.getTerminal1().getVoltageLevel().getId()));
            branchVoltageLevelNum2.getPtr().write(i, voltageLevelNum.get(branch.getTerminal2().getVoltageLevel().getId()));
            Bus bus1 = branch.getTerminal1().getBusBreakerView().getBus();
            Bus bus2 = branch.getTerminal2().getBusBreakerView().getBus();
            branchBusGlobalNum1[i] = bus1 == null ? -1 : busIdToGlobalNum.get(bus1.getId());
            branchBusGlobalNum2[i] = bus2 == null ? -1 : busIdToGlobalNum.get(bus2.getId());
            branchBusLocalNum1.write(i, globalToLocalBusNum[branchBusGlobalNum1[i]]);
            branchBusLocalNum2.write(i, globalToLocalBusNum[branchBusGlobalNum2[i]]);
        }

        updateBuses();
        updateLoads();
        updateGenerators();
        updateShunts();
        updateBranches();
    }

    private static void fillIntBuffer(CIntPointer ptr, int length, int value) {
        for (int i = 0; i < length; i++) {
            ptr.write(i, value);
        }
    }

    private static double fixNan(double f) {
        return Double.isNaN(f) ? 0 : f;
    }

    public void updateBuses() {
        for (int i = 0; i < buses.size(); i++) {
            Bus bus = buses.get(i);
            busV[i] = fixNan(bus.getV());
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
            shuntV.getPtr().write(i, busV[shuntBusGlobalNum[i]]);
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
            case LOAD_VOLTAGE_LEVEL_NUM -> loadVoltageLevelNum;
            case GENERATOR_VOLTAGE_LEVEL_NUM -> generatorVoltageLevelNum;
            case SHUNT_VOLTAGE_LEVEL_NUM -> shuntVoltageLevelNum;
            case BRANCH_VOLTAGE_LEVEL_NUM_1 -> branchVoltageLevelNum1;
            case BRANCH_VOLTAGE_LEVEL_NUM_2 -> branchVoltageLevelNum2;
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

    @Override
    public void close() {
        Util.freeCharPtrArray(voltageLevelName);

        Util.freeCharPtrArray(loadName);
        PyPowsyblApiHeader.freeArrayPointer(loadVoltageLevelNum);
        PyPowsyblApiHeader.freeArrayPointer(loadP);
        PyPowsyblApiHeader.freeArrayPointer(loadQ);
        PyPowsyblApiHeader.freeArrayPointer(loadV);
        UnmanagedMemory.free(loadBusLocalNum);

        Util.freeCharPtrArray(generatorName);
        PyPowsyblApiHeader.freeArrayPointer(generatorVoltageLevelNum);
        PyPowsyblApiHeader.freeArrayPointer(generatorP);
        PyPowsyblApiHeader.freeArrayPointer(generatorQ);
        PyPowsyblApiHeader.freeArrayPointer(generatorV);
        UnmanagedMemory.free(generatorBusLocalNum);

        Util.freeCharPtrArray(shuntName);
        PyPowsyblApiHeader.freeArrayPointer(shuntVoltageLevelNum);
        PyPowsyblApiHeader.freeArrayPointer(shuntP);
        PyPowsyblApiHeader.freeArrayPointer(shuntQ);
        PyPowsyblApiHeader.freeArrayPointer(shuntV);
        UnmanagedMemory.free(shuntBusLocalNum);

        Util.freeCharPtrArray(branchName);
        PyPowsyblApiHeader.freeArrayPointer(branchVoltageLevelNum1);
        PyPowsyblApiHeader.freeArrayPointer(branchVoltageLevelNum2);
        PyPowsyblApiHeader.freeArrayPointer(branchP1);
        PyPowsyblApiHeader.freeArrayPointer(branchP2);
        PyPowsyblApiHeader.freeArrayPointer(branchQ1);
        PyPowsyblApiHeader.freeArrayPointer(branchQ2);
        PyPowsyblApiHeader.freeArrayPointer(branchV1);
        PyPowsyblApiHeader.freeArrayPointer(branchV2);
        PyPowsyblApiHeader.freeArrayPointer(branchI1);
        PyPowsyblApiHeader.freeArrayPointer(branchI2);
        UnmanagedMemory.free(branchBusLocalNum1);
        UnmanagedMemory.free(branchBusLocalNum2);
    }
}
