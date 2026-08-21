#
# Copyright (c) 2026, SuperGrid Institute (http://www.supergrid-institute.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#

import math
from typing import cast

from pypowsybl.opf.impl.model.network_cache import NetworkCache

VoltageDifference = tuple[str, str, float]


def dc_voltage_differences(network_cache: NetworkCache) -> list[VoltageDifference]:
    """Each entry (a, b, value) means v(a) - v(b) = value.

    Lines and closed switches state zero, a converter its signed target_v_dc.
    """
    differences = []

    for row in network_cache.dc_lines.itertuples(index=False):
        differences.append((str(row.dc_node1_id), str(row.dc_node2_id), 0.0))

    for row in network_cache.dc_switches.itertuples(index=False):
        if not row.open:
            differences.append((str(row.dc_node1_id), str(row.dc_node2_id), 0.0))

    for row in network_cache.voltage_source_converters.itertuples(index=False):
        target_v_dc = cast(float, row.target_v_dc)
        if row.dc_connected1 and row.dc_connected2 and not math.isnan(target_v_dc):
            differences.append((str(row.dc_node1_id), str(row.dc_node2_id), target_v_dc))

    return differences


def propagate_dc_voltages(differences: list[VoltageDifference],
                          known: dict[str, float]) -> dict[str, float]:
    """Extend the known voltages to every node the differences reach."""
    voltages = dict(known)
    changed = True
    while changed:
        changed = False
        for node1_id, node2_id, value in differences:
            if node1_id in voltages and node2_id not in voltages:
                voltages[node2_id] = voltages[node1_id] - value
                changed = True
            elif node2_id in voltages and node1_id not in voltages:
                voltages[node1_id] = voltages[node2_id] + value
                changed = True
    return voltages


def compute_dc_node_voltage_starts(network_cache: NetworkCache) -> dict[str, float]:
    """Start voltage per DC node, in per unit. A node nothing reaches is left out for the caller."""
    differences = dc_voltage_differences(network_cache)
    starts: dict[str, float] = {}

    for node1_id, node2_id, value in differences:
        # A zero difference cannot anchor: it would place both terminals at the same voltage.
        if value == 0.0 or node1_id in starts or node2_id in starts:
            continue
        starts[node1_id] = value / 2.0
        starts[node2_id] = -value / 2.0
        starts = propagate_dc_voltages(differences, starts)

    # Grounds last. Applied first, their 0 spreads and lands both terminals of a converter together.
    for row in network_cache.dc_grounds.itertuples(index=False):
        if str(row.dc_node_id) not in starts:
            starts[str(row.dc_node_id)] = 0.0
            starts = propagate_dc_voltages(differences, starts)

    return starts
