# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import networkx as nx
from pandas import DataFrame
import pypowsybl._pypowsybl as _pp
from pypowsybl.utils import create_data_frame_from_series_array


class BusBreakerTopology:
    """
    Bus-breaker representation of the topology of a voltage level.

    The topology is actually represented as a graph, where
    vertices are buses while edges are switches (breakers and disconnectors).

    For each element of the voltage level, we also provide the bus breaker bus where it is connected.
    """

    def __init__(self, network_handle: _pp.JavaHandle, voltage_level_id: str):
        self._elements = create_data_frame_from_series_array(
            _pp.get_bus_breaker_view_elements(network_handle, voltage_level_id))
        self._switchs = create_data_frame_from_series_array(
            _pp.get_bus_breaker_view_switches(network_handle, voltage_level_id))
        self._buses = create_data_frame_from_series_array(
            _pp.get_bus_breaker_view_buses(network_handle, voltage_level_id))

    @property
    def switches(self) -> DataFrame:
        """
        The list of switches of the bus breaker view, together with their connection status, as a dataframe.

        The dataframe includes the following columns:

        - **kind**: Switch kind (BREAKER, DISCONNECTOR, ...)
        - **open**: True if the switch is opened
        - **bus1_id**: node where the switch is connected at side 1
        - **bus2_id**: node where the switch is connected at side 2

        This dataframe is indexed by the id of the switches.
        """
        return self._switchs

    @property
    def buses(self) -> DataFrame:
        """
        The list of buses of the bus breaker view, as a dataframe.

        The dataframe includes the following columns:

        - **name**: Name of the bus breaker view bus
        - **bus_id**: id of the corresponding bus in the bus view.

        This dataframe is indexed by the id of the bus breaker view bus.
        """
        return self._buses

    @property
    def elements(self) -> DataFrame:
        """
        The list of elements (lines, generators...) of this voltage level, together with the bus
        of the bus breaker view where they are connected.

        The dataframe includes the following columns:

        - **type**: Type of the connected element (GENERATOR, LINE, ...)
        - **bus_id**: bus id of the bus breaker view
        - **side**: Side of the connected element

        This dataframe is indexed by the id of the connected elements.
        """
        return self._elements

    def create_graph(self) -> nx.Graph:
        """
        Representation of the topology as a networkx graph.
        """
        graph = nx.Graph()
        graph.add_nodes_from(self._buses.index.tolist())
        graph.add_edges_from(self._switchs[['bus1_id', 'bus2_id']].values.tolist())
        return graph
