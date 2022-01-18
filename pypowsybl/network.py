#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
from __future__ import annotations  # Necessary for type alias like _DataFrame to work with sphinx

import _pypowsybl
import sys as _sys
from typing import List as _List
from typing import Set as _Set
from _pypowsybl import ElementType

from pandas import DataFrame as _DataFrame
import networkx as _nx
import datetime as _datetime
import pandas as _pd
import numpy as _np

from pypowsybl.util import create_data_frame_from_series_array as _create_data_frame_from_series_array


_pypowsybl.SeriesMetadata.__repr__ = lambda s: f'SeriesMetadata(name={s.name}, type={s.type}, ' \
                                               f'is_index={s.is_index}, is_modifiable={s.is_modifiable})'


class Svg:
    """
    This class represents a single line diagram."""

    def __init__(self, content: str):
        self._content = content

    @property
    def svg(self):
        return self._content

    def __str__(self):
        return self._content

    def _repr_svg_(self):
        return self._content


class NodeBreakerTopology:
    """
    Node-breaker representation of the topology of a voltage level.

    The topology is actually represented as a graph, where
    vertices are called "nodes" and are identified by a unique number in the voltage level,
    while edges are switches (breakers and disconnectors), or internal connections (plain "wires").
    """

    def __init__(self, network_handle, voltage_level_id):
        self._internal_connections = _create_data_frame_from_series_array(
            _pypowsybl.get_node_breaker_view_internal_connections(network_handle, voltage_level_id))
        self._switchs = _create_data_frame_from_series_array(
            _pypowsybl.get_node_breaker_view_switches(network_handle, voltage_level_id))
        self._nodes = _create_data_frame_from_series_array(
            _pypowsybl.get_node_breaker_view_nodes(network_handle, voltage_level_id))

    @property
    def switches(self) -> _DataFrame:
        """
        The list of switches of the voltage level, together with their connection status, as a dataframe.
        """
        return self._switchs

    @property
    def nodes(self) -> _DataFrame:
        """
        The list of nodes of the voltage level, together with their corresponding network element (if any),
        as a dataframe.
        """
        return self._nodes

    @property
    def internal_connections(self) -> _DataFrame:
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
        graph.add_edges_from(self._switchs[['node1', 'node2']].values.tolist())
        graph.add_edges_from(self._internal_connections[['node1', 'node2']].values.tolist())
        return graph


class BusBreakerTopology:
    """
    Bus-breaker representation of the topology of a voltage level.

    The topology is actually represented as a graph, where
    vertices are buses while edges are switches (breakers and disconnectors).

    For each element of the voltage level, we also provide the bus breaker bus where it is connected.
    """

    def __init__(self, network_handle, voltage_level_id):
        self._elements = _create_data_frame_from_series_array(
            _pypowsybl.get_bus_breaker_view_elements(network_handle, voltage_level_id))
        self._switchs = _create_data_frame_from_series_array(
            _pypowsybl.get_bus_breaker_view_switches(network_handle, voltage_level_id))
        self._buses = _create_data_frame_from_series_array(
            _pypowsybl.get_bus_breaker_view_buses(network_handle, voltage_level_id))

    @property
    def switches(self) -> _DataFrame:
        """
        The list of switches of the bus breaker view, together with their connection status, as a dataframe.
        """
        return self._switchs

    @property
    def buses(self) -> _DataFrame:
        """
        The list of buses of the  bus breaker view, as a dataframe.
        """
        return self._buses

    @property
    def elements(self) -> _DataFrame:
        """
        The list of elements (lines, generators...) of this voltage level, together with the bus
        of the bus breaker view where they are connected.
        """
        return self._elements

    def create_graph(self) -> _nx.Graph:
        """
        Representation of the topology as a networkx graph.
        """
        graph = _nx.Graph()
        graph.add_nodes_from(self._buses.index.tolist())
        graph.add_edges_from(self._switchs[['bus1_id', 'bus2_id']].values.tolist())
        return graph


