"""Timings of the stock pandas API on a node/breaker network (the "before" column).

    python bench_stock.py NETWORK.biidm [--runs 15] [--out stock.json]

Every figure is the median of --runs repetitions, in milliseconds, after 3 warm-up repetitions.
Works with any pypowsybl >= 1.16 (stock, RTE build, or this fork).
"""
import argparse
import json
import statistics
import sys
import time

import numpy as np

import pypowsybl as pp


def median_ms(fn, runs, setup=None, warmup=3):
    samples = []
    for i in range(warmup + runs):
        if setup is not None:
            setup(i)
        t0 = time.perf_counter()
        fn()
        dt = time.perf_counter() - t0
        if i >= warmup:
            samples.append(dt * 1e3)
    return statistics.median(samples)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('network')
    parser.add_argument('--runs', type=int, default=15)
    parser.add_argument('--out')
    args = parser.parse_args()
    runs = args.runs

    res = {}
    t0 = time.perf_counter()
    n = pp.network.load(args.network)
    res['load_network'] = (time.perf_counter() - t0) * 1e3

    sw = n.get_switches(attributes=['open'])
    sw_ids = sw.index.to_numpy()
    sw_open = sw['open'].to_numpy().copy()
    # a closed breaker to toggle (a state change that invalidates the topology cache)
    kinds = n.get_switches(attributes=['kind', 'open', 'retained'])
    toggle_id = kinds.index[(kinds['kind'] == 'BREAKER') & (~kinds['open'])][len(kinds) // 7 % 1000]
    state = {'open': False}

    def toggle(_i=None):
        state['open'] = not state['open']
        n.update_switches(id=toggle_id, open=state['open'])

    res['n_voltage_levels'] = len(n.get_voltage_levels(attributes=[]))
    res['n_switches'] = len(sw_ids)
    res['n_loads'] = len(n.get_loads(attributes=[]))
    res['n_lines'] = len(n.get_lines(attributes=[]))
    res['n_buses'] = len(n.get_buses(attributes=[]))

    # 1. open one switch (measured over many calls: one call is far below timer noise otherwise)
    def one_switch_loop():
        for k in range(200):
            n.update_switches(id=toggle_id, open=(k % 2 == 0))
    res['open_one_switch'] = median_ms(one_switch_loop, runs) / 200
    n.update_switches(id=toggle_id, open=False)

    # 2. update all switches by id (same values: measures the round trip, not the topology)
    res['update_all_switches'] = median_ms(lambda: n.update_switches(id=sw_ids, open=sw_open), runs)

    # 3. bus id of every load, after a switch change. The topology recomputation triggered by the change is paid
    #    by a first bus query in the setup, so the figure is the read itself.
    def toggle_and_refresh(_i=None):
        toggle()
        n.get_buses(attributes=[])
    res['loads_bus_id'] = median_ms(lambda: n.get_loads(attributes=['bus_id']), runs, setup=toggle_and_refresh)

    # 4. bus ids of both ends of every line
    res['lines_bus_ids'] = median_ms(lambda: n.get_lines(attributes=['bus1_id', 'bus2_id']), runs,
                                     setup=toggle_and_refresh)

    # 5. loads p0/q0
    res['loads_p0_q0'] = median_ms(lambda: n.get_loads(attributes=['p0', 'q0']), runs)

    # 5b. write loads p0/q0
    loads = n.get_loads(attributes=['p0', 'q0'])
    load_ids = loads.index.to_numpy()
    p0 = loads['p0'].to_numpy().copy()
    q0 = loads['q0'].to_numpy().copy()
    res['update_loads_p0_q0'] = median_ms(lambda: n.update_loads(id=load_ids, p0=p0, q0=q0), runs)

    # 6. node/breaker topology of every voltage level
    vl_ids = list(n.get_voltage_levels(attributes=['topology_kind']).query("topology_kind == 'NODE_BREAKER'").index)

    def all_topologies():
        for vl in vl_ids:
            t = n.get_node_breaker_topology(vl)
            _ = (t.switches, t.nodes, t.internal_connections)
    total = median_ms(all_topologies, max(3, runs // 5), warmup=1)
    res['node_breaker_topology_all'] = total
    res['node_breaker_topology_per_vl'] = total / len(vl_ids)

    # 7. all bus columns right after one switch change (includes powsybl's component recomputation)
    res['buses_after_switch_change'] = median_ms(lambda: n.get_buses(), runs, setup=toggle)

    # 8. the compound operation done with the stock API: apply a set of switch states, then bus of every terminal
    #    (loads, generators, lines, 2WT) and connected component of every bus
    some = np.arange(0, len(sw_ids), 97)
    some_ids = sw_ids[some]
    some_open = sw_open[some]

    def compound_setup(i):
        some_open[i % len(some_open)] = not some_open[i % len(some_open)]

    def compound():
        n.update_switches(id=some_ids, open=some_open)
        n.get_buses(attributes=['connected_component'])
        n.get_loads(attributes=['bus_id'])
        n.get_generators(attributes=['bus_id'])
        n.get_lines(attributes=['bus1_id', 'bus2_id'])
        n.get_2_windings_transformers(attributes=['bus1_id', 'bus2_id'])
    res['compound_switches_then_topology'] = median_ms(compound, runs, setup=compound_setup)
    n.update_switches(id=sw_ids, open=sw_open)

    res['runs'] = runs
    res['pypowsybl_version'] = pp.__version__
    res['python'] = sys.version.split()[0]
    text = json.dumps(res, indent=1)
    print(text)
    if args.out:
        with open(args.out, 'w') as f:
            f.write(text)


if __name__ == '__main__':
    main()
