/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.fast;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import org.graalvm.nativeimage.c.type.CDoublePointer;
import org.graalvm.nativeimage.c.type.CIntPointer;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

/**
 * Positional, array oriented access to a network: the "fast path" next to the dataframe API.
 *
 * <p>Elements of a type are referred to by their position in the id list of that type ({@link #getIds}), which is
 * the row order of the corresponding dataframe. Columns are written straight into (or read straight from) native
 * memory owned by the caller. Every value is a snapshot at call time, in SI units (no per-unit).
 *
 * <p>The lists of loads, generators, lines, transformers, switches, busbar sections and voltage levels are captured
 * when the object is created and stay valid as long as no element of these types is created or removed (checked on
 * every call, see {@link #checkUpToDate}). Bus view buses are different: they are recomputed by powsybl after any
 * topology change, so a bus number is only meaningful until the next topology change.
 *
 * <p>Calls on one instance are serialized (the Python side releases the GIL around them); this does not make
 * concurrent modifications of the underlying network through other APIs safe.
 *
 * @author pypowsybl fast path fork
 */
public final class FastNetwork {

    public static final int LOAD = 0;
    public static final int GENERATOR = 1;
    public static final int LINE = 2;
    public static final int TWO_WINDINGS_TRANSFORMER = 3;
    public static final int SWITCH = 4;
    public static final int BUS = 5;
    public static final int VOLTAGE_LEVEL = 6;
    public static final int BUSBAR_SECTION = 7;
    public static final int TYPE_COUNT = 8;

    static final List<String> TYPE_NAMES = List.of("load", "generator", "line", "two_windings_transformer", "switch",
            "bus", "voltage_level", "busbar_section");

    /** Value of an integer column when there is nothing to refer to (no bus, no node, no component). */
    public static final int NONE = -1;

    /** A numeric column of one element type: exactly one of the double / int accessors is set. */
    static final class Column<T> {
        final String name;
        final ToDoubleFunction<T> doubleGetter;
        final ObjDoubleConsumer<T> doubleSetter;
        final ToIntFunction<T> intGetter;
        final ObjIntConsumer<T> intSetter;
        /** the getter needs the bus numbering of the current topology */
        final boolean busNumber;
        /**
         * Int column whose values cannot change while the element exists (the nodes of a switch): computed once per
         * {@link #refresh} then copied, as some of these getters are slow (a lookup by id for the nodes of a switch).
         */
        boolean constant;
        int[] constantValues;

        Column(String name, ToDoubleFunction<T> doubleGetter, ObjDoubleConsumer<T> doubleSetter,
               ToIntFunction<T> intGetter, ObjIntConsumer<T> intSetter, boolean busNumber) {
            this.name = name;
            this.doubleGetter = doubleGetter;
            this.doubleSetter = doubleSetter;
            this.intGetter = intGetter;
            this.intSetter = intSetter;
            this.busNumber = busNumber;
        }

        boolean isDouble() {
            return doubleGetter != null;
        }

        boolean isWritable() {
            return doubleSetter != null || intSetter != null;
        }
    }

    /** The columns of one element type, in a fixed order: the attribute code is the position in this list. */
    final class Table<T> {
        final List<Column<T>> columns = new ArrayList<>();

        Table<T> doubles(String name, ToDoubleFunction<T> getter) {
            return doubles(name, getter, null);
        }

        Table<T> doubles(String name, ToDoubleFunction<T> getter, ObjDoubleConsumer<T> setter) {
            columns.add(new Column<>(name, getter, setter, null, null, false));
            return this;
        }

        Table<T> ints(String name, ToIntFunction<T> getter) {
            return ints(name, getter, null);
        }

        Table<T> ints(String name, ToIntFunction<T> getter, ObjIntConsumer<T> setter) {
            columns.add(new Column<>(name, null, null, getter, setter, false));
            return this;
        }

        Table<T> constantInts(String name, ToIntFunction<T> getter) {
            ints(name, getter);
            columns.get(columns.size() - 1).constant = true;
            return this;
        }

