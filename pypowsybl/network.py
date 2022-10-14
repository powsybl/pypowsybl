#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
from __future__ import annotations  # Necessary for type alias like _DataFrame to work with sphinx

from os import PathLike as _PathLike
import sys as _sys
import datetime as _datetime
from datetime import timezone as _timezone
import warnings
from typing import (
    Sequence as _Sequence,
    List as _List,
    Set as _Set,
    Dict as _Dict,
    Optional as _Optional,
    Union as _Union,
    TYPE_CHECKING as _TYPE_CHECKING
)

from numpy import Inf
from numpy.typing import ArrayLike as _ArrayLike
from pandas import DataFrame as _DataFrame
import networkx as _nx
import pandas as pd

import pypowsybl._pypowsybl as _pp
from pypowsybl._pypowsybl import ElementType
from pypowsybl._pypowsybl import ValidationLevel
from pypowsybl.util import create_data_frame_from_series_array as _create_data_frame_from_series_array
from pypowsybl.utils.dataframes import (
    _adapt_df_or_kwargs,
    _create_c_dataframe,
    _create_properties_c_dataframe,
    _adapt_properties_kwargs,
    _get_c_dataframes
)
from pypowsybl.report import Reporter as _Reporter

# Type definitions
if _TYPE_CHECKING:
    ParamsDict = _Optional[_Dict[str, str]]
    PathOrStr = _Union[str, _PathLike]


def _series_metadata_repr(self: _pp.SeriesMetadata) -> str:
    return f'SeriesMetadata(name={self.name}, type={self.type}, ' \
           f'is_index={self.is_index}, is_modifiable={self.is_modifiable}, is_default={self.is_default})'


_pp.SeriesMetadata.__repr__ = _series_metadata_repr  # type: ignore