class Network(object):

    def __init__(self, handle):
        self._handle = handle
        att = _pypowsybl.get_network_metadata(self._handle)
        self._id = att.id
        self._name = att.name
        self._source_format = att.source_format
        self._forecast_distance = _datetime.timedelta(minutes=att.forecast_distance)
        self._case_date = _datetime.datetime.utcfromtimestamp(att.case_date)

    @property
    def id(self) -> str:
        """
        ID of this network
        """
        return self._id

    @property
    def name(self) -> str:
        """
        Name of this network
        """
        return self._name

    @property
    def source_format(self) -> str:
        """
        Format of the source where this network came from.
        """
        return self._source_format

    @property
    def case_date(self) -> _datetime.datetime:
        """
        Date of this network case, in UTC timezone.
        """
        return self._case_date

    @property
    def forecast_distance(self) -> _datetime.timedelta:
        """
        The forecast distance: 0 for a snapshot.
        """
        return self._forecast_distance

    def __str__(self) -> str:
        return f'Network(id={self.id}, name={self.name}, case_date={self.case_date}, ' \
               f'forecast_distance={self.forecast_distance}, source_format={self.source_format})'

    def __repr__(self) -> str:
        return str(self)

    def __getstate__(self):
        return {'xml': self.dump_to_string()}

    def __setstate__(self, state):
        xml = state['xml']
        n = _pypowsybl.load_network_from_string('tmp.xiidm', xml, {})
        self._handle = n

    def open_switch(self, id: str):
        return _pypowsybl.update_switch_position(self._handle, id, True)

    def close_switch(self, id: str):
        return _pypowsybl.update_switch_position(self._handle, id, False)

    def connect(self, id: str):
        return _pypowsybl.update_connectable_status(self._handle, id, True)

    def disconnect(self, id: str):
        return _pypowsybl.update_connectable_status(self._handle, id, False)

    def dump(self, file: str, format: str = 'XIIDM', parameters: dict = {}):
        """
        Save a network to a file using a specified format.

        Args:
            file (str): a file
            format (str, optional): format to save the network, defaults to 'XIIDM'
            parameters (dict, optional): a map of parameters
        """
        _pypowsybl.dump_network(self._handle, file, format, parameters)

    def dump_to_string(self, format: str = 'XIIDM', parameters: dict = {}) -> str:
        """
        Save a network to a string using a specified format.

        Args:
            format (str, optional): format to export, only support mono file type, defaults to 'XIIDM'
            parameters (dict, optional): a map of parameters

        Returns:
            a string representing network
        """
        return _pypowsybl.dump_network_to_string(self._handle, format, parameters)

    def reduce(self, v_min: float = 0, v_max: float = _sys.float_info.max, ids: _List[str] = [],
               vl_depths: tuple = (), with_dangling_lines: bool = False):
        vls = []
        depths = []
        for v in vl_depths:
            vls.append(v[0])
            depths.append(v[1])
        _pypowsybl.reduce_network(self._handle, v_min, v_max, ids, vls, depths, with_dangling_lines)

    def write_single_line_diagram_svg(self, container_id: str, svg_file: str):
        """
        Create a single line diagram in SVG format from a voltage level or a substation and write to a file.

        Args:
            container_id: a voltage level id or a substation id
            svg_file: a svg file path
        """
        _pypowsybl.write_single_line_diagram_svg(self._handle, container_id, svg_file)

    def get_single_line_diagram(self, container_id: str):
        """
        Create a single line diagram from a voltage level or a substation.

        Args:
            container_id: a voltage level id or a substation id

        Returns:
            the single line diagram
        """
        return Svg(_pypowsybl.get_single_line_diagram_svg(self._handle, container_id))

    def write_network_area_diagram_svg(self, svg_file: str, voltage_level_id: str = None, depth: int = 0):
        """
        Create a network area diagram in SVG format and write it to a file.

        Args:
            svg_file: a svg file path
            voltage_level_id: the voltage level ID, center of the diagram (None for the full diagram)
            depth: the diagram depth around the voltage level
        """
        _pypowsybl.write_network_area_diagram_svg(self._handle, svg_file, voltage_level_id if voltage_level_id else '', depth)

    def get_network_area_diagram(self, voltage_level_id: str = None, depth: int = 0):
        """
        Create a network area diagram.

        Args:
            voltage_level_id: the voltage level ID, center of the diagram (None for the full diagram)
            depth: the diagram depth around the voltage level

        Returns:
            the network area diagram
        """
        return Svg(_pypowsybl.get_network_area_diagram_svg(self._handle, voltage_level_id if voltage_level_id else '', depth))

    def get_elements_ids(self, element_type: _pypowsybl.ElementType, nominal_voltages: _Set[float] = None,
                         countries: _Set[str] = None,
                         main_connected_component: bool = True, main_synchronous_component: bool = True,
                         not_connected_to_same_bus_at_both_sides: bool = False) -> _List[str]:
        return _pypowsybl.get_network_elements_ids(self._handle, element_type,
                                                   [] if nominal_voltages is None else list(nominal_voltages),
                                                   [] if countries is None else list(countries),
                                                   main_connected_component, main_synchronous_component,
                                                   not_connected_to_same_bus_at_both_sides)

    def get_elements(self, element_type: _pypowsybl.ElementType) -> _DataFrame:
        """
        Get network elements as a :class:`~pandas.DataFrame` for a specified element type.

        Args:
            element_type (ElementType): the element type

        Returns:
            a network elements data frame for the specified element type
        """
        series_array = _pypowsybl.create_network_elements_series_array(self._handle, element_type)
        return _create_data_frame_from_series_array(series_array)

    def get_buses(self) -> _DataFrame:
        """
        Get a dataframe of buses.

        Notes:
            The resulting dataframe will have the following columns:

              - **v_mag**: Get the voltage magnitude of the bus (in kV)
              - **v_angle**: the voltage angle of the bus (in degree)
              - **connected_component**: the number of terminals connected to this bus
              - **synchronous_component**: the number of synchronous components that the bus is part of
              - **voltage_level_id**: at which substation the bus is connected

            This dataframe is indexed by the id of the LCC converter

        Examples:

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_buses()

            It outputs something like:

            ======= ======== ======= =================== ===================== ================
            \          v_mag v_angle connected_component synchronous_component voltage_level_id
            ======= ======== ======= =================== ===================== ================
            id
            S1VL1_0 224.6139  2.2822                   0                     1            S1VL1
            S1VL2_0 400.0000  0.0000                   0                     1            S1VL2
            S2VL1_0 408.8470  0.7347                   0                     0            S2VL1
            S3VL1_0 400.0000  0.0000                   0                     0            S3VL1
            S4VL1_0 400.0000 -1.1259                   0                     0            S4VL1
            ======= ======== ======= =================== ===================== ================

        Returns:
            A dataframe of buses.
        """
        return self.get_elements(_pypowsybl.ElementType.BUS)

    def get_generators(self) -> _DataFrame:
        """
        Get a dataframe of generators.

        Returns:
            the generator data frame.

        Notes:
            The resulting dataframe will have the following columns:

              - **energy_source**: the energy source used to fuel the generator
              - **target_p**: the target active value for the generator (in MW)
              - **max_p**: the maximum active value for the generator  (MW)
              - **min_p**: the minimum active value for the generator  (MW)
              - **target_v**: the target voltage magnitude value for the generator (in kV)
              - **target_q**: the target reactive value for the generator (in MVAr)
              - **voltage_regulator_on**:
              - **p**: the actual active production of the generator (``NaN`` if no loadflow has been computed)
              - **q**: the actual reactive production of the generator (``NaN`` if no loadflow has been computed)
              - **voltage_level_id**: at which substation this generator is connected
              - **bus_id**: at which bus this generator is computed

            This dataframe is indexed by the id of the generators

        Examples:

            .. code-block:: python

                net = pp.network.create_ieee14()
                net.get_generators()

            will output something like:

            ==== ============= ======== ====== ======= ======== ======== ==================== === === ================ ======
            \    energy_source target_p  max_p   min_p target_v target_q voltage_regulator_on   p   q voltage_level_id bus_id
            ==== ============= ======== ====== ======= ======== ======== ==================== === === ================ ======
            id
            B1-G         OTHER    232.4 9999.0 -9999.0    1.060    -16.9                 True NaN NaN              VL1  VL1_0
            B2-G         OTHER     40.0 9999.0 -9999.0    1.045     42.4                 True NaN NaN              VL2  VL2_0
            B3-G         OTHER      0.0 9999.0 -9999.0    1.010     23.4                 True NaN NaN              VL3  VL3_0
            B6-G         OTHER      0.0 9999.0 -9999.0    1.070     12.2                 True NaN NaN              VL6  VL6_0
            B8-G         OTHER      0.0 9999.0 -9999.0    1.090     17.4                 True NaN NaN              VL8  VL8_0
            ==== ============= ======== ====== ======= ======== ======== ==================== === === ================ ======


        .. warning::

            The "generator convention" is used for the "input" columns (`target_p`, `max_p`,
            `min_p`, `target_v` and `target_q`) while the "load convention" is used for the ouput columns
            (`p` and `q`).

            Most of the time, this means that `p` and `target_p` will have opposite sign. This also entails that
            `p` can be lower than `min_p`. Actually, the relation: :math:`\\text{min_p} <= -p <= \\text{max_p}`
            should hold.
        """
        return self.get_elements(_pypowsybl.ElementType.GENERATOR)

    def get_loads(self) -> _DataFrame:
        """
        Get a dataframe of loads.

        Returns:
            the load data frame

        Notes:
            The resulting dataframe will have the following columns:

              - **type**: type of load
              - **p0**: the active load consumption setpoint (MW)
              - **q0**: the reactive load consumption setpoint  (MVAr)
              - **p**: the result active load consumption, it is ``NaN`` is not loadflow has been computed (MW)
              - **q**: the result reactive load consumption, it is ``NaN`` is not loadflow has been computed (MVAr)
              - **i**: the current on the load, ``NaN`` if no loadflow has been computed (in A)
              - **voltage_level_id**: at which substation this load is connected
              - **bus_id**: at which bus this load is connected

            This dataframe is indexed by the id of the loads.

        Examples:

            .. code-block:: python

                net = pp.network.create_ieee14()
                net.get_loads()

            will output something like:

            ===== ========== ===== ===== === === ================ ======= =========
            \           type    p0    q0   p   q voltage_level_id  bus_id connected
            ===== ========== ===== ===== === === ================ ======= =========
            id
            B2-L   UNDEFINED  21.7  12.7 NaN NaN              VL2   VL2_0      True
            B3-L   UNDEFINED  94.2  19.0 NaN NaN              VL3   VL3_0      True
            B4-L   UNDEFINED  47.8  -3.9 NaN NaN              VL4   VL4_0      True
            B5-L   UNDEFINED   7.6   1.6 NaN NaN              VL5   VL5_0      True
            B6-L   UNDEFINED  11.2   7.5 NaN NaN              VL6   VL6_0      True
            B9-L   UNDEFINED  29.5  16.6 NaN NaN              VL9   VL9_0      True
            B10-L  UNDEFINED   9.0   5.8 NaN NaN             VL10  VL10_0      True
            B11-L  UNDEFINED   3.5   1.8 NaN NaN             VL11  VL11_0      True
            B12-L  UNDEFINED   6.1   1.6 NaN NaN             VL12  VL12_0      True
            B13-L  UNDEFINED  13.5   5.8 NaN NaN             VL13  VL13_0      True
            B14-L  UNDEFINED  14.9   5.0 NaN NaN             VL14  VL14_0      True
            ===== ========== ===== ===== === === ================ ======= =========

        """
        return self.get_elements(_pypowsybl.ElementType.LOAD)

    def get_batteries(self) -> _DataFrame:
        """
        Get a dataframe of batteries.

        Returns:
            A dataframe of batteries.
        """
        return self.get_elements(_pypowsybl.ElementType.BATTERY)

    def get_lines(self) -> _DataFrame:
        """
        Get a dataframe of lines data.

        Returns:
            A dataframe of lines data.

        Notes:
            The resulting dataframe will have the following columns:

            - **r**: the resistance of the line (in Ohm)
            - **x**: the reactance of the line (in Ohm)
            - **g1**: the  conductance of line at its "1" side (in Siemens)
            - **b1**: the susceptance of line at its "1" side (in Siemens)
            - **g2**: the  conductance of line at its "2" side (in Siemens)
            - **b2**: the susceptance of line at its "2" side (in Siemens)
            - **p1**: the active flow on the line at its "1" side, ``NaN`` if no loadflow has been computed (in MW)
            - **q1**: the reactive flow on the line at its "1" side, ``NaN`` if no loadflow has been computed (in MVAr)
            - **i1**: the current on the line at its "1" side, ``NaN`` if no loadflow has been computed (in A)
            - **p2**: the active flow on the line at its "2" side, ``NaN`` if no loadflow has been computed (in MW)
            - **q2**: the reactive flow on the line at its "2" side, ``NaN`` if no loadflow has been computed (in MVAr)
            - **i2**: the current on the line at its "2" side, ``NaN`` if no loadflow has been computed (in A)
            - **voltage_level1_id**: at which substation the "1" side of the powerline is connected
            - **voltage_level2_id**: at which substation the "2" side of the powerline is connected
            - **bus1_id**: at which bus the "1" side of the powerline is connected
            - **bus2_id**: at which bus the "2" side of the powerline is connected
            - **connected1**: ``True`` if the side "1" of the line is connected to a bus
            - **connected2**: ``True`` if the side "2" of the line is connected to a bus

            This dataframe is indexed by the id of the lines.

        Examples:

            .. code-block:: python

                net = pp.network.create_ieee14()
                net.get_lines()

            will output something like:

            ========  ========  ========  ===  ====  ===  ==== === === === === === === ================= ================= ======= ======= ========== ==========
            \                r         x   g1    b1   g2    b2  p1  q1  i1  p2  q2  i2 voltage_level1_id voltage_level2_id bus1_id bus2_id connected1 connected2
            ========  ========  ========  ===  ====  ===  ==== === === === === === === ================= ================= ======= ======= ========== ==========
            id
            L1-2-1    0.000194  0.000592  0.0  2.64  0.0  2.64 NaN NaN NaN NaN NaN NaN               VL1               VL2   VL1_0   VL2_0       True       True
            L1-5-1    0.000540  0.002230  0.0  2.46  0.0  2.46 NaN NaN NaN NaN NaN NaN               VL1               VL5   VL1_0   VL5_0       True       True
            ========  ========  ========  ===  ====  ===  ==== === === === === === === ================= ================= ======= ======= ========== ==========
        """
        return self.get_elements(_pypowsybl.ElementType.LINE)

    def get_2_windings_transformers(self) -> _DataFrame:
        """
        Get a dataframe of 2 windings transformers.

        Returns:
            A dataframe of 2 windings transformers.

        Notes:
            The resulting dataframe will have the following columns:

              - **r**: the resistance of the transformer at its "2" side  (in Ohm)
              - **x**: the reactance of the transformer at its "2" side (in Ohm)
              - **b**: the susceptance of transformer at its "2" side (in Siemens)
              - **g**: the  conductance of transformer at its "2" side (in Siemens)
              - **rated_u1**: The rated voltage of the transformer at side 1 (in kV)
              - **rated_u2**: The rated voltage of the transformer at side 2 (in kV)
              - **rated_s**:
              - **p1**: the active flow on the transformer at its "1" side, ``NaN`` if no loadflow has been computed (in MW)
              - **q1**: the reactive flow on the transformer at its "1" side, ``NaN`` if no loadflow has been computed  (in MVAr)
              - **i1**: the current on the transformer at its "1" side, ``NaN`` if no loadflow has been computed (in A)
              - **p2**: the active flow on the transformer at its "2" side, ``NaN`` if no loadflow has been computed  (in MW)
              - **q2**: the reactive flow on the transformer at its "2" side, ``NaN`` if no loadflow has been computed  (in MVAr)
              - **i2**: the current on the transformer at its "2" side, ``NaN`` if no loadflow has been computed (in A)
              - **voltage_level1_id**: at which substation the "1" side of the transformer is connected
              - **voltage_level2_id**: at which substation the "2" side of the transformer is connected
              - **connected1**: ``True`` ifthe side "1" of the transformer is connected to a bus
              - **connected2**: ``True`` ifthe side "2" of the transformer is connected to a bus

            This dataframe is indexed by the id of the two windings transformers

        Examples:

            .. code-block:: python

                net = pp.network.create_ieee14()
                net.get_2_windings_transformers()

            will output something like:

            ====== ==== ======== === === ======== ======== ======= === === === === === === ================= ================= ======= ======= ========== ==========
            \         r        x   g   b rated_u1 rated_u2 rated_s  p1  q1  i1  p2  q2  i2 voltage_level1_id voltage_level2_id bus1_id bus2_id connected1 connected2
            ====== ==== ======== === === ======== ======== ======= === === === === === === ================= ================= ======= ======= ========== ==========
            id
            T4-7-1  0.0 0.409875 0.0 0.0  132.030     14.0     NaN NaN NaN NaN NaN NaN NaN               VL4               VL7   VL4_0   VL7_0       True       True
            T4-9-1  0.0 0.800899 0.0 0.0  130.815     12.0     NaN NaN NaN NaN NaN NaN NaN               VL4               VL9   VL4_0   VL9_0       True       True
            T5-6-1  0.0 0.362909 0.0 0.0  125.820     12.0     NaN NaN NaN NaN NaN NaN NaN               VL5               VL6   VL5_0   VL6_0       True       True
            ====== ==== ======== === === ======== ======== ======= === === === === === === ================= ================= ======= ======= ========== ==========
        """
        return self.get_elements(_pypowsybl.ElementType.TWO_WINDINGS_TRANSFORMER)

    def get_3_windings_transformers(self) -> _DataFrame:
        """
        Get a dataframe of 3 windings transformers.

        Returns:
            A dataframe of 3 windings transformers.
        """
        return self.get_elements(_pypowsybl.ElementType.THREE_WINDINGS_TRANSFORMER)

    def get_shunt_compensators(self) -> _DataFrame:
        """
        Get a dataframe of shunt compensators.

        Returns:
            A dataframe of shunt compensators.

        Notes:
            The resulting dataframe will have the following columns:

              - **model_type**:
              - **max_section_count**: The maximum number of sections that may be switched on
              - **section_count**: The current number of section that may be switched on
              - **p**: the active flow on the shunt, ``NaN`` if no loadflow has been computed (in MW)
              - **q**: the reactive flow on the shunt, ``NaN`` if no loadflow has been computed  (in MVAr)
              - **i**: the current in the shunt, ``NaN`` if no loadflow has been computed  (in A)
              - **voltage_level_id**: at which substation the shunt is connected
              - **bus_id**: indicate at which bus the shunt is connected
              - **connected**: ``True`` ifthe shunt is connected to a bus

            This dataframe is indexed by the id of the shunt compensators

        Examples:

            .. code-block:: python

                net = pp.network.create_ieee14()
                net.get_shunt_compensators()

            will output something like:

            ===== ========== ================= ============= === === === ================ ====== =========
            \     model_type max_section_count section_count   p   q   i voltage_level_id bus_id connected
            ===== ========== ================= ============= === === === ================ ====== =========
            id
            B9-SH     LINEAR                 1             1 NaN NaN NaN              VL9  VL9_0      True
            ===== ========== ================= ============= === === === ================ ====== =========
        """
        return self.get_elements(_pypowsybl.ElementType.SHUNT_COMPENSATOR)

    def get_non_linear_shunt_compensator_sections(self) -> _DataFrame:
        """
        Get a dataframe of shunt compensators sections for non linear model.

        Notes:
            The resulting dataframe will have the following columns:

              - **g**: the accumulated conductance in S if the section and all the previous ones are activated.
              - **b**: the accumulated susceptance in S if the section and all the previous ones are activated

            This dataframe is multi-indexed, by the tuple (id of shunt, section number).

        Returns:
            A dataframe of non linear model shunt compensators sections.
        """
        return self.get_elements(_pypowsybl.ElementType.NON_LINEAR_SHUNT_COMPENSATOR_SECTION)

    def get_linear_shunt_compensator_sections(self) -> _DataFrame:
        """
        Get a dataframe of shunt compensators sections for linear model.

        Notes:
            The resulting dataframe will have the following columns:

              - **g_per_section**: the conductance per section in S
              - **b_per_section**: the susceptance per section in S
              - **max_section_count**: the maximum number of sections

            This dataframe is indexed by the shunt compensator ID.

        Returns:
           A dataframe of linear models of shunt compensators.
        """
        return self.get_elements(_pypowsybl.ElementType.LINEAR_SHUNT_COMPENSATOR_SECTION)

    def get_dangling_lines(self) -> _DataFrame:
        """
        Get a dataframe of dangling lines.

        Returns:
            A dataframe of dangling lines.

        Notes:
            The resulting dataframe will have the following columns:

              - **r**: The resistance of the dangling line (Ohm)
              - **x**: The reactance of the dangling line (Ohm)
              - **g**: the conductance of dangling line (in Siemens)
              - **b**: the susceptance of dangling line (in Siemens)
              - **p0**: The active power setpoint
              - **q0**: The reactive power setpoint
              - **p**: active flow on the dangling line, ``NaN`` if no loadflow has been computed (in MW)
              - **q**: the reactive flow on the dangling line, ``NaN`` if no loadflow has been computed  (in MVAr)
              - **i**: The current on the dangling line, ``NaN`` if no loadflow has been computed (in A)
              - **voltage_level_id**: at which substation the dangling line is connected
              - **bus_id**: at which bus the dangling line is connected
              - **connected**: ``True`` ifthe dangling line is connected to a bus

            This dataframe is indexed by the id of the dangling lines

        Examples:

            .. code-block:: python

                net = pp.network._create_dangling_lines_network()
                net.get_dangling_lines()

            will output something like:

            == ==== === ====== ======= ==== ==== === === === ================ ====== =========
            \     r   x      g       b   p0   q0   p   q   i voltage_level_id bus_id connected
            == ==== === ====== ======= ==== ==== === === === ================ ====== =========
            id
            DL 10.0 1.0 0.0001 0.00001 50.0 30.0 NaN NaN NaN               VL   VL_0      True
            == ==== === ====== ======= ==== ==== === === === ================ ====== =========
        """
        return self.get_elements(_pypowsybl.ElementType.DANGLING_LINE)

    def get_lcc_converter_stations(self) -> _DataFrame:
        """
        Get a dataframe of LCC converter stations.

        Returns:
            A dataframe of LCC converter stations.

        Notes:
            The resulting dataframe will have the following columns:

              - **power_factor**: the power factor
              - **loss_factor**: the loss factor
              - **p**: active flow on the LCC converter station, ``NaN`` if no loadflow has been computed (in MW)
              - **q**: the reactive flow on the LCC converter station, ``NaN`` if no loadflow has been computed  (in MVAr)
              - **i**: The current on the LCC converter station, ``NaN`` if no loadflow has been computed (in A)
              - **voltage_level_id**: at which substation the LCC converter station is connected
              - **bus_id**: at which bus the LCC converter station is connected
              - **connected**: ``True`` ifthe LCC converter station is connected to a bus

            This dataframe is indexed by the id of the LCC converter

        Examples:

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_lcc_converter_stations()

            will output something like:

            ======== ============ ===========  ====== === === ================ ======= =========
                .    power_factor loss_factor       p   q   i voltage_level_id  bus_id connected
            ======== ============ ===========  ====== === === ================ ======= =========
            id
                LCC1          0.6         1.1   80.88 NaN NaN            S1VL2 S1VL2_0      True
                LCC2          0.6         1.1  -79.12 NaN NaN            S3VL1 S3VL1_0      True
            ======== ============ ===========  ====== === === ================ ======= =========
        """
        return self.get_elements(_pypowsybl.ElementType.LCC_CONVERTER_STATION)

    def get_vsc_converter_stations(self) -> _DataFrame:
        """
        Get a dataframe of VSC converter stations.

        Returns:
            A dataframe of VCS converter stations.

        Notes:
            The resulting dataframe will have the following columns:

              - **loss_factor**: correspond to the loss of power due to ac dc conversion
              - **voltage_setpoint**: The voltage setpoint
              - **reactive_power_setpoint**: The reactive power setpoint
              - **voltage_regulator_on**: The voltage regulator status
              - **p**: active flow on the VSC  converter station, ``NaN`` if no loadflow has been computed (in MW)
              - **q**: the reactive flow on the VSC converter station, ``NaN`` if no loadflow has been computed  (in MVAr)
              - **i**: The current on the VSC converter station, ``NaN`` if no loadflow has been computed (in A)
              - **voltage_level_id**: at which substation the VSC converter station is connected
              - **bus_id**: at which bus the VSC converter station is connected
              - **connected**: ``True`` ifthe VSC converter station is connected to a bus

            This dataframe is indexed by the id of the VSC converter

        Examples:

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_vsc_converter_stations()



            will output something like:

            ======== =========== ================ ======================= ==================== ====== ========= ========== ================ ======= =========
            \        loss_factor voltage_setpoint reactive_power_setpoint voltage_regulator_on      p         q          i voltage_level_id  bus_id connected
            ======== =========== ================ ======================= ==================== ====== ========= ========== ================ ======= =========
            id
                VSC1         1.1            400.0                   500.0                 True  10.11 -512.0814 739.269871            S1VL2 S1VL2_0      True
                VSC2         1.1              0.0                   120.0                False  -9.89 -120.0000 170.031658            S2VL1 S2VL1_0      True
            ======== =========== ================ ======================= ==================== ====== ========= ========== ================ ======= =========
        """
        return self.get_elements(_pypowsybl.ElementType.VSC_CONVERTER_STATION)

    def get_static_var_compensators(self) -> _DataFrame:
        """
        Get a dataframe of static var compensators.

        Returns:
            A dataframe of static var compensators.

        Notes:
            The resulting dataframe will have the following columns:
              - **b_min**: the minimum susceptance
              - **b_max**: the maximum susceptance
              - **voltage_setpoint**: The voltage setpoint
              - **reactive_power_setpoint**: The reactive power setpoint
              - **regulation_mode**: The regulation mode
              - **p**: active flow on the var compensator, ``NaN`` if no loadflow has been computed (in MW)
              - **q**: the reactive flow on the var compensator, ``NaN`` if no loadflow has been computed  (in MVAr)
              - **i**: The current on the var compensator, ``NaN`` if no loadflow has been computed (in A)
              - **voltage_level_id**: at which substation the var compensator is connected
              - **bus_id**: at which bus the var compensator is connected
              - **connected**: ``True`` ifthe var compensator is connected to a bus

            This dataframe is indexed by the id of the var compensator

        Examples:

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_static_var_compensators()

            will output something like:

            ======== ===== ===== ================ ======================= =============== === ======== === ================ ======= =========
            \        b_min b_max voltage_setpoint reactive_power_setpoint regulation_mode  p        q   i  voltage_level_id  bus_id connected
            ======== ===== ===== ================ ======================= =============== === ======== === ================ ======= =========
            id
                 SVC -0.05  0.05            400.0                     NaN         VOLTAGE NaN -12.5415 NaN            S4VL1 S4VL1_0      True
            ======== ===== ===== ================ ======================= =============== === ======== === ================ ======= =========
        """
        return self.get_elements(_pypowsybl.ElementType.STATIC_VAR_COMPENSATOR)

    def get_voltage_levels(self) -> _DataFrame:
        """
        Get a dataframe of voltage levels.

        Returns:
            A dataframe of voltage levels.

        Notes:
            The resulting dataframe will have the following columns:

              - **substation_id**: at which substation the voltage level belongs
              - **nominal_v**: The nominal voltage
              - **high_voltage_limit**: the high voltage limit
              - **low_voltage_limit**: the low voltage limit

            This dataframe is indexed by the id of the voltage levels

        Examples:

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_voltage_levels()

            will output something like:

            ========= ============= ========= ================== =================
            \         substation_id nominal_v high_voltage_limit low_voltage_limit
            ========= ============= ========= ================== =================
            id
                S1VL1            S1     225.0              240.0             220.0
                S1VL2            S1     400.0              440.0             390.0
                S2VL1            S2     400.0              440.0             390.0
                S3VL1            S3     400.0              440.0             390.0
                S4VL1            S4     400.0              440.0             390.0
            ========= ============= ========= ================== =================
        """
        return self.get_elements(_pypowsybl.ElementType.VOLTAGE_LEVEL)

    def get_busbar_sections(self) -> _DataFrame:
        """
        Get a dataframe of busbar sections.

        Returns:
            A dataframe of busbar sections.

        Notes:
            The resulting dataframe will have the following columns:

              - **fictitious**: ``True`` ifthe busbar section is part of the model and not of the actual network
              - **v**: The voltage magnitude of the busbar section (in kV)
              - **angle**: the voltage angle of the busbar section (in radian)
              - **voltage_level_id**: at which substation the busbar section is connected
              - **connected**: ``True`` ifthe busbar section is connected to a bus

            This dataframe is indexed by the id of the busbar sections

        Examples:

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_busbar_sections()

            will output something like:

            ========== ========== ======== ======== ================ =========
            \          fictitious        v    angle voltage_level_id connected
            ========== ========== ======== ======== ================ =========
            id
             S1VL1_BBS      False 224.6139   2.2822            S1VL1      True
            S1VL2_BBS1      False 400.0000   0.0000            S1VL2      True
            S1VL2_BBS2      False 400.0000   0.0000            S1VL2      True
             S2VL1_BBS      False 408.8470   0.7347            S2VL1      True
             S3VL1_BBS      False 400.0000   0.0000            S3VL1      True
             S4VL1_BBS      False 400.0000  -1.1259            S4VL1      True
            ========== ========== ======== ======== ================ =========
        """
        return self.get_elements(_pypowsybl.ElementType.BUSBAR_SECTION)

    def get_substations(self) -> _DataFrame:
        """
        Get substations :class:`~pandas.DataFrame`.

        Returns:
            A dataframe of substations.
        """
        return self.get_elements(_pypowsybl.ElementType.SUBSTATION)

    def get_hvdc_lines(self) -> _DataFrame:
        """
        Get a dataframe of HVDC lines.

        Returns:
            A dataframe of HVDC lines.

        Notes:
            The resulting dataframe will have the following columns:

              - **converters_mode**:
              - **active_power_setpoint**: (in MW)
              - **max_p**: the maximum of active power that can pass through the hvdc line (in MW)
              - **nominal_v**: nominal voltage (in kV)
              - **r**: the resistance of the hvdc line (in Ohm)
              - **converter_station1_id**: at which converter station the hvdc line is connected on side "1"
              - **converter_station2_id**: at which converter station the hvdc line is connected on side "2"
              - **connected1**: ``True`` ifthe busbar section on side "1" is connected to a bus
              - **connected2**: ``True`` ifthe busbar section on side "2" is connected to a bus

            This dataframe is indexed by the id of the hvdc lines

        Examples:

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_hvdc_lines()

            will output something like:

            ===== ================================ ===================== ===== ========= ==== ===================== ===================== ========== ==========
            \                      converters_mode active_power_setpoint max_p nominal_v    r converter_station1_id converter_station2_id connected1 connected2
            ===== ================================ ===================== ===== ========= ==== ===================== ===================== ========== ==========
            id
            HVDC1 SIDE_1_RECTIFIER_SIDE_2_INVERTER                  10.0 300.0     400.0  1.0                  VSC1                  VSC2       True       True
            HVDC2 SIDE_1_RECTIFIER_SIDE_2_INVERTER                  80.0 300.0     400.0  1.0                  LCC1                  LCC2       True       True
            ===== ================================ ===================== ===== ========= ==== ===================== ===================== ========== ==========
        """
        return self.get_elements(_pypowsybl.ElementType.HVDC_LINE)

    def get_switches(self) -> _DataFrame:
        """
        Get a dataframe of switches.

        Returns:
            A dataframe of HVDC lines.

        Notes:
            The resulting dataframe will have the following columns:

              - **kind**: the kind of switch
              - **open**: the open status of the switch
              - **retained**: the retain status of the switch
              - **voltage_level_id**: at which substation the switch is connected

            This dataframe is indexed by the id of the switches

        Examples:
            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_switches()

            will output something like:

            ============================ ============ ====== ======== ================
            \                                    kind   open retained voltage_level_id
            ============================ ============ ====== ======== ================
            id
              S1VL1_BBS_LD1_DISCONNECTOR DISCONNECTOR  False    False            S1VL1
                       S1VL1_LD1_BREAKER      BREAKER  False     True            S1VL1
              S1VL1_BBS_TWT_DISCONNECTOR DISCONNECTOR  False    False            S1VL1
                       S1VL1_TWT_BREAKER      BREAKER  False     True            S1VL1
             S1VL2_BBS1_TWT_DISCONNECTOR DISCONNECTOR  False    False            S1VL2
             S1VL2_BBS2_TWT_DISCONNECTOR DISCONNECTOR   True    False            S1VL2
                       S1VL2_TWT_BREAKER      BREAKER  False     True            S1VL2
            S1VL2_BBS1_VSC1_DISCONNECTOR DISCONNECTOR   True    False            S1VL2
                                     ...          ...    ...      ...              ...
            ============================ ============ ====== ======== ================
        """
        return self.get_elements(_pypowsybl.ElementType.SWITCH)

    def get_ratio_tap_changer_steps(self) -> _DataFrame:
        """
        Get a dataframe of ratio tap changer steps.

        Returns:
            A dataframe of HVDC lines.

        Notes:
            The resulting dataframe will have the following columns:

              - **rho**:
              - **r**: the resistance of the ratio tap changer step (in Ohm)
              - **x**: The reactance of the ratio tap changer step (Ohm)
              - **g**: the conductance of the ratio tap changer step (in Siemens)
              - **b**: the susceptance of the ratio tap changer step (in Siemens)

            This dataframe is index by the id of the transformer and the position of the ratio tap changer step

        Examples:
            .. code-block:: python

                net = pp.network.create_eurostag_tutorial_example1_network()
                net.get_ratio_tap_changer_steps()

            will output something like:

            ========== ======== ======== === === === ===
            \                        rho   r   x   g   b
            ========== ======== ======== === === === ===
            id         position
            NHV2_NLOAD        0 0.850567 0.0 0.0 0.0 0.0
            \                 1 1.000667 0.0 0.0 0.0 0.0
            \                 2 1.150767 0.0 0.0 0.0 0.0
            ========== ======== ======== === === === ===
        """
        return self.get_elements(_pypowsybl.ElementType.RATIO_TAP_CHANGER_STEP)

    def get_phase_tap_changer_steps(self) -> _DataFrame:
        """
        Get a dataframe of phase tap changer steps.

        Returns:
            A dataframe of phase tap changer steps.

        Notes:
            The resulting dataframe will have the following columns:

              - **rho**: the voltage ratio (in per unit)
              - **alpha**: the angle difference (in degree)
              - **r**: the resistance of the phase tap changer step (in Ohm)
              - **x**: The reactance of the phase tap changer step (Ohm)
              - **g**: the conductance of the phase tap changer step (in Siemens)
              - **b**: the susceptance of the phase tap changer step (in Siemens)

            This dataframe is index by the id of the transformer and the position of the phase tap changer step

        Examples:
            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_phase_tap_changer_steps()

            will output something like:

            === ======== ==== ====== ========= ========= === ===
            \             rho  alpha         r         x   g   b
            === ======== ==== ====== ========= ========= === ===
            id  position
            TWT        0  1.0 -42.80 39.784730 29.784725 0.0 0.0
            \          1  1.0 -40.18 31.720245 21.720242 0.0 0.0
            \          2  1.0 -37.54 23.655737 13.655735 0.0 0.0
            ...      ...  ...    ...       ...       ... ... ...
            === ======== ==== ====== ========= ========= === ===
        """
        return self.get_elements(_pypowsybl.ElementType.PHASE_TAP_CHANGER_STEP)

    def get_ratio_tap_changers(self) -> _DataFrame:
        """
        Create a ratio tap changers:class:`~pandas.DataFrame`.

        Returns:
            the ratio tap changers data frame

        Notes:
            The resulting dataframe will have the following columns:

              - **tap**:
              - **low_tap**:
              - **high_tap**:
              - **step_count**:
              - **on_load**:
              - **regulating**:
              - **target_v**:
              - **target_deadband**:
              - **regulationg_bus_id**:

            This dataframe is indexed by the id of the transformer

        Examples:
            .. code-block:: python

                net = pp.network.create_eurostag_tutorial_example1_network()
                net.get_ratio_tap_changers()

            will output something like:

            ========== === ======= ======== ========== ======= ========== ======== =============== =================
            \          tap low_tap high_tap step_count on_load regulating target_v target_deadband regulating_bus_id
            ========== === ======= ======== ========== ======= ========== ======== =============== =================
            id
            NHV2_NLOAD   1       0        2          3    True       True    158.0             0.0          VLLOAD_0
            ========== === ======= ======== ========== ======= ========== ======== =============== =================
        """
        return self.get_elements(_pypowsybl.ElementType.RATIO_TAP_CHANGER)

    def get_phase_tap_changers(self) -> _DataFrame:
        """
        Create a phase tap changers:class:`~pandas.DataFrame`.

        Returns:
            the phase tap changers data frame

        Notes:
            The resulting dataframe will have the following columns:

              - **tap**:
              - **low_tap**:
              - **high_tap**:
              - **step_count**:
              - **regulating**:
              - **regulation_mode**:
              - **target_deadband**:
              - **regulationg_bus_id**:

            This dataframe is indexed by the id of the transformer

        Examples:
            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_phase_tap_changers()

            will output something like:

            === === ======= ======== ========== ========== =============== ================ =============== =================
            \   tap low_tap high_tap step_count regulating regulation_mode regulation_value target_deadband regulating_bus_id
            === === ======= ======== ========== ========== =============== ================ =============== =================
            id
            TWT  15       0       32         33      False       FIXED_TAP              NaN             NaN           S1VL1_0
            === === ======= ======== ========== ========== =============== ================ =============== =================
        """
        return self.get_elements(_pypowsybl.ElementType.PHASE_TAP_CHANGER)

    def get_reactive_capability_curve_points(self) -> _DataFrame:
        """
        Get a dataframe of reactive capability curve points.

        Returns:
            A dataframe of reactive capability curve points.
        """
        return self.get_elements(_pypowsybl.ElementType.REACTIVE_CAPABILITY_CURVE_POINT)

    def update_elements(self, element_type: _pypowsybl.ElementType, df: _DataFrame = None, **kwargs):
        """
        Update network elements with data provided as a :class:`~pandas.DataFrame` or as named arguments.for a specified element type.

        The data frame columns are mapped to IIDM element attributes and each row is mapped to an element using the
        index.

        Args:
            element_type (ElementType): the element type
            df: the data to be updated
        """
        series_metadata = _pypowsybl.get_series_metadata(element_type)
        metadata_by_name = {s.name: s for s in series_metadata}

        is_index = []
        columns_names = []
        columns_values = []
        columns_types = []
        index_count = 0
        col_list = []
        if df is None:
            expected_size = None
            for key, value in kwargs.items():
                if not key in metadata_by_name:
                    raise ValueError('No column named {}'.format(key))
                columns_names.append(key)
                metadata = metadata_by_name[key]
                is_index.append(metadata.is_index)
                columns_types.append(metadata.type)
                values_array = _np.array(value, ndmin=1, copy=False)
                if values_array.ndim != 1:
                    raise ValueError('Network elements update: expecting only scalar or 1 dimension array '
                                     'as keyword argument, got {} dimensions'.format(values_array.ndim))
                size = values_array.shape[0]
                if expected_size is None:
                    expected_size = size
                elif size != expected_size:
                    raise ValueError('Network elements update: all arguments must have the same size, '
                                     'got size {} for series {}, expected {}'.format(size, key, expected_size))
                columns_values.append(values_array)
                index_count += 1
        else:
            if kwargs:
                raise RuntimeError('You must provide data in only one form: dataframe or named arguments')
            is_multi_index = len(df.index.names) > 1

            for idx, index_name in enumerate(df.index.names):
                if index_name is None:
                    index_name = series_metadata[idx].name
                if is_multi_index:
                    columns_values.append(df.index.get_level_values(index_name))
                else:
                    columns_values.append(df.index.values)
                columns_names.append(index_name)
                columns_types.append(metadata_by_name[index_name].type)
                index_count += 1
                is_index.append(True)
            columns_names.extend(df.columns.values)
            for series_name in df.columns.values:
                if not series_name in metadata_by_name:
                    raise ValueError('No column named {}'.format(series_name))
                series = df[series_name]
                series_type = metadata_by_name[series_name].type
                columns_types.append(series_type)
                columns_values.append(series.values)
                is_index.append(False)
        array = _pypowsybl.create_dataframe(columns_values, columns_names, columns_types, is_index)
        _pypowsybl.update_network_elements_with_series(self._handle, array, element_type)

    def update_buses(self, df: _DataFrame = None, **kwargs):
        """
        Update buses with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `v_mag`
        - `v_angle`

        See Also:
            :meth:`get_buses`

        Args:
            df: the data to be updated, as a data frame.
            **kwargs: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.
        """
        return self.update_elements(_pypowsybl.ElementType.BUS, df, **kwargs)

    def update_switches(self, df: _DataFrame = None, **kwargs):
        """
        Update switches with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

            - `open`
            - `retained`

        See Also:
            :meth:`get_switches`

        Args:
            df: the data to be updated, as a data frame.
            **kwargs: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.
        """
        return self.update_elements(_pypowsybl.ElementType.SWITCH, df, **kwargs)

    def update_generators(self, df: _DataFrame = None, **kwargs):
        """
        Update generators with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `target_p`
        - `max_p`
        - `min_p`
        - `target_v`
        - `target_q`
        - `voltage_regulator_on`
        - `p`
        - `q`
        - `connected`

        See Also:
            :meth:`get_generators`

        Args:
            df: the data to be updated, as a data frame.
            **kwargs: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.
        """
        return self.update_elements(_pypowsybl.ElementType.GENERATOR, df, **kwargs)

    def update_loads(self, df: _DataFrame = None, **kwargs):
        """
        Update loads with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `p0`
        - `q0`
        - `connected`

        See Also:
            :meth:`get_loads`

        Args:
            df: the data to be updated, as a data frame.
            **kwargs: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.
        """
        return self.update_elements(_pypowsybl.ElementType.LOAD, df, **kwargs)

    def update_batteries(self, df: _DataFrame = None, **kwargs):
        """
        Update batteries with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `p0`
        - `q0`
        - `connected`

        See Also:
            :meth:`get_batteries`

        Args:
            df: the data to be updated, as a data frame.
            **kwargs: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.
        """
        return self.update_elements(_pypowsybl.ElementType.BATTERY, df, **kwargs)

    def update_dangling_lines(self, df: _DataFrame = None, **kwargs):
        """
        Update dangling lines with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `r`
        - `x`
        - `g`
        - `b`
        - `p0`
        - `q0`
        - `p`
        - `q`
        - `connected`

        See Also:
            :meth:`get_dangling_lines`

        Args:
            df: the data to be updated, as a data frame.
            **kwargs: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.
        """
        return self.update_elements(_pypowsybl.ElementType.DANGLING_LINE, df, **kwargs)

    def update_vsc_converter_stations(self, df: _DataFrame = None, **kwargs):
        """
        Update VSC converter stations with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `voltage_setpoint`
        - `reactive_power_setpoint`
        - `voltage_regulator_on`
        - `p`
        - `q`
        - `connected`

        See Also:
            :meth:`get_vsc_converter_stations`

        Args:
          df: the data to be updated, as a data frame.
          **kwargs: the data to be updated, as named arguments.
              Arguments can be single values or any type of sequence.
              In the case of sequences, all arguments must have the same length.
        """
        return self.update_elements(_pypowsybl.ElementType.VSC_CONVERTER_STATION, df, **kwargs)

    def update_static_var_compensators(self, df: _DataFrame = None, **kwargs):
        """
        Update static var compensators with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:
        - `b_min`
        - `b_max`
        - `voltage_setpoint`
        - `reactive_power_setpoint`
        - `regulation_mode`
        - `p`
        - `q`
        - `connected`

        See Also:
            :meth:`get_static_var_compensators`

        Args:
            df: the data to be updated, as a data frame.
            **kwargs: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.
        """
        return self.update_elements(_pypowsybl.ElementType.STATIC_VAR_COMPENSATOR, df, **kwargs)

    def update_hvdc_lines(self, df: _DataFrame = None, **kwargs):
        """
        Update HVDC lines with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `converters_mode`
        - `active_power_setpoint`
        - `max_p`
        - `nominal_v`
        - `r`
        - `connected1`
        - `connected2`

        See Also:
            :meth:`get_hvdc_lines`

        Args:
            df: the data to be updated, as a data frame.
            **kwargs: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.
        """
        return self.update_elements(_pypowsybl.ElementType.HVDC_LINE, df, **kwargs)

    def update_lines(self, df: _DataFrame = None, **kwargs):
        """
        Update lines data with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        See Also:
            :meth:`get_lines`

        Args:
            df: lines data to be updated.
            **kwargs: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.
                - `r`
                - `x`
                - `g1`
                - `b1`
                - `g2`
                - `b2`
                - `p1`
                - `q1`
                - `p2`
                - `q2`
                - `connected1`
                - `connected2`
        """
        return self.update_elements(_pypowsybl.ElementType.LINE, df, **kwargs)

    def update_2_windings_transformers(self, df: _DataFrame = None, **kwargs):
        """
        Update 2 windings transformers with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `r`
        - `x`
        - `g`
        - `b`
        - `rated_u1`
        - `rated_u2`
        - `rated_s`
        - `p1`
        - `q1`
        - `p2`
        - `q2`
        - `connected1`
        - `connected2`

        See Also:
            :meth:`get_2_windings_transformers`

        Args:
            df: the data to be updated, as a data frame.
            **kwargs: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.
        """
        return self.update_elements(_pypowsybl.ElementType.TWO_WINDINGS_TRANSFORMER, df, **kwargs)

    def update_ratio_tap_changers(self, df: _DataFrame = None, **kwargs):
        """
        Update ratio tap changers with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `tap`
        - `on_load`
        - `regulating`
        - `target_v`
        - `target_deadband`

        See Also:
            :meth:`get_ratio_tap_changers`

        Args:
            df: the data to be updated, as a data frame.
            **kwargs: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.
        """
        return self.update_elements(_pypowsybl.ElementType.RATIO_TAP_CHANGER, df, **kwargs)

    def update_ratio_tap_changer_steps(self, df: _DataFrame = None, **kwargs):
        """
        Update ratio tap changer steps with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `rho`
        - `r`
        - `x`
        - `g`
        - `b`

        See Also:
            :meth:`get_ratio_tap_changer_steps`

        Args:
            df: the data to be updated, as a data frame.
            **kwargs: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.
        """
        return self.update_elements(_pypowsybl.ElementType.RATIO_TAP_CHANGER_STEP, df, **kwargs)

    def update_phase_tap_changers(self, df: _DataFrame = None, **kwargs):
        """
        Update phase tap changers with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated :

        - `tap`
        - `regulating`
        - `regulation_mode`
        - `regulation_value`
        - `target_deadband`

        See Also:
            :meth:`get_phase_tap_changers`

        Args:
            df: the data to be updated, as a data frame.
            **kwargs: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.
        """
        return self.update_elements(_pypowsybl.ElementType.PHASE_TAP_CHANGER, df, **kwargs)

    def update_phase_tap_changer_steps(self, df: _DataFrame = None, **kwargs):
        """
        Update phase tap changer steps with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated :

        - `rho`
        - `alpha`
        - `r`
        - `x`
        - `g`
        - `b`

        See Also:
            :meth:`get_phase_tap_changer_steps`

        Args:
            df: the data to be updated, as a data frame.
            **kwargs: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.
        """
        return self.update_elements(_pypowsybl.ElementType.PHASE_TAP_CHANGER_STEP, df, **kwargs)

    def update_shunt_compensators(self, df: _DataFrame = None, **kwargs):
        """
        Update shunt compensators with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `section_count`
        - `p`
        - `q`
        - `connected`

        See Also:
            :meth:`get_shunt_compensators`

        Args:
           df: the data to be updated, as a data frame.
           **kwargs: the data to be updated, as named arguments.
               Arguments can be single values or any type of sequence.
               In the case of sequences, all arguments must have the same length.
        """
        return self.update_elements(_pypowsybl.ElementType.SHUNT_COMPENSATOR, df, **kwargs)

    def update_linear_shunt_compensator_sections(self, df: _DataFrame = None, **kwargs):
        """
        Update shunt compensators with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `g_per_section`
        - `b_per_section`
        - `max_section_count`

        See Also:
            :meth:`get_linear_shunt_compensator_sections`

        Args:
            df: the data to be updated, as a data frame.
            **kwargs: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.

        """
        return self.update_elements(_pypowsybl.ElementType.LINEAR_SHUNT_COMPENSATOR_SECTION, df, **kwargs)

    def update_non_linear_shunt_compensator_sections(self, df: _DataFrame = None, **kwargs):
        """
        Update non linear shunt compensators sections with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are :

        - `g`
        - `b`

        See Also:
            :meth:`get_non_linear_shunt_compensator_sections`

        Args:
            df: the data to be updated, as a data frame.
            **kwargs: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.
        """
        return self.update_elements(_pypowsybl.ElementType.NON_LINEAR_SHUNT_COMPENSATOR_SECTION, df, **kwargs)

    def get_working_variant_id(self):
        """
        The current working variant ID.

        Returns:
            the id of the currently selected variant.
        """
        return _pypowsybl.get_working_variant_id(self._handle)

    def clone_variant(self, src: str, target: str, may_overwrite=True):
        """
        Creates a copy of the source variant

        Args:
            src: variant to copy
            target: id of the new variant that will be a copy of src
            may_overwrite: indicates if the target can be overwritten when it already exists
        """
        _pypowsybl.clone_variant(self._handle, src, target, may_overwrite)

    def set_working_variant(self, variant: str):
        """
        Changes the working variant. The provided variant ID must correspond
        to an existing variant, for example created by a call to `clone_variant`.

        Args:
            variant: id of the variant selected (it must exist)
        """
        _pypowsybl.set_working_variant(self._handle, variant)

    def remove_variant(self, variant: str):
        """
        Removes a variant from the network.

        Args:
            variant: id of the variant to be deleted
        """
        _pypowsybl.remove_variant(self._handle, variant)

    def get_variant_ids(self):
        """
        Get the list of existing variant IDs.

        Returns:
            all the ids of the existing variants
        """
        return _pypowsybl.get_variant_ids(self._handle)

    def get_current_limits(self):
        """
        Get the list of all current limits on the network paired with their branch id.

        Returns:
            all current limits on the network
        """
        return self.get_elements(_pypowsybl.ElementType.CURRENT_LIMITS)

    def get_node_breaker_topology(self, voltage_level_id: str) -> NodeBreakerTopology:
        """
        Get the node breaker description of the topology of a voltage level.

        Args:
            voltage_level_id: id of the voltage level

        Returns:
            The node breaker description of the topology of the voltage level
        """
        return NodeBreakerTopology(self._handle, voltage_level_id)

    def get_bus_breaker_topology(self, voltage_level_id: str) -> BusBreakerTopology:
        """
        Get the bus breaker description of the topology of a voltage level.

        Args:
            voltage_level_id: id of the voltage level

        Returns:
            The bus breaker description of the topology of the voltage level
        """
        return BusBreakerTopology(self._handle, voltage_level_id)

    def merge(self, *args):
        networkList = list(args)
        handleList = []
        for n in networkList:
            handleList.append(n._handle)
        return _pypowsybl.merge(self._handle, handleList)


