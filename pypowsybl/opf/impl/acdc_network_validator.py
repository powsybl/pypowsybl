# Copyright (c) 2026, SuperGrid Institute (http://www.supergrid-institute.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from pandas import DataFrame

from pypowsybl.network import Network


def validate_acdc_network(network: Network) -> None:
    """
    Validate the ACDC network for ACDC OPF solver.

    Performs pre-solver validation checks on the ACDC network to ensure consistency
    and proper configuration before optimization.

    Args:
        network: The network to validate.

    Raises:
        ValueError: If any of the following conditions are violated:
            - A DC component uses multiple nominal voltages.
            - A DC component has no connected DcGround.
    """
    dc_nodes = network.get_dc_nodes()
    if dc_nodes.empty:
        return

    dc_buses = network.get_dc_buses()
    dc_lines = network.get_dc_lines()
    dc_grounds = network.get_dc_grounds()
    dc_nodes_with_component = get_dc_nodes_with_component(dc_nodes, dc_buses)

    check_no_dangling_dc_lines(dc_lines)

    check_dc_nodes_have_same_nominal_voltage_per_dc_component(dc_nodes_with_component)

    check_dc_components_have_a_connected_ground(dc_grounds, dc_nodes_with_component)


def get_dc_nodes_with_component(dc_nodes: DataFrame, dc_buses: DataFrame) -> DataFrame:
    # DC nodes reference their DC bus. The DC component is carried by the DC bus,
    # so map each DC node to its component before running component-level checks.

    return dc_nodes.merge(
        dc_buses[["dc_component"]],
        left_on="dc_bus_id",
        right_index=True,
        how="left",
        validate="m:1",
    )


def check_dc_nodes_have_same_nominal_voltage_per_dc_component(dc_nodes_with_component: DataFrame) -> None:
    if dc_nodes_with_component["nominal_v"].isna().any():
        nodes_without_nominal_v = sorted(
            dc_nodes_with_component[dc_nodes_with_component["nominal_v"].isna()].index
        )

        raise ValueError(
            "Invalid detailed-DC network for ACDC OPF: "
            f"some DC nodes have no nominal voltage: {nodes_without_nominal_v}"
        )

    for dc_component, component_nodes in dc_nodes_with_component.groupby("dc_component"):
        nominal_voltages = sorted(component_nodes["nominal_v"].unique())

        if len(nominal_voltages) != 1:
            component_node_ids = sorted(component_nodes.index)

            raise ValueError(
                "Invalid detailed-DC network for ACDC OPF: "
                f"DC component {dc_component} has several nominal voltages: {nominal_voltages}. "
                f"DC nodes: {component_node_ids}"
            )

def check_dc_components_have_a_connected_ground(
    dc_grounds: DataFrame,
    dc_nodes_with_component: DataFrame,
) -> None:
    node_to_component = (
        dc_nodes_with_component["dc_component"]
        .dropna()
        .to_dict()
    )

    grounded_components = {
        node_to_component[ground.dc_node_id]
        for ground in dc_grounds.itertuples()
        if ground.connected and ground.dc_node_id in node_to_component
    }

    all_dc_components = set(node_to_component.values())
    ungrounded_components = all_dc_components - grounded_components

    if ungrounded_components:
        raise ValueError(
            "Invalid detailed-DC network for ACDC OPF: "
            "DC components have no connected ground: "
            f"{sorted(ungrounded_components)}"
        )

def check_no_dangling_dc_lines(dc_lines: DataFrame) -> None:
    invalid_lines: list[str] = []

    for row in dc_lines.itertuples():
        if not (row.dc_node1_id and row.dc_node2_id):
            invalid_lines.append(str(row.Index))

    if invalid_lines:
        raise ValueError(
            "Invalid detailed-DC network for ACDC OPF: "
            f"DC lines must be connected on both sides. Invalid lines: {sorted(invalid_lines)}"
        )
