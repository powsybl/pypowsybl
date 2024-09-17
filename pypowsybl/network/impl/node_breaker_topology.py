# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from os import PathLike
from pathlib import Path
from typing import Union

import networkx as nx
from pandas import DataFrame
import networkx as _nx
import pypowsybl._pypowsybl as _pp
from pypowsybl.utils import create_data_frame_from_series_array


class NodeBreakerTopology:
    """
    Node-breaker representation of the topology of a voltage level.

    The topology is actually represented as a graph, where
    vertices are called "nodes" and are identified by a unique number in the voltage level,
    while edges are switches (breakers and disconnectors), or internal connections (plain "wires").
    """

    def __init__(self, network_handle: _pp.JavaHandle, voltage_level_id: str):
        self._internal_connections = create_data_frame_from_series_array(
            _pp.get_node_breaker_view_internal_connections(network_handle, voltage_level_id))
        self._switches = create_data_frame_from_series_array(
            _pp.get_node_breaker_view_switches(network_handle, voltage_level_id))
        self._nodes = create_data_frame_from_series_array(
            _pp.get_node_breaker_view_nodes(network_handle, voltage_level_id))

    @property
    def switches(self) -> DataFrame:
        """
        The list of switches of the voltage level, together with their connection status, as a dataframe.
        """
        return self._switches

    @property
    def nodes(self) -> DataFrame:
        """
        The list of nodes of the voltage level, together with their corresponding network element (if any),
        as a dataframe.
        """
        return self._nodes

    @property
    def internal_connections(self) -> DataFrame:
        """
        The list of internal connection of the voltage level, together with the nodes they connect.
        """
        return self._internal_connections

    def create_graph(self) -> _nx.Graph:
        """
        Representation of the topology as a networkx graph.
        """
        graph = _nx.Graph()
        graph.add_nodes_from(self._nodes.index.tolist())
        graph.add_edges_from(self._switches[['node1', 'node2']].values.tolist())
        graph.add_edges_from(self._internal_connections[['node1', 'node2']].values.tolist())
        return graph

    def write_ampl(self, dir: Union[str, Path]):
        """
        Write the node/breaker topology in a columned file format (with space as a separator) so that it is
        easily readable by AMPL for running topology optimization.

        3 files are being generated:
         - topo_connections.txt: the list of node connections in the graph.
         - topo_elements.txt: the elements ID and type associated to each of the nodes
         - topo_valid.txt : the buses to which each of the nodes belong

        Args:
            dir: directory path to generate the 3 files
        """
        dir_path = Path(dir)
        with open(dir_path / 'topo_connections.txt', 'w') as file:
            file.write("# switch_id node1 node2 open\n")
            for i, row in enumerate(self.switches.itertuples(index=False)):
                file.write(f"{i} {row[4]} {row[5]} {1 if row[2] else 0}\n")

        with open(dir_path / 'topo_elements.txt', 'w') as file:
            file.write("# node element_id element_type\n")
            for node, row in self.nodes.iterrows():
                file.write(f"{node} '{row['connectable_id']}' {row['connectable_type']}\n")

        graph = nx.Graph()
        graph.add_nodes_from(self.nodes.index.tolist())
        topo_switches = self.switches
        closed_topo_switches = topo_switches[~topo_switches['open']]
        graph.add_edges_from(closed_topo_switches[['node1', 'node2']].values.tolist())
        graph.add_edges_from(self.internal_connections[['node1', 'node2']].values.tolist())
        components = list(nx.connected_components(graph))

        i_topo = 0 # TODO will be used later on to generate multiple topologies
        with open(dir_path / 'topo_valid.txt', 'w') as file:
            file.write("# topo_id bus_id node\n")
            for i_component, component in enumerate(components):
                for node in component:
                    file.write(f"{i_topo} {i_component} {node}\n")