def _create_network(name, network_id=''):
    return Network(_pypowsybl.create_network(name, network_id))


def create_empty(id: str = "Default") -> Network:
    """
    Create an empty network.

    Args:
        id: id of the network, defaults to 'Default'

    Returns:
        a new empty network
    """
    return _create_network('empty', network_id=id)


def create_ieee9() -> Network:
    """
    Create an instance of IEEE 9 bus network

    Returns:
        a new instance of IEEE 9 bus network
    """
    return _create_network('ieee9')


def create_ieee14() -> Network:
    """
    Create an instance of IEEE 14 bus network

    Returns:
        a new instance of IEEE 14 bus network
    """
    return _create_network('ieee14')


def create_ieee30() -> Network:
    """
    Create an instance of IEEE 30 bus network

    Returns:
        a new instance of IEEE 30 bus network
    """
    return _create_network('ieee30')


def create_ieee57() -> Network:
    """
    Create an instance of IEEE 57 bus network

    Returns:
        a new instance of IEEE 57 bus network
    """
    return _create_network('ieee57')


def create_ieee118() -> Network:
    """
    Create an instance of IEEE 118 bus network

    Returns:
        a new instance of IEEE 118 bus network
    """
    return _create_network('ieee118')


def create_ieee300() -> Network:
    """
    Create an instance of IEEE 300 bus network

    Returns:
        a new instance of IEEE 300 bus network
    """
    return _create_network('ieee300')


