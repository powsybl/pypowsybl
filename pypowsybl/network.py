#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
from __future__ import annotations  # Necessary for type alias like _DataFrame to work with sphinx

import sys as _sys
import datetime as _datetime
import warnings
from typing import (
    List as _List,
    Set as _Set,
    Dict as _Dict,
    Optional as _Optional, Union,
)

from numpy import Inf
from pandas import DataFrame as _DataFrame
import networkx as _nx
from numpy.typing import ArrayLike as _ArrayLike
import pandas as pd

import pypowsybl._pypowsybl as _pp
from pypowsybl._pypowsybl import ElementType
from pypowsybl._pypowsybl import ArrayStruct
from pypowsybl._pypowsybl import ValidationLevel
from pypowsybl.util import create_data_frame_from_series_array as _create_data_frame_from_series_array
from pypowsybl.utils.dataframes import _adapt_df_or_kwargs, _create_c_dataframe

def _series_metadata_repr(self: _pp.SeriesMetadata) -> str:
    return f'SeriesMetadata(name={self.name}, type={self.type}, ' \
           f'is_index={self.is_index}, is_modifiable={self.is_modifiable}, is_default={self.is_default})'


_pp.SeriesMetadata.__repr__ = _series_metadata_repr  # type: ignore

ParamsDict = _Optional[_Dict[str, str]]

class Svg:
    """
    This class represents a single line diagram."""

    def __init__(self, content: str):
        self._content = content

    @property
    def svg(self) -> str:
        return self._content

    def __str__(self) -> str:
        return self._content

    def _repr_svg_(self) -> str:
        return self._content


class NodeBreakerTopology:
    """
    Node-breaker representation of the topology of a voltage level.

    The topology is actually represented as a graph, where
    vertices are called "nodes" and are identified by a unique number in the voltage level,
    while edges are switches (breakers and disconnectors), or internal connections (plain "wires").
    """

    def __init__(self, network_handle: _pp.JavaHandle, voltage_level_id: str):
        self._internal_connections = _create_data_frame_from_series_array(
            _pp.get_node_breaker_view_internal_connections(network_handle, voltage_level_id))
        self._switchs = _create_data_frame_from_series_array(
            _pp.get_node_breaker_view_switches(network_handle, voltage_level_id))
        self._nodes = _create_data_frame_from_series_array(
            _pp.get_node_breaker_view_nodes(network_handle, voltage_level_id))

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

    def __init__(self, network_handle: _pp.JavaHandle, voltage_level_id: str):
        self._elements = _create_data_frame_from_series_array(
            _pp.get_bus_breaker_view_elements(network_handle, voltage_level_id))
        self._switchs = _create_data_frame_from_series_array(
            _pp.get_bus_breaker_view_switches(network_handle, voltage_level_id))
        self._buses = _create_data_frame_from_series_array(
            _pp.get_bus_breaker_view_buses(network_handle, voltage_level_id))

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