def _path_to_str(path: PathOrStr) -> str:
    if isinstance(path, str):
        return path
    return path.__fspath__()


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
        self._case_date = _datetime.datetime.fromtimestamp(att.case_date, _timezone.utc)

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
        self._handle = _pp.load_network_from_string('tmp.xiidm', xml, {}, None)

    def open_switch(self, id: str) -> bool:
        return _pp.update_switch_position(self._handle, id, True)

    def close_switch(self, id: str) -> bool:
        return _pp.update_switch_position(self._handle, id, False)

    def connect(self, id: str) -> bool:
        return _pp.update_connectable_status(self._handle, id, True)

    def disconnect(self, id: str) -> bool:
        return _pp.update_connectable_status(self._handle, id, False)

    def dump(self, file: PathOrStr, format: str = 'XIIDM', parameters: ParamsDict = None, reporter: _Reporter = None) -> None:
        """
        Save a network to a file using the specified format.

        Basic compression formats are also supported:
        for example if file name ends with '.gz', the resulting files will be gzipped.

        Args:
            file:       path to the exported file
            format:     format to save the network, defaults to 'XIIDM'
            parameters: a dictionary of export parameters
            reporter:   the reporter to be used to create an execution report, default is None (no report)

        Examples:
            Various usage examples:

            .. code-block:: python

                network.dump('network.xiidm')
                network.dump('network.xiidm.gz')  # produces a gzipped file
                network.dump('/path/to/network.uct', format='UCTE')
        """
        file = _path_to_str(file)
        if parameters is None:
            parameters = {}
        _pp.dump_network(self._handle, file, format, parameters, None if reporter is None else reporter._reporter_model)  # pylint: disable=protected-access

    def dump_to_string(self, format: str = 'XIIDM', parameters: ParamsDict = None, reporter: _Reporter = None) -> str:
        """
        Save a network to a string using a specified format.

        Args:
            format:     format to export, only support mono file type, defaults to 'XIIDM'
            parameters: a dictionary of export parameters
            reporter:   the reporter to be used to create an execution report, default is None (no report)

        Returns:
            A string representing this network
        """
        if parameters is None:
            parameters = {}
        return _pp.dump_network_to_string(self._handle, format, parameters, None if reporter is None else reporter._reporter_model) # pylint: disable=protected-access

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

    def write_single_line_diagram_svg(self, container_id: str, svg_file: PathOrStr) -> None:
        """
        Create a single line diagram in SVG format from a voltage level or a substation and write to a file.

        Args:
            container_id: a voltage level id or a substation id
            svg_file: a svg file path
        """
        svg_file = _path_to_str(svg_file)
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

    def write_network_area_diagram_svg(self, svg_file: PathOrStr, voltage_level_ids: _Union[str, _List[str]] = None, depth: int = 0) -> None:
        """
        Create a network area diagram in SVG format and write it to a file.

        Args:
            svg_file: a svg file path
            voltage_level_id: the voltage level ID, center of the diagram (None for the full diagram)
            depth: the diagram depth around the voltage level
        """
        svg_file = _path_to_str(svg_file)
        if voltage_level_ids is None:
            voltage_level_ids = []
        if isinstance(voltage_level_ids, str):
            voltage_level_ids = [voltage_level_ids]
        _pp.write_network_area_diagram_svg(self._handle, svg_file, voltage_level_ids, depth)

    def get_network_area_diagram(self, voltage_level_ids: _Union[str, _List[str]] = None, depth: int = 0) -> Svg:
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
        if isinstance(voltage_level_ids, str):
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
            a network elements dataframe for the specified element type
        """
        filter_attributes = _pp.FilterAttributesType.DEFAULT_ATTRIBUTES
        if all_attributes:
            filter_attributes = _pp.FilterAttributesType.ALL_ATTRIBUTES
        elif attributes is not None:
            filter_attributes = _pp.FilterAttributesType.SELECTION_ATTRIBUTES
        if attributes is None:
            attributes = []
        if all_attributes and len(attributes) > 0:
            raise RuntimeError('parameters "all_attributes" and "attributes" are mutually exclusive')

        if kwargs:
            metadata = _pp.get_network_elements_dataframe_metadata(element_type)
            df = _adapt_df_or_kwargs(metadata, None, **kwargs)
            elements_array = _create_c_dataframe(df, metadata)
        else:
            elements_array = None

        series_array = _pp.create_network_elements_series_array(self._handle, element_type, filter_attributes, attributes, elements_array)
        result = _create_data_frame_from_series_array(series_array)
        if attributes:
            result = result[attributes]
        return result

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
              - **max_q**: the maximum reactive value for the generator only if reactive_limits_kind is MIN_MAX (MVar)
              - **min_q**: the minimum reactive value for the generator only if reactive_limits_kind is MIN_MAX (MVar)
              - **max_q_at_target_p** (optional): the maximum reactive value for the generator for the target p specified (MVar)
              - **min_q_at_target_p** (optional): the minimum reactive value for the generator for the target p specified (MVar)
              - **max_q_at_p** (optional): the maximum reactive value for the generator at current p (MVar)
              - **min_q_at_p** (optional): the minimum reactive value for the generator at current p (MVar)
              - **reactive_limits_kind**: type of the reactive limit of the generator (can be MIN_MAX, CURVE or NONE)
              - **target_v**: the target voltage magnitude value for the generator (in kV)
              - **target_q**: the target reactive value for the generator (in MVAr)
              - **voltage_regulator_on**: ``True`` if the generator regulates voltage
              - **regulated_element_id**: the ID of the network element where voltage is regulated
              - **p**: the actual active production of the generator (``NaN`` if no loadflow has been computed)
              - **q**: the actual reactive production of the generator (``NaN`` if no loadflow has been computed)
              - **i**: the current on the load, ``NaN`` if no loadflow has been computed (in A)
              - **voltage_level_id**: at which substation this generator is connected
              - **bus_id**: bus where this generator is connected
              - **bus_breaker_bus_id** (optional): bus of the bus-breaker view where this generator is connected
              - **node**  (optional): node where this generator is connected, in node-breaker voltage levels
              - **connected**: ``True`` if the generator is connected to a bus
              - **fictitious** (optional): ``True`` if the generator is part of the model and not of the actual network

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
            The resulting dataframe, depending on the parameters, will include the following columns:

              - **type**: type of load
              - **p0**: the active load consumption setpoint (MW)
              - **q0**: the reactive load consumption setpoint  (MVAr)
              - **p**: the result active load consumption, it is ``NaN`` is not loadflow has been computed (MW)
              - **q**: the result reactive load consumption, it is ``NaN`` is not loadflow has been computed (MVAr)
              - **i**: the current on the load, ``NaN`` if no loadflow has been computed (in A)
              - **voltage_level_id**: at which substation this load is connected
              - **bus_id**: bus where this load is connected
              - **bus_breaker_bus_id** (optional): bus of the bus-breaker view where this load is connected
              - **node**  (optional): node where this load is connected, in node-breaker voltage levels
              - **connected**: ``True`` if the load is connected to a bus
              - **fictitious** (optional): ``True`` if the load is part of the model and not of the actual network

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

        Notes:
            The resulting dataframe, depending on the parameters, will include the following columns:

              - **name**: type of load
              - **max_p**: the maximum active value for the battery  (MW)
              - **min_p**: the minimum active value for the battery  (MW)
              - **min_q**: the maximum reactive value for the battery only if reactive_limits_kind is MIN_MAX (MVar)
              - **max_q**: the minimum reactive value for the battery only if reactive_limits_kind is MIN_MAX (MVar)
              - **target_p**: The active power setpoint  (MW)
              - **target_q**: The reactive power setpoint  (MVAr)
              - **p**: the result active battery consumption, it is ``NaN`` is not loadflow has been computed (MW)
              - **q**: the result reactive battery consumption, it is ``NaN`` is not loadflow has been computed (MVAr)
              - **i**: the current on the battery, ``NaN`` if no loadflow has been computed (in A)
              - **voltage_level_id**: at which substation this load is connected
              - **bus_id**: bus where this load is connected
              - **bus_breaker_bus_id** (optional): bus of the bus-breaker view where this battery is connected
              - **node**  (optional): node where this battery is connected, in node-breaker voltage levels
              - **connected**: ``True`` if the battery is connected to a bus
              - **fictitious** (optional): ``True`` if the battery is part of the model and not of the actual network

            This dataframe is indexed on the battery ID.
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
            The resulting dataframe, depending on the parameters, will include the following columns:

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
            - **voltage_level1_id**: voltage level where the line is connected, on side 1
            - **voltage_level2_id**: voltage level where the line is connected, on side 2
            - **bus1_id**: bus where this line is connected, on side 1
            - **bus2_id**: bus where this line is connected, on side 2
            - **bus_breaker_bus1_id** (optional): bus of the bus-breaker view where this line is connected, on side 1
            - **bus_breaker_bus2_id** (optional): bus of the bus-breaker view where this line is connected, on side 2
            - **node1** (optional): node where this line is connected on side 1, in node-breaker voltage levels
            - **node2** (optional): node where this line is connected on side 2, in node-breaker voltage levels
            - **connected1**: ``True`` if the side "1" of the line is connected to a bus
            - **connected2**: ``True`` if the side "2" of the line is connected to a bus
              - **fictitious** (optional): ``True`` if the line is part of the model and not of the actual network

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
            The resulting dataframe, depending on the parameters, will include the following columns:

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
            - **voltage_level1_id**: voltage level where the line is connected, on side 1
            - **voltage_level2_id**: voltage level where the line is connected, on side 2
            - **bus1_id**: bus where this line is connected, on side 1
            - **bus2_id**: bus where this line is connected, on side 2
            - **bus_breaker_bus1_id** (optional): bus of the bus-breaker view where this line is connected, on side 1
            - **bus_breaker_bus2_id** (optional): bus of the bus-breaker view where this line is connected, on side 2
            - **node1** (optional): node where this line is connected on side 1, in node-breaker voltage levels
            - **node2** (optional): node where this line is connected on side 2, in node-breaker voltage levels
            - **connected1**: ``True`` if the side "1" of the transformer is connected to a bus
            - **connected2**: ``True`` if the side "2" of the transformer is connected to a bus
            - **fictitious** (optional): ``True`` if the transformer is part of the model and not of the actual network

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
            The resulting dataframe, depending on the parameters, will include the following columns:

              - **model_type**:
              - **max_section_count**: The maximum number of sections that may be switched on
              - **section_count**: The current number of section that may be switched on
              - **p**: the active flow on the shunt, ``NaN`` if no loadflow has been computed (in MW)
              - **q**: the reactive flow on the shunt, ``NaN`` if no loadflow has been computed  (in MVAr)
              - **i**: the current in the shunt, ``NaN`` if no loadflow has been computed  (in A)
              - **voltage_level_id**: at which substation the shunt is connected
              - **bus_id**: bus where this shunt is connected
              - **bus_breaker_bus_id** (optional): bus of the bus-breaker view where this shunt is connected
              - **node**  (optional): node where this shunt is connected, in node-breaker voltage levels
              - **connected**: ``True`` if the shunt is connected to a bus
              - **fictitious** (optional): ``True`` if the shunt is part of the model and not of the actual network


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
            The resulting dataframe, depending on the parameters, will include the following columns:

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
            The resulting dataframe, depending on the parameters, will include the following columns:

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
              - **bus_id**: bus where this line is connected
              - **bus_breaker_bus_id** (optional): bus of the bus-breaker view where this line is connected
              - **node**  (optional): node where this line is connected, in node-breaker voltage levels
              - **connected**: ``True`` if the dangling line is connected to a bus
              - **fictitious** (optional): ``True`` if the dangling line is part of the model and not of the actual network

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
            The resulting dataframe, depending on the parameters, will include the following columns:

              - **power_factor**: the power factor
              - **loss_factor**: the loss factor
              - **p**: active flow on the LCC converter station, ``NaN`` if no loadflow has been computed (in MW)
              - **q**: the reactive flow on the LCC converter station, ``NaN`` if no loadflow has been computed  (in MVAr)
              - **i**: The current on the LCC converter station, ``NaN`` if no loadflow has been computed (in A)
              - **voltage_level_id**: at which substation the LCC converter station is connected
              - **bus_id**: bus where this station is connected
              - **bus_breaker_bus_id** (optional): bus of the bus-breaker view where this station is connected
              - **node**  (optional): node where this station is connected, in node-breaker voltage levels
              - **connected**: ``True`` if the LCC converter station is connected to a bus
              - **fictitious** (optional): ``True`` if the LCC converter is part of the model and not of the actual network

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
            The resulting dataframe, depending on the parameters, will include the following columns:

              - **loss_factor**: correspond to the loss of power due to ac dc conversion
              - **target_v**: The voltage setpoint
              - **target_q**: The reactive power setpoint
              - **max_q**: the maximum reactive value for the generator only if reactive_limits_kind is MIN_MAX (MVar)
              - **min_q**: the minimum reactive value for the generator only if reactive_limits_kind is MIN_MAX (MVar)
              - **max_q_at_p** (optional): the maximum reactive value for the generator at current p (MVar)
              - **min_q_at_p** (optional): the minimum reactive value for the generator at current p (MVar)
              - **reactive_limits_kind**: type of the reactive limit of the vsc converter station (can be MIN_MAX, CURVE or NONE)
              - **voltage_regulator_on**: The voltage regulator status
              - **regulated_element_id**: The ID of the network element where voltage is regulated
              - **p**: active flow on the VSC  converter station, ``NaN`` if no loadflow has been computed (in MW)
              - **q**: the reactive flow on the VSC converter station, ``NaN`` if no loadflow has been computed  (in MVAr)
              - **i**: The current on the VSC converter station, ``NaN`` if no loadflow has been computed (in A)
              - **voltage_level_id**: at which substation the VSC converter station is connected
              - **bus_id**: bus where this station is connected
              - **bus_breaker_bus_id** (optional): bus of the bus-breaker view where this station is connected
              - **node**  (optional): node where this station is connected, in node-breaker voltage levels
              - **connected**: ``True`` if the VSC converter station is connected to a bus
              - **fictitious** (optional): ``True`` if the VSC converter is part of the model and not of the actual network

            This dataframe is indexed by the id of the VSC converter

        Examples:

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_vsc_converter_stations()

            will output something like:

            ======== =========== ================ ======================= ==================== ==================== ====== ========= ========== ================ ======= =========
            \        loss_factor voltage_setpoint reactive_power_setpoint voltage_regulator_on regulated_element_id      p         q          i voltage_level_id  bus_id connected
            ======== =========== ================ ======================= ==================== ==================== ====== ========= ========== ================ ======= =========
            id
                VSC1         1.1            400.0                   500.0                 True                 VSC1  10.11 -512.0814 739.269871            S1VL2 S1VL2_0      True
                VSC2         1.1              0.0                   120.0                False                 VSC2  -9.89 -120.0000 170.031658            S2VL1 S2VL1_0      True
            ======== =========== ================ ======================= ==================== ==================== ====== ========= ========== ================ ======= =========

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_vsc_converter_stations(all_attributes=True)

            will output something like:

            ======== =========== ================ ======================= ==================== ==================== ====== ========= ========== ================ ======= =========
            \        loss_factor         target_v                target_q voltage_regulator_on regulated_element_id      p         q          i voltage_level_id  bus_id connected
            ======== =========== ================ ======================= ==================== ==================== ====== ========= ========== ================ ======= =========
            id
                VSC1         1.1            400.0                   500.0                 True                 VSC1  10.11 -512.0814 739.269871            S1VL2 S1VL2_0      True
                VSC2         1.1              0.0                   120.0                False                 VSC2  -9.89 -120.0000 170.031658            S2VL1 S2VL1_0      True
            ======== =========== ================ ======================= ==================== ==================== ====== ========= ========== ================ ======= =========

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
            The resulting dataframe, depending on the parameters, will include the following columns:

              - **b_min**: the minimum susceptance
              - **b_max**: the maximum susceptance
              - **target_v**: The voltage setpoint
              - **target_q**: The reactive power setpoint
              - **regulation_mode**: The regulation mode
              - **regulated_element_id**: The ID of the network element where voltage is regulated
              - **p**: active flow on the var compensator, ``NaN`` if no loadflow has been computed (in MW)
              - **q**: the reactive flow on the var compensator, ``NaN`` if no loadflow has been computed  (in MVAr)
              - **i**: The current on the var compensator, ``NaN`` if no loadflow has been computed (in A)
              - **voltage_level_id**: at which substation the var compensator is connected
              - **bus_id**: bus where this SVC is connected
              - **bus_breaker_bus_id** (optional): bus of the bus-breaker view where this SVC is connected
              - **node**  (optional): node where this SVC is connected, in node-breaker voltage levels
              - **connected**: ``True`` if the var compensator is connected to a bus
              - **fictitious** (optional): ``True`` if the var compensator is part of the model and not of the actual network

            This dataframe is indexed by the id of the var compensator

        Examples:

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_static_var_compensators()

            will output something like:

            ======== ===== ===== ================ ======================= =============== ==================== === ======== === ================ ======= =========
            \        b_min b_max         target_v                target_q regulation_mode regulated_element_id  p        q   i  voltage_level_id  bus_id connected
            ======== ===== ===== ================ ======================= =============== ==================== === ======== === ================ ======= =========
            id
                 SVC -0.05  0.05            400.0                     NaN         VOLTAGE                  SVC NaN -12.5415 NaN            S4VL1 S4VL1_0      True
            ======== ===== ===== ================ ======================= =============== ==================== === ======== === ================ ======= =========

            .. code-block:: python

                net = pp.network.create_four_substations_node_breaker_network()
                net.get_static_var_compensators(all_attributes=True)

            will output something like:

            ======== ===== ===== ================ ======================= =============== ==================== === ======== === ================ ======= =========
            \        b_min b_max voltage_setpoint reactive_power_setpoint regulation_mode regulated_element_id  p        q   i  voltage_level_id  bus_id connected
            ======== ===== ===== ================ ======================= =============== ==================== === ======== === ================ ======= =========
            id
                 SVC -0.05  0.05            400.0                     NaN         VOLTAGE                  SVC NaN -12.5415 NaN            S4VL1 S4VL1_0      True
            ======== ===== ===== ================ ======================= =============== ==================== === ======== === ================ ======= =========

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
            The resulting dataframe, depending on the parameters, will include the following columns:

              - **substation_id**: at which substation the voltage level belongs
              - **nominal_v**: The nominal voltage
              - **high_voltage_limit**: the high voltage limit
              - **low_voltage_limit**: the low voltage limit
              - **fictitious** (optional): ``True`` if the voltage level is part of the model and not of the actual network

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
            The resulting dataframe, depending on the parameters, will include the following columns:

              - **fictitious** (optional): ``True`` if the busbar section is part of the model and not of the actual network
              - **v**: The voltage magnitude of the busbar section (in kV)
              - **angle**: the voltage angle of the busbar section (in degree)
              - **voltage_level_id**: at which substation the busbar section is connected
              - **bus_id**: bus this busbar section belongs to
              - **bus_breaker_bus_id** (optional): bus of the bus-breaker view this busbar section  belongs to
              - **node**  (optional): node associated to the this busbar section, in node-breaker voltage levels
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

        Notes:
            The resulting dataframe, depending on the parameters, will include the following columns:

              - **name**: the name of the substations
              - **TSO**: the TSO which the substation belongs to
              - **geo_tags**: additional geographical information about the substation
              - **country**: the country which the substation belongs to
              - **fictitious** (optional): ``True`` if the substation is part of the model and not of the actual network

            This dataframe is indexed on the substation ID.
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
            The resulting dataframe, depending on the parameters, will include the following columns:

              - **converters_mode**:
              - **target_p**: (in MW)
              - **max_p**: the maximum of active power that can pass through the hvdc line (in MW)
              - **nominal_v**: nominal voltage (in kV)
              - **r**: the resistance of the hvdc line (in Ohm)
              - **converter_station1_id**: at which converter station the hvdc line is connected on side "1"
              - **converter_station2_id**: at which converter station the hvdc line is connected on side "2"
              - **connected1**: ``True`` if the busbar section on side "1" is connected to a bus
              - **connected2**: ``True`` if the busbar section on side "2" is connected to a bus
              - **fictitious** (optional): ``True`` if the hvdc is part of the model and not of the actual network

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
            The resulting dataframe, depending on the parameters, will include the following columns:

            - **kind**: the kind of switch
            - **open**: the open status of the switch
            - **retained**: the retain status of the switch
            - **voltage_level_id**: at which substation the switch is connected
            - **bus_breaker_bus1_id** (optional): bus where this switch is connected on side 1, in bus-breaker voltage levels
            - **bus_breaker_bus1_id** (optional): bus where this switch is connected on side 1, in bus-breaker voltage levels
            - **node1** (optional): node where this switch is connected on side 1, in node-breaker voltage levels
            - **node2** (optional): node where this switch is connected on side 2, in node-breaker voltage levels
            - **fictitious** (optional): ``True`` if the switch is part of the model and not of the actual network


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
            The resulting dataframe, depending on the parameters, will include the following columns:

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
            The resulting dataframe, depending on the parameters, will include the following columns:

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
            the ratio tap changers dataframe

        Notes:
            The resulting dataframe, depending on the parameters, will include the following columns:

              - **tap**: the current tap position
              - **low_tap**: the low tap position (usually 0, but could be different depending on the data origin)
              - **high_tap**: the high tap position
              - **step_count**: the count of taps, should be equal to (high_tap - low_tap)
              - **on_load**: true if the tap changer has on-load regulation capability
              - **regulating**: true if the tap changer is in regulation
              - **target_v**: the target voltage in kV, if the tap changer is in regulation
              - **target_deadband**: the regulation deadband around the target voltage, in kV
              - **regulating_bus_id**: the bus where the tap changer regulates voltage
              - **regulated_side** (optional): the side where the tap changer regulates voltage (redundant with regulating_bus_id)
              - **fictitious** (optional): ``True`` if the tap changer is part of the model and not of the actual network

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
            the phase tap changers dataframe

        Notes:
            The resulting dataframe, depending on the parameters, will include the following columns:

              - **tap**: the current tap position
              - **low_tap**: the low tap position (usually 0, but could be different depending on the data origin)
              - **high_tap**: the high tap position
              - **step_count**: the count of taps, should be equal to (high_tap - low_tap)
              - **regulating**: true if the phase shifter is in regulation
              - **regulation_mode**: regulation mode, among CURRENT_LIMITER, ACTIVE_POWER_CONTROL, and FIXED_TAP
              - **regulation_value**: the target value, in A or MW, depending on regulation_mode
              - **target_deadband**: the regulation deadband around the target value
              - **regulationg_bus_id**: the bus where the phase shifter regulates
              - **regulated_side** (optional): the side bus where the phase shifter regulates current or active power
              - **fictitious** (optional): ``True`` if the tap changer is part of the model and not of the actual network

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

        For each generator, the min/max reactive capabilities can be represented as curves.
        This dataframe describes those curves as a list of points, which associate a min and a max value of Q
        to a given value of P.

        Args:
            all_attributes: flag for including all attributes in the dataframe, default is false
            attributes: attributes to include in the dataframe. The 2 parameters are mutually exclusive.
                        If no parameter is specified, the dataframe will include the default attributes.

        Returns:
            A dataframe of reactive capability curve points.

        Notes:
            The resulting dataframe, depending on the parameters, will include the following columns:

              - **num**: the point position in the curve description (starts 0 for a given generator)
              - **p**: the active power of the point, in MW
              - **min_q**: the minimum value of reactive power, in MVar, for this value of P
              - **max_q**: the maximum value of reactive power, in MVar, for this value of P

            This dataframe is indexed on the generator ID.
        """
        return self.get_elements(ElementType.REACTIVE_CAPABILITY_CURVE_POINT, all_attributes, attributes)

    def _update_elements(self, element_type: ElementType, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update network elements with data provided as a :class:`~pandas.DataFrame` or as named arguments.for a specified element type.

        The dataframe columns are mapped to IIDM element attributes and each row is mapped to an element using the
        index.

        Args:
            element_type: the element type
            df: the data to be updated
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.
        """
        metadata = _pp.get_network_elements_dataframe_metadata(element_type)
        df = _adapt_df_or_kwargs(metadata, df, **kwargs)
        c_df = _create_c_dataframe(df, metadata)
        _pp.update_network_elements_with_series(self._handle, c_df, element_type)

    def update_buses(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update buses with data provided as a dataframe or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.
        Notes:
            Attributes that can be updated are:

            - `v_mag`
            - `v_angle`
            - `fictitious`

        See Also:
            :meth:`get_buses`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_buses(id='B1', v_mag=400.0)
                network.update_buses(id=['B1', 'B2'], v_mag=[400.0, 63.5])
        """
        return self._update_elements(ElementType.BUS, df, **kwargs)

    def update_switches(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update switches with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated are:

            - `open`
            - `retained`
            - `fictitious`

        See Also:
            :meth:`get_switches`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_switches(id='BREAKER-1', open=True)
                network.update_switches(id=['BREAKER-1', 'DISC-2'], open=[True, False])
        """
        return self._update_elements(ElementType.SWITCH, df, **kwargs)

    def update_generators(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update generators with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
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
            - `fictitious`

        See Also:
            :meth:`get_generators`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_generators(id='G-1', connected=True, target_p=500)
                network.update_generators(id=['G-1', 'G-2'], target_v=[403, 401])
        """
        return self._update_elements(ElementType.GENERATOR, df, **kwargs)

    def update_loads(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update loads with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated are:

            - `p0`
            - `q0`
            - `connected`
            - `fictitious`

        See Also:
            :meth:`get_loads`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_loads(id='L-1', p0=10, q0=3)
                network.update_loads(id=['L-1', 'L-2'],  p0=[10, 20], q0=[3, 5])
        """
        return self._update_elements(ElementType.LOAD, df, **kwargs)

    def update_batteries(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update batteries with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated are:

            - `target_p`
            - `target_q`
            - `connected`
            - `max_q`
            - `min_q`
            - `fictitious`

        See Also:
            :meth:`get_batteries`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_batteries(id='B-1', p0=10, q0=3)
                network.update_batteries(id=['B-1', 'B-2'],  p0=[10, 20], q0=[3, 5])
        """
        return self._update_elements(ElementType.BATTERY, df, **kwargs)

    def update_dangling_lines(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update dangling lines with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
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
            - `fictitious`

        See Also:
            :meth:`get_dangling_lines`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_dangling_lines(id='L-1', p0=10, q0=3)
                network.update_dangling_lines(id=['L-1', 'L-2'],  p0=[10, 20], q0=[3, 5])
        """
        return self._update_elements(ElementType.DANGLING_LINE, df, **kwargs)

    def update_vsc_converter_stations(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update VSC converter stations with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated are:

            - `loss_factor`
            - `target_v`
            - `target_q`
            - `voltage_regulator_on`
            - `p`
            - `q`
            - `connected`
            - `regulated_element_id`
            - `fictitious`

        See Also:
            :meth:`get_vsc_converter_stations`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_vsc_converter_stations(id='S-1', target_v=400, voltage_regulator_on=True)
                network.update_vsc_converter_stations(id=['S-1', 'S-2'], target_v=[400, 400])
        """
        return self._update_elements(ElementType.VSC_CONVERTER_STATION, df, **kwargs)

    def update_lcc_converter_stations(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update VSC converter stations with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated are:

            - `power_factor`
            - `loss_factor`
            - `p`
            - `q`
            - `connected`
            - `fictitious`

        See Also:
            :meth:`get_vsc_converter_stations`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_lcc_converter_stations(id='S-1', connected=True)
                network.update_lcc_converter_stations(id=['S-1', 'S-2'], connected=[True, False])
        """
        return self._update_elements(ElementType.LCC_CONVERTER_STATION, df, **kwargs)

    def update_static_var_compensators(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update static var compensators with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated are:

            - `b_min`
            - `b_max`
            - `target_v`
            - `target_q`
            - `regulation_mode`
            - `p`
            - `q`
            - `connected`
            - `regulated_element_id`
            - `fictitious`

        See Also:
            :meth:`get_static_var_compensators`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_static_var_compensators(id='SVC-1', target_v=225)
                network.update_static_var_compensators(id=['SVC-1', 'SVC-2'], target_v=[226, 405])
        """
        return self._update_elements(ElementType.STATIC_VAR_COMPENSATOR, df, **kwargs)

    def update_hvdc_lines(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update HVDC lines with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated are:

            - `converters_mode`
            - `target_p`
            - `max_p`
            - `nominal_v`
            - `r`
            - `connected1`
            - `connected2`
            - `fictitious`

        See Also:
            :meth:`get_hvdc_lines`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_hvdc_lines(id='HVDC-1', target_p=800)
                network.update_hvdc_lines(id=['HVDC-1', 'HVDC-2'], target_p=[800, 600])
        """
        return self._update_elements(ElementType.HVDC_LINE, df, **kwargs)

    def update_lines(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update lines data with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated are:

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
            - `fictitious`

        See Also:
            :meth:`get_lines`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_lines(id='L-1', connected1=False, connected2=True)
                network.update_lines(id=['L-1', 'L-2'], r=[0.5, 2.0], x=[5, 10])
        """
        return self._update_elements(ElementType.LINE, df, **kwargs)

    def update_2_windings_transformers(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update 2 windings transformers with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
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
            - `fictitious`

        See Also:
            :meth:`get_2_windings_transformers`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_2_windings_transformers(id='T-1', connected1=False, connected2=False)
                network.update_2_windings_transformers(id=['T-1', 'T-2'], r=[0.5, 2.0], x=[5, 10])
        """
        return self._update_elements(ElementType.TWO_WINDINGS_TRANSFORMER, df, **kwargs)

    def update_ratio_tap_changers(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update ratio tap changers with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated are:

            - `tap`
            - `on_load`
            - `regulating`,
            - `regulated_side`,
            - `target_v`
            - `target_deadband`
            - `fictitious`

        See Also:
            :meth:`get_ratio_tap_changers`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_ratio_tap_changers(id='T-1', tap=12)
                network.update_ratio_tap_changers(id=['T-1', 'T-2'], target_v=[64, 65], regulating=[True, True])
        """
        return self._update_elements(ElementType.RATIO_TAP_CHANGER, df, **kwargs)

    def update_ratio_tap_changer_steps(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update ratio tap changer steps with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated are:

            - `rho`
            - `r`
            - `x`
            - `g`
            - `b`

        See Also:
            :meth:`get_ratio_tap_changer_steps`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_ratio_tap_changer_steps(id='T-1', position=2, rho=1.1)
        """
        return self._update_elements(ElementType.RATIO_TAP_CHANGER_STEP, df, **kwargs)

    def update_phase_tap_changers(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update phase tap changers with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated :

            - `tap`
            - `regulating`
            - `regulation_mode`
            - `regulation_value`
            - `regulated_side`
            - `target_deadband`
            - `fictitious`

        See Also:
            :meth:`get_phase_tap_changers`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_phase_tap_changers(id='T-1', regulation_mode=CURRENT_LIMITER, regulation_value=500)
                network.update_phase_tap_changers(id=['T-1', 'T-2'], tap=[12, 25])
      """
        return self._update_elements(ElementType.PHASE_TAP_CHANGER, df, **kwargs)

    def update_phase_tap_changer_steps(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update phase tap changer steps with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated :

            - `rho`
            - `alpha`
            - `r`
            - `x`
            - `g`
            - `b`

        See Also:
            :meth:`get_phase_tap_changer_steps`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_phase_tap_changer_steps(id='T-1', position=2, rho=1.1, alpha=-12.3)
        """
        return self._update_elements(ElementType.PHASE_TAP_CHANGER_STEP, df, **kwargs)

    def update_shunt_compensators(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update shunt compensators with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated are:

            - `section_count`
            - `p`
            - `q`
            - `connected`
            - `fictitious`

        See Also:
            :meth:`get_shunt_compensators`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_shunt_compensators(id='IND-1', section_count=1, connected=True)
                network.update_shunt_compensators(id=['IND-1', 'CAP-1'], section_count=[1, 0])
        """
        return self._update_elements(ElementType.SHUNT_COMPENSATOR, df, **kwargs)

    def update_linear_shunt_compensator_sections(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update shunt compensators with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated are:

            - `g_per_section`
            - `b_per_section`
            - `max_section_count`

        See Also:
            :meth:`get_linear_shunt_compensator_sections`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_linear_shunt_compensator_sections(id='CAP-1', max_section_count=3)
        """
        return self._update_elements(ElementType.LINEAR_SHUNT_COMPENSATOR_SECTION, df, **kwargs)

    def update_non_linear_shunt_compensator_sections(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update non linear shunt compensators sections with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated are :

            - `g`
            - `b`

        See Also:
            :meth:`get_non_linear_shunt_compensator_sections`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_non_linear_shunt_compensator_sections(id='CAP-1', section=1, b=1e-5)
        """
        return self._update_elements(ElementType.NON_LINEAR_SHUNT_COMPENSATOR_SECTION, df, **kwargs)

    def update_busbar_sections(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """Update phase tap changers with a ``Pandas`` dataframe.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        """
        return self._update_elements(ElementType.BUSBAR_SECTION, df, **kwargs)

    def update_voltage_levels(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update voltage levels with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated are :

            - `high_voltage_limit`
            - `low_voltage_limit`
            - `nominal_v`
            - `fictitious`

        See Also:
            :meth:`get_voltage_levels`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_voltage_levels(id='VL-1', high_voltage_limit=420)
                network.update_voltage_levels(id=['VL-1', 'VL-2'], low_voltage_limit=[385, 390])
        """
        return self._update_elements(ElementType.VOLTAGE_LEVEL, df, **kwargs)

    def update_substations(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update substations with data provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the data to be updated, as a dataframe.
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            Attributes that can be updated are :

            - `TSO`
            - `country`
            - `fictitious`

        See Also:
            :meth:`get_substations`

        Examples:
            Some examples using keyword arguments:

            .. code-block:: python

                network.update_substations(id='S-1', TSO='ELIA', country='BE')
                network.update_substations(id=['S-1', 'S-2'], country=['BE', 'FR'])
        """
        return self._update_elements(ElementType.SUBSTATION, df, **kwargs)

    def update_extensions(self, extension_name: str, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Update extensions of network elements with data provided as a :class:`~pandas.DataFrame`.

        Args:
            extension_name: name of the extension
            df: the data to be updated
            kwargs: the data to be updated, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            The id column in the dataframe provides the link to the extensions parent elements
        """
        metadata = _pp.get_network_extensions_dataframe_metadata(extension_name)
        df = _adapt_df_or_kwargs(metadata, df, **kwargs)
        c_df = _create_c_dataframe(df, metadata)
        _pp.update_extensions(self._handle, extension_name, c_df)

    def create_extensions(self, extension_name: str, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        create extensions of network elements with data provided as a :class:`~pandas.DataFrame`.

        Args:
            extension_name: name of the extension
            dfs: the data to be created
            kwargs: the data to be created, as named arguments.
                    Arguments can be single values or any type of sequence.
                    In the case of sequences, all arguments must have the same length.

        Notes:
            The id column in the dataframe provides the link to the extensions parent elements
        """
        self._create_extensions(extension_name, [df], **kwargs)

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
        if 'fictitious' in current_limits.columns:
            return current_limits[['side', 'value', 'acceptable_duration', 'fictitious']]
        else:
            return current_limits[['side', 'value', 'acceptable_duration']]

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

    def merge(self, networks: _Union[Network, _Sequence[Network]]) -> None:
        """
        Merges networks into this one.

        Args:
            networks:  List of networks to be merged into this one.

        Examples:
            If you have 3 networks, you can merge this way:

            .. code-block:: python

                network1.merge([network2, network3])

            Note that network1 is modified: it absorbs network2 and network3.
        """
        if isinstance(networks, Network):
            networks = [networks]
        return _pp.merge(self._handle, [n._handle for n in networks])

    def _create_elements(self, element_type: ElementType, dfs: _List[_Optional[_DataFrame]], **kwargs: _ArrayLike) -> None:
        metadata = _pp.get_network_elements_creation_dataframes_metadata(element_type)
        c_dfs = _get_c_dataframes(dfs, metadata, **kwargs)
        _pp.create_element(self._handle, c_dfs, element_type)

    def _create_extensions(self, extension_name: str, dfs: _List[_Optional[_DataFrame]], **kwargs: _ArrayLike) -> None:
        metadata = _pp.get_network_extensions_creation_dataframes_metadata(extension_name)
        c_dfs = _get_c_dataframes(dfs, metadata, **kwargs)
        _pp.create_extensions(self._handle, c_dfs, extension_name)

    def create_substations(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates substations.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:
            - **id**: the identifier of the substation
            - **name**: an optional human readable name for the substation
            - **country**: an optional country code ('DE', 'IT', ...)
            - **TSO**: an optional TSO name

        Examples:
            Using keyword arguments:

            .. code-block:: python

                network.create_substations(id='S-1', country='IT', TSO='TERNA')

            Or using a dataframe:

           .. code-block:: python

                stations = pd.DataFrame.from_records(index='id', data=[
                    {'id': 'S1', 'country': 'BE'},
                    {'id': 'S2', 'country': 'DE'}
                ])
                network.create_substations(stations)
        """
        self._create_elements(ElementType.SUBSTATION, [df], **kwargs)

    def create_generators(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates generators.

        Args:
            df: Attributes as dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**: the identifier of the new generator
            - **voltage_level_id**: the voltage level where the new generator will be created.
              The voltage level must already exist.
            - **bus_id**: the bus where the new generator will be connected,
              if the voltage level has a bus-breaker topology kind.
            - **connectable_bus_id**: the bus where the new generator will be connectable,
              if the voltage level has a bus-breaker topology kind.
            - **node**: the node where the new generator will be connected,
              if the voltage level has a node-breaker topology kind.
            - **energy_source**: the type of energy source (HYDRO, NUCLEAR, ...)
            - **max_p**: maximum active power in MW
            - **min_p**: minimum active power in MW
            - **target_p**: target active power in MW
            - **target_q**: target reactive power in MVar, when the generator does not regulate voltage
            - **rated_s**: nominal power in MVA
            - **target_v**: target voltage in kV, when the generator regulates voltage
            - **voltage_regulator_on**: true if the generator regulates voltage

        Examples:
            Using keyword arguments:

            .. code-block:: python

                network.create_generators(id='GEN',
                                          voltage_level_id='VL1',
                                          bus_id='B1',
                                          target_p=100,
                                          min_p=0,
                                          max_p=200,
                                          target_v=400,
                                          voltage_regulator_on=True)
        """
        self._create_elements(ElementType.GENERATOR, [df], **kwargs)

    def create_busbar_sections(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates bus bar sections.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**: the identifier of the new busbar section
            - **voltage_level_id**: the voltage level where the new busbar section will be created.
              The voltage level must already exist.
            - **node**: the node where the new generator will be connected,
              if the voltage level has a node-breaker topology kind.
            - **name**: an optional human-readable name

        Examples:
            Using keyword arguments:

            .. code-block:: python

                network.create_busbar_sections(id='BBS', voltage_level_id='VL1', node=0)
        """
        self._create_elements(ElementType.BUSBAR_SECTION, [df], **kwargs)

    def create_buses(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates buses in bus-breaker voltage levels.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            This method can only create "configured buses", in bus-breaker voltage levels,
            as opposed to electrical buses computed from this underlying topology.

            Valid attributes are:

            - **id**: the identifier of the new configured bus
            - **voltage_level_id**: the voltage level where the new bus will be created.
              The voltage level must already exist, and must have a bus-breaker topology kind.
            - **name**: an optional human-readable name

        Examples:
            Using keyword arguments:

            .. code-block:: python

                network.create_buses(id='B1', voltage_level_id='VL1')
        """
        return self._create_elements(ElementType.BUS, [df], **kwargs)

    def create_loads(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Create loads.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**: the identifier of the new load
            - **voltage_level_id**: the voltage level where the new load will be created.
              The voltage level must already exist.
            - **bus_id**: the bus where the new load will be connected,
              if the voltage level has a bus-breaker topology kind.
            - **connectable_bus_id**: the bus where the new load will be connectable,
              if the voltage level has a bus-breaker topology kind.
            - **node**: the node where the new load will be connected,
              if the voltage level has a node-breaker topology kind.
            - **name**: an optional human-readable name
            - **type**: optionally, the type of load (UNDEFINED, AUXILIARY, FICTITIOUS)
            - **p0**: active power load, in MW
            - **q0**: reactive power load, in MVar

        Examples:
            Using keyword arguments:

            .. code-block:: python

                network.create_loads(id='LOAD-1', voltage_level_id='VL1', bus_id='B1', p0=10, q0=3)
        """
        return self._create_elements(ElementType.LOAD, [df], **kwargs)

    def create_batteries(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates batteries.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**: the identifier of the new battery
            - **voltage_level_id**: the voltage level where the new battery will be created.
              The voltage level must already exist.
            - **bus_id**: the bus where the new battery will be connected,
              if the voltage level has a bus-breaker topology kind.
            - **connectable_bus_id**: the bus where the new battery will be connectable,
              if the voltage level has a bus-breaker topology kind.
            - **node**: the node where the new battery will be connected,
              if the voltage level has a node-breaker topology kind.
            - **name**: an optional human-readable name
            - **min_p**: minimum active power, in MW
            - **max_p**: maximum active power, in MW
            - **target_p**: active power consumption, in MW
            - **target_q**: reactive power consumption, in MVar

        Examples:
            Using keyword arguments:

            .. code-block:: python

                network.create_batteries(id='BAT-1', voltage_level_id='VL1', bus_id='B1',
                                         min_p=5, max_p=50, p0=10, q0=3)
        """
        return self._create_elements(ElementType.BATTERY, [df], **kwargs)

    def create_dangling_lines(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates dangling lines.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**: the identifier of the new line
            - **voltage_level_id**: the voltage level where the new line will be created.
              The voltage level must already exist.
            - **bus_id**: the bus where the new line will be connected,
              if the voltage level has a bus-breaker topology kind.
            - **connectable_bus_id**: the bus where the new line will be connectable,
              if the voltage level has a bus-breaker topology kind.
            - **node**: the node where the new line will be connected,
              if the voltage level has a node-breaker topology kind.
            - **name**: an optional human-readable name
            - **p0**: the active power consumption, in MW
            - **q0**: the reactive power consumption, in MVar
            - **r**: the resistance, in Ohms
            - **x**: the reactance, in Ohms
            - **g**: the shunt conductance, in S
            - **b**: the shunt susceptance, in S

        Examples:
            Using keyword arguments:

            .. code-block:: python

                network.create_dangling_lines(id='BAT-1', voltage_level_id='VL1', bus_id='B1',
                                              p0=10, q0=3, r=0, x=5, g=0, b=1e-6)
        """
        return self._create_elements(ElementType.DANGLING_LINE, [df], **kwargs)

    def create_lcc_converter_stations(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates LCC converter stations.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**: the identifier of the new station
            - **voltage_level_id**: the voltage level where the new station will be created.
              The voltage level must already exist.
            - **bus_id**: the bus where the new station will be connected,
              if the voltage level has a bus-breaker topology kind.
            - **connectable_bus_id**: the bus where the new station will be connectable,
              if the voltage level has a bus-breaker topology kind.
            - **node**: the node where the new station will be connected,
              if the voltage level has a node-breaker topology kind.
            - **name**: an optional human-readable name
            - **power_factor**: the power factor (ratio of the active power to the apparent power)
            - **loss_factor**: the loss factor of the station

        Examples:
            Using keyword arguments:

            .. code-block:: python

                network.create_lcc_converter_stations(id='CS-1', voltage_level_id='VL1', bus_id='B1',
                                                      power_factor=0.3, loss_factor=0.1)
        """
        return self._create_elements(ElementType.LCC_CONVERTER_STATION, [df], **kwargs)

    def create_vsc_converter_stations(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates VSC converter stations.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**: the identifier of the new station
            - **voltage_level_id**: the voltage level where the new station will be created.
              The voltage level must already exist.
            - **bus_id**: the bus where the new station will be connected,
              if the voltage level has a bus-breaker topology kind.
            - **connectable_bus_id**: the bus where the new station will be connectable,
              if the voltage level has a bus-breaker topology kind.
            - **node**: the node where the new station will be connected,
              if the voltage level has a node-breaker topology kind.
            - **name**: an optional human-readable name
            - **loss_factor**: the loss factor of the new station
            - **voltage_regulator_on**: true if the station regulated voltage
            - **target_v**: the target voltage, in kV, when the station regulates voltage
            - **target_q**: the target reactive power, in MVar, when the station does not regulate voltage

        Examples:
            Using keyword arguments:

            .. code-block:: python

                network.create_vsc_converter_stations(id='CS-1', voltage_level_id='VL1', bus_id='B1',
                                                      loss_factor=0.1, voltage_regulator_on=True, target_v=400.0)
        """
        return self._create_elements(ElementType.VSC_CONVERTER_STATION, [df], **kwargs)

    def create_static_var_compensators(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates static var compensators.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**: the identifier of the new SVC
            - **voltage_level_id**: the voltage level where the new SVC will be created.
              The voltage level must already exist.
            - **bus_id**: the bus where the new SVC will be connected,
              if the voltage level has a bus-breaker topology kind.
            - **connectable_bus_id**: the bus where the new SVC will be connectable,
              if the voltage level has a bus-breaker topology kind.
            - **node**: the node where the new SVC will be connected,
              if the voltage level has a node-breaker topology kind.
            - **name**: an optional human-readable name
            - **b_max**: the maximum susceptance, in S
            - **b_min**: the minimum susceptance, in S
            - **regulation_mode**: the regulation mode (VOLTAGE, REACTIVE_POWER, OFF)
            - **target_v**: the target voltage, in kV, when the regulation mode is VOLTAGE
            - **target_q**: the target reactive power, in MVar, when the regulation mode is not VOLTAGE

        Examples:
            Using keyword arguments:

            .. code-block:: python

                network.create_static_var_compensators(id='CS-1', voltage_level_id='VL1', bus_id='B1',
                                                       b_min=-0.01, b_max=0.01, regulation_mode='VOLTAGE',
                                                       target_v=400.0)
        """
        return self._create_elements(ElementType.STATIC_VAR_COMPENSATOR, [df], **kwargs)

    def create_lines(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates lines.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**: the identifier of the new line
            - **voltage_level1_id**: the voltage level where the new line will be connected on side 1.
              The voltage level must already exist.
            - **bus1_id**: the bus where the new line will be connected on side 1,
              if the voltage level has a bus-breaker topology kind.
            - **connectable_bus1_id**: the bus where the new line will be connectable on side 1,
              if the voltage level has a bus-breaker topology kind.
            - **node1**: the node where the new line will be connected on side 1,
              if the voltage level has a node-breaker topology kind.
            - **voltage_level2_id**: the voltage level where the new line will be connected on side 2.
              The voltage level must already exist.
            - **bus2_id**: the bus where the new line will be connected on side 2,
              if the voltage level has a bus-breaker topology kind.
            - **connectable_bus2_id**: the bus where the new line will be connectable on side 2,
              if the voltage level has a bus-breaker topology kind.
            - **node2**: the node where the new line will be connected on side 2,
              if the voltage level has a node-breaker topology kind.
            - **name**: an optional human-readable name
            - **b1**: the shunt susceptance, in S, on side 1
            - **b2**: the shunt susceptance, in S, on side 2
            - **g1**: the shunt conductance, in S, on side 1
            - **g2**: the shunt conductance, in S, on side 2
            - **r**: the resistance, in Ohm
            - **x**: the reactance, in Ohm


        Examples:
            Using keyword arguments:

            .. code-block:: python

                network.create_lines(id='LINE-1', voltage_level1_id='VL1', bus1_id='B1',
                                     voltage_level2_id='VL2', bus2_id='B2',
                                     b1=1e-6, b2=1e-6, g1=0, , g2=0, r=0.5, x=10)
        """
        return self._create_elements(ElementType.LINE, [df], **kwargs)

    def create_2_windings_transformers(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates 2 windings transformers.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**: the identifier of the new transformer
            - **voltage_level1_id**: the voltage level where the new transformer will be connected on side 1.
              The voltage level must already exist.
            - **bus1_id**: the bus where the new transformer will be connected on side 1,
              if the voltage level has a bus-breaker topology kind.
            - **connectable_bus1_id**: the bus where the new transformer will be connectable on side 1,
              if the voltage level has a bus-breaker topology kind.
            - **node1**: the node where the new transformer will be connected on side 1,
              if the voltage level has a node-breaker topology kind.
            - **voltage_level2_id**: the voltage level where the new transformer will be connected on side 2.
              The voltage level must already exist.
            - **bus2_id**: the bus where the new transformer will be connected on side 2,
              if the voltage level has a bus-breaker topology kind.
            - **connectable_bus2_id**: the bus where the new transformer will be connectable on side 2,
              if the voltage level has a bus-breaker topology kind.
            - **node2**: the node where the new transformer will be connected on side 2,
              if the voltage level has a node-breaker topology kind.
            - **name**: an optional human-readable name
            - **rated_u1**: nominal voltage of the side 1 of the transformer
            - **rated_u2**: nominal voltage of the side 2 of the transformer
            - **rated_s**: nominal power of the transformer
            - **b**: the shunt susceptance, in S
            - **g**: the shunt conductance, in S
            - **r**: the resistance, in Ohm
            - **x**: the reactance, in Ohm

        Examples:
            Using keyword arguments:

            .. code-block:: python

                network.create_2_windings_transformers(id='T-1', voltage_level1_id='VL1', bus1_id='B1',
                                                       voltage_level2_id='VL2', bus2_id='B2',
                                                       b=1e-6, g=1e-6, r=0.5, x=10, rated_u1=400, rated_u2=225)
        """
        return self._create_elements(ElementType.TWO_WINDINGS_TRANSFORMER, [df], **kwargs)

    def create_shunt_compensators(self, shunt_df: _DataFrame,
                                  linear_model_df: _Optional[_DataFrame] = None,
                                  non_linear_model_df: _Optional[_DataFrame] = None,
                                  **kwargs: _ArrayLike) -> None:
        """
        Create shunt compensators.

        Shunt compensator sections can be described in 1 of 2 ways:
        either with a linear model, with a maximum section count and a per-section values,
        or with a non linear model, where each section is described individually.

        For this reason, 2 or 3 dataframes need to be provided:
        one for shunt compensators data, optionally one for linear models,
        and optionally one for non linear models.

        Args:
            shunt_df: dataframe for shunt compensators data
            linear_model_df: dataframe for linear model sections data
            non_linear_model_df: dataframe for sections data

        Notes:

            Valid attributes for the shunt compensators dataframe are:

            - **id**: the identifier of the new shunt
            - **voltage_level_id**: the voltage level where the new shunt will be created.
              The voltage level must already exist.
            - **bus_id**: the bus where the new shunt will be connected,
              if the voltage level has a bus-breaker topology kind.
            - **connectable_bus_id**: the bus where the new shunt will be connectable,
              if the voltage level has a bus-breaker topology kind.
            - **node**: the node where the new shunt will be connected,
              if the voltage level has a node-breaker topology kind.
            - **name**: an optional human-readable name
            - **model_type**: either LINEAR or NON_LINEAR
            - **section_count**: the current count of connected sections
            - **target_v**: an optional target voltage in kV
            - **target_v**: an optional deadband for the target voltage, in kV

            Valid attributes for the linear sections models are:

            - **id**: the identifier of the new shunt
            - **g_per_section**: the conductance, in Ohm, for each section
            - **b_per_section**: the susceptance, in Ohm, for each section
            - **max_section_count**: the maximum number of connectable sections

            This dataframe must have only one row for each shunt compensator.

            Valid attributes for the non linear sections models are:

            - **id**: the identifier of the new shunt
            - **g**: the conductance, in Ohm, for this section
            - **b**: the susceptance, in Ohm, for this section

            This dataframe will have multiple rows for each shunt compensator: one by section.

        Examples:
            For example, to create linear model shunts, we need 1 dataframe for the shunts and 1 dataframe
            for the linear model of sections:

            .. code-block:: python

                shunt_df = pd.DataFrame.from_records(
                    index='id',
                    columns=['id', 'name', 'model_type', 'section_count', 'target_v',
                             'target_deadband', 'voltage_level_id', 'node'],
                    data=[('SHUNT-1', '', 'LINEAR', 1, 400, 2, 'S1VL2', 2)])
                model_df = pd.DataFrame.from_records(
                    index='id',
                    columns=['id', 'g_per_section', 'b_per_section', 'max_section_count'],
                    data=[('SHUNT-1', 0.14, -0.01, 2)])
                n.create_shunt_compensators(shunt_df, model_df)


            For non linear model shunts, we need 1 dataframe for the shunts and 1 dataframe
            for the sections:

            .. code-block:: python

                shunt_df = pd.DataFrame.from_records(
                    index='id',
                    columns=['id', 'name', 'model_type', 'section_count', 'target_v',
                             'target_deadband', 'voltage_level_id', 'node'],
                    data=[('SHUNT1', '', 'NON_LINEAR', 1, 400, 2, 'S1VL2', 2),
                          ('SHUNT2', '', 'NON_LINEAR', 1, 400, 2, 'S1VL2', 10)])
                model_df = pd.DataFrame.from_records(
                    index='id',
                    columns=['id', 'g', 'b'],
                    data=[('SHUNT1', 1, 2),
                          ('SHUNT1', 3, 4),
                          ('SHUNT2', 5, 6),
                          ('SHUNT2', 7, 8)])
                n.create_shunt_compensators(shunt_df, non_linear_model_df=model_df)
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

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**: the identifier of the new switch
            - **voltage_level1_id**: the voltage level where the new switch will be connected on side 1.
              The voltage level must already exist.
            - **bus1_id**: the bus where the new switch will be connected on side 1,
              if the voltage level has a bus-breaker topology kind.
            - **bus2_id**: the bus where the new switch will be connected on side 2,
              if the voltage level has a bus-breaker topology kind.
            - **node1**: the node where the new switch will be connected on side 1,
              if the voltage level has a node-breaker topology kind.
            - **node2**: the node where the new switch will be connected on side 2,
              if the voltage level has a node-breaker topology kind.
            - **name**: an optional human-readable name
            - **kind**: the kind of switch (BREAKER, DISCONNECTOR, LOAD_BREAK_SWITCH)
            - **open**: true if the switch is open, default false
            - **retained**: true if the switch should be retained in bus-breaker topology, default false
            - **fictitious**: true if the switch is fictitious, default false

        Examples:
            Using keyword arguments:

            .. code-block:: python

                # In a bus-breaker voltage level, between configured buses B1 and B2
                network.create_switches(id='BREAKER-1', voltage_level1_id='VL1', bus1_id='B1', bus2_id='B2',
                                        kind='BREAKER', open=False)

                # In a node-breaker voltage level, between nodes 5 and 7
                network.create_switches(id='BREAKER-1', voltage_level1_id='VL1', node1=5, node2=7,
                                        kind='BREAKER', open=False)
        """
        return self._create_elements(ElementType.SWITCH, [df], **kwargs)

    def create_voltage_levels(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Creates voltage levels.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**: the identifier of the new voltage level
            - **substation_id**: the identifier of the substation which the new voltage level belongs to.
              Optional. If defined, the substation must already exist.
            - **name**: an optional human-readable name
            - **topology_kind**: the topology kind, BUS_BREAKER or NODE_BREAKER
            - **nominal_v**: the nominal voltage, in kV
            - **low_voltage_limit**: the lower operational voltage limit, in kV
            - **high_voltage_limit**: the upper operational voltage limit, in kV

        Examples:
            Using keyword arguments:

            .. code-block:: python

                network.create_voltage_levels(id='VL1', substation_id='S1', topology_kind='BUS_BREAKER',
                                              nominal_v=400, low_voltage_limit=380, high_voltage_limit=420)
        """
        return self._create_elements(ElementType.VOLTAGE_LEVEL, [df], **kwargs)

    def create_ratio_tap_changers(self, rtc_df: _DataFrame, steps_df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Create ratio tap changers on transformers.

        Tap changers data must be provided in 2 separate dataframes:
        one for the tap changers attributes, and another one for tap changers steps attributes.
        The latter one will generally have multiple lines for one transformer ID.

        Args:
            rtc_df: dataframe of tap changers data
            steps_df: dataframe of steps data

        Notes:

            Valid attributes for the tap changers dataframe are:

            - **id**: the transformer where this tap changer will be created
            - **tap**: the current tap position
            - **low_tap**: the number of the lowest tap position (default 0)
            - **on_load**: true if the transformer has on-load voltage regulation capability
            - **target_v**: the target voltage, in kV
            - **target_deadband**: the target voltage regulation deadband, in kV
            - **regulating**: true if the tap changer should regulate voltage
            - **regulated_side**: the side where voltage is regulated (ONE or TWO)

            Valid attributes for the steps dataframe are:

            - **id**: the transformer where this step will be added
            - **g**: the shunt conductance increase compared to the transformer, for this step, in percentage
            - **b**: the shunt susceptance increase compared to the transformer, for this step, in percentage
            - **r**: the resistance increase compared to the transformer, for this step, in percentage
            - **x**: the reactance increased compared to the transformer, for this step, in percentage
            - **rho**: the transformer ratio for this step (1 means real ratio is rated_u2/rated_u1)

        Examples:
            We need to provide 2 dataframes, 1 for tap changer basic data, and one for step-wise data:

            .. code-block:: python

                rtc_df = pd.DataFrame.from_records(
                    index='id',
                    columns=['id', 'target_deadband', 'target_v', 'on_load', 'low_tap', 'tap'],
                    data=[('NGEN_NHV1', 2, 200, False, 0, 1)])
                steps_df = pd.DataFrame.from_records(
                    index='id',
                    columns=['id', 'b', 'g', 'r', 'x', 'rho'],
                    data=[('NGEN_NHV1', 2, 2, 1, 1, 0.5),
                          ('NGEN_NHV1', 2, 2, 1, 1, 0.5)])
                network.create_ratio_tap_changers(rtc_df, steps_df)
        """
        return self._create_elements(ElementType.RATIO_TAP_CHANGER, [rtc_df, steps_df], **kwargs)

    def create_phase_tap_changers(self, ptc_df: _DataFrame, steps_df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Create phase tap changers on transformers.

        Tap changers data must be provided in 2 separate dataframes:
        one for the tap changers attributes, and another one for tap changers steps attributes.
        The latter one will generally have multiple lines for one transformer ID.

        Args:
            ptc_df: dataframe of tap changers data
            steps_df: dataframe of steps data

        Notes:

            Valid attributes for the tap changers dataframe are:

            - **id**: the transformer where this tap changer will be created
            - **tap**: the current tap position
            - **low_tap**: the number of the lowest tap position (default 0)
            - **regulation_mode**: the regulation mode (CURRENT_LIMITER, ACTIVE_POWER_CONTROL, FIXED_TAP)
            - **target_deadband**: the regulation deadband
            - **regulating**: true if the tap changer should regulate
            - **regulated_side**: the side where the current or active power is regulated (ONE or TWO)

            Valid attributes for the steps dataframe are:

            - **id**: the transformer where this step will be added
            - **g**: the shunt conductance increase compared to the transformer, for this step, in percentage
            - **b**: the shunt susceptance increase compared to the transformer, for this step, in percentage
            - **r**: the resistance increase compared to the transformer, for this step, in percentage
            - **x**: the reactance increased compared to the transformer, for this step, in percentage
            - **rho**: the transformer ratio for this step (1 means real ratio is rated_u2/rated_u1)
            - **alpha**: the phase shift, in degrees, for this step

        Examples:
            We need to provide 2 dataframes, 1 for tap changer basic data, and one for step-wise data:

            .. code-block:: python

                ptc_df = pd.DataFrame.from_records(
                    index='id', columns=['id', 'target_deadband', 'regulation_mode', 'low_tap', 'tap'],
                    data=[('TWT_TEST', 2, 'CURRENT_LIMITER', 0, 1)])
                steps_df = pd.DataFrame.from_records(
                    index='id', columns=['id', 'b', 'g', 'r', 'x', 'rho', 'alpha'],
                    data=[('TWT_TEST', 2, 2, 1, 1, 0.5, 0.1),
                          ('TWT_TEST', 2, 2, 1, 1, 0.5, 0.1)])
                n.create_phase_tap_changers(ptc_df, steps_df)
        """
        return self._create_elements(ElementType.PHASE_TAP_CHANGER, [ptc_df, steps_df], **kwargs)

    def create_hvdc_lines(self, df: _DataFrame, **kwargs: _ArrayLike) -> None:
        """
        Creates HVDC lines.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**: the identifier of the new HVDC line
            - **name**: an optional human-readable name
            - **converter_station1_id**: the station where the new HVDC line will be connected on side 1.
              It must already exist.
            - **converter_station2_id**: the station where the new HVDC line will be connected on side 2.
              It must already exist.
            - **r**: the resistance of the HVDC line, in Ohm
            - **nominal_v**: the nominal voltage of the HVDC line, in kV
            - **max_p**: the maximum transmissible power, in MW
            - **target_p**: the active power target, in MW
            - **converters_mode**: SIDE_1_RECTIFIER_SIDE_2_INVERTER or SIDE_1_INVERTER_SIDE_2_RECTIFIER

        Examples:
            Using keyword arguments:

            .. code-block:: python

                network.create_hvdc_lines(id='HVDC-1', converter_station1_id='CS-1', converter_station2_id='CS-2',
                                          r=1.0, nominal_v=400, converters_mode='SIDE_1_RECTIFIER_SIDE_2_INVERTER',
                                          max_p=1000, target_p=800)
        """
        return self._create_elements(ElementType.HVDC_LINE, [df], **kwargs)

    def create_operational_limits(self, df: _DataFrame, **kwargs: _ArrayLike) -> None:
        """
        Creates operational limits.

        Notes:

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
            kwargs: Attributes as keyword arguments.
        """
        df['acceptable_duration'] = df['acceptable_duration'].map(lambda x: -1 if x == Inf else int(x))
        return self._create_elements(ElementType.OPERATIONAL_LIMITS, [df], **kwargs)

    def create_minmax_reactive_limits(self, df: _DataFrame, **kwargs: _ArrayLike) -> None:
        """
        Creates reactive limits of type min/max.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**: the identifier of the generator
            - **min_q**: minimum reactive limit, in MVAr
            - **max_q**: maximum reactive limit, in MVAr

            Previously defined limits for a given generator, if present,
            will be replaced by the new ones.

        Examples:
            Using keyword arguments:

            .. code-block:: python

                network.create_minmax_reactive_limits(id='GEN-1', min_q=-100, max_q=100)

        See Also:
            :meth:`create_curve_reactive_limits`
        """
        return self._create_elements(ElementType.MINMAX_REACTIVE_LIMITS, [df], **kwargs)

    def create_curve_reactive_limits(self, df: _DataFrame, **kwargs: _ArrayLike) -> None:
        """
        Creates reactive limits as "curves".

        Curves are actually composed of line segments, defined by a list of points.
        Each row of the input data actually defines 2 points:
        one for the minimum limit, one for the maximum limit, for the given
        active power value.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**:    the identifier of the generator
            - **p**:     active power, in MW, for which this row defines limits
            - **min_q**: minimum reactive limit at this active power value, in MVAr
            - **max_q**: maximum reactive limit at this active power value, in MVAr

            At least 2 rows must be defined for each generator, for 2
            different active power values.
            Previously defined limits for a given generator, if present,
            will be replaced by the new ones.

        Examples:
            Generator GEN-1 will be able to provide 150MVAr when P=0MW,
            and only 100MVAr when it generates 100MW:

            .. code-block:: python

                network.create_curve_reactive_limits(id=['GEN-1', 'GEN-1'],
                                                     p=[0, 100],
                                                     min_q=[-150, -100],
                                                     max_q=[150, 100])

        See Also:
            :meth:`create_minmax_reactive_limits`
        """
        return self._create_elements(ElementType.REACTIVE_CAPABILITY_CURVE_POINT, [df], **kwargs)

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

    def remove_elements(self, elements_ids: _Union[str, _List[str]]) -> None:
        """
        Removes elements from the network.

        Args:
            elements_ids: IDs of the elements to be removed.

        Notes:
            Elements can be provided as a list of IDs or as a single ID.

            Elements can be any identifiable object of the network
            (line, generator, switch, substation ...).

        Examples:

            .. code-block:: python

                network.remove_elements('GENERATOR-1')  # Removes only 1 element
                network.remove_elements(['GENERATOR-1', 'BUS])
        """
        if isinstance(elements_ids, str):
            elements_ids = [elements_ids]
        _pp.remove_elements(self._handle, elements_ids)

    def get_extensions(self, extension_name: str) -> _DataFrame:
        """
        Get an extension as a :class:`~pandas.DataFrame` for a specified extension name.

        Args:
            extension_name: name of the extension

        Returns:
            A dataframe with the extensions data.

        Notes:
            The extra id column in the resulting dataframe provides the link to the extensions parent elements
        """
        return _create_data_frame_from_series_array(_pp.create_network_elements_extension_series_array(self._handle, extension_name))

    def get_extension(self, extension_name: str) -> _DataFrame:
        warnings.warn("get_extension is deprecated, use get_extensions instead", DeprecationWarning)
        return self.get_extensions(extension_name)

    def add_elements_properties(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Add properties to network elements, provided as a :class:`~pandas.DataFrame` or as named arguments.

        Args:
            df: the properties to be created or updated. The index has to be the `id`
                identifying the network elements.
            kwargs: the properties to be added as named arguments.
                    Arguments can be a single string or any type of sequence of strings.
                    In the case of sequences, all arguments must have the same length.


        Examples:

            For example, to add the properties prop1 = value1 and prop2 = value2 to a network element:

            .. code-block:: python

                >>> network.add_elements_properties(id='GENERATOR-1', prop1='value1', prop2='value2')
                >>> network.get_generators(attributes=['prop1', 'prop2'], id='GENERATOR-1')
                         toto
                id
                VLEJUP7  tutu

            You can also update multiple elements at once, for example with a dataframe:

            .. code-block:: python

                >>> properties_df = pd.Dataframe(index=pd.Series('id', ['G1', 'G2']),
                                                 data={
                                                     'prop1': [ 'val11', 'val12'],
                                                     'prop2': [ 'val12', 'val22'],
                                                 })
                >>> network.add_elements_properties(properties_df)
                >>> network.get_generators(attributes=['prop1', 'prop2'], id=['G1', 'G2'])
                         prop1  prop2
                id
                G1       val11  val12
                G2       val21  val22

        """
        if df is None:
            df = _adapt_properties_kwargs(**kwargs)
        if df.isnull().values.any():
            raise _pp.PyPowsyblError("dataframe can not contain NaN values")
        for series_name in df.columns.values:
            df[series_name] = df[series_name].astype(str)
        c_df = _create_properties_c_dataframe(df)
        _pp.add_network_element_properties(self._handle, c_df)

    def remove_elements_properties(self, ids: _Union[str, _List[str]], properties: _Union[str, _List[str]]) -> None:
        """
        Remove properties from a list of network elements

        Args:
            ids: list of the network elements that will have their properties removed
            properties: list of the properties that will be removed

        Examples:

            To remove properties prop1 and prop2 from network elements GEN1 and GEN2:

            .. code-block:: python

                network.remove_elements_properties(ids=['GEN1', 'GEN2'], properties=['prop1', 'prop2'])

        """
        if isinstance(ids, str):
            ids = [ids]
        if isinstance(properties, str):
            properties = [properties]
        _pp.remove_network_element_properties(self._handle, ids, properties)

    def remove_extensions(self, extension_name: str, ids: _Union[str, _List[str]]) -> None:
        """
        Removes network elements extensions, given the extension's name.

        Args:
            extension_name: name of the extension
            ids: IDs of the elements to be removed.

        Notes:
            ids can be provided as a list of IDs or as a single ID.
        """
        if isinstance(ids, str):
            ids = [ids]
        _pp.remove_extensions(self._handle, extension_name, ids)


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
        import parameters dataframe

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
        export parameters dataframe
    """
    series_array = _pp.create_exporter_parameters_series_array(fmt)
    return _create_data_frame_from_series_array(series_array)


def load(file: _Union[str, _PathLike], parameters: _Dict[str, str] = None, reporter: _Reporter = None) -> Network:
    """
    Load a network from a file. File should be in a supported format.

    Basic compression formats are also supported (gzip, bzip2).

    Args:
       file:       path to the network file
       parameters: a dictionary of import parameters
       reporter:   the reporter to be used to create an execution report, default is None (no report)

    Returns:
        The loaded network

    Examples:

        Some examples of file loading, including relative or absolute paths, and compressed files:

        .. code-block:: python

            network = pp.network.load('network.xiidm')
            network = pp.network.load('/path/to/network.xiidm')
            network = pp.network.load('network.xiidm.gz')
            network = pp.network.load('network.uct')
            ...
    """
    file = _path_to_str(file)
    if parameters is None:
        parameters = {}
    return Network(_pp.load_network(file, parameters, None if reporter is None else reporter._reporter_model))  # pylint: disable=protected-access


def load_from_string(file_name: str, file_content: str, parameters: _Dict[str, str] = None, reporter: _Reporter = None) -> Network:
    """
    Load a network from a string. File content should be in a supported format.

    Args:
       file_name:    file name
       file_content: file content
       parameters:   a dictionary of import parameters

    Returns:
        The loaded network
    """
    if parameters is None:
        parameters = {}
    return Network(_pp.load_network_from_string(file_name, file_content, parameters, None if reporter is None else reporter._reporter_model))  # pylint: disable=protected-access


def get_extensions_names() -> _List[str]:
    """
    Get the list of available extensions.

    Returns:
        the names of the available extensions
    """
    return _pp.get_extensions_names()


def create_line_on_line(network: Network, bbs_or_bus_id: str, new_line_id: str, new_line_r: float, new_line_x: float, new_line_b1: float,
                        new_line_b2: float, new_line_g1: float, new_line_g2: float, line_id: str, line1_id: str = '', line1_name: str = '', line2_id: str = '', line2_name: str = '',
                        position_percent: float = 50.0, create_fictitious_substation: bool = False, fictitious_voltage_level_id: str = '', fictitious_voltage_level_name: str = '',
                        fictitious_substation_id: str = '', fictitious_substation_name: str = '') -> None:
    """
    Connects an existing voltage level to an existing line through a tee point.

    Connects an existing voltage level (in practice a voltage level where we have some loads or generations)
    to an existing line through a tee point. This method cuts an existing line in two, creating a fictitious
    voltage level between them (an a fictitious substation if asked).
    Then it links an existing voltage level to this fictitious voltage level by creating a new line
    described in the provided dataframe.

    Args:
        network: the network
        bbs_or_bus_id: the ID of the existing bus or bus bar section of the voltage level voltage_level_id.
        new_line_id: ID of the new line
        new_line_r: resistance of the new line, in ohms
        new_line_x: reactance of the new line, in ohms
        new_line_b1: shunt susceptance on side 1 of the new line
        new_line_b2: shunt susceptance on side 2 of the new line
        new_line_g1: shunt conductance on side 1 of the new line
        new_line_g2: shunt conductance on side 2 of the new line
        line_id: the id on of the line on which we want to create a tee point.
        line1_id: when the initial line is cut, the line segment at side 1 has a given ID (optional).
        line1_name: when the initial line is cut, the line segment at side 1 has a given name (optional).
        line2_id: when the initial line is cut, the line segment at side 2 has a given ID (optional).
        line2_name: when the initial line is cut, the line segment at side 2 has a given name (optional).
        position_percent: when the existing line is cut in two lines, percent is equal to the ratio between the parameters of the first line
                    and the parameters of the line that is cut multiplied by 100. 100 minus percent is equal to the ratio
                    between the parameters of the second line and the parameters of the line that is cut multiplied by 100.
        create_fictitious_substation: True to create the fictitious voltage level inside a fictitious substation (false by default).
        fictitious_voltage_level_id: the ID of the fictitious voltage level (optional) containing the tee point.
        fictitious_voltage_level_name: the name of the fictitious voltage level (optional) containing the tee point.
        fictitious_substation_id: the ID of the fictitious substation (optional).
        fictitious_substation_name: the name of the fictitious substation (optional).
    """
    _pp.create_line_on_line(network._handle, bbs_or_bus_id, new_line_id, new_line_r, new_line_x, new_line_b1,
                            new_line_b2, new_line_g1, new_line_g2, line_id, line1_id, line1_name, line2_id, line2_name, position_percent,
                            create_fictitious_substation, fictitious_voltage_level_id, fictitious_voltage_level_name, fictitious_substation_id,
                            fictitious_substation_name)


def connect_voltage_level_on_line(network: Network, bbs_or_bus_id: str, line_id: str, position_percent: float = 50.0,
                                  line1_id: str = '', line1_name: str = '', line2_id: str = '', line2_name: str = '') -> None:
    """
    Cuts an existing line in two lines and connects an existing voltage level between them.

    This method cuts an existing line in two lines and connect an existing voltage level between them. The voltage level should
    be added to the network just before calling this method, and should contains at least a configured bus in bus/breaker topology or a bus bar section in node/breaker topology.

    Args:
        network: the network
        bbs_or_bus_id: The ID of the configured bus or bus bar section to which the lines will be connected.
        line_id: the ID ot the line on which the voltage level should be connected.
        position_percent: when the existing line is cut, percent is equal to the ratio between the parameters of the first line
                          and the parameters of the line that is cut multiplied by 100. 100 minus percent is equal to the ratio
                          between the parameters of the second line and the parameters of the line that is cut multiplied by 100.
        line1_id: when the initial line is cut, the line segment at side 1 will receive this ID (optional).
        line1_name: when the initial line is cut, the line segment at side 1 will receive this name (optional).
        line2_id: when the initial line is cut, the line segment at side 2 will receive this ID (optional).
        line2_name: when the initial line is cut, the line segment at side 2 will receive this name (optional).
    """
    _pp.connect_voltage_level_on_line(network._handle, bbs_or_bus_id, line_id, line1_id, line1_name, line2_id, line2_name, position_percent)


def create_load_bay(network: Network, df: _DataFrame = None, raise_exception: bool = False, reporter: _Reporter = None, **kwargs: _ArrayLike) -> None:
    """
    Creates a load, connects it to the network on a given busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the load
        df: Attributes as a dataframe.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is False.
        reporter: optionally, the reporter to be used to create an execution report, default is None (no report).

    Notes:
        The voltage level containing the busbar section should be described in node/breaker topology. The load is
        connected to the busbar with a breaker and a closed disconnector. If the network has position extensions,
        the load will also be connected to every parallel busbar section with an open disconnector.

        Valid attributes are:

        - **id**: the identifier of the new load
        - **name**: an optional human-readable name
        - **type**: optionally, the type of load (UNDEFINED, AUXILIARY, FICTITIOUS)
        - **p0**: active power load, in MW
        - **q0**: reactive power load, in MVar
        - **busbar_section_id**: id of the busbar section to which the injection will be connected with a closed disconnector.
        - **load_position_order**: the order of the load, will fill the ConnectablePosition extension
        - **load_direction**: optionally, the direction of the load, will fill the ConnectablePosition extension, default is BOTTOM.

    """
    return _create_feeder_bay(network, [df], ElementType.LOAD, raise_exception, reporter, **kwargs)


def create_battery_bay(network: Network, df: _DataFrame = None, raise_exception: bool = False, reporter: _Reporter = None, **kwargs: _ArrayLike) -> None:
    """
    Creates a battery, connects it to the network on a given busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the battery
        df: Attributes as a dataframe.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is False.
        reporter: optionally, the reporter to be used to create an execution report, default is None (no report).

    Notes:
        The voltage level containing the busbar section should be described in node/breaker topology. The battery is
        connected to the busbar with a breaker and a closed disconnector. If the network has position extensions,
        the battery will also be connected to every parallel busbar section with an open disconnector.

        Valid attributes are:

        - **id**: the identifier of the new battery
        - **name**: an optional human-readable name
        - **min_p**: minimum active power, in MW
        - **max_p**: maximum active power, in MW
        - **target_p**: active power consumption, in MW
        - **target_q**: reactive power consumption, in MVar
        - **busbar_section_id**: id of the busbar section to which the injection will be connected with a closed disconnector.
        - **battery_position_order**: the order of the battery, will fill the ConnectablePosition extension
        - **battery_direction**: optionally, the direction of the battery, will fill the ConnectablePosition extension, default is BOTTOM.

    """
    return _create_feeder_bay(network, [df], ElementType.BATTERY, raise_exception, reporter, **kwargs)


def create_generator_bay(network: Network, df: _DataFrame = None, raise_exception: bool = False, reporter: _Reporter = None, **kwargs: _ArrayLike) -> None:
    """
    Creates a generator, connects it to the network on a given busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the generator
        df: Attributes as a dataframe.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is False.
        reporter: optionally, the reporter to be used to create an execution report, default is None (no report).
        kwargs: Attributes as keyword arguments.

    Notes:
        The voltage level containing the busbar section should be described in node/breaker topology. The generator
        is connected to the busbar with a breaker and a closed disconnector. If the network has position extensions,
        the generator will also be connected to every parallel busbar section with an open disconnector.

        Valid attributes are:

        - **id**: the identifier of the new generator
        - **energy_source**: the type of energy source (HYDRO, NUCLEAR, ...)
        - **max_p**: maximum active power in MW
        - **min_p**: minimum active power in MW
        - **target_p**: target active power in MW
        - **target_q**: target reactive power in MVar, when the generator does not regulate voltage
        - **rated_s**: nominal power in MVA
        - **target_v**: target voltage in kV, when the generator regulates voltage
        - **voltage_regulator_on**: true if the generator regulates voltage
        - **busbar_section_id**: id of the busbar section to which the injection will be connected with a closed disconnector.
        - **generator_position_order**: the order of the generator, will fill the ConnectablePosition extension
        - **generator_direction**: optionally, the direction of the generator, will fill the ConnectablePosition extension, default is BOTTOM.

    """
    return _create_feeder_bay(network, [df], ElementType.GENERATOR, raise_exception, reporter, **kwargs)


def create_dangling_line_bay(network: Network, df: _DataFrame = None, raise_exception: bool = False, reporter: _Reporter = None, **kwargs: _ArrayLike) -> None:
    """
    Creates a dangling line, connects it to the network on a given busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the dangling line
        df: Attributes as a dataframe.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is False.
        reporter: optionally, the reporter to be used to create an execution report, default is None (no report).
        kwargs: the data to be selected, as named arguments.

    Notes:
        The voltage level containing the busbar section should be described in node/breaker topology.
        The dangling line is connected to the busbar with a breaker and a closed disconnector. If the network
        has position extensions, the dangling line will also be connected to every parallel busbar section with
        an open disconnector.

        Valid attributes are:

        - **id**: the identifier of the new line
        - **name**: an optional human-readable name
        - **p0**: the active power consumption, in MW
        - **q0**: the reactive power consumption, in MVar
        - **r**: the resistance, in Ohms
        - **x**: the reactance, in Ohms
        - **g**: the shunt conductance, in S
        - **b**: the shunt susceptance, in S
        - **busbar_section_id**: id of the busbar section to which the injection will be connected with a closed disconnector.
        - **dangling_line_position_order**: the order of the dangling line, will fill the ConnectablePosition extension
        - **dangling_line_direction**: the direction of the dangling line, will fill the ConnectablePosition extension, default is BOTTOM.

    """
    return _create_feeder_bay(network, [df], ElementType.DANGLING_LINE, raise_exception, reporter, **kwargs)


def create_shunt_compensator_bay(network: Network, shunt_df: _DataFrame,
                                 linear_model_df: _Optional[_DataFrame] = None,
                                 non_linear_model_df: _Optional[_DataFrame] = None,
                                 raise_exception: bool = False, reporter: _Reporter = None) -> None:
    """
    Creates a shunt compensator, connects it to the network on a given busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the shunt compensator
        shunt_df: dataframe for shunt compensators data
        linear_model_df: dataframe for linear model sections data
        non_linear_model_df: dataframe for sections data
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is False.
        reporter: optionally, the reporter to be used to create an execution report, default is None (no report).

    Notes:
        The voltage level containing the busbar section should be described in node/breaker topology.
        The shunt compensator is connected to the busbar with a breaker and a closed disconnector. If the network
        has position extensions, the shunt compensator will also be connected to every parallel busbar section with
        an open disconnector.

        Valid attributes for the shunt compensators dataframe are:

        - **id**: the identifier of the new shunt
        - **name**: an optional human-readable name
        - **model_type**: either LINEAR or NON_LINEAR
        - **section_count**: the current count of connected sections
        - **target_v**: an optional target voltage in kV
        - **target_v**: an optional deadband for the target voltage, in kV
        - **busbar_section_id**: id of the busbar section to which the injection will be connected with a closed disconnector.
        - **shunt_compensator_position_order**: the order of the shunt compensator, will fill the ConnectablePosition extension
        - **shunt_compensator_direction**: the direction of the shunt compensator, will fill the ConnectablePosition extension, default is BOTTOM.

        Valid attributes for the linear sections models are:

        - **id**: the identifier of the new shunt
        - **g_per_section**: the conductance, in Ohm, for each section
        - **b_per_section**: the susceptance, in Ohm, for each section
        - **max_section_count**: the maximum number of connectable sections

        This dataframe must have only one row for each shunt compensator.

        Valid attributes for the non-linear sections models are:

        - **id**: the identifier of the new shunt
        - **g**: the conductance, in Ohm, for this section
        - **b**: the susceptance, in Ohm, for this section

    """
    if linear_model_df is None:
        linear_model_df = pd.DataFrame()
    if non_linear_model_df is None:
        non_linear_model_df = pd.DataFrame()
    dfs: _List[_Optional[_DataFrame]] = [shunt_df, linear_model_df, non_linear_model_df]
    return _create_feeder_bay(network, dfs, ElementType.SHUNT_COMPENSATOR, raise_exception, reporter)


def create_static_var_compensator_bay(network: Network, df: _DataFrame = None, raise_exception: bool = False, reporter: _Reporter = None, **kwargs: _ArrayLike) -> None:
    """
    Creates a static var compensator, connects it to the network on a given busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the static var compensator
        df: Attributes as a dataframe.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is False.
        reporter: optionally, the reporter to be used to create an execution report, default is None (no report).
        kwargs: the data to be selected, as named arguments.

    Notes:
        The voltage level containing the busbar section should be described in node/breaker topology.
        The static var compensator is connected to the busbar with a breaker and a closed disconnector. If the
        network has position extensions, the static var compensator will also be connected to every parallel busbar
        section with an open disconnector.

        Valid attributes are:

        - **id**: the identifier of the new SVC
        - **name**: an optional human-readable name
        - **b_max**: the maximum susceptance, in S
        - **b_min**: the minimum susceptance, in S
        - **regulation_mode**: the regulation mode (VOLTAGE, REACTIVE_POWER, OFF)
        - **target_v**: the target voltage, in kV, when the regulation mode is VOLTAGE
        - **target_q**: the target reactive power, in MVar, when the regulation mode is not VOLTAGE
        - **busbar_section_id**: id of the busbar section to which the injection will be connected with a closed disconnector.
        - **static_var_compensator_position_order**: the order of the static var compensator, will fill the ConnectablePosition extension
        - **static_var_compensator_direction**: the direction of the static var compensator, will fill the ConnectablePosition extension, default is BOTTOM.

    """
    return _create_feeder_bay(network, [df], ElementType.STATIC_VAR_COMPENSATOR, raise_exception, reporter, **kwargs)


def create_lcc_converter_station_bay(network: Network, df: _DataFrame = None, raise_exception: bool = False, reporter: _Reporter = None, **kwargs: _ArrayLike) -> None:
    """
    Creates a lcc converter station, connects it to the network on a given busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the lcc converter station
        df: Attributes as a dataframe.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is False.
        reporter: optionally, the reporter to be used to create an execution report, default is None (no report).
        kwargs: the data to be selected, as named arguments.

    Notes:
        The voltage level containing the busbar section should be described in node/breaker topology.
        The lcc converter station is connected to the busbar with a breaker and a closed disconnector. If the
        network has position extensions, the lcc converter station will also be connected to every parallel busbar
        section with an open disconnector.

        Valid attributes are:

        - **id**: the identifier of the new station
        - **name**: an optional human-readable name
        - **power_factor**: the power factor (ratio of the active power to the apparent power)
        - **loss_factor**: the loss factor of the station
        - **busbar_section_id**: id of the busbar section to which the injection will be connected with a closed disconnector.
        - **lcc_converter_station_position_order**: the order of the lcc converter station, will fill the ConnectablePosition extension
        - **lcc_converter_station_direction**: the direction of the lcc converter station, will fill the ConnectablePosition extension, default is BOTTOM.

    """
    return _create_feeder_bay(network, [df], ElementType.LCC_CONVERTER_STATION, raise_exception, reporter, **kwargs)


def create_vsc_converter_station_bay(network: Network, df: _DataFrame = None, raise_exception: bool = False, reporter: _Reporter = None, **kwargs: _ArrayLike) -> None:
    """
    Creates a vsc converter station, connects it to the network on a given busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the vsc converter station
        df: Attributes as a dataframe.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is False.
        reporter: optionally, the reporter to be used to create an execution report, default is None (no report).
        kwargs: the data to be selected, as named arguments.

    Notes:
        The voltage level containing the busbar section should be described in node/breaker topology.
        The vsc converter station is connected to the busbar with a breaker and a closed disconnector. If the
        network has position extensions, the vsc converter station will also be connected to every parallel busbar
        section with an open disconnector.

        Valid attributes are:

        - **id**: the identifier of the new station
        - **name**: an optional human-readable name
        - **loss_factor**: the loss factor of the new station
        - **voltage_regulator_on**: true if the station regulated voltage
        - **target_v**: the target voltage, in kV, when the station regulates voltage
        - **target_q**: the target reactive power, in MVar, when the station does not regulate voltage
        - **busbar_section_id**: id of the busbar section to which the injection will be connected with a closed disconnector.
        - **vsc_converter_station_position_order**: the order of the vsc converter station, will fill the ConnectablePosition extension
        - **vsc_converter_station_direction**: the direction of the vsc converter station, will fill the ConnectablePosition extension, default is BOTTOM.

    """
    return _create_feeder_bay(network, [df], ElementType.VSC_CONVERTER_STATION, raise_exception, reporter, **kwargs)


def _create_feeder_bay(network: Network, dfs: _List[_Optional[_DataFrame]], element_type: _pp.ElementType, raise_exception: bool, reporter: _Optional[_Reporter], **kwargs: _ArrayLike) -> None:
    """
    Creates an injection, connects it to the network on a given busbar section and creates the associated topology.

    Args:
        network: the network to which we want to add the feeder
        dfs: Attributes as a dataframe.
        element_type: the type of the element to be added.
        raise_exception: optionally, whether the calculation should throw exceptions. In any case, errors will
         be logged. Default is False.
        reporter: optionally, the reporter to be used to create an execution report, default is None (no report).
        kwargs: the data to be selected, as named arguments.

    Notes:
        The voltage level containing the busbar section should be described in node/breaker topology.
        The injection is connected to the busbar with a breaker and a closed disconnector.
        If the network has position extensions, the injection will also be connected to every parallel busbar
        section with an open disconnector.

    """
    metadata = _pp.get_network_elements_creation_dataframes_metadata(element_type)
    c_dfs = _get_c_dataframes_and_add_voltage_level_id(network, dfs, metadata, **kwargs)
    _pp.create_feeder_bay(network._handle, raise_exception, None if reporter is None else reporter._reporter_model, c_dfs, element_type)


def _get_c_dataframes_and_add_voltage_level_id(network: Network, dfs: _List[_Optional[_DataFrame]], metadata: _List[_List[_pp.SeriesMetadata]], **kwargs: _ArrayLike) -> _List[_Optional[_pp.Dataframe]]:
    c_dfs: _List[_Optional[_pp.Dataframe]] = []
    dfs[0] = _adapt_df_or_kwargs(metadata[0], dfs[0], **kwargs)
    if dfs[0] is not None:
        dfs[0]['voltage_level_id'] = dfs[0].apply(lambda row: network.get_busbar_sections(attributes=['voltage_level_id']).loc[row['busbar_section_id']].get(0), axis=1)
    for i, df in enumerate(dfs):
        if df is None:
            c_dfs.append(None)
        else:
            c_dfs.append(_create_c_dataframe(df, metadata[i]))
    return c_dfs


def create_line_bays(network: Network, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
    """
    Creates a line and connects it to busbar sections through standard feeder bays.

    The created bays are composed of one breaker, and one disconnector for each busbar section
    parallel to the section specified in arguments. Only the disconnector on the specified
    section is closed, others are left open.

    Args:
        network: the network to which we want to add the new line
        df: Attributes as a dataframe.
        kwargs: Attributes as keyword arguments.

    Notes:
        The voltage level containing the busbar section must be described in node/breaker topology.

        The input dataframe expects same attributes as :meth:`Network.create_lines`, except for the
        additional following attributes:

        - **busbar_section_id_1**: the identifier of the busbars section on side 1
        - **position_order_1**: the position of the feeder on side 1
        - **direction_1**: the direction, TOP or BOTTOM, of the feeder on side 1
        - **busbar_section_id_2**: the identifier of the busbars section on side 2
        - **position_order_2**: the position of the feeder on side 2
        - **direction_2**: the direction, TOP or BOTTOM, of the feeder on side 2

    Examples:

        .. code-block:: python

            pp.network.create_line_bays(network, id='L', r=0.1, x=10, g1=0, b1=0, g2=0, b2=0,
                                        busbar_section_id_1='BBS1',
                                        position_order_1=115,
                                        direction_1='TOP',
                                        busbar_section_id_2='BBS2',
                                        position_order_2=121,
                                        direction_2='BOTTOM')

    See Also:
        :meth:`Network.create_lines`
    """
    metadata = _pp.get_line_feeder_bays_metadata()
    df = _adapt_df_or_kwargs(metadata, df, **kwargs)
    c_df = _create_c_dataframe(df, metadata)
    _pp.create_branch_feeder_bays_line(network._handle, c_df)


def create_2_windings_transformer_bays(network: Network, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
    """
    Creates a transformer and connects it to busbar sections through standard feeder bays.

    The created bays are composed of one breaker, and one disconnector for each busbar section
    parallel to the section specified in arguments. Only the disconnector on the specified
    section is closed, others are left open.

    Args:
        network: the network to which we want to add the new line
        df: Attributes as a dataframe.
        kwargs: Attributes as keyword arguments.

    Notes:
        The voltage level containing the busbar section must be described in node/breaker topology.

        The input dataframe expects same attributes as :meth:`Network.create_2_windings_transformers`, except for the
        additional following attributes:

        - **busbar_section_id_1**: the identifier of the busbars section on side 1
        - **position_order_1**: the position of the feeder on side 1
        - **direction_1**: the direction, TOP or BOTTOM, of the feeder on side 1
        - **busbar_section_id_2**: the identifier of the busbars section on side 2
        - **position_order_2**: the position of the feeder on side 2
        - **direction_2**: the direction, TOP or BOTTOM, of the feeder on side 2

    Examples:

        .. code-block:: python

            pp.network.create_2_windings_transformers_bays(
                            network, id='L', b=1e-6, g=1e-6, r=0.5, x=10, rated_u1=400, rated_u2=225,
                            busbar_section_id_1='BBS1',
                            position_order_1=115,
                            direction_1='TOP',
                            busbar_section_id_2='BBS2',
                            position_order_2=121,
                            direction_2='BOTTOM')

    See Also:
        :meth:`Network.create_2_windings_transformers`
    """
    metadata = _pp.get_twt_feeder_bays_metadata()
    df = _adapt_df_or_kwargs(metadata, df, **kwargs)
    c_df = _create_c_dataframe(df, metadata)
    _pp.create_branch_feeder_bays_twt(network._handle, c_df)


def get_connectables_order_positions(network: Network, voltage_level_id: str) -> _DataFrame:
    """
    Gets the order positions of every connectable of a given voltage level in a dataframe.

    Args:
        network: the network containing the voltage level.
        voltage_level_id: id of the voltage level for which we want to get the order positions.

    Note:
        About order positions: order positions represent the relative positions of every connectable
        compared to each other on a busbar section. It is filled in the ConnectablePosition extension
        under Order. Each connectable has as many positions as it has feeders. In this method, we get all the
        taken order positions by every connectable at the scale of a voltage level.
    """
    series_array = _pp.get_connectables_order_positions(network._handle, voltage_level_id)
    position_df = _create_data_frame_from_series_array(series_array).sort_values(by=['order_position'])
    position_df['extension_name'] = position_df.apply(lambda row: row['extension_name'].rstrip(), axis=1)
    return position_df


def get_unused_order_positions_before(network: Network, busbar_section_id: str) -> _Optional[pd.Interval]:
    """
    Gets all the available order positions before a busbar section.

    Args:
        network: the network in which the busbar section is.
        busbar_section_id: the id of the busbar section from which we want to get all the available positions before.

    Notes:
        Gets all the available positions between the lowest used position of a given busbar section and the
        highest used position of the busbar section with the section index equal to the section index of the given
        busbar section minus one. The result is an interval that includes the lowest available value and the
        highest available value.
        About order positions: order positions represent the relative positions of every connectable
        compared to each other on a busbar section. It is filled in the ConnectablePosition extension
        under Order. Each connectable has as many positions as it has feeders.

    Examples:
        Let's take two busbar sections. The first one, bbs1, has 3 feeders with taken order positions 5,6,7. The
        second, bbs2 has two feeder with taken order positions 11 and 12.
        Then, get_unused_order_positions_before(bbs1) will return [-infinity, 4] as an interval and
        get_unused_order_positions_before(bbs2) will return [8,10] as an interval.

    """
    positions = _pp.get_unused_order_positions(network._handle, busbar_section_id, 'BEFORE')
    if len(positions) == 0:
        return None
    return pd.Interval(left=positions[0], right=positions[1], closed='both')


def get_unused_order_positions_after(network: Network, busbar_section_id: str) -> _Optional[pd.Interval]:
    """
    Gets all the available order positions after a busbar section.

    Args:
        network: the network in which the busbar section is.
        busbar_section_id: the id of the busbar section from which we want to get all the available positions after.

    Notes:
        Gets all the available positions between the highest used position of a given busbar section and the
        lowest used position of the busbar section with the section index equal to the section index of the given
        busbar section plus one. The result is a list with the lowest available value as the first integer and the
        highest available value as the second integer.
        About order positions: order positions represent the relative positions of every connectable
        compared to each other on a busbar section. It is filled in the ConnectablePosition extension
        under Order. Each connectable has as many positions as it has feeders.

    Examples:
        Let's take two busbar sections. The first one, bbs1, has 3 feeders with taken order positions 5,6,7. The
        second, bbs2 has two feeder with taken order positions 11 and 12.
        Then, get_unused_order_positions_after(bbs1) will return [8, 10] as an interval and
        get_unused_order_positions_before(bbs2) will return [13, +infinity] as an interval.
    """
    positions = _pp.get_unused_order_positions(network._handle, busbar_section_id, 'AFTER')
    if len(positions) == 0:
        return None
    return pd.Interval(left=positions[0], right=positions[1], closed='both')