def create_eurostag_tutorial_example1_network() -> Network:
    """
    Create an instance of example 1 network of Eurostag tutorial

    Returns:
        a new instance of example 1 network of Eurostag tutorial
    """
    return _create_network('eurostag_tutorial_example1')


def create_four_substations_node_breaker_network() -> Network:
    """
    Create an instance of powsybl "4 substations" test case.

    It is meant to contain most network element types that can be
    represented in powsybl networks.
    The topology is in node-breaker representation.

    Returns:
        a new instance of powsybl "4 substations" test case
    """
    return _create_network('four_substations_node_breaker')


def create_micro_grid_be_network() -> Network:
    """
    Create an instance of micro grid BE CGMES test case

    Returns:
        a new instance of micro grid BE CGMES test case
    """
    return _create_network('micro_grid_be')


def create_micro_grid_nl_network() -> Network:
    """
    Create an instance of micro grid NL CGMES test case

    Returns:
        a new instance of micro grid NL CGMES test case
    """
    return _create_network('micro_grid_nl')


def get_import_formats() -> _List[str]:
    """
    Get list of supported import formats

    :return: the list of supported import formats
    :rtype: List[str]
    """
    return _pypowsybl.get_network_import_formats()


def get_export_formats() -> _List[str]:
    """
    Get list of supported export formats

    :return: the list of supported export formats
    :rtype: List[str]
    """
    return _pypowsybl.get_network_export_formats()