        /** bus view bus number of a terminal, {@link #NONE} when the terminal is not in a bus */
        Table<T> bus(String name, java.util.function.Function<T, Terminal> terminal) {
            columns.add(new Column<>(name, null, null, e -> busNumber(terminal.apply(e)), null, true));
            return this;
        }

        Table<T> terminal(String suffix, java.util.function.Function<T, Terminal> terminal) {
            doubles("p" + suffix, e -> terminal.apply(e).getP(), (e, v) -> terminal.apply(e).setP(v));
            doubles("q" + suffix, e -> terminal.apply(e).getQ(), (e, v) -> terminal.apply(e).setQ(v));
            doubles("i" + suffix, e -> terminal.apply(e).getI());
            bus("bus" + suffix, terminal);
            ints("node" + suffix, e -> node(terminal.apply(e)));
            ints("connected" + suffix, e -> terminal.apply(e).isConnected() ? 1 : 0);
            ints("voltage_level" + suffix, e -> voltageLevelNumber(terminal.apply(e).getVoltageLevel()));
            return this;
        }
    }

    private final Network network;

    private final Object[][] elements = new Object[TYPE_COUNT][];
    private final Table<?>[] tables = new Table<?>[TYPE_COUNT];
    private final Map<VoltageLevel, Integer> voltageLevelNumbers = new IdentityHashMap<>();

    /** bus numbering of the call in progress (buses are not stable across topology changes: never kept) */
    private Map<Bus, Integer> busNumbers;
    private int lastBusCount = 1024;

    public FastNetwork(Network network) {
        this.network = network;
        buildTables();
        refresh();
    }

    public Network getNetwork() {
        return network;
    }

    /** Captures again the element lists; needed after elements have been created or removed. */
    public synchronized void refresh() {
        elements[LOAD] = network.getLoadStream().toArray();
        elements[GENERATOR] = network.getGeneratorStream().toArray();
        elements[LINE] = network.getLineStream().toArray();
        elements[TWO_WINDINGS_TRANSFORMER] = network.getTwoWindingsTransformerStream().toArray();
        elements[SWITCH] = network.getSwitchStream().toArray();
        elements[VOLTAGE_LEVEL] = network.getVoltageLevelStream().toArray();
        elements[BUSBAR_SECTION] = network.getBusbarSectionStream().toArray();
        elements[BUS] = null;
        for (Table<?> table : tables) {
            for (Column<?> column : table.columns) {
                column.constantValues = null;
            }
        }
        voltageLevelNumbers.clear();
        Object[] voltageLevels = elements[VOLTAGE_LEVEL];
        for (int i = 0; i < voltageLevels.length; i++) {
            voltageLevelNumbers.put((VoltageLevel) voltageLevels[i], i);
        }
    }

