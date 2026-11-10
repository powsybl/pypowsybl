import com.powsybl.iidm.network.*;

import java.nio.file.Path;
import java.util.*;
import java.util.function.IntConsumer;

/**
 * Pure Java reference timings for the operations of bench_stock.py / bench_fast.py (same definitions, same network).
 *
 *   java -cp &lt;powsybl classpath&gt;:. JavaBench NETWORK.biidm [runs] [warmup]
 *
 * Each figure is the median of `runs` repetitions after `warmup` repetitions (warm JIT), in milliseconds.
 * Output: one JSON object.
 */
public final class JavaBench {

    static double sink; // keeps results alive

    interface Body {
        void run();
    }

    static double medianMs(int warmup, int runs, IntConsumer setup, Body body) {
        double[] samples = new double[runs];
        for (int i = 0; i < warmup + runs; i++) {
            if (setup != null) {
                setup.accept(i);
            }
            long t0 = System.nanoTime();
            body.run();
            long dt = System.nanoTime() - t0;
            if (i >= warmup) {
                samples[i - warmup] = dt / 1e6;
            }
        }
        Arrays.sort(samples);
        return runs % 2 == 1 ? samples[runs / 2] : 0.5 * (samples[runs / 2 - 1] + samples[runs / 2]);
    }

    static int busNumber(Map<Bus, Integer> numbers, Terminal t) {
        Bus bus = t.getBusView().getBus();
        return bus == null ? -1 : numbers.get(bus);
    }

