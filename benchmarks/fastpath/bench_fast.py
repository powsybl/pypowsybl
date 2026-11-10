"""Timings of the positional fast path (pypowsybl.fast) for the operations of bench_stock.py (the "after" column).

    python bench_fast.py NETWORK.biidm [--runs 51] [--out fast.json]

Same operation definitions, same switch choices as bench_stock.py and JavaBench.java. Medians in milliseconds.
"""
import argparse
import json
import sys
import time

import numpy as np
from bench_stock import median_ms

import pypowsybl as pp
from pypowsybl.fast import FastNetwork


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('network')
    parser.add_argument('--runs', type=int, default=51)
    parser.add_argument('--out')
    args = parser.parse_args()
    runs = args.runs

    res = {}
    n = pp.network.load(args.network)
    t0 = time.perf_counter()
    fast = FastNetwork(n)
    res['create_fast_view'] = (time.perf_counter() - t0) * 1e3
    t0 = time.perf_counter()
    for t in fast.element_types:
        fast.ids(t)
    res['fetch_all_id_lists_once'] = (time.perf_counter() - t0) * 1e3

    n_sw = fast.count('switch')
    sw_open = fast.get('switch', 'open')
    kind = fast.get('switch', 'kind')
    closed_breakers = np.flatnonzero((kind == 0) & (sw_open == 0))
    toggle_pos = int(closed_breakers[n_sw // 7 % 1000])
    state = {'open': 0}

    def toggle(_i=None):
        state['open'] = 1 - state['open']
        fast.set_at('switch', 'open', toggle_pos, state['open'])

    def toggle_and_refresh(_i=None):
        toggle()
        fast.count('bus')

    # 1. open one switch
    def one_switch_loop():
        for k in range(200):
            fast.set_at('switch', 'open', toggle_pos, k % 2)
    res['open_one_switch'] = median_ms(one_switch_loop, runs) / 200
    fast.set_at('switch', 'open', toggle_pos, 0)

    # 2. update all switches
    res['update_all_switches'] = median_ms(lambda: fast.set('switch', 'open', sw_open), runs)

    # 3. bus of every load after a switch change (as a bus number; the ids are one more call, see below)
    load_bus = np.empty(fast.count('load'), dtype=np.int32)
    res['loads_bus_id'] = median_ms(lambda: fast.get('load', 'bus', out=load_bus), runs, setup=toggle_and_refresh)
    res['bus_ids_list'] = median_ms(lambda: fast.ids('bus'), runs, setup=toggle_and_refresh)

    # 4. buses of both ends of every line
    bus1 = np.empty(fast.count('line'), dtype=np.int32)
    bus2 = np.empty(fast.count('line'), dtype=np.int32)

    def lines_buses():
        fast.get('line', 'bus1', out=bus1)
        fast.get('line', 'bus2', out=bus2)
    res['lines_bus_ids'] = median_ms(lines_buses, runs, setup=toggle_and_refresh)

    # 5. loads p0/q0, read and write
    p0 = np.empty(fast.count('load'))
    q0 = np.empty(fast.count('load'))

    def loads_p0_q0():
        fast.get('load', 'p0', out=p0)
        fast.get('load', 'q0', out=q0)
    res['loads_p0_q0'] = median_ms(loads_p0_q0, runs)

    def update_loads():
        fast.set('load', 'p0', p0)
        fast.set('load', 'q0', q0)
    res['update_loads_p0_q0'] = median_ms(update_loads, runs)

    # 6. node/breaker topology of the whole network: switch nodes and states, internal connections
    def all_topologies():
        fast.get('switch', 'voltage_level')
        fast.get('switch', 'node1')
        fast.get('switch', 'node2')
        fast.get('switch', 'open')
        fast.internal_connections()
    res['node_breaker_topology_all'] = median_ms(all_topologies, runs)
    res['node_breaker_topology_per_vl'] = res['node_breaker_topology_all'] / fast.count('voltage_level')

    # 7. all bus columns right after one switch change
    def all_bus_columns():
        fast.ids('bus')
        for column in fast.columns('bus'):
            fast.get('bus', column)
    res['buses_after_switch_change'] = median_ms(all_bus_columns, runs, setup=toggle)

    # 8. compound: switch states in, bus of every terminal and component of every bus out
    some = np.arange(0, n_sw, 97, dtype=np.int32)
    some_open = sw_open[some].copy()

    def compound_setup(i):
        some_open[i % len(some_open)] = 1 - some_open[i % len(some_open)]
    res['compound_switches_then_topology'] = median_ms(
        lambda: fast.apply_switches_and_get_topology(some, some_open), runs, setup=compound_setup)
    fast.set('switch', 'open', sw_open)

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