class Network:  # pylint: disable=too-many-public-methods

    def __init__(self, handle: _pp.JavaHandle):
        self._handle = handle
        att = _pp.get_network_metadata(self._handle)
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

    def __getstate__(self) -> _Dict[str, str]:
        return {'xml': self.dump_to_string()}

    def __setstate__(self, state: _Dict[str, str]) -> None:
        xml = state['xml']
        self._handle = _pp.load_network_from_string('tmp.xiidm', xml, {})

    def open_switch(self, id: str) -> bool:
        return _pp.update_switch_position(self._handle, id, True)

    def close_switch(self, id: str) -> bool:
        return _pp.update_switch_position(self._handle, id, False)

    def connect(self, id: str) -> bool:
        return _pp.update_connectable_status(self._handle, id, True)

    def disconnect(self, id: str) -> bool:
        return _pp.update_connectable_status(self._handle, id, False)

    def dump(self, file: str, format: str = 'XIIDM', parameters: ParamsDict = None) -> None:
        """
        Save a network to a file using a specified format.

        Args:
            file (str): a file
            format (str, optional): format to save the network, defaults to 'XIIDM'
            parameters (dict, optional): a map of parameters
        """
        if parameters is None:
            parameters = {}
        _pp.dump_network(self._handle, file, format, parameters)

    def dump_to_string(self, format: str = 'XIIDM', parameters: ParamsDict = None) -> str:
        """
        Save a network to a string using a specified format.

        Args:
            format (str, optional): format to export, only support mono file type, defaults to 'XIIDM'
            parameters (dict, optional): a map of parameters

        Returns:
            a string representing network
        """
        if parameters is None:
            parameters = {}
        return _pp.dump_network_to_string(self._handle, format, parameters)

    def reduce(self, v_min: float = 0, v_max: float = _sys.float_info.max, ids: _List[str] = None,
               vl_depths: tuple = (), with_dangling_lines: bool = False) -> None:
        if ids is None:
            ids = []
        vls = []
        depths = []
        for v in vl_depths:
            vls.append(v[0])
            depths.append(v[1])
        _pp.reduce_network(self._handle, v_min, v_max, ids, vls, depths, with_dangling_lines)

    def write_single_line_diagram_svg(self, container_id: str, svg_file: str) -> None:
        """
        Create a single line diagram in SVG format from a voltage level or a substation and write to a file.

        Args:
            container_id: a voltage level id or a substation id
            svg_file: a svg file path
        """
        _pp.write_single_line_diagram_svg(self._handle, container_id, svg_file)

    def get_single_line_diagram(self, container_id: str) -> Svg:
        """
        Create a single line diagram from a voltage level or a substation.

        Args:
            container_id: a voltage level id or a substation id

        Returns:
            the single line diagram
        """
        return Svg(_pp.get_single_line_diagram_svg(self._handle, container_id))

    def write_network_area_diagram_svg(self, svg_file: str, voltage_level_ids: Union[str, _List[str]]=None, depth: int = 0) -> None:
        """
        Create a network area diagram in SVG format and write it to a file.

        Args:
            svg_file: a svg file path
            voltage_level_id: the voltage level ID, center of the diagram (None for the full diagram)
            depth: the diagram depth around the voltage level
        """
        if voltage_level_ids is None:
            voltage_level_ids = []
        if type(voltage_level_ids) == str:
            voltage_level_ids = [voltage_level_ids]
        _pp.write_network_area_diagram_svg(self._handle, svg_file, voltage_level_ids, depth)

    def get_network_area_diagram(self, voltage_level_ids: Union[str, _List[str]]=None, depth: int = 0) -> Svg:
        """
        Create a network area diagram.

        Args:
            voltage_level_id: the voltage level ID, center of the diagram (None for the full diagram)
            depth: the diagram depth around the voltage level

        Returns:
            the network area diagram
        """
        if voltage_level_ids is None:
            voltage_level_ids = []
        if type(voltage_level_ids) == str:
            voltage_level_ids = [voltage_level_ids]
        return Svg(_pp.get_network_area_diagram_svg(self._handle, voltage_level_ids, depth))

    def get_elements_ids(self, element_type: ElementType, nominal_voltages: _Set[float] = None,
                         countries: _Set[str] = None,
                         main_connected_component: bool = True, main_synchronous_component: bool = True,
                         not_connected_to_same_bus_at_both_sides: bool = False) -> _List[str]:
        return _pp.get_network_elements_ids(self._handle, element_type,
                                            [] if nominal_voltages is None else list(nominal_voltages),
                                            [] if countries is None else list(countries),
                                            main_connected_component, main_synchronous_component,
                                            not_connected_to_same_bus_at_both_sides)

    def get_elements(self, element_type: ElementType, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        """
        Get network elements as a :class:`~pandas.DataFrame` for a specified element type.

        Args:
            element_type: the element type
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 optional parameters are mutually exclusive. If no optional parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Keyword Args:
            kwargs: the data to be selected, as named arguments.

        Returns:
            a network elements data frame for the specified element type
        """
        if attributes is None:
            attributes = []
        filter_attributes = _pp.FilterAttributesType.DEFAULT_ATTRIBUTES
        if all_attributes and len(attributes) > 0:
            raise RuntimeError('parameters "all_attributes" and "attributes" are mutually exclusive')
        if all_attributes:
            filter_attributes = _pp.FilterAttributesType.ALL_ATTRIBUTES
        elif len(attributes) > 0:
            filter_attributes = _pp.FilterAttributesType.SELECTION_ATTRIBUTES

        if kwargs:
            metadata = _pp.get_network_elements_dataframe_metadata(element_type)
            df = _adapt_df_or_kwargs(metadata, None, **kwargs)
            elements_array = _create_c_dataframe(df, metadata)
        else:
            elements_array = None

        series_array = _pp.create_network_elements_series_array(self._handle, element_type, filter_attributes, attributes, elements_array)
        return _create_data_frame_from_series_array(series_array)

    def get_buses(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of buses.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of buses.

        Notes:
            The resulting dataframe, depending on the parameters, will include the following columns:

              - **v_mag**: Get the voltage magnitude of the bus (in kV)
              - **v_angle**: the voltage angle of the bus (in degree)
              - **connected_component**: the number of terminals connected to this bus
              - **synchronous_component**: the number of synchronous components that the bus is part of
              - **voltage_level_id**: at which substation the bus is connected

            This dataframe is indexed on the bus ID.

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

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_buses(all_attributes=True)

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

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_buses(attributes=['v_mag','v_angle','voltage_level_id'])

            It outputs something like:

            ======= ======== ======= ================
            \          v_mag v_angle voltage_level_id
            ======= ======== ======= ================
            id
            S1VL1_0 224.6139  2.2822            S1VL1
            S1VL2_0 400.0000  0.0000            S1VL2
            S2VL1_0 408.8470  0.7347            S2VL1
            S3VL1_0 400.0000  0.0000            S3VL1
            S4VL1_0 400.0000 -1.1259            S4VL1
            ======= ======== ======= ================
        """
        return self.get_elements(ElementType.BUS, all_attributes, attributes, **kwargs)

    def get_generators(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of generators.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            the generators dataframe.

        Notes:
            The resulting dataframe, depending on the parameters, will include the following columns:

              - **energy_source**: the energy source used to fuel the generator
              - **target_p**: the target active value for the generator (in MW)
              - **max_p**: the maximum active value for the generator  (MW)
              - **min_p**: the minimum active value for the generator  (MW)
              - **target_v**: the target voltage magnitude value for the generator (in kV)
              - **target_q**: the target reactive value for the generator (in MVAr)
              - **voltage_regulator_on**: ``True`` if the generator regulates voltage
              - **regulated_element_id**: the ID of the network element where voltage is regulated
              - **p**: the actual active production of the generator (``NaN`` if no loadflow has been computed)
              - **q**: the actual reactive production of the generator (``NaN`` if no loadflow has been computed)
              - **voltage_level_id**: at which substation this generator is connected
              - **bus_id**: at which bus this generator is computed

            This dataframe is indexed on the generator ID.

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

            .. code-block:: python

                net = pp.network.create_ieee14()
                net.get_generators(all_attributes=True)

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

            .. code-block:: python

                net = pp.network.create_ieee14()
                net.get_generators(attributes=['energy_source','target_p','max_p','min_p','p','voltage_level_id','bus_id'])

            will output something like:

            ==== ============= ======== ====== ======= === ================ ======
            \    energy_source target_p  max_p   min_p   p voltage_level_id bus_id
            ==== ============= ======== ====== ======= === ================ ======
            id
            B1-G         OTHER    232.4 9999.0 -9999.0 NaN              VL1  VL1_0
            B2-G         OTHER     40.0 9999.0 -9999.0 NaN              VL2  VL2_0
            B3-G         OTHER      0.0 9999.0 -9999.0 NaN              VL3  VL3_0
            B6-G         OTHER      0.0 9999.0 -9999.0 NaN              VL6  VL6_0
            B8-G         OTHER      0.0 9999.0 -9999.0 NaN              VL8  VL8_0
            ==== ============= ======== ====== ======= === ================ ======

        .. warning::

            The "generator convention" is used for the "input" columns (`target_p`, `max_p`,
            `min_p`, `target_v` and `target_q`) while the "load convention" is used for the ouput columns
            (`p` and `q`).

            Most of the time, this means that `p` and `target_p` will have opposite sign. This also entails that
            `p` can be lower than `min_p`. Actually, the relation: :math:`\\text{min_p} <= -p <= \\text{max_p}`
            should hold.
        """
        return self.get_elements(ElementType.GENERATOR, all_attributes, attributes, **kwargs)

    def get_loads(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of loads.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            the loads dataframe

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

              - **type**: type of load
              - **p0**: the active load consumption setpoint (MW)
              - **q0**: the reactive load consumption setpoint  (MVAr)
              - **p**: the result active load consumption, it is ``NaN`` is not loadflow has been computed (MW)
              - **q**: the result reactive load consumption, it is ``NaN`` is not loadflow has been computed (MVAr)
              - **i**: the current on the load, ``NaN`` if no loadflow has been computed (in A)
              - **voltage_level_id**: at which substation this load is connected
              - **bus_id**: at which bus this load is connected

            This dataframe is indexed on the load ID.

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

            .. code-block:: python

                net = pp.network.create_ieee14()
                net.get_loads(all_attributes=True)

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

            .. code-block:: python

                net = pp.network.create_ieee14()
                net.get_loads(attributes=['type','p','q','voltage_level_id','bus_id','connected'])

            will output something like:

            ===== ========== === === ================ ======= =========
            \           type   p   q voltage_level_id  bus_id connected
            ===== ========== === === ================ ======= =========
            id
            B2-L   UNDEFINED NaN NaN              VL2   VL2_0      True
            B3-L   UNDEFINED NaN NaN              VL3   VL3_0      True
            B4-L   UNDEFINED NaN NaN              VL4   VL4_0      True
            B5-L   UNDEFINED NaN NaN              VL5   VL5_0      True
            B6-L   UNDEFINED NaN NaN              VL6   VL6_0      True
            B9-L   UNDEFINED NaN NaN              VL9   VL9_0      True
            B10-L  UNDEFINED NaN NaN             VL10  VL10_0      True
            B11-L  UNDEFINED NaN NaN             VL11  VL11_0      True
            B12-L  UNDEFINED NaN NaN             VL12  VL12_0      True
            B13-L  UNDEFINED NaN NaN             VL13  VL13_0      True
            B14-L  UNDEFINED NaN NaN             VL14  VL14_0      True
            ===== ========== === === ================ ======= =========
        """
        return self.get_elements(ElementType.LOAD, all_attributes, attributes, **kwargs)

    def get_batteries(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of batteries.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of batteries.
        """
        return self.get_elements(ElementType.BATTERY, all_attributes, attributes, **kwargs)

    def get_lines(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of lines data.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of lines data.

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

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

            .. code-block:: python

                net = pp.network.create_ieee14()
                net.get_lines(all_attributes=True)

            will output something like:

            ========  ========  ========  ===  ====  ===  ==== === === === === === === ================= ================= ======= ======= ========== ==========
            \                r         x   g1    b1   g2    b2  p1  q1  i1  p2  q2  i2 voltage_level1_id voltage_level2_id bus1_id bus2_id connected1 connected2
            ========  ========  ========  ===  ====  ===  ==== === === === === === === ================= ================= ======= ======= ========== ==========
            id
            L1-2-1    0.000194  0.000592  0.0  2.64  0.0  2.64 NaN NaN NaN NaN NaN NaN               VL1               VL2   VL1_0   VL2_0       True       True
            L1-5-1    0.000540  0.002230  0.0  2.46  0.0  2.46 NaN NaN NaN NaN NaN NaN               VL1               VL5   VL1_0   VL5_0       True       True
            ========  ========  ========  ===  ====  ===  ==== === === === === === === ================= ================= ======= ======= ========== ==========

            .. code-block:: python

                net = pp.network.create_ieee14()
                net.get_lines(attributes=['p1','q1','i1','p2','q2','i2','voltage_level1_id','voltage_level2_id','bus1_id','bus2_id','connected1','connected2'])

            will output something like:

            ======== === === === === === === ================= ================= ======= ======= ========== ==========
            \         p1  q1  i1  p2  q2  i2 voltage_level1_id voltage_level2_id bus1_id bus2_id connected1 connected2
            ======== === === === === === === ================= ================= ======= ======= ========== ==========
            id
            L1-2-1   NaN NaN NaN NaN NaN NaN               VL1               VL2   VL1_0   VL2_0       True       True
            L1-5-1   NaN NaN NaN NaN NaN NaN               VL1               VL5   VL1_0   VL5_0       True       True
            ======== === === === === === === ================= ================= ======= ======= ========== ==========
        """
        return self.get_elements(ElementType.LINE, all_attributes, attributes, **kwargs)

    def get_2_windings_transformers(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of 2 windings transformers.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of 2 windings transformers.

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

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
              - **connected1**: ``True`` if the side "1" of the transformer is connected to a bus
              - **connected2**: ``True`` if the side "2" of the transformer is connected to a bus

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

            .. code-block:: python

                net = pp.network.create_ieee14()
                net.get_2_windings_transformers(all_attributes=True)

            will output something like:

            ====== ==== ======== === === ======== ======== ======= === === === === === === ================= ================= ======= ======= ========== ==========
            \         r        x   g   b rated_u1 rated_u2 rated_s  p1  q1  i1  p2  q2  i2 voltage_level1_id voltage_level2_id bus1_id bus2_id connected1 connected2
            ====== ==== ======== === === ======== ======== ======= === === === === === === ================= ================= ======= ======= ========== ==========
            id
            T4-7-1  0.0 0.409875 0.0 0.0  132.030     14.0     NaN NaN NaN NaN NaN NaN NaN               VL4               VL7   VL4_0   VL7_0       True       True
            T4-9-1  0.0 0.800899 0.0 0.0  130.815     12.0     NaN NaN NaN NaN NaN NaN NaN               VL4               VL9   VL4_0   VL9_0       True       True
            T5-6-1  0.0 0.362909 0.0 0.0  125.820     12.0     NaN NaN NaN NaN NaN NaN NaN               VL5               VL6   VL5_0   VL6_0       True       True
            ====== ==== ======== === === ======== ======== ======= === === === === === === ================= ================= ======= ======= ========== ==========

            .. code-block:: python

                net = pp.network.create_ieee14()
                net.get_2_windings_transformers(attributes=['p1','q1','i1','p2','q2','i2','voltage_level1_id','voltage_level2_id','bus1_id','bus2_id','connected1','connected2'])

            will output something like:

            ====== === === === === === === ================= ================= ======= ======= ========== ==========
            \       p1  q1  i1  p2  q2  i2 voltage_level1_id voltage_level2_id bus1_id bus2_id connected1 connected2
            ====== === === === === === === ================= ================= ======= ======= ========== ==========
            id
            T4-7-1 NaN NaN NaN NaN NaN NaN               VL4               VL7   VL4_0   VL7_0       True       True
            T4-9-1 NaN NaN NaN NaN NaN NaN               VL4               VL9   VL4_0   VL9_0       True       True
            T5-6-1 NaN NaN NaN NaN NaN NaN               VL5               VL6   VL5_0   VL6_0       True       True
            ====== === === === === === === ================= ================= ======= ======= ========== ==========
        """
        return self.get_elements(ElementType.TWO_WINDINGS_TRANSFORMER, all_attributes, attributes, **kwargs)

    def get_3_windings_transformers(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of 3 windings transformers.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of 3 windings transformers.
        """
        return self.get_elements(ElementType.THREE_WINDINGS_TRANSFORMER, all_attributes, attributes, **kwargs)

    def get_shunt_compensators(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of shunt compensators.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of shunt compensators.

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

              - **model_type**:
              - **max_section_count**: The maximum number of sections that may be switched on
              - **section_count**: The current number of section that may be switched on
              - **p**: the active flow on the shunt, ``NaN`` if no loadflow has been computed (in MW)
              - **q**: the reactive flow on the shunt, ``NaN`` if no loadflow has been computed  (in MVAr)
              - **i**: the current in the shunt, ``NaN`` if no loadflow has been computed  (in A)
              - **voltage_level_id**: at which substation the shunt is connected
              - **bus_id**: indicate at which bus the shunt is connected
              - **connected**: ``True`` if the shunt is connected to a bus

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

            .. code-block:: python

                net = pp.network.create_ieee14()
                net.get_shunt_compensators(all_attributes=True)

            will output something like:

            ===== ========== ================= ============= === === === ================ ====== =========
            \     model_type max_section_count section_count   p   q   i voltage_level_id bus_id connected
            ===== ========== ================= ============= === === === ================ ====== =========
            id
            B9-SH     LINEAR                 1             1 NaN NaN NaN              VL9  VL9_0      True
            ===== ========== ================= ============= === === === ================ ====== =========

            .. code-block:: python

                net = pp.network.create_ieee14()
                net.get_shunt_compensators(attributes=['model_type','p','q','i','voltage_level_id','bus_id','connected'])

            will output something like:

            ===== ========== === === === ================ ====== =========
            \     model_type   p   q   i voltage_level_id bus_id connected
            ===== ========== === === === ================ ====== =========
            id
            B9-SH     LINEAR NaN NaN NaN              VL9  VL9_0      True
            ===== ========== === === === ================ ====== =========
        """
        return self.get_elements(ElementType.SHUNT_COMPENSATOR, all_attributes, attributes, **kwargs)

    def get_non_linear_shunt_compensator_sections(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of shunt compensators sections for non linear model.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Notes:
            The resulting dataframe will have the following columns:

              - **g**: the accumulated conductance in S if the section and all the previous ones are activated.
              - **b**: the accumulated susceptance in S if the section and all the previous ones are activated

            This dataframe is multi-indexed, by the tuple (id of shunt, section number).

        Returns:
            A dataframe of non linear model shunt compensators sections.
        """
        return self.get_elements(ElementType.NON_LINEAR_SHUNT_COMPENSATOR_SECTION, all_attributes, attributes, **kwargs)

    def get_linear_shunt_compensator_sections(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of shunt compensators sections for linear model.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

              - **g_per_section**: the conductance per section in S
              - **b_per_section**: the susceptance per section in S
              - **max_section_count**: the maximum number of sections

            This dataframe is indexed by the shunt compensator ID.

        Returns:
           A dataframe of linear models of shunt compensators.
        """
        return self.get_elements(ElementType.LINEAR_SHUNT_COMPENSATOR_SECTION, all_attributes, attributes, **kwargs)

    def get_dangling_lines(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of dangling lines.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of dangling lines.

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

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
              - **connected**: ``True`` if the dangling line is connected to a bus

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

            .. code-block:: python

                net = pp.network._create_dangling_lines_network()
                net.get_dangling_lines(all_attributes=True)

            will output something like:

            == ==== === ====== ======= ==== ==== === === === ================ ====== =========
            \     r   x      g       b   p0   q0   p   q   i voltage_level_id bus_id connected
            == ==== === ====== ======= ==== ==== === === === ================ ====== =========
            id
            DL 10.0 1.0 0.0001 0.00001 50.0 30.0 NaN NaN NaN               VL   VL_0      True
            == ==== === ====== ======= ==== ==== === === === ================ ====== =========

            .. code-block:: python

                net = pp.network._create_dangling_lines_network()
                net.get_dangling_lines(attributes=['p','q','i','voltage_level_id','bus_id','connected'])

            will output something like:

            == === === === ================ ====== =========
            \    p   q   i voltage_level_id bus_id connected
            == === === === ================ ====== =========
            id
            DL NaN NaN NaN               VL   VL_0      True
            == === === === ================ ====== =========
        """
        return self.get_elements(ElementType.DANGLING_LINE, all_attributes, attributes, **kwargs)

    def get_lcc_converter_stations(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of LCC converter stations.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of LCC converter stations.

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

              - **power_factor**: the power factor
              - **loss_factor**: the loss factor
              - **p**: active flow on the LCC converter station, ``NaN`` if no loadflow has been computed (in MW)
              - **q**: the reactive flow on the LCC converter station, ``NaN`` if no loadflow has been computed  (in MVAr)
              - **i**: The current on the LCC converter station, ``NaN`` if no loadflow has been computed (in A)
              - **voltage_level_id**: at which substation the LCC converter station is connected
              - **bus_id**: at which bus the LCC converter station is connected
              - **connected**: ``True`` if the LCC converter station is connected to a bus

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

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_lcc_converter_stations(all_attributes=True)

            will output something like:

            ======== ============ ===========  ====== === === ================ ======= =========
                .    power_factor loss_factor       p   q   i voltage_level_id  bus_id connected
            ======== ============ ===========  ====== === === ================ ======= =========
            id
                LCC1          0.6         1.1   80.88 NaN NaN            S1VL2 S1VL2_0      True
                LCC2          0.6         1.1  -79.12 NaN NaN            S3VL1 S3VL1_0      True
            ======== ============ ===========  ====== === === ================ ======= =========

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_lcc_converter_stations(attributes=['p','q','i','voltage_level_id','bus_id','connected'])

            will output something like:

            ======== ====== === === ================ ======= =========
                .         p   q   i voltage_level_id  bus_id connected
            ======== ====== === === ================ ======= =========
            id
                LCC1  80.88 NaN NaN            S1VL2 S1VL2_0      True
                LCC2 -79.12 NaN NaN            S3VL1 S3VL1_0      True
            ======== ====== === === ================ ======= =========
        """
        return self.get_elements(ElementType.LCC_CONVERTER_STATION, all_attributes, attributes, **kwargs)

    def get_vsc_converter_stations(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of VSC converter stations.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of VCS converter stations.

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

              - **loss_factor**: correspond to the loss of power due to ac dc conversion
              - **target_v**: The voltage setpoint
              - **target_q**: The reactive power setpoint
              - **voltage_regulator_on**: The voltage regulator status
              - **p**: active flow on the VSC  converter station, ``NaN`` if no loadflow has been computed (in MW)
              - **q**: the reactive flow on the VSC converter station, ``NaN`` if no loadflow has been computed  (in MVAr)
              - **i**: The current on the VSC converter station, ``NaN`` if no loadflow has been computed (in A)
              - **voltage_level_id**: at which substation the VSC converter station is connected
              - **bus_id**: at which bus the VSC converter station is connected
              - **connected**: ``True`` if the VSC converter station is connected to a bus

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

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_vsc_converter_stations(all_attributes=True)

            will output something like:

            ======== =========== ================ ======================= ==================== ====== ========= ========== ================ ======= =========
            \        loss_factor         target_v                target_q voltage_regulator_on      p         q          i voltage_level_id  bus_id connected
            ======== =========== ================ ======================= ==================== ====== ========= ========== ================ ======= =========
            id
                VSC1         1.1            400.0                   500.0                 True  10.11 -512.0814 739.269871            S1VL2 S1VL2_0      True
                VSC2         1.1              0.0                   120.0                False  -9.89 -120.0000 170.031658            S2VL1 S2VL1_0      True
            ======== =========== ================ ======================= ==================== ====== ========= ========== ================ ======= =========

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_vsc_converter_stations(attributes=['p','q','i','voltage_level_id','bus_id','connected'])

            will output something like:

            ======== ====== ========= ========== ================ ======= =========
            \             p         q          i voltage_level_id  bus_id connected
            ======== ====== ========= ========== ================ ======= =========
            id
                VSC1  10.11 -512.0814 739.269871            S1VL2 S1VL2_0      True
                VSC2  -9.89 -120.0000 170.031658            S2VL1 S2VL1_0      True
            ======== ====== ========= ========== ================ ======= =========
        """
        return self.get_elements(ElementType.VSC_CONVERTER_STATION, all_attributes, attributes, **kwargs)

    def get_static_var_compensators(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of static var compensators.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of static var compensators.

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

              - **b_min**: the minimum susceptance
              - **b_max**: the maximum susceptance
              - **target_v**: The voltage setpoint
              - **target_q**: The reactive power setpoint
              - **regulation_mode**: The regulation mode
              - **p**: active flow on the var compensator, ``NaN`` if no loadflow has been computed (in MW)
              - **q**: the reactive flow on the var compensator, ``NaN`` if no loadflow has been computed  (in MVAr)
              - **i**: The current on the var compensator, ``NaN`` if no loadflow has been computed (in A)
              - **voltage_level_id**: at which substation the var compensator is connected
              - **bus_id**: at which bus the var compensator is connected
              - **connected**: ``True`` if the var compensator is connected to a bus

            This dataframe is indexed by the id of the var compensator

        Examples:

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_static_var_compensators()

            will output something like:

            ======== ===== ===== ================ ======================= =============== === ======== === ================ ======= =========
            \        b_min b_max         target_v                target_q regulation_mode  p        q   i  voltage_level_id  bus_id connected
            ======== ===== ===== ================ ======================= =============== === ======== === ================ ======= =========
            id
                 SVC -0.05  0.05            400.0                     NaN         VOLTAGE NaN -12.5415 NaN            S4VL1 S4VL1_0      True
            ======== ===== ===== ================ ======================= =============== === ======== === ================ ======= =========

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_static_var_compensators(all_attributes=True)

            will output something like:

            ======== ===== ===== ================ ======================= =============== === ======== === ================ ======= =========
            \        b_min b_max voltage_setpoint reactive_power_setpoint regulation_mode  p        q   i  voltage_level_id  bus_id connected
            ======== ===== ===== ================ ======================= =============== === ======== === ================ ======= =========
            id
                 SVC -0.05  0.05            400.0                     NaN         VOLTAGE NaN -12.5415 NaN            S4VL1 S4VL1_0      True
            ======== ===== ===== ================ ======================= =============== === ======== === ================ ======= =========

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_static_var_compensators(attributes=['p','q','i','voltage_level_id','bus_id','connected'])

            will output something like:

            ======== === ======== === ================ ======= =========
            \         p        q   i  voltage_level_id  bus_id connected
            ======== === ======== === ================ ======= =========
            id
                 SVC NaN -12.5415 NaN            S4VL1 S4VL1_0      True
            ======== === ======== === ================ ======= =========
        """
        return self.get_elements(ElementType.STATIC_VAR_COMPENSATOR, all_attributes, attributes, **kwargs)

    def get_voltage_levels(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of voltage levels.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of voltage levels.

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

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

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_voltage_levels(all_attributes=True)

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

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_voltage_levels(attributes=['substation_id','nominal_v'])

            will output something like:

            ========= ============= =========
            \         substation_id nominal_v
            ========= ============= =========
            id
                S1VL1            S1     225.0
                S1VL2            S1     400.0
                S2VL1            S2     400.0
                S3VL1            S3     400.0
                S4VL1            S4     400.0
            ========= ============= =========
        """
        return self.get_elements(ElementType.VOLTAGE_LEVEL, all_attributes, attributes, **kwargs)

    def get_busbar_sections(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of busbar sections.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of busbar sections.

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

              - **fictitious**: ``True`` if the busbar section is part of the model and not of the actual network
              - **v**: The voltage magnitude of the busbar section (in kV)
              - **angle**: the voltage angle of the busbar section (in radian)
              - **voltage_level_id**: at which substation the busbar section is connected
              - **connected**: ``True`` if the busbar section is connected to a bus

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

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_busbar_sections(all_attributes=True)

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

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_busbar_sections(attributes=['v','angle','voltage_level_id','connected'])

            will output something like:

            ========== ======== ======== ================ =========
            \                 v    angle voltage_level_id connected
            ========== ======== ======== ================ =========
            id
             S1VL1_BBS 224.6139   2.2822            S1VL1      True
            S1VL2_BBS1 400.0000   0.0000            S1VL2      True
            S1VL2_BBS2 400.0000   0.0000            S1VL2      True
             S2VL1_BBS 408.8470   0.7347            S2VL1      True
             S3VL1_BBS 400.0000   0.0000            S3VL1      True
             S4VL1_BBS 400.0000  -1.1259            S4VL1      True
            ========== ======== ======== ================ =========
        """
        return self.get_elements(ElementType.BUSBAR_SECTION, all_attributes, attributes, **kwargs)

    def get_substations(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get substations :class:`~pandas.DataFrame`.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of substations.
        """
        return self.get_elements(ElementType.SUBSTATION, all_attributes, attributes, **kwargs)

    def get_hvdc_lines(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of HVDC lines.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of HVDC lines.

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

              - **converters_mode**:
              - **target_p**: (in MW)
              - **max_p**: the maximum of active power that can pass through the hvdc line (in MW)
              - **nominal_v**: nominal voltage (in kV)
              - **r**: the resistance of the hvdc line (in Ohm)
              - **converter_station1_id**: at which converter station the hvdc line is connected on side "1"
              - **converter_station2_id**: at which converter station the hvdc line is connected on side "2"
              - **connected1**: ``True`` if the busbar section on side "1" is connected to a bus
              - **connected2**: ``True`` if the busbar section on side "2" is connected to a bus

            This dataframe is indexed by the id of the hvdc lines

        Examples:

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_hvdc_lines()

            will output something like:

            ===== ================================ ===================== ===== ========= ==== ===================== ===================== ========== ==========
            \                      converters_mode              target_p max_p nominal_v    r converter_station1_id converter_station2_id connected1 connected2
            ===== ================================ ===================== ===== ========= ==== ===================== ===================== ========== ==========
            id
            HVDC1 SIDE_1_RECTIFIER_SIDE_2_INVERTER                  10.0 300.0     400.0  1.0                  VSC1                  VSC2       True       True
            HVDC2 SIDE_1_RECTIFIER_SIDE_2_INVERTER                  80.0 300.0     400.0  1.0                  LCC1                  LCC2       True       True
            ===== ================================ ===================== ===== ========= ==== ===================== ===================== ========== ==========

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_hvdc_lines(all_attributes=True)

            will output something like:

            ===== ================================ ===================== ===== ========= ==== ===================== ===================== ========== ==========
            \                      converters_mode active_power_setpoint max_p nominal_v    r converter_station1_id converter_station2_id connected1 connected2
            ===== ================================ ===================== ===== ========= ==== ===================== ===================== ========== ==========
            id
            HVDC1 SIDE_1_RECTIFIER_SIDE_2_INVERTER                  10.0 300.0     400.0  1.0                  VSC1                  VSC2       True       True
            HVDC2 SIDE_1_RECTIFIER_SIDE_2_INVERTER                  80.0 300.0     400.0  1.0                  LCC1                  LCC2       True       True
            ===== ================================ ===================== ===== ========= ==== ===================== ===================== ========== ==========

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_hvdc_lines(attributes=['converters_mode','active_power_setpoint','nominal_v','converter_station1_id','converter_station2_id','connected1','connected2'])

            will output something like:

            ===== ================================ ===================== ========= ===================== ===================== ========== ==========
            \                      converters_mode active_power_setpoint nominal_v converter_station1_id converter_station2_id connected1 connected2
            ===== ================================ ===================== ========= ===================== ===================== ========== ==========
            id
            HVDC1 SIDE_1_RECTIFIER_SIDE_2_INVERTER                  10.0     400.0                  VSC1                  VSC2       True       True
            HVDC2 SIDE_1_RECTIFIER_SIDE_2_INVERTER                  80.0     400.0                  LCC1                  LCC2       True       True
            ===== ================================ ===================== ========= ===================== ===================== ========== ==========
        """
        return self.get_elements(ElementType.HVDC_LINE, all_attributes, attributes, **kwargs)

    def get_switches(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of switches.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of switches.

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

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

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_switches(all_attributes=True)

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

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_switches(attributes=['kind','open','nominal_v','voltage_level_id'])

            will output something like:

            ============================ ============ ====== ================
            \                                    kind   open voltage_level_id
            ============================ ============ ====== ================
            id
              S1VL1_BBS_LD1_DISCONNECTOR DISCONNECTOR  False            S1VL1
                       S1VL1_LD1_BREAKER      BREAKER  False            S1VL1
              S1VL1_BBS_TWT_DISCONNECTOR DISCONNECTOR  False            S1VL1
                       S1VL1_TWT_BREAKER      BREAKER  False            S1VL1
             S1VL2_BBS1_TWT_DISCONNECTOR DISCONNECTOR  False            S1VL2
             S1VL2_BBS2_TWT_DISCONNECTOR DISCONNECTOR   True            S1VL2
                       S1VL2_TWT_BREAKER      BREAKER  False            S1VL2
            S1VL2_BBS1_VSC1_DISCONNECTOR DISCONNECTOR   True            S1VL2
                                     ...          ...    ...              ...
            ============================ ============ ====== ================
        """
        return self.get_elements(ElementType.SWITCH, all_attributes, attributes, **kwargs)

    def get_ratio_tap_changer_steps(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of ratio tap changer steps.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of ratio tap changer steps.

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

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

            .. code-block:: python

                net = pp.network.create_eurostag_tutorial_example1_network()
                net.get_ratio_tap_changer_steps(all_attributes=True)

            will output something like:

            ========== ======== ======== === === === ===
            \                        rho   r   x   g   b
            ========== ======== ======== === === === ===
            id         position
            NHV2_NLOAD        0 0.850567 0.0 0.0 0.0 0.0
            \                 1 1.000667 0.0 0.0 0.0 0.0
            \                 2 1.150767 0.0 0.0 0.0 0.0
            ========== ======== ======== === === === ===

            .. code-block:: python

                net = pp.network.create_eurostag_tutorial_example1_network()
                net.get_ratio_tap_changer_steps(attributes=['rho','r','x'])

            will output something like:

            ========== ======== ======== === ===
            \                        rho   r   x
            ========== ======== ======== === ===
            id         position
            NHV2_NLOAD        0 0.850567 0.0 0.0
            \                 1 1.000667 0.0 0.0
            \                 2 1.150767 0.0 0.0
            ========== ======== ======== === ===
        """
        return self.get_elements(ElementType.RATIO_TAP_CHANGER_STEP, all_attributes, attributes, **kwargs)

    def get_phase_tap_changer_steps(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Get a dataframe of phase tap changer steps.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            A dataframe of phase tap changer steps.

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

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

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_phase_tap_changer_steps(all_attributes=True)

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

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_phase_tap_changer_steps(attributes=['rho','r','x'])

            will output something like:

            === ======== ==== ========= =========
            \             rho         r         x
            === ======== ==== ========= =========
            id  position
            TWT        0  1.0 39.784730 29.784725
            \          1  1.0 31.720245 21.720242
            \          2  1.0 23.655737 13.655735
            ...      ...  ...       ...       ...
            === ======== ==== ========= =========
        """
        return self.get_elements(ElementType.PHASE_TAP_CHANGER_STEP, all_attributes, attributes, **kwargs)

    def get_ratio_tap_changers(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Create a ratio tap changers:class:`~pandas.DataFrame`.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            the ratio tap changers data frame

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

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

            .. code-block:: python

                net = pp.network.create_eurostag_tutorial_example1_network()
                net.get_ratio_tap_changers(all_attributes=True)

            will output something like:

            ========== === ======= ======== ========== ======= ========== ======== =============== =================
            \          tap low_tap high_tap step_count on_load regulating target_v target_deadband regulating_bus_id
            ========== === ======= ======== ========== ======= ========== ======== =============== =================
            id
            NHV2_NLOAD   1       0        2          3    True       True    158.0             0.0          VLLOAD_0
            ========== === ======= ======== ========== ======= ========== ======== =============== =================

            .. code-block:: python

                net = pp.network.create_eurostag_tutorial_example1_network()
                net.get_ratio_tap_changers(attributes=['tap','low_tap','high_tap','step_count','target_v','regulating_bus_id'])

            will output something like:

            ========== === ======= ======== ========== ======== =================
            \          tap low_tap high_tap step_count target_v regulating_bus_id
            ========== === ======= ======== ========== ======== =================
            id
            NHV2_NLOAD   1       0        2          3    158.0          VLLOAD_0
            ========== === ======= ======== ========== ======== =================
        """
        return self.get_elements(ElementType.RATIO_TAP_CHANGER, all_attributes, attributes, **kwargs)

    def get_phase_tap_changers(self, all_attributes: bool = False, attributes: _List[str] = None, **kwargs: _ArrayLike) -> _DataFrame:
        r"""
        Create a phase tap changers:class:`~pandas.DataFrame`.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.
            kwargs: the data to be selected, as named arguments.

        Returns:
            the phase tap changers data frame

        Notes:
            The resulting dataframe, depending on the parameters, could have the following columns:

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

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_phase_tap_changers(all_attributes=True)

            will output something like:

            === === ======= ======== ========== ========== =============== ================ =============== =================
            \   tap low_tap high_tap step_count regulating regulation_mode regulation_value target_deadband regulating_bus_id
            === === ======= ======== ========== ========== =============== ================ =============== =================
            id
            TWT  15       0       32         33      False       FIXED_TAP              NaN             NaN           S1VL1_0
            === === ======= ======== ========== ========== =============== ================ =============== =================

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_phase_tap_changers(attributes=['tap','low_tap','high_tap','step_count','regulating_bus_id'])

            will output something like:

            === === ======= ======== ========== =================
            \   tap low_tap high_tap step_count regulating_bus_id
            === === ======= ======== ========== =================
            id
            TWT  15       0       32         33           S1VL1_0
            === === ======= ======== ========== =================
        """
        return self.get_elements(ElementType.PHASE_TAP_CHANGER, all_attributes, attributes, **kwargs)

    def get_reactive_capability_curve_points(self, all_attributes: bool = False, attributes: _List[str] = None) -> _DataFrame:
        """
        Get a dataframe of reactive capability curve points.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.

        Returns:
            A dataframe of reactive capability curve points.
        """
        return self.get_elements(ElementType.REACTIVE_CAPABILITY_CURVE_POINT, all_attributes, attributes)

    def _update_elements(self, element_type: ElementType, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update network elements with data provided as a :class:`~pandas.DataFrame` or as named arguments.for a specified element type.

        The data frame columns are mapped to IIDM element attributes and each row is mapped to an element using the
        index.

        Args:
            element_type (ElementType): the element type
            df: the data to be updated
        """
        metadata = _pp.get_network_elements_dataframe_metadata(element_type)
        df = _adapt_df_or_kwargs(metadata, df, **kwargs)
        c_df = _create_c_dataframe(df, metadata)
        _pp.update_network_elements_with_series(self._handle, c_df, element_type)

    def update_buses(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update buses with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `v_mag`
        - `v_angle`

        See Also:
            :meth:`get_buses`

        Args:
            df: the data to be updated, as a data frame.
            **kwargs: _ArrayLike: the data to be updated, as named arguments.
                Arguments can be single values or any type of sequence.
                In the case of sequences, all arguments must have the same length.
        """
        return self._update_elements(ElementType.BUS, df, **kwargs)

    def update_switches(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
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
        return self._update_elements(ElementType.SWITCH, df, **kwargs)

    def update_generators(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update generators with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `target_p`
        - `max_p`
        - `min_p`
        - `target_v`
        - `target_q`
        - `voltage_regulator_on`
        - `regulated_element_id` : you may define any injection or busbar section as the regulated location.
           Only supported in node breaker voltage levels.
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
        return self._update_elements(ElementType.GENERATOR, df, **kwargs)

    def update_loads(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
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
        return self._update_elements(ElementType.LOAD, df, **kwargs)

    def update_batteries(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
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
        return self._update_elements(ElementType.BATTERY, df, **kwargs)

    def update_dangling_lines(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
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
        return self._update_elements(ElementType.DANGLING_LINE, df, **kwargs)

    def update_vsc_converter_stations(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update VSC converter stations with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `target_v`
        - `target_q`
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
        return self._update_elements(ElementType.VSC_CONVERTER_STATION, df, **kwargs)

    def update_lcc_converter_stations(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update VSC converter stations with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `target_v`
        - `target_q`
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
        return self._update_elements(ElementType.LCC_CONVERTER_STATION, df, **kwargs)

    def update_static_var_compensators(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update static var compensators with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:
        - `b_min`
        - `b_max`
        - `target_v`
        - `target_q`
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
        return self._update_elements(ElementType.STATIC_VAR_COMPENSATOR, df, **kwargs)

    def update_hvdc_lines(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update HVDC lines with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Attributes that can be updated are:

        - `converters_mode`
        - `target_p`
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
        return self._update_elements(ElementType.HVDC_LINE, df, **kwargs)

    def update_lines(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
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
        return self._update_elements(ElementType.LINE, df, **kwargs)

    def update_2_windings_transformers(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
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
        return self._update_elements(ElementType.TWO_WINDINGS_TRANSFORMER, df, **kwargs)

    def update_ratio_tap_changers(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
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
        return self._update_elements(ElementType.RATIO_TAP_CHANGER, df, **kwargs)

    def update_ratio_tap_changer_steps(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
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
        return self._update_elements(ElementType.RATIO_TAP_CHANGER_STEP, df, **kwargs)

    def update_phase_tap_changers(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
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
        return self._update_elements(ElementType.PHASE_TAP_CHANGER, df, **kwargs)

    def update_phase_tap_changer_steps(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
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
        return self._update_elements(ElementType.PHASE_TAP_CHANGER_STEP, df, **kwargs)

    def update_shunt_compensators(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
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
        return self._update_elements(ElementType.SHUNT_COMPENSATOR, df, **kwargs)

    def update_linear_shunt_compensator_sections(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
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
        return self._update_elements(ElementType.LINEAR_SHUNT_COMPENSATOR_SECTION, df, **kwargs)

    def update_non_linear_shunt_compensator_sections(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
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
        return self._update_elements(ElementType.NON_LINEAR_SHUNT_COMPENSATOR_SECTION, df, **kwargs)

    def update_busbar_sections(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """Update phase tap changers with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame

        """
        return self._update_elements(ElementType.BUSBAR_SECTION, df, **kwargs)

    def update_voltage_levels(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
                Update voltage levels with data provided as a :class:`~pandas.DataFrame` or as named arguments.

                Attributes that can be updated are :

                - `high_voltage_limit`
                - `low_voltage_limit`
                - `nominal_v`

                See Also:
                    :meth:`get_voltage_levels`

                Args:
                    df: the data to be updated, as a data frame.
                    **kwargs: the data to be updated, as named arguments.
                        Arguments can be single values or any type of sequence.
                        In the case of sequences, all arguments must have the same length.
                """
        return self._update_elements(ElementType.VOLTAGE_LEVEL, df, **kwargs)

    def update_substations(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
                Update substations with data provided as a :class:`~pandas.DataFrame` or as named arguments.

                Attributes that can be updated are :

                - `TSO`
                - `country`

                See Also:
                    :meth:`get_substations`

                Args:
                    df: the data to be updated, as a data frame.
                    **kwargs: the data to be updated, as named arguments.
                        Arguments can be single values or any type of sequence.
                        In the case of sequences, all arguments must have the same length.
                """
        return self._update_elements(ElementType.SUBSTATION, df, **kwargs)

    def get_working_variant_id(self) -> str:
        """
        The current working variant ID.

        Returns:
            the id of the currently selected variant.
        """
        return _pp.get_working_variant_id(self._handle)

    def clone_variant(self, src: str, target: str, may_overwrite: bool = True) -> None:
        """
        Creates a copy of the source variant

        Args:
            src: variant to copy
            target: id of the new variant that will be a copy of src
            may_overwrite: indicates if the target can be overwritten when it already exists
        """
        _pp.clone_variant(self._handle, src, target, may_overwrite)

    def set_working_variant(self, variant: str) -> None:
        """
        Changes the working variant. The provided variant ID must correspond
        to an existing variant, for example created by a call to `clone_variant`.

        Args:
            variant: id of the variant selected (it must exist)
        """
        _pp.set_working_variant(self._handle, variant)

    def remove_variant(self, variant: str) -> None:
        """
        Removes a variant from the network.

        Args:
            variant: id of the variant to be deleted
        """
        _pp.remove_variant(self._handle, variant)

    def get_variant_ids(self) -> _List[str]:
        """
        Get the list of existing variant IDs.

        Returns:
            all the ids of the existing variants
        """
        return _pp.get_variant_ids(self._handle)

    def get_current_limits(self, all_attributes: bool = False, attributes: _List[str] = None) -> _DataFrame:
        """
        Get the list of all current limits on the network paired with their branch id.
        get_current_limits is deprecated, use get_operational_limits instead

        Args:
            all_attributes (bool, optional): flag for including all attributes in the dataframe, default is false
            attributes (List[str], optional): attributes to include in the dataframe. The 2 parameters are mutually exclusive. If no parameter is specified, the dataframe will include the default attributes.

        Returns:
            all current limits on the network
        """
        warnings.warn("get_current_limits is deprecated, use get_operational_limits instead", DeprecationWarning)
        limits = self.get_operational_limits(all_attributes, attributes)
        current_limits = limits[limits['element_type'].isin(['LINE', 'TWO_WINDINGS_TRANSFORMER']) & (limits['type'] == 'CURRENT')]
        current_limits.index.rename('branch_id', inplace=True)
        current_limits.set_index('name', append=True, inplace=True)
        return current_limits[['side', 'value', 'acceptable_duration', 'is_fictitious']]

    def get_operational_limits(self, all_attributes: bool = False, attributes: _List[str] = None) -> _DataFrame:
        """
        Get the list of operational limits.

        The resulting dataframe, depending on the parameters, will have some of the following columns:

          - **element_id**: Identifier of the network element on which this limit applies (could be for example
            a line or a transformer). This is the index column.
          - **element_type**: Type of the network element on which this limit applies (LINE, TWO_WINDINGS_TRANSFORMER,
            THREE_WINDINGS_TRANSFORMER, DANGLING_LINE)
          - **side**:       The side of the element on which this limit applies (ONE, TWO, THREE)
          - **name**:       The name of the limit
          - **type**:       The type of the limit (CURRENT, ACTIVE_POWER, APPARENT_POWER)
          - **value**:      The value of the limit
          - **acceptable_duration**: The duration, in seconds, for which the element can securely be
            operated under the limit value. By convention, the value -1 represents an infinite duration.
          - **is_fictitious**: true if this limit is fictitious

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes:     attributes to include in the dataframe. The 2 parameters are mutually
                            exclusive. If no parameter is specified, the dataframe will include the default attributes.

        Returns:
            All limits on the network
        """
        return self.get_elements(ElementType.OPERATIONAL_LIMITS, all_attributes, attributes)

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

    def merge(self, *networks: Network) -> None:
        return _pp.merge(self._handle, [net._handle for net in networks])

    def _create_elements(self, element_type: ElementType, dfs: _List[_Optional[_DataFrame]], **kwargs: _ArrayLike) -> None:
        metadata = _pp.get_network_elements_creation_dataframes_metadata(element_type)
        c_dfs: _List[_Optional[_pp.Dataframe]] = []
        dfs[0] = _adapt_df_or_kwargs(metadata[0], dfs[0], **kwargs)
        for i, df in enumerate(dfs):
            if df is None:
                c_dfs.append(None)
            else:
                c_dfs.append(_create_c_dataframe(df, metadata[i]))
        _pp.create_element(self._handle, c_dfs, element_type)

    def create_substations(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates substations.

        Data may be provided as a dataframe or as keyword arguments.
        In the latter case, all arguments must have the same length.

        Valid attributes are:
          - id
          - name
          - country
          - tso


        Args:
            df: Attributes as a dataframe.
            **kwargs: Attributes as keyword arguments.
        """
        return self._create_elements(ElementType.SUBSTATION, [df], **kwargs)

    def create_generators(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates generators.

        Data may be provided as a dataframe or as keyword arguments.
        In the latter case, all arguments must have the same length.

        Expected attributes are:
          - id
          - voltage_level_id
          - bus_id
          - connectable_bus_id
          - node
          - energy_source
          - max_p
          - min_p
          - target_p
          - target_q
          - rated_s
          - target_v
          - voltage_regulator_on

        Args:
            df: Attributes as dataframe.
            **kwargs: Attributes as keyword arguments.

        """
        self._create_elements(ElementType.GENERATOR, [df], **kwargs)

    def create_busbar_sections(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates bus bar sections.

        Data may be provided as a dataframe or as keyword arguments.
        In the latter case, all arguments must have the same length.

        Valid attributes are:
          - id
          - voltage_level_id
          - node
          - name

        Args:
            df: Attributes as a dataframe.
            **kwargs: Attributes as keyword arguments.
        """
        self._create_elements(ElementType.BUSBAR_SECTION, [df], **kwargs)

    def create_buses(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates buses.

        Data may be provided as a dataframe or as keyword arguments.
        In the latter case, all arguments must have the same length.

        Valid attributes are:
          - id
          - voltage_level_id
          - name

        Args:
            df: Attributes as a dataframe.
            **kwargs: Attributes as keyword arguments.
        """
        return self._create_elements(ElementType.BUS, [df], **kwargs)

    def create_loads(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        create loads on a network

        Args:
            df: dataframe of the loads creation data
        """
        return self._create_elements(ElementType.LOAD, [df], **kwargs)

    def create_batteries(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates loads.

        Data may be provided as a dataframe or as keyword arguments.
        In the latter case, all arguments must have the same length.

        Valid attributes are:
          - id
          - voltage_level_id
          - bus_id
          - connectable_bus_id
          - node
          - name
          - type
          - p0
          - q0

        Args:
            df: Attributes as a dataframe.
            **kwargs: Attributes as keyword arguments.
        """
        return self._create_elements(ElementType.BATTERY, [df], **kwargs)

    def create_dangling_lines(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates dangling lines.

        Data may be provided as a dataframe or as keyword arguments.
        In the latter case, all arguments must have the same length.

        Valid attributes are:
          - id
          - voltage_level_id
          - bus_id
          - connectable_bus_id
          - node
          - name
          - p0
          - q0
          - r
          - x
          - g
          - b

        Args:
            df: Attributes as a dataframe.
            **kwargs: Attributes as keyword arguments.
        """
        return self._create_elements(ElementType.DANGLING_LINE, [df], **kwargs)

    def create_lcc_converter_stations(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates LCC converter stations.

        Data may be provided as a dataframe or as keyword arguments.
        In the latter case, all arguments must have the same length.

        Valid attributes are:
          - id
          - voltage_level_id
          - bus_id
          - connectable_bus_id
          - node
          - name
          - power_factor
          - loss_factor

        Args:
            df: Attributes as a dataframe.
            **kwargs: Attributes as keyword arguments.
        """
        return self._create_elements(ElementType.LCC_CONVERTER_STATION, [df], **kwargs)

    def create_vsc_converter_stations(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates VSC converter stations.

        Data may be provided as a dataframe or as keyword arguments.
        In the latter case, all arguments must have the same length.

        Valid attributes are:
          - id
          - voltage_level_id
          - bus_id
          - connectable_bus_id
          - node
          - name
          - target_v
          - target_q
          - loss_factor
          - voltage_regulator_on

        Args:
            df: Attributes as a dataframe.
            **kwargs: Attributes as keyword arguments.
        """
        return self._create_elements(ElementType.VSC_CONVERTER_STATION, [df], **kwargs)

    def create_static_var_compensators(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates static var compensators.

        Data may be provided as a dataframe or as keyword arguments.
        In the latter case, all arguments must have the same length.

        Valid attributes are:
          - id
          - voltage_level_id
          - bus_id
          - connectable_bus_id
          - node
          - name
          - b_max
          - b_min
          - regulation_mode
          - target_v
          - target_q

        Args:
            df: Attributes as a dataframe.
            **kwargs: Attributes as keyword arguments.
        """
        return self._create_elements(ElementType.STATIC_VAR_COMPENSATOR, [df], **kwargs)

    def create_lines(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates lines.

        Data may be provided as a dataframe or as keyword arguments.
        In the latter case, all arguments must have the same length.

        Valid attributes are:
          - id
          - voltage_level1_id
          - bus1_id
          - connectable_bus1_id
          - node1
          - voltage_level2_id
          - bus2_id
          - connectable_bus2_id
          - node2
          - name
          - b1
          - b2
          - g1
          - g2
          - r
          - x

        Args:
            df: Attributes as a dataframe.
            **kwargs: Attributes as keyword arguments.
        """
        return self._create_elements(ElementType.LINE, [df], **kwargs)

    def create_2_windings_transformers(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates 2 windings transformers.

        Data may be provided as a dataframe or as keyword arguments.
        In the latter case, all arguments must have the same length.

        Valid attributes are:
          - id
          - voltage_level1_id
          - bus1_id
          - connectable_bus1_id
          - node1
          - voltage_level2_id
          - bus2_id
          - connectable_bus2_id
          - node2
          - name
          - rated_u1
          - rated_u2
          - rated_s
          - b
          - g
          - r
          - x

        Args:
            df: Attributes as a dataframe.
            **kwargs: Attributes as keyword arguments.
        """
        return self._create_elements(ElementType.TWO_WINDINGS_TRANSFORMER, [df], **kwargs)

    def create_shunt_compensators(self, shunt_df: _DataFrame,
                                  linear_model_df: _Optional[_DataFrame] = None,
                                  non_linear_model_df: _Optional[_DataFrame] = None,
                                  **kwargs: _ArrayLike) -> None:
        """
        create shunt compensators on a network

        Args:
            df: dataframe of the shunt compensators creation data
        """
        if linear_model_df is None:
            linear_model_df = pd.DataFrame()
        if non_linear_model_df is None:
            non_linear_model_df = pd.DataFrame()
        dfs: _List[_Optional[_DataFrame]] = [shunt_df, linear_model_df, non_linear_model_df]
        return self._create_elements(ElementType.SHUNT_COMPENSATOR, dfs, **kwargs)

    def create_switches(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates switches.

        Data may be provided as a dataframe or as keyword arguments.
        In the latter case, all arguments must have the same length.

        Valid attributes are:
          - id
          - voltage_level_id
          - bus1_id
          - bus2_id
          - node1
          - node2
          - name
          - kind
          - open
          - retained
          - fictitious

        Args:
            df: Attributes as a dataframe.
            **kwargs: Attributes as keyword arguments.
        """
        return self._create_elements(ElementType.SWITCH, [df], **kwargs)

    def create_voltage_levels(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates voltage levels.

        Data may be provided as a dataframe or as keyword arguments.
        In the latter case, all arguments must have the same length.

        Valid attributes are:
          - id
          - substation_id
          - name
          - high_voltage_limit
          - low_voltage_limit
          - nominal_v
          - topology_kind

        Args:
            df: Attributes as a dataframe.
            **kwargs: Attributes as keyword arguments.
        """
        return self._create_elements(ElementType.VOLTAGE_LEVEL, [df], **kwargs)

    def create_ratio_tap_changers(self, rtc_df: _DataFrame, steps_df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        create ratio tap changers on a network

        Args:
            df: dataframe of the ratio tap changers creation data
        """
        return self._create_elements(ElementType.RATIO_TAP_CHANGER, [rtc_df, steps_df], **kwargs)

    def create_phase_tap_changers(self, ptc_df: _DataFrame, steps_df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        create phase tap changers on a network

        Args:
            df: dataframe of the phase tap changers creation data
        """
        return self._create_elements(ElementType.PHASE_TAP_CHANGER, [ptc_df, steps_df], **kwargs)

    def create_hvdc_lines(self, df: _DataFrame, **kwargs: _ArrayLike) -> None:
        """
        Creates HVDC lines.

        Data may be provided as a dataframe or as keyword arguments.
        In the latter case, all arguments must have the same length.

        Valid attributes are:
          - id
          - name
          - converter_station1_id
          - converter_station2_id
          - max_p
          - converters_mode
          - target_p
          - r
          - nominal_v

        Args:
            df: Attributes as a dataframe.
            **kwargs: Attributes as keyword arguments.
        """
        return self._create_elements(ElementType.HVDC_LINE, [df], **kwargs)

    def create_operational_limits(self, df: _DataFrame, **kwargs: _ArrayLike) -> None:
        """
        Creates operational limits.

        Data may be provided as a dataframe or as keyword arguments.
        In the latter case, all arguments must have the same length.

        Valid attributes are:
          - **element_id**: the ID of the network element on which we want to create new limits
          - **element_type**: the type of the network element (LINE, TWO_WINDINGS_TRANSFORMER,
            THREE_WINDINGS_TRANSFORMER, DANGLING_LINE)
          - **side**: the side of the network element where we want to create new limits (ONE, TWO, THREE)
          - **name**: the name of the limit
          - **type**: the type of limit to be created (CURRENT, APPARENT_POWER, ACTIVE_POWER)
          - **value**: the value of the limit in A, MVA or MW
          - **acceptable_duration**: the maximum number of seconds during which we can operate under that limit
          - **is_fictitious** : fictitious limit ?

        For each location of the network defined by a couple (element_id, side):
          - if operational limits already exist, they will be replaced
          - multiple limits may be defined, typically with different acceptable_duration
          - you can only define ONE permanent limit, identified by an acceptable_duration of -1

        Args:
            df: Attributes as a dataframe.
            **kwargs: Attributes as keyword arguments.
        """
        df['acceptable_duration'] = df['acceptable_duration'].map(lambda x: -1 if x == Inf else int(x))
        return self._create_elements(ElementType.OPERATIONAL_LIMITS, [df], **kwargs)

    def get_validation_level(self) -> ValidationLevel:
        """
        The network's validation level.

        This it the network validation level as computed by validation checks.

        Returns:
            the ValidationLevel.
        """
        return _pp.get_validation_level(self._handle)

    def validate(self) -> ValidationLevel:
        """
        Validate the network.

        The validation will raise an exception if any check is not consistent with the
        configured minimum validation level.

        Returns:
            the computed ValidationLevel, which may be higher than the configured minimum level.

        Raises:
            pypowsybl.PyPowsyblError: if any validation check is not consistent
                                      with the configured minimum validation level.
        """
        return _pp.validate(self._handle)

    def set_min_validation_level(self, validation_level: ValidationLevel) -> None:
        """
        Set the minimum validation level for the network.

        Args:
            validation_level (ValidationLevel): the validation level

        Raises:
            pypowsybl.PyPowsyblError: if any validation check is not consistent
                                      with the new minimum validation level.
        """
        _pp.set_min_validation_level(self._handle, validation_level)


def _create_network(name: str, network_id: str = '') -> Network:
    return Network(_pp.create_network(name, network_id))


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

def create_eurostag_tutorial_example1_with_power_limits_network() -> Network:
    """
    Create an instance of example 1 network of Eurostag tutorial with Power limits

    Returns:
        a new instance of example 1 network of Eurostag tutorial with Power limits
    """
    return _create_network('eurostag_tutorial_example1_with_power_limits')


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

    Returns:
         the list of supported import formats
    """
    return _pp.get_network_import_formats()


def get_export_formats() -> _List[str]:
    """
    Get list of supported export formats

    Returns:
        the list of supported export formats
    """
    return _pp.get_network_export_formats()


def get_import_parameters(fmt: str) -> _DataFrame:
    """
    Supported import parameters for a given format.

    Args:
       fmt (str): the format

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
    series_array = _pp.create_importer_parameters_series_array(fmt)
    return _create_data_frame_from_series_array(series_array)


def get_export_parameters(fmt: str) -> _DataFrame:
    """
    Get supported export parameters infos for a given format

    Args:
       fmt (str): the format

    Returns:
        export parameters data frame
    """
    series_array = _pp.create_exporter_parameters_series_array(fmt)
    return _create_data_frame_from_series_array(series_array)


def load(file: str, parameters: _Dict[str, str] = None) -> Network:
    """
    Load a network from a file. File should be in a supported format.

    Args:
       file (str): a file
       parameters (dict, optional): a map of parameters

    Returns:
        a network
    """
    if parameters is None:
        parameters = {}
    return Network(_pp.load_network(file, parameters))


def load_from_string(file_name: str, file_content: str, parameters: _Dict[str, str] = None) -> Network:
    """
    Load a network from a string. File content should be in a supported format.

    Args:
       file_name (str): file name
       file_content (str): file content
       parameters (dict, optional): a map of parameters

    Returns:
        a network
    """
    if parameters is None:
        parameters = {}
    return Network(_pp.load_network_from_string(file_name, file_content, parameters))
