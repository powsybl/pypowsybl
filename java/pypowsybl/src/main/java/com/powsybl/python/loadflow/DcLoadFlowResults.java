/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.python.loadflow;

import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.NetworkListener;
import com.powsybl.iidm.network.ShuntCompensator;
import com.powsybl.iidm.network.StaticVarCompensator;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Interpretation of the state left on a network by a DC load flow.
 *
 * <p>A DC load flow only solves active powers and voltage angles: voltage magnitudes are left undefined on the
 * network, and so are the currents which derive from them. This class remembers, for each variant, which components
 * have been solved by a DC load flow and with which power factor, so that network dataframes can report the values
 * which are implied by the DC approximation:
 * <ul>
 *     <li>voltage magnitudes are the nominal voltages;</li>
 *     <li>currents are {@code |p| / (sqrt(3) * nominalV * dcPowerFactor)} at every terminal, like the branch
 *     currents monitored by a DC security analysis. As the same rule applies to all the terminals of a bus, which
 *     share the same nominal voltage, these currents are balanced at each bus like the active powers are;</li>
 *     <li>currents of the equipment which only exchanges reactive power (shunt and static var compensators)
 *     remain undefined.</li>
 * </ul>
 *
 * <p>The network itself is never modified: as soon as a voltage magnitude is defined on a bus, the values of the
 * network are reported. The interpretation is kept outside of the network (it is not an extension), so it is neither
 * exported nor merged with the network.
 *
 * @author Samuel Monroe {@literal <smn at n-side.com>}
 */
public final class DcLoadFlowResults implements NetworkListener {

    private record Component(int connected, int synchronous) {
    }

    private record State(double powerFactor, Set<Component> components) {
    }

    /**
     * One context per root network. Keys are weakly referenced and a context never references its network,
     * so that a context does not outlive its network.
     */
    private static final Map<Network, DcLoadFlowResults> CONTEXTS = Collections.synchronizedMap(new WeakHashMap<>());

    private final Map<String, State> states = new ConcurrentHashMap<>();

    private DcLoadFlowResults() {
    }

    /**
     * Forgets the DC interpretation of a variant. To be called before any load flow which may write on the variant.
     */
    public static void clear(Network network, String variantId) {
        DcLoadFlowResults context = CONTEXTS.get(network.getNetwork());
        if (context != null) {
            context.states.remove(variantId);
        }
    }

    /**
     * Records the result of a load flow which has been run on a variant of the network (or of one of its subnetworks).
     */
    public static void record(Network network, String variantId, LoadFlowParameters parameters, LoadFlowResult result) {
        Set<Component> components = parameters.isDc() ? convergedComponents(result) : Set.of();
        if (components.isEmpty()) {
            clear(network, variantId);
            return;
        }
        // listeners and variants are managed by the root network
        DcLoadFlowResults context = CONTEXTS.computeIfAbsent(network.getNetwork(), root -> {
            DcLoadFlowResults created = new DcLoadFlowResults();
            root.addListener(created);
            return created;
        });
        context.states.put(variantId, new State(parameters.getDcPowerFactor(), components));
    }