    private void buildTables() {
        tables[LOAD] = new Table<Load>()
                .doubles("p0", Load::getP0, Load::setP0)
                .doubles("q0", Load::getQ0, Load::setQ0)
                .terminal("", Load::getTerminal);
        tables[GENERATOR] = new Table<Generator>()
                .doubles("target_p", Generator::getTargetP, Generator::setTargetP)
                .doubles("target_q", Generator::getTargetQ, Generator::setTargetQ)
                .doubles("target_v", Generator::getTargetV, Generator::setTargetV)
                .doubles("min_p", Generator::getMinP)
                .doubles("max_p", Generator::getMaxP)
                .doubles("rated_s", Generator::getRatedS)
                .ints("voltage_regulator_on", g -> g.isVoltageRegulatorOn() ? 1 : 0, (g, v) -> g.setVoltageRegulatorOn(v != 0))
                .terminal("", Generator::getTerminal);
        tables[LINE] = new Table<Line>()
                .doubles("r", Line::getR)
                .doubles("x", Line::getX)
                .doubles("g1", Line::getG1)
                .doubles("b1", Line::getB1)
                .doubles("g2", Line::getG2)
                .doubles("b2", Line::getB2)
                .terminal("1", Line::getTerminal1)
                .terminal("2", Line::getTerminal2);
        tables[TWO_WINDINGS_TRANSFORMER] = new Table<TwoWindingsTransformer>()
                .doubles("r", TwoWindingsTransformer::getR)
                .doubles("x", TwoWindingsTransformer::getX)
                .doubles("g", TwoWindingsTransformer::getG)
                .doubles("b", TwoWindingsTransformer::getB)
                .doubles("rated_u1", TwoWindingsTransformer::getRatedU1)
                .doubles("rated_u2", TwoWindingsTransformer::getRatedU2)
                .doubles("rated_s", TwoWindingsTransformer::getRatedS)
                .terminal("1", TwoWindingsTransformer::getTerminal1)
                .terminal("2", TwoWindingsTransformer::getTerminal2);
        tables[SWITCH] = new Table<Switch>()
                .ints("open", s -> s.isOpen() ? 1 : 0, (s, v) -> s.setOpen(v != 0))
                .ints("retained", s -> s.isRetained() ? 1 : 0, (s, v) -> s.setRetained(v != 0))
                .constantInts("kind", s -> s.getKind().ordinal())
                .constantInts("node1", s -> switchNode(s, true))
                .constantInts("node2", s -> switchNode(s, false))
                .constantInts("voltage_level", s -> voltageLevelNumber(s.getVoltageLevel()));
        tables[BUS] = new Table<Bus>()
                .doubles("v_mag", Bus::getV, Bus::setV)
                .doubles("v_angle", Bus::getAngle, Bus::setAngle)
                .ints("connected_component", b -> componentNumber(b.getConnectedComponent()))
                .ints("synchronous_component", b -> componentNumber(b.getSynchronousComponent()))
                .ints("voltage_level", b -> voltageLevelNumber(b.getVoltageLevel()));
        tables[VOLTAGE_LEVEL] = new Table<VoltageLevel>()
                .doubles("nominal_v", VoltageLevel::getNominalV)
                .doubles("low_voltage_limit", VoltageLevel::getLowVoltageLimit)
                .doubles("high_voltage_limit", VoltageLevel::getHighVoltageLimit)
                .ints("node_breaker", vl -> vl.getTopologyKind() == TopologyKind.NODE_BREAKER ? 1 : 0);
        tables[BUSBAR_SECTION] = new Table<BusbarSection>()
                .doubles("v", BusbarSection::getV)
                .doubles("angle", BusbarSection::getAngle)
                .bus("bus", BusbarSection::getTerminal)
                .ints("node", bbs -> node(bbs.getTerminal()))
                .ints("connected", bbs -> bbs.getTerminal().isConnected() ? 1 : 0)
                .ints("voltage_level", bbs -> voltageLevelNumber(bbs.getTerminal().getVoltageLevel()));
    }

    // ---- value helpers -------------------------------------------------------------------------------------------

    private static int node(Terminal t) {
        return t.getVoltageLevel().getTopologyKind() == TopologyKind.NODE_BREAKER ? t.getNodeBreakerView().getNode() : NONE;
    }

    private static int switchNode(Switch s, boolean side1) {
        VoltageLevel vl = s.getVoltageLevel();
        if (vl.getTopologyKind() != TopologyKind.NODE_BREAKER) {
            return NONE;
        }
        return side1 ? vl.getNodeBreakerView().getNode1(s.getId()) : vl.getNodeBreakerView().getNode2(s.getId());
    }

    private static int componentNumber(Component component) {
        return component != null ? component.getNum() : NONE;
    }

    private int voltageLevelNumber(VoltageLevel vl) {
        Integer num = voltageLevelNumbers.get(vl);
        if (num == null) {
            throw staleException();
        }
        return num;
    }

    private int busNumber(Terminal t) {
        Bus bus = t.getBusView().getBus();
        if (bus == null) {
            return NONE;
        }
        Integer num = busNumbers.get(bus);
        if (num == null) {
            throw new PowsyblException("Bus " + bus.getId() + " is not in the bus view of the network");
        }
        return num;
    }