    public static void main(String[] args) {
        int runs = args.length > 1 ? Integer.parseInt(args[1]) : 51;
        int warmup = args.length > 2 ? Integer.parseInt(args[2]) : 30;
        Map<String, Object> res = new LinkedHashMap<>();

        long t0 = System.nanoTime();
        Network network = Network.read(Path.of(args[0]));
        res.put("load_network_cold", (System.nanoTime() - t0) / 1e6);

        Load[] loads = network.getLoadStream().toArray(Load[]::new);
        Generator[] generators = network.getGeneratorStream().toArray(Generator[]::new);
        Line[] lines = network.getLineStream().toArray(Line[]::new);
        TwoWindingsTransformer[] twts = network.getTwoWindingsTransformerStream().toArray(TwoWindingsTransformer[]::new);
        Switch[] switches = network.getSwitchStream().toArray(Switch[]::new);
        String[] switchIds = Arrays.stream(switches).map(Switch::getId).toArray(String[]::new);
        boolean[] switchOpen = new boolean[switches.length];
        for (int i = 0; i < switches.length; i++) {
            switchOpen[i] = switches[i].isOpen();
        }
        // same choice as bench_stock.py: a closed breaker
        List<Switch> closedBreakers = new ArrayList<>();
        for (Switch s : switches) {
            if (s.getKind() == SwitchKind.BREAKER && !s.isOpen()) {
                closedBreakers.add(s);
            }
        }
        Switch toggled = closedBreakers.get(switches.length / 7 % 1000);
        boolean[] state = {false};
        IntConsumer toggle = i -> {
            state[0] = !state[0];
            toggled.setOpen(state[0]);
        };
        IntConsumer toggleAndRefresh = i -> {
            toggle.accept(i);
            for (Bus b : network.getBusView().getBuses()) {
                sink += b.getConnectedComponent() == null ? 0 : 1;
            }
        };

        res.put("n_voltage_levels", network.getVoltageLevelCount());
        res.put("n_switches", switches.length);
        res.put("n_loads", loads.length);
        res.put("n_lines", lines.length);

        // 1. open one switch: the bare setter, by reference (the Java figure of the brief), and by id
        res.put("open_one_switch", medianMs(warmup, runs, null, () -> {
            for (int k = 0; k < 200; k++) {
                toggled.setOpen(k % 2 == 0);
            }
        }) / 200);
        String toggledId = toggled.getId();
        res.put("open_one_switch_by_id", medianMs(warmup, runs, null, () -> {
            for (int k = 0; k < 200; k++) {
                network.getSwitch(toggledId).setOpen(k % 2 == 0);
            }
        }) / 200);
        toggled.setOpen(false);

        // 2. update all switches: by id (hash lookup per switch), and by reference
        res.put("update_all_switches", medianMs(warmup, runs, null, () -> {
            for (int i = 0; i < switchIds.length; i++) {
                network.getSwitch(switchIds[i]).setOpen(switchOpen[i]);
            }
        }));
        res.put("update_all_switches_by_reference", medianMs(warmup, runs, null, () -> {
            for (int i = 0; i < switches.length; i++) {
                switches[i].setOpen(switchOpen[i]);
            }
        }));

        // 3. bus id of every load after a switch change (topology already recomputed in the setup)
        String[] loadBus = new String[loads.length];
        res.put("loads_bus_id", medianMs(warmup, runs, toggleAndRefresh, () -> {
            for (int i = 0; i < loads.length; i++) {
                Bus b = loads[i].getTerminal().getBusView().getBus();
                loadBus[i] = b == null ? "" : b.getId();
            }
        }));

        // 4. bus ids of both ends of every line
        String[] lineBus = new String[2 * lines.length];
        res.put("lines_bus_ids", medianMs(warmup, runs, toggleAndRefresh, () -> {
            for (int i = 0; i < lines.length; i++) {
                Bus b1 = lines[i].getTerminal1().getBusView().getBus();
                Bus b2 = lines[i].getTerminal2().getBusView().getBus();
                lineBus[2 * i] = b1 == null ? "" : b1.getId();
                lineBus[2 * i + 1] = b2 == null ? "" : b2.getId();
            }
        }));

        // 5. loads p0/q0, read and write
        double[] p0 = new double[loads.length];
        double[] q0 = new double[loads.length];
        res.put("loads_p0_q0", medianMs(warmup, runs, null, () -> {
            for (int i = 0; i < loads.length; i++) {
                p0[i] = loads[i].getP0();
                q0[i] = loads[i].getQ0();
            }
        }));
        res.put("update_loads_p0_q0", medianMs(warmup, runs, null, () -> {
            for (int i = 0; i < loads.length; i++) {
                loads[i].setP0(p0[i]);
                loads[i].setQ0(q0[i]);
            }
        }));

        // 6. node/breaker topology of every voltage level: switch nodes and states, internal connections
        res.put("node_breaker_topology_all", medianMs(warmup, runs, null, () -> {
            long acc = 0;
            for (VoltageLevel vl : network.getVoltageLevels()) {
                if (vl.getTopologyKind() != TopologyKind.NODE_BREAKER) {
                    continue;
                }
                VoltageLevel.NodeBreakerView view = vl.getNodeBreakerView();
                for (Switch s : view.getSwitches()) {
                    acc += view.getNode1(s.getId()) + view.getNode2(s.getId()) + (s.isOpen() ? 1 : 0);
                }
                for (VoltageLevel.NodeBreakerView.InternalConnection ic : view.getInternalConnections()) {
                    acc += ic.getNode1() + ic.getNode2();
                }
            }
            sink += acc;
        }));

        // 7. all bus columns right after one switch change (includes the component recomputation)
        res.put("buses_after_switch_change", medianMs(warmup, runs, toggle, () -> {
            double acc = 0;
            int n = 0;
            for (Bus b : network.getBusView().getBuses()) {
                acc += b.getId().length() + b.getV() + b.getAngle() + b.getVoltageLevel().getId().length();
                Component cc = b.getConnectedComponent();
                Component sc = b.getSynchronousComponent();
                acc += (cc == null ? -1 : cc.getNum()) + (sc == null ? -1 : sc.getNum());
                n++;
            }
            sink += acc;
            res.put("n_buses", n);
        }));

        // 8. compound: apply a set of switch states, then bus number of every terminal and component of every bus
        int nSome = (switches.length + 96) / 97;
        Switch[] some = new Switch[nSome];
        boolean[] someOpen = new boolean[nSome];
        for (int i = 0; i < nSome; i++) {
            some[i] = switches[i * 97];
            someOpen[i] = switchOpen[i * 97];
        }
        int[] terminalBus = new int[loads.length + generators.length + 2 * lines.length + 2 * twts.length];
        res.put("compound_switches_then_topology", medianMs(warmup, runs, i -> someOpen[i % nSome] = !someOpen[i % nSome], () -> {
            for (int i = 0; i < nSome; i++) {
                some[i].setOpen(someOpen[i]);
            }
            Map<Bus, Integer> numbers = new IdentityHashMap<>();
            List<Bus> buses = new ArrayList<>();
            for (Bus b : network.getBusView().getBuses()) {
                numbers.put(b, buses.size());
                buses.add(b);
            }
            int k = 0;
            for (Load l : loads) {
                terminalBus[k++] = busNumber(numbers, l.getTerminal());
            }
            for (Generator g : generators) {
                terminalBus[k++] = busNumber(numbers, g.getTerminal());
            }
            for (Line l : lines) {
                terminalBus[k++] = busNumber(numbers, l.getTerminal1());
            }
            for (Line l : lines) {
                terminalBus[k++] = busNumber(numbers, l.getTerminal2());
            }
            for (TwoWindingsTransformer t : twts) {
                terminalBus[k++] = busNumber(numbers, t.getTerminal1());
            }
            for (TwoWindingsTransformer t : twts) {
                terminalBus[k++] = busNumber(numbers, t.getTerminal2());
            }
            int[] cc = new int[buses.size()];
            for (int i = 0; i < cc.length; i++) {
                Component c = buses.get(i).getConnectedComponent();
                cc[i] = c == null ? -1 : c.getNum();
            }
            sink += cc.length + terminalBus[0];
        }));

        // network loading, warm
        res.put("load_network", medianMs(3, 7, null, () -> sink += Network.read(Path.of(args[0])).getLoadCount()));

        res.put("runs", runs);
        res.put("warmup", warmup);
        res.put("java", System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
        StringBuilder sb = new StringBuilder("{\n");
        Iterator<Map.Entry<String, Object>> it = res.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> e = it.next();
            Object v = e.getValue();
            sb.append(" \"").append(e.getKey()).append("\": ").append(v instanceof Number ? v.toString() : "\"" + v + "\"")
                    .append(it.hasNext() ? ",\n" : "\n");
        }
        System.out.println(sb.append("}"));
        if (sink == 42.4242) {
            System.out.println();
        }
    }
}