    private static Set<Component> convergedComponents(LoadFlowResult result) {
        return result.getComponentResults().stream()
                .filter(c -> c.getStatus() == LoadFlowResult.ComponentResult.Status.CONVERGED)
                .map(c -> new Component(c.getConnectedComponentNum(), c.getSynchronousComponentNum()))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Returns the DC state of a bus, or null if the bus does not hold the result of a DC load flow:
     * a DC load flow leaves a voltage angle but no voltage magnitude on the buses of the components it has solved.
     */
    private static State state(Bus bus) {
        if (bus == null || !Double.isNaN(bus.getV()) || !Double.isFinite(bus.getAngle())) {
            return null;
        }
        Network network = bus.getNetwork();
        DcLoadFlowResults context = CONTEXTS.get(network);
        if (context == null) {
            return null;
        }
        State state = context.states.get(network.getVariantManager().getWorkingVariantId());
        if (state == null || bus.getConnectedComponent() == null || bus.getSynchronousComponent() == null) {
            return null;
        }
        Component component = new Component(bus.getConnectedComponent().getNum(), bus.getSynchronousComponent().getNum());
        return state.components().contains(component) ? state : null;
    }

    /**
     * Voltage magnitude of a bus, in kV: the nominal voltage if the bus holds the result of a DC load flow.
     */
    public static double voltage(Bus bus) {
        return state(bus) != null ? bus.getVoltageLevel().getNominalV() : bus.getV();
    }

    /**
     * Voltage magnitude of a busbar section, in kV: the nominal voltage if its bus holds the result of a DC load flow.
     */
    public static double voltage(BusbarSection busbarSection) {
        double v = busbarSection.getV();
        return Double.isNaN(v) && state(busbarSection.getTerminal().getBusView().getBus()) != null
                ? busbarSection.getTerminal().getVoltageLevel().getNominalV() : v;
    }

    /**
     * Current at a terminal, in A: an estimate at nominal voltage if its bus holds the result of a DC load flow.
     */
    public static double current(Terminal terminal) {
        double i = terminal.getI();
        if (!Double.isNaN(i)) {
            return i;
        }
        State state = state(terminal.getBusView().getBus());
        if (state == null || isReactiveOnly(terminal.getConnectable())) {
            return i;
        }
        // A DC load flow does not compute reactive powers: the configured power factor stands for them, like in a
        // DC security analysis. The reactive power which may be defined at the terminal of an injection is not
        // used, so that the same rule applies to all the terminals of a bus and currents are balanced at each bus.
        return amperes(Math.abs(terminal.getP()) / state.powerFactor(), terminal.getVoltageLevel().getNominalV());
    }

    /**
     * Voltage magnitude at the boundary side of a boundary line, in kV: the nominal voltage if its bus holds the
     * result of a DC load flow.
     */
    public static double boundaryVoltage(BoundaryLine boundaryLine) {
        double v = boundaryLine.getBoundary().getV();
        return Double.isNaN(v) && state(boundaryLine.getTerminal().getBusView().getBus()) != null
                ? boundaryLine.getTerminal().getVoltageLevel().getNominalV() : v;
    }

    /**
     * Current at the boundary side of a boundary line, in A: an estimate at nominal voltage if its bus holds the
     * result of a DC load flow.
     */
    public static double boundaryCurrent(BoundaryLine boundaryLine) {
        double i = boundaryLine.getBoundary().getI();
        if (!Double.isNaN(i)) {
            return i;
        }
        State state = state(boundaryLine.getTerminal().getBusView().getBus());
        return state == null ? i
                : amperes(Math.abs(boundaryLine.getBoundary().getP()) / state.powerFactor(), boundaryLine.getTerminal().getVoltageLevel().getNominalV());
    }

    /**
     * Current in A for an apparent power in MVA and a voltage in kV.
     */
    private static double amperes(double apparentPower, double voltage) {
        return apparentPower * 1000 / (Math.sqrt(3) * voltage);
    }

    /**
     * The active power of such an equipment is zero by nature: a null current would be read as a result, whereas
     * the reactive current which is its purpose is out of the scope of a DC load flow.
     */
    private static boolean isReactiveOnly(Connectable<?> connectable) {
        return connectable instanceof ShuntCompensator || connectable instanceof StaticVarCompensator;
    }

    @Override
    public void onVariantCreated(String sourceVariantId, String targetVariantId) {
        onVariantOverwritten(sourceVariantId, targetVariantId);
    }

    @Override
    public void onVariantOverwritten(String sourceVariantId, String targetVariantId) {
        State state = states.get(sourceVariantId);
        if (state == null) {
            states.remove(targetVariantId);
        } else {
            states.put(targetVariantId, state);
        }
    }

    @Override
    public void onVariantRemoved(String variantId) {
        states.remove(variantId);
    }
}