    // ---- element lists -------------------------------------------------------------------------------------------

    private static PowsyblException staleException() {
        return new PowsyblException("Elements have been created or removed since this fast view was built: call refresh()");
    }

    private static void checkType(int type) {
        if (type < 0 || type >= TYPE_COUNT) {
            throw new PowsyblException("Unknown fast element type: " + type);
        }
    }

    /**
     * Element counts are an O(1) check that catches creations and removals (not a removal followed by a creation
     * of the same type between two calls, for which {@link #refresh} has to be called).
     */
    private void checkUpToDate(int type) {
        int actual = switch (type) {
            case LOAD -> network.getLoadCount();
            case GENERATOR -> network.getGeneratorCount();
            case LINE -> network.getLineCount();
            case TWO_WINDINGS_TRANSFORMER -> network.getTwoWindingsTransformerCount();
            case SWITCH -> network.getSwitchCount();
            case VOLTAGE_LEVEL -> network.getVoltageLevelCount();
            case BUSBAR_SECTION -> network.getBusbarSectionCount();
            default -> -1;
        };
        if (actual != -1 && actual != elements[type].length) {
            throw staleException();
        }
        if (network.getVoltageLevelCount() != elements[VOLTAGE_LEVEL].length) {
            throw staleException();
        }
    }

    /** Bus view buses in the order of the buses dataframe, numbered for the duration of the call. */
    private Object[] snapshotBuses() {
        // same order as network.getBusView().getBuses(): voltage level by voltage level
        List<Bus> buses = new ArrayList<>(lastBusCount + 64);
        Map<Bus, Integer> numbers = new IdentityHashMap<>(lastBusCount + 64);
        for (Object vl : elements[VOLTAGE_LEVEL]) {
            for (Bus bus : ((VoltageLevel) vl).getBusView().getBuses()) {
                numbers.put(bus, buses.size());
                buses.add(bus);
            }
        }
        lastBusCount = buses.size();
        busNumbers = numbers;
        return buses.toArray();
    }

    private Object[] elements(int type, boolean needsBusNumbers) {
        checkType(type);
        checkUpToDate(type);
        if (type == BUS) {
            return snapshotBuses();
        }
        if (needsBusNumbers) {
            snapshotBuses();
        }
        return elements[type];
    }

    private void endCall() {
        busNumbers = null;
    }

    public synchronized int getCount(int type) {
        try {
            return elements(type, false).length;
        } finally {
            endCall();
        }
    }

    public synchronized List<String> getIds(int type) {
        try {
            Object[] list = elements(type, false);
            List<String> ids = new ArrayList<>(list.length);
            for (Object o : list) {
                ids.add(((Identifiable<?>) o).getId());
            }
            return ids;
        } finally {
            endCall();
        }
    }

    /** Column descriptions of a type, "name:kind:access" with kind d (double) or i (int32), access r or rw. */
    public List<String> getColumnDescriptions(int type) {
        checkType(type);
        List<String> descriptions = new ArrayList<>();
        for (Column<?> column : tables[type].columns) {
            descriptions.add(column.name + ":" + (column.isDouble() ? "d" : "i") + ":" + (column.isWritable() ? "rw" : "r"));
        }
        return descriptions;
    }

    public static List<String> getTypeNames() {
        return TYPE_NAMES;
    }

