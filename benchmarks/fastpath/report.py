"""Merges the JSON outputs of bench_stock.py, bench_fast.py and JavaBench into one markdown table.

python report.py --stock stock.json [--rte rte.json] --fast fast.json --java java.json
"""

import argparse
import json

ROWS = [
    ("open_one_switch", "Open one switch"),
    ("update_all_switches", "Update all switches"),
    ("loads_bus_id", "Bus of every load, after a switch change"),
    ("lines_bus_ids", "Buses of both ends of every line"),
    ("loads_p0_q0", "Read loads p0/q0"),
    ("update_loads_p0_q0", "Write loads p0/q0"),
    ("node_breaker_topology_all", "Node/breaker topology, all voltage levels"),
    ("buses_after_switch_change", "All bus columns after one switch change"),
    ("compound_switches_then_topology", "Compound: switch states in, topology out"),
]


def fmt(ms):
    if ms is None:
        return "-"
    if ms < 0.01:
        return f"{ms * 1e3:.2f} us"
    if ms < 10:
        return f"{ms:.2f} ms"
    return f"{ms:.0f} ms"


def main():
    parser = argparse.ArgumentParser()
    for name in ("stock", "rte", "fast", "java"):
        parser.add_argument(f"--{name}")
    args = parser.parse_args()
    data = {}
    for name in ("java", "rte", "stock", "fast"):
        path = getattr(args, name)
        if path:
            with open(path) as f:
                data[name] = json.load(f)
    titles = {
        "java": "Pure Java (warm JVM)",
        "rte": "pypowsybl-rte 1.16.1",
        "stock": "Stock API, unmodified build",
        "fast": "Fork fast path",
    }
    columns = [c for c in titles if c in data]
    header = ["Operation"] + [titles[c] for c in columns]
    if "stock" in data and "fast" in data:
        header.append("Speed-up vs stock")
    print("| " + " | ".join(header) + " |")
    print("|" + "---|" * len(header))
    for key, label in ROWS:
        cells = [label] + [fmt(data[c].get(key)) for c in columns]
        if "stock" in data and "fast" in data:
            cells.append(f"{data['stock'][key] / data['fast'][key]:.0f}x" if key in data["fast"] else "-")
        print("| " + " | ".join(cells) + " |")


if __name__ == "__main__":
    main()
