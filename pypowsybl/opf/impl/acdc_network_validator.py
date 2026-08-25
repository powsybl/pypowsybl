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
            - A DC component has no voltage-controlled (V_DC mode) VSC converter.
    """
    dc_nodes = network.get_dc_nodes()
    if dc_nodes.empty:
        return

    dc_buses = network.get_dc_buses()
    dc_lines = network.get_dc_lines()
    voltage_source_converters = network.get_voltage_source_converters()
    dc_nodes_with_component = get_dc_nodes_with_component(dc_nodes, dc_buses)

    check_no_dangling_dc_lines(dc_lines)
    
    check_dc_nodes_have_same_nominal_voltage_per_dc_component(dc_nodes_with_component)
    
    check_dc_components_have_vdc_converter(voltage_source_converters, dc_nodes_with_component)


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

def check_dc_components_have_vdc_converter(
    voltage_source_converters: DataFrame,
    dc_nodes_with_component: DataFrame,
) -> None:
    node_to_component = (
        dc_nodes_with_component["dc_component"]
        .dropna()
        .to_dict()
    )

    connected_converter_nodes = set()

    for _, converter in voltage_source_converters.iterrows():
        if converter.dc_connected1:
            connected_converter_nodes.add(converter.dc_node1_id)

        if converter.dc_connected2:
            connected_converter_nodes.add(converter.dc_node2_id)

    # Validate components participating in the converter network.
    # Do not require isolated topology fragments to contain a VDC converter.
    relevant_dc_components = {
        node_to_component[node_id]
        for node_id in connected_converter_nodes
        if node_id in node_to_component
    }

    vdc_controlled_components = set()

    vdc_converters = voltage_source_converters[
        voltage_source_converters["control_mode"] == "V_DC"
    ]

    for _, converter in vdc_converters.iterrows():
        if converter.dc_connected1:
            component = node_to_component.get(converter.dc_node1_id)
            if component is not None:
                vdc_controlled_components.add(component)

        if converter.dc_connected2:
            component = node_to_component.get(converter.dc_node2_id)
            if component is not None:
                vdc_controlled_components.add(component)

    components_without_vdc = (
        relevant_dc_components - vdc_controlled_components
    )

    if components_without_vdc:
        raise ValueError(
            "Invalid detailed-DC network for ACDC OPF: "
            "converter-connected DC components have no VSC in V_DC mode: "
            f"{sorted(components_without_vdc)}"
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