def get_import_parameters(format: str) -> _DataFrame:
    """
    Supported import parameters for a given format.

    Args:
       format (str): the format

    Returns:
        import parameters data frame

    Examples:
       .. doctest::

           >>> parameters = pp.network.get_import_parameters('PSS/E')
           >>> parameters.index.tolist()
           ['psse.import.ignore-base-voltage']
           >>> parameters['description']['psse.import.ignore-base-voltage']
           'Ignore base voltage specified in the file'
           >>> parameters['type']['psse.import.ignore-base-voltage']
           'BOOLEAN'
           >>> parameters['default']['psse.import.ignore-base-voltage']
           'false'
    """
    series_array = _pypowsybl.create_importer_parameters_series_array(format)
    return _create_data_frame_from_series_array(series_array)


def get_export_parameters(format: str) -> _DataFrame:
    """
    Get supported export parameters infos for a given format

    Args:
       format (str): the format

    Returns:
        export parameters data frame
    """
    series_array = _pypowsybl.create_exporter_parameters_series_array(format)
    return _create_data_frame_from_series_array(series_array)


def load(file: str, parameters: dict = {}) -> Network:
    """
    Load a network from a file. File should be in a supported format.

    Args:
       file (str): a file
       parameters (dict, optional): a map of parameters

    Returns:
        a network
    """
    return Network(_pypowsybl.load_network(file, parameters))


def load_from_string(file_name: str, file_content: str, parameters: dict = {}) -> Network:
    """
    Load a network from a string. File content should be in a supported format.

    Args:
       file_name (str): file name
       file_content (str): file content
       parameters (dict, optional): a map of parameters

    Returns:
        a network
    """
    return Network(_pypowsybl.load_network_from_string(file_name, file_content, parameters))
