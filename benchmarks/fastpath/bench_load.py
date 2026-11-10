"""Load time of a .biidm file with parts skipped (import parameter pypowsybl.import.skip of the fork).

    python bench_load.py NETWORK.biidm [--runs 9] [--out load.json]

Median of --runs loads after 2 warm-up loads, in milliseconds.
"""
import argparse
import json
import statistics
import time

import pypowsybl as pp
import pypowsybl.fast

VARIANTS = {
    'load_stock': None,
    'load_skip_temporary_limits': ['temporary-limits'],
    'load_skip_limits': ['limits'],
    'load_skip_bus_state': ['bus-state'],
    'load_skip_properties': ['properties'],
    'load_skip_limits_bus_state': ['limits', 'bus-state'],
    'load_skip_all': list(pp.fast.SKIPPABLE),
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('network')
    parser.add_argument('--runs', type=int, default=9)
    parser.add_argument('--out')
    args = parser.parse_args()
    res = {}
    for name, skip in VARIANTS.items():
        samples = []
        for _ in range(args.runs + 2):
            t0 = time.perf_counter()
            if skip is None:
                pp.network.load(args.network)
            else:
                pp.fast.load(args.network, skip=skip)
            samples.append((time.perf_counter() - t0) * 1e3)
        res[name] = statistics.median(samples[2:])
    res['runs'] = args.runs
    text = json.dumps(res, indent=1)
    print(text)
    if args.out:
        with open(args.out, 'w') as f:
            f.write(text)


if __name__ == '__main__':
    main()
