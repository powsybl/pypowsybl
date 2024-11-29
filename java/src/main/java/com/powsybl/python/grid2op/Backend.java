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
    private final List<Bus> buses;
    private final List<Generator> generators;

    private final ArrayPointer<CCharPointerPointer> voltageLevelName;

    private final double[] busV;

    private final ArrayPointer<CCharPointerPointer> generatorName;
    private final ArrayPointer<CIntPointer> generatorVoltageLevelNum;
    private final ArrayPointer<CDoublePointer> generatorP;
    private final ArrayPointer<CDoublePointer> generatorQ;
    private final ArrayPointer<CDoublePointer> generatorV;
    private final int[] generatorBusGlobalNum;
    private final CIntPointer generatorBusLocalNum;

    public Backend(Network network) {
        this.network = Objects.requireNonNull(network);

        List<VoltageLevel> voltageLevels = network.getVoltageLevelStream().toList();
        voltageLevelName = Util.createCharPtrArray(voltageLevels.stream().map(Identifiable::getId).toList());
        Map<String, Integer> voltageLevelNum = new HashMap<>(voltageLevels.size());
        for (int i = 0; i < voltageLevels.size(); i++) {
            VoltageLevel voltageLevel = voltageLevels.get(i);
            voltageLevelNum.put(voltageLevel.getId(), i);
        }

        buses = network.getBusBreakerView().getBusStream().toList();
        busV = new double[buses.size()];
        Map<String, Integer> busIdToNum = new HashMap<>(buses.size());
        for (int i = 0; i < buses.size(); i++) {
            Bus bus = buses.get(i);
            busIdToNum.put(bus.getId(), i);
        }

        generators = network.getGeneratorStream().toList();
        generatorName = Util.createCharPtrArray(generators.stream().map(Identifiable::getId).toList());
        generatorVoltageLevelNum = allocArrayPointer(UnmanagedMemory.calloc(generators.size() * SizeOf.get(CIntPointer.class)), generators.size());
        generatorP = allocArrayPointer(UnmanagedMemory.calloc(generators.size() * SizeOf.get(CDoublePointer.class)), generators.size());
        generatorQ = allocArrayPointer(UnmanagedMemory.calloc(generators.size() * SizeOf.get(CDoublePointer.class)), generators.size());
        generatorV = allocArrayPointer(UnmanagedMemory.calloc(generators.size() * SizeOf.get(CDoublePointer.class)), generators.size());
        generatorBusGlobalNum = new int[generators.size()];
        for (int i = 0; i < generators.size(); i++) {
            Generator generator = generators.get(i);
            generatorVoltageLevelNum.getPtr().write(voltageLevelNum.get(generator.getTerminal().getVoltageLevel().getId()));
            Bus bus = generator.getTerminal().getBusBreakerView().getBus();
            generatorBusGlobalNum[i] = bus == null ? -1 : busIdToNum.get(bus.getId());
        }
        generatorBusLocalNum = UnmanagedMemory.calloc(generators.size() * SizeOf.get(CIntPointer.class));
        fillIntBuffer(generatorBusLocalNum, generators.size(), -1);

        updateBuses();
        updateGenerators();
    }

    private static void fillIntBuffer(CIntPointer ptr, int length, int value) {
        for (int i = 0; i < length; i++) {
            ptr.write(value);
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

    public void updateGenerators() {
        for (int i = 0; i < generators.size(); i++) {
            Generator generator = generators.get(i);
            Terminal terminal = generator.getTerminal();
            generatorP.getPtr().write(fixNan(terminal.getP()));
            generatorQ.getPtr().write(fixNan(terminal.getQ()));
            generatorV.getPtr().write(busV[generatorBusGlobalNum[i]]);
        }
    }

    public ArrayPointer<CCharPointerPointer> getStringValue(Grid2opCFunctions.Grid2opStringValueType valueType) {
        return switch (Objects.requireNonNull(valueType)) {
            case VOLTAGE_LEVEL_NAME -> voltageLevelName;
            case GENERATOR_NAME -> generatorName;
        };
    }

    public ArrayPointer<CIntPointer> getIntegerValue(Grid2opCFunctions.Grid2opIntegerValueType valueType) {
        return switch (Objects.requireNonNull(valueType)) {
            case GENERATOR_VOLTAGE_LEVEL_NUM -> generatorVoltageLevelNum;
        };
    }

    public ArrayPointer<CDoublePointer> getDoubleValue(Grid2opCFunctions.Grid2opDoubleValueType valueType) {
        return switch (Objects.requireNonNull(valueType)) {
            case GENERATOR_P -> generatorP;
            case GENERATOR_Q -> generatorQ;
            case GENERATOR_V -> generatorV;
        };
    }

    @Override
    public void close() {
        Util.freeCharPtrArray(voltageLevelName);

        Util.freeCharPtrArray(generatorName);
        PyPowsyblApiHeader.freeArrayPointer(generatorVoltageLevelNum);
        PyPowsyblApiHeader.freeArrayPointer(generatorP);
        PyPowsyblApiHeader.freeArrayPointer(generatorQ);
        PyPowsyblApiHeader.freeArrayPointer(generatorV);
        UnmanagedMemory.free(generatorBusLocalNum);
    }
}