    // ---- columns -------------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Column<Object> column(int type, int attribute) {
        checkType(type);
        List<? extends Column<?>> columns = tables[type].columns;
        if (attribute < 0 || attribute >= columns.size()) {
            throw new PowsyblException("Unknown column " + attribute + " for element type " + TYPE_NAMES.get(type));
        }
        return (Column<Object>) columns.get(attribute);
    }

    private static void checkLength(Object[] list, int length, int type) {
        if (length != list.length) {
            throw new PowsyblException("Array of length " + length + " given for " + list.length + " elements of type "
                    + TYPE_NAMES.get(type));
        }
    }

    private static void checkPosition(int position, int count) {
        if (position < 0 || position >= count) {
            throw new PowsyblException("Position " + position + " out of range [0, " + count + ")");
        }
    }

    /** Writes a double column for all the elements of the type; returns the element count. */
    public synchronized int getDoubles(int type, int attribute, CDoublePointer out, int length) {
        try {
            Column<Object> column = column(type, attribute);
            if (!column.isDouble()) {
                throw new PowsyblException("Column " + column.name + " is an integer column");
            }
            Object[] list = elements(type, column.busNumber);
            checkLength(list, length, type);
            ToDoubleFunction<Object> getter = column.doubleGetter;
            for (int i = 0; i < list.length; i++) {
                out.write(i, getter.applyAsDouble(list[i]));
            }
            return list.length;
        } finally {
            endCall();
        }
    }

    /** Writes an int column for all the elements of the type; returns the element count. */
    public synchronized int getInts(int type, int attribute, CIntPointer out, int length) {
        try {
            Column<Object> column = column(type, attribute);
            if (column.isDouble()) {
                throw new PowsyblException("Column " + column.name + " is a double column");
            }
            Object[] list = elements(type, column.busNumber);
            checkLength(list, length, type);
            ToIntFunction<Object> getter = column.intGetter;
            if (column.constant) {
                int[] values = column.constantValues;
                if (values == null) {
                    values = new int[list.length];
                    for (int i = 0; i < list.length; i++) {
                        values[i] = getter.applyAsInt(list[i]);
                    }
                    column.constantValues = values;
                }
                for (int i = 0; i < list.length; i++) {
                    out.write(i, values[i]);
                }
                return list.length;
            }
            for (int i = 0; i < list.length; i++) {
                out.write(i, getter.applyAsInt(list[i]));
            }
            return list.length;
        } finally {
            endCall();
        }
    }

    /**
     * Sets a double column for the elements at the given positions (all the elements, in order, when
     * {@code positions} is null). Positions are validated before anything is modified.
     */
    public synchronized void setDoubles(int type, int attribute, CIntPointer positions, CDoublePointer values, int count) {
        try {
            Column<Object> column = column(type, attribute);
            if (column.doubleSetter == null) {
                throw new PowsyblException("Column " + column.name + " is not a writable double column");
            }
            Object[] list = elements(type, false);
            ObjDoubleConsumer<Object> setter = column.doubleSetter;
            if (positions.isNull()) {
                checkLength(list, count, type);
                for (int i = 0; i < count; i++) {
                    setter.accept(list[i], values.read(i));
                }
            } else {
                for (int i = 0; i < count; i++) {
                    checkPosition(positions.read(i), list.length);
                }
                for (int i = 0; i < count; i++) {
                    setter.accept(list[positions.read(i)], values.read(i));
                }
            }
        } finally {
            endCall();
        }
    }

    /** Same as {@link #setDoubles} for an int column (booleans are 0 / 1). */
    public synchronized void setInts(int type, int attribute, CIntPointer positions, CIntPointer values, int count) {
        try {
            Column<Object> column = column(type, attribute);
            if (column.intSetter == null) {
                throw new PowsyblException("Column " + column.name + " is not a writable integer column");
            }
            Object[] list = elements(type, false);
            ObjIntConsumer<Object> setter = column.intSetter;
            if (positions.isNull()) {
                checkLength(list, count, type);
                for (int i = 0; i < count; i++) {
                    setter.accept(list[i], values.read(i));
                }
            } else {
                for (int i = 0; i < count; i++) {
                    checkPosition(positions.read(i), list.length);
                }
                for (int i = 0; i < count; i++) {
                    setter.accept(list[positions.read(i)], values.read(i));
                }
            }
        } finally {
            endCall();
        }
    }

    // ---- compound: switch states in, topology out ----------------------------------------------------------------

    /** Number of terminals written by {@link #applySwitchesAndGetTopology}. */
    public synchronized int getTerminalCount() {
        return elements[LOAD].length + elements[GENERATOR].length + 2 * elements[LINE].length
                + 2 * elements[TWO_WINDINGS_TRANSFORMER].length;
    }

    /**
     * Applies switch states, then writes the resulting topology in one call:
     * <ul>
     *     <li>{@code terminalBus}: bus number of every terminal, laid out as loads, generators, lines side 1,
     *     lines side 2, two windings transformers side 1, two windings transformers side 2 (each block in id list
     *     order); {@link #NONE} for a terminal which is not in a bus;</li>
     *     <li>{@code busComponent}: connected component number of every bus (bus numbers are positions in the bus
     *     id list at the time of the call), written only if the buses fit in {@code busCapacity}.</li>
     * </ul>
     * Returns the bus count: when it is larger than {@code busCapacity} the caller reads the bus column separately.
     */
    public synchronized int applySwitchesAndGetTopology(CIntPointer switchPositions, CIntPointer switchOpen, int switchCount,
                                           CIntPointer terminalBus, int terminalCount,
                                           CIntPointer busComponent, int busCapacity) {
        try {
            for (int type : new int[] {LOAD, GENERATOR, LINE, TWO_WINDINGS_TRANSFORMER, SWITCH}) {
                checkUpToDate(type);
            }
            if (terminalCount != getTerminalCount()) {
                throw new PowsyblException("Terminal array of length " + terminalCount + " given for "
                        + getTerminalCount() + " terminals");
            }
            Object[] switches = elements[SWITCH];
            for (int i = 0; i < switchCount; i++) {
                checkPosition(switchPositions.read(i), switches.length);
            }
            for (int i = 0; i < switchCount; i++) {
                ((Switch) switches[switchPositions.read(i)]).setOpen(switchOpen.read(i) != 0);
            }

            Object[] buses = snapshotBuses();
            int k = 0;
            for (Object o : elements[LOAD]) {
                terminalBus.write(k++, busNumber(((Load) o).getTerminal()));
            }
            for (Object o : elements[GENERATOR]) {
                terminalBus.write(k++, busNumber(((Generator) o).getTerminal()));
            }
            for (Object o : elements[LINE]) {
                terminalBus.write(k++, busNumber(((Line) o).getTerminal1()));
            }
            for (Object o : elements[LINE]) {
                terminalBus.write(k++, busNumber(((Line) o).getTerminal2()));
            }
            for (Object o : elements[TWO_WINDINGS_TRANSFORMER]) {
                terminalBus.write(k++, busNumber(((TwoWindingsTransformer) o).getTerminal1()));
            }
            for (Object o : elements[TWO_WINDINGS_TRANSFORMER]) {
                terminalBus.write(k++, busNumber(((TwoWindingsTransformer) o).getTerminal2()));
            }
            if (buses.length <= busCapacity) {
                for (int i = 0; i < buses.length; i++) {
                    busComponent.write(i, componentNumber(((Bus) buses[i]).getConnectedComponent()));
                }
            }
            return buses.length;
        } finally {
            endCall();
        }
    }

    // ---- node/breaker topology -----------------------------------------------------------------------------------

    /**
     * Internal connections of all the node/breaker voltage levels as (voltage level number, node1, node2) triplets,
     * written if they fit in {@code capacity} triplets. Returns the number of internal connections.
     * With the node columns of the switches and of the terminals this is the whole node/breaker graph.
     */
    public synchronized int getInternalConnections(CIntPointer out, int capacity) {
        checkUpToDate(VOLTAGE_LEVEL);
        int count = 0;
        Object[] voltageLevels = elements[VOLTAGE_LEVEL];
        for (int v = 0; v < voltageLevels.length; v++) {
            VoltageLevel vl = (VoltageLevel) voltageLevels[v];
            if (vl.getTopologyKind() != TopologyKind.NODE_BREAKER) {
                continue;
            }
            for (VoltageLevel.NodeBreakerView.InternalConnection ic : vl.getNodeBreakerView().getInternalConnections()) {
                if (count < capacity) {
                    out.write(3 * count, v);
                    out.write(3 * count + 1, ic.getNode1());
                    out.write(3 * count + 2, ic.getNode2());
                }
                count++;
            }
        }
        return count;
    }
}
