#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _pypowsybl
import sys
from _pypowsybl import ElementType
from _pypowsybl import PyPowsyblError
from typing import List
from typing import Set

import pandas as pd
import networkx as _nx
import datetime

from pypowsybl.util import create_data_frame_from_series_array


class SingleLineDiagram:
    """ This class represents a single line diagram."""

    def __init__(self, svg: str):
        self._svg = svg

    @property
    def svg(self):
        return self._svg

    def __str__(self):
        return self._svg

    def _repr_svg_(self):
        return self._svg


class NodeBreakerTopology:
    """ Node-breaker representation of the topology of a voltage level.

    The topology is actually represented as a graph, where
    vertices are called "nodes" and are identified by a unique number in the voltage level,
    while edges are switches (breakers and disconnectors), or internal connections (plain "wires").
    """

    def __init__(self, network_handle, voltage_level_id):
        self._internal_connections = create_data_frame_from_series_array(
            _pypowsybl.get_node_breaker_view_internal_connections(network_handle, voltage_level_id))
        self._switchs = create_data_frame_from_series_array(
            _pypowsybl.get_node_breaker_view_switches(network_handle, voltage_level_id))
        self._nodes = create_data_frame_from_series_array(
            _pypowsybl.get_node_breaker_view_nodes(network_handle, voltage_level_id))

    @property
    def switches(self) -> pd.DataFrame:
        """ The list of switches of the voltage level, together with their connection status, as a dataframe.
        """
        return self._switchs

    @property
    def nodes(self) -> pd.DataFrame:
        """ The list of nodes of the voltage level, together with their corresponding network element (if any),
        as a dataframe.
        """
        return self._nodes

    @property
    def internal_connections(self) -> pd.DataFrame:
        """ The list of internal connection of the voltage level, together with the nodes they connect.
        """
        return self._internal_connections

    def create_graph(self) -> _nx.Graph:
        """ Representation of the topology as a networkx graph.
        """
        graph = _nx.Graph()
        graph.add_nodes_from(self._nodes.index.tolist())
        graph.add_edges_from(self._switchs[['node1', 'node2']].values.tolist())
        graph.add_edges_from(self._internal_connections[['node1', 'node2']].values.tolist())
        return graph


class Network(object):
    def __init__(self, handle):
        self._handle = handle
        att = _pypowsybl.get_network_metadata(self._handle)
        self._id = att.id
        self._name = att.name
        self._source_format = att.source_format
        self._forecast_distance = datetime.timedelta(minutes=att.forecast_distance)
        self._case_date = datetime.datetime.utcfromtimestamp(att.case_date)

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
    def case_date(self) -> datetime.datetime:
        """
        Date of this network case, in UTC timezone.
        """
        return self._case_date

    @property
    def forecast_distance(self) -> datetime.timedelta:
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
        """Save a network to a file using a specified format.

        Args:
            file (str): a file
            format (str, optional): format to save the network, defaults to 'XIIDM'
            parameters (dict, optional): a map of parameters
        """
        _pypowsybl.dump_network(self._handle, file, format, parameters)

    def dump_to_string(self, format: str = 'XIIDM', parameters: dict = {}) -> str:
        """Save a network to a string using a specified format.

        Args:
            format (str, optional): format to export, only support mono file type, defaults to 'XIIDM'
            parameters (dict, optional): a map of parameters

        Returns:
            a string representing network
        """
        return _pypowsybl.dump_network_to_string(self._handle, format, parameters)

    def reduce(self, v_min: float = 0, v_max: float = sys.float_info.max, ids: List[str] = [],
               vl_depths: tuple = (), with_dangling_lines: bool = False):
        vls = []
        depths = []
        for v in vl_depths:
            vls.append(v[0])
            depths.append(v[1])
        _pypowsybl.reduce_network(self._handle, v_min, v_max, ids, vls, depths, with_dangling_lines)

    def write_single_line_diagram_svg(self, container_id: str, svg_file: str):
        """ Create a single line diagram in SVG format from a voltage level or a substation and write to a file.

        Args:
            container_id: a voltage level id or a substation id
            svg_file: a svg file path
        """
        _pypowsybl.write_single_line_diagram_svg(self._handle, container_id, svg_file)

    def get_single_line_diagram(self, container_id: str):
        """ Create a single line diagram from a voltage level or a substation.

        Args:
            container_id: a voltage level id or a substation id

        Returns:
            the single line diagram
        """
        return SingleLineDiagram(_pypowsybl.get_single_line_diagram_svg(self._handle, container_id))

    def get_elements_ids(self, element_type: _pypowsybl.ElementType, nominal_voltages: Set[float] = None,
                         countries: Set[str] = None,
                         main_connected_component: bool = True, main_synchronous_component: bool = True,
                         not_connected_to_same_bus_at_both_sides: bool = False) -> List[str]:
        return _pypowsybl.get_network_elements_ids(self._handle, element_type,
                                                   [] if nominal_voltages is None else list(nominal_voltages),
                                                   [] if countries is None else list(countries),
                                                   main_connected_component, main_synchronous_component,
                                                   not_connected_to_same_bus_at_both_sides)

    def get_elements(self, element_type: _pypowsybl.ElementType) -> pd.DataFrame:
        """ Get network elements as a ``Pandas`` data frame for a specified element type.

        Args:
            element_type (ElementType): the element type
        Returns:
            a network elements data frame for the specified element type
        """
        series_array = _pypowsybl.create_network_elements_series_array(self._handle, element_type)
        return create_data_frame_from_series_array(series_array)

    def get_buses(self) -> pd.DataFrame:
        return self.get_elements(_pypowsybl.ElementType.BUS)

    def get_generators(self) -> pd.DataFrame:
        """ Get generators as a ``Pandas`` data frame.

        Returns:
            a generators data frame
        """
        return self.get_elements(_pypowsybl.ElementType.GENERATOR)

    def get_loads(self) -> pd.DataFrame:
        """ Get loads as a ``Pandas`` data frame.

        Returns:
            a loads data frame
        """
        return self.get_elements(_pypowsybl.ElementType.LOAD)

    def get_batteries(self) -> pd.DataFrame:
        """ Get batteries as a ``Pandas`` data frame.

        Returns:
            a batteries data frame
        """
        return self.get_elements(_pypowsybl.ElementType.BATTERY)

    def get_lines(self) -> pd.DataFrame:
        """ Get lines as a ``Pandas`` data frame.

        Returns:
            a lines data frame
        """
        return self.get_elements(_pypowsybl.ElementType.LINE)

    def get_2_windings_transformers(self) -> pd.DataFrame:
        """ Get 2 windings transformers as a ``Pandas`` data frame.

        Returns:
            a 2 windings transformers data frame
        """
        return self.get_elements(_pypowsybl.ElementType.TWO_WINDINGS_TRANSFORMER)

    def get_3_windings_transformers(self) -> pd.DataFrame:
        """ Get 3 windings transformers as a ``Pandas`` data frame.

        Returns:
            a 3 windings transformers data frame
        """
        return self.get_elements(_pypowsybl.ElementType.THREE_WINDINGS_TRANSFORMER)

    def get_shunt_compensators(self) -> pd.DataFrame:
        """ Get shunt compensators as a ``Pandas`` data frame.

        Returns:
            a shunt compensators data frame
        """
        return self.get_elements(_pypowsybl.ElementType.SHUNT_COMPENSATOR)

    def get_non_linear_shunt_compensator_sections(self) -> pd.DataFrame:
        """ Get shunt compensators sections for non linear model as a ``Pandas`` data frame.

        Returns:
            a shunt compensators data frame
        """
        return self.get_elements(_pypowsybl.ElementType.NON_LINEAR_SHUNT_COMPENSATOR_SECTION)

    def get_dangling_lines(self) -> pd.DataFrame:
        """ Get dangling lines as a ``Pandas`` data frame.

        Returns:
            a dangling lines data frame
        """
        return self.get_elements(_pypowsybl.ElementType.DANGLING_LINE)

    def get_lcc_converter_stations(self) -> pd.DataFrame:
        """ Get LCC converter stations as a ``Pandas`` data frame.

        Returns:
            a LCC converter stations data frame
        """
        return self.get_elements(_pypowsybl.ElementType.LCC_CONVERTER_STATION)

    def get_vsc_converter_stations(self) -> pd.DataFrame:
        """ Get VSC converter stations as a ``Pandas`` data frame.

        Returns:
            a VSC converter stations data frame
        """
        return self.get_elements(_pypowsybl.ElementType.VSC_CONVERTER_STATION)

    def get_static_var_compensators(self) -> pd.DataFrame:
        """ Get static var compensators as a ``Pandas`` data frame.

        Returns:
            a static var compensators data frame
        """
        return self.get_elements(_pypowsybl.ElementType.STATIC_VAR_COMPENSATOR)

    def get_voltage_levels(self) -> pd.DataFrame:
        """ Get voltage levels as a ``Pandas`` data frame.

        Returns:
            a voltage levels data frame
        """
        return self.get_elements(_pypowsybl.ElementType.VOLTAGE_LEVEL)

    def get_busbar_sections(self) -> pd.DataFrame:
        """ Get busbar sections as a ``Pandas`` data frame.

        Returns:
            a busbar sections data frame
        """
        return self.get_elements(_pypowsybl.ElementType.BUSBAR_SECTION)

    def get_substations(self) -> pd.DataFrame:
        """ Get substations ``Pandas`` data frame.

        Returns:
            a substations data frame
        """
        return self.get_elements(_pypowsybl.ElementType.SUBSTATION)

    def get_hvdc_lines(self) -> pd.DataFrame:
        """ Get HVDC lines as a ``Pandas`` data frame.

        Returns:
            a HVDC lines data frame
        """
        return self.get_elements(_pypowsybl.ElementType.HVDC_LINE)

    def get_switches(self) -> pd.DataFrame:
        """ Get switches as a ``Pandas`` data frame.

        Returns:
            a switches data frame
        """
        return self.get_elements(_pypowsybl.ElementType.SWITCH)

    def get_ratio_tap_changer_steps(self) -> pd.DataFrame:
        """ Get ratio tap changer steps as a ``Pandas`` data frame.

        Returns:
            a ratio tap changer steps data frame
        """
        return self.get_elements(_pypowsybl.ElementType.RATIO_TAP_CHANGER_STEP)

    def get_phase_tap_changer_steps(self) -> pd.DataFrame:
        """ Get phase tap changer steps as a ``Pandas`` data frame.

        Returns:
            a phase tap changer steps data frame
        """
        return self.get_elements(_pypowsybl.ElementType.PHASE_TAP_CHANGER_STEP)

    def get_ratio_tap_changers(self) -> pd.DataFrame:
        """ Create a ratio tap changers``Pandas`` data frame.

        Returns:
            the ratio tap changers data frame
        """
        return self.get_elements(_pypowsybl.ElementType.RATIO_TAP_CHANGER)

    def get_phase_tap_changers(self) -> pd.DataFrame:
        """ Create a phase tap changers``Pandas`` data frame.

        Returns:
            the phase tap changers data frame
        """
        return self.get_elements(_pypowsybl.ElementType.PHASE_TAP_CHANGER)

    def get_reactive_capability_curve_points(self) -> pd.DataFrame:
        """ Get reactive capability curve points as a ``Pandas`` data frame.

        Returns:
            a reactive capability curve points data frame
        """
        return self.get_elements(_pypowsybl.ElementType.REACTIVE_CAPABILITY_CURVE_POINT)

    def update_elements(self, element_type: _pypowsybl.ElementType, df: pd.DataFrame):
        """ Update network elements with a ``Pandas`` data frame for a specified element type.
        The data frame columns are mapped to IIDM element attributes and each row is mapped to an element using the
        index.

        Args:
            element_type (ElementType): the element type
            df (DataFrame): the ``Pandas`` data frame
        """
        for series_name in df.columns.values:
            series = df[series_name]
            series_type = _pypowsybl.get_series_type(element_type, series_name)
            if series_type == 2 or series_type == 3:
                _pypowsybl.update_network_elements_with_int_series(self._handle, element_type, series_name,
                                                                   df.index.values,
                                                                   series.values, len(series))
            elif series_type == 1:
                _pypowsybl.update_network_elements_with_double_series(self._handle, element_type, series_name,
                                                                      df.index.values,
                                                                      series.values, len(series))
            elif series_type == 0:
                _pypowsybl.update_network_elements_with_string_series(self._handle, element_type, series_name,
                                                                      df.index.values,
                                                                      series.values, len(series))
            else:
                raise PyPowsyblError(
                    f'Unsupported series type {series_type}, element type: {element_type}, series_name: {series_name}')

    def update_buses(self, df: pd.DataFrame):
        """ Update buses with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements(_pypowsybl.ElementType.BUS, df)

    def update_switches(self, df: pd.DataFrame):
        """ Update switches with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements(_pypowsybl.ElementType.SWITCH, df)

    def update_generators(self, df: pd.DataFrame):
        """ Update generators with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements(_pypowsybl.ElementType.GENERATOR, df)

    def update_loads(self, df: pd.DataFrame):
        """ Update loads with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements(_pypowsybl.ElementType.LOAD, df)

    def update_batteries(self, df: pd.DataFrame):
        """ Update batteries with a ``Pandas`` data frame.

        Available columns names:
        - p0
        - q0
        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements(_pypowsybl.ElementType.BATTERY, df)

    def update_dangling_lines(self, df: pd.DataFrame):
        """ Update dangling lines with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements(_pypowsybl.ElementType.DANGLING_LINE, df)

    def update_vsc_converter_stations(self, df: pd.DataFrame):
        """ Update VSC converter stations with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements(_pypowsybl.ElementType.VSC_CONVERTER_STATION, df)

    def update_static_var_compensators(self, df: pd.DataFrame):
        """ Update static var compensators with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements(_pypowsybl.ElementType.STATIC_VAR_COMPENSATOR, df)

    def update_hvdc_lines(self, df: pd.DataFrame):
        """ Update HVDC lines with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements(_pypowsybl.ElementType.HVDC_LINE, df)

    def update_lines(self, df: pd.DataFrame):
        """ Update lines with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements(_pypowsybl.ElementType.LINE, df)

    def update_2_windings_transformers(self, df: pd.DataFrame):
        """ Update 2 windings transformers with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements(_pypowsybl.ElementType.TWO_WINDINGS_TRANSFORMER, df)

    def update_ratio_tap_changers(self, df: pd.DataFrame):
        """ Update ratio tap changers with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements(_pypowsybl.ElementType.RATIO_TAP_CHANGER, df)

    def update_phase_tap_changers(self, df: pd.DataFrame):
        """ Update phase tap changers with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements(_pypowsybl.ElementType.PHASE_TAP_CHANGER, df)

    def update_shunt_compensators(self, df: pd.DataFrame):
        """ Update shunt compensators with a ``Pandas`` data frame.

        Args:
           df (DataFrame): the ``Pandas`` data frame
               columns that can be updated :
                   - p
                   - q
                   - section_count
                   - connected

        Returns:
            a dataframe updated
        """
        return self.update_elements(_pypowsybl.ElementType.SHUNT_COMPENSATOR, df)

    def get_working_variant_id(self):
        """ The current working variant ID

        Returns:
            the id of the currently selected variant

        """
        return _pypowsybl.get_working_variant_id(self._handle)

    def clone_variant(self, src: str, target: str, may_overwrite=True):
        """ Creates a copy of the source variant

        Args:
            src: variant to copy
            target: id of the new variant that will be a copy of src
            may_overwrite: indicates if the target can be overwritten when it already exists
        """
        _pypowsybl.clone_variant(self._handle, src, target, may_overwrite)

    def set_working_variant(self, variant: str):
        """ Changes the working variant. The provided variant ID must correspond
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

    def get_voltage_level_topology(self, voltage_level_id: str) -> NodeBreakerTopology:
        """ Get the node breaker description of the topology of a voltage level.

        Args:
            voltage_level_id: id of the voltage level

        Returns:
            The node breaker description of the topology of the voltage level
        """
        return NodeBreakerTopology(self._handle, voltage_level_id)


def _create_network(name, network_id=''):
    return Network(_pypowsybl.create_network(name, network_id))


def create_empty(id: str = "Default") -> Network:
    """ Create an empty network.

    Args:
        id: id of the network, defaults to 'Default'

    Returns:
        a new empty network
    """
    return _create_network('empty', network_id=id)


def create_ieee9() -> Network:
    """ Create an instance of IEEE 9 bus network

    Returns:
        a new instance of IEEE 9 bus network
    """
    return _create_network('ieee9')


def create_ieee14() -> Network:
    """ Create an instance of IEEE 14 bus network

    Returns:
        a new instance of IEEE 14 bus network
    """
    return _create_network('ieee14')


def create_ieee30() -> Network:
    """ Create an instance of IEEE 30 bus network

    Returns:
        a new instance of IEEE 30 bus network
    """
    return _create_network('ieee30')


def create_ieee57() -> Network:
    """ Create an instance of IEEE 57 bus network

    Returns:
        a new instance of IEEE 57 bus network
    """
    return _create_network('ieee57')


def create_ieee118() -> Network:
    """ Create an instance of IEEE 118 bus network

    Returns:
        a new instance of IEEE 118 bus network
    """
    return _create_network('ieee118')


def create_ieee300() -> Network:
    """ Create an instance of IEEE 300 bus network

    Returns:
        a new instance of IEEE 300 bus network
    """
    return _create_network('ieee300')


def create_eurostag_tutorial_example1_network() -> Network:
    """ Create an instance of example 1 network of Eurostag tutorial

    Returns:
        a new instance of example 1 network of Eurostag tutorial
    """
    return _create_network('eurostag_tutorial_example1')


def create_four_substations_node_breaker_network() -> Network:
    """ Create an instance of powsybl "4 substations" test case.

    It is meant to contain most network element types that can be
    represented in powsybl networks.
    The topology is in node-breaker representation.

    Returns:
        a new instance of powsybl "4 substations" test case
    """
    return _create_network('four_substations_node_breaker')


def get_import_formats() -> List[str]:
    """ Get list of supported import formats

    :return: the list of supported import formats
    :rtype: List[str]
    """
    return _pypowsybl.get_network_import_formats()


def get_export_formats() -> List[str]:
    """ Get list of supported export formats

    :return: the list of supported export formats
    :rtype: List[str]
    """
    return _pypowsybl.get_network_export_formats()


def get_import_parameters(format: str) -> pd.DataFrame:
    """ Get supported parameters infos for a given format

    :param format: the format
    :return: parameters infos
    :rtype: pd.DataFrame
    """
    series_array = _pypowsybl.create_importer_parameters_series_array(format)
    return create_data_frame_from_series_array(series_array)


def load(file: str, parameters: dict = {}) -> Network:
    """ Load a network from a file. File should be in a supported format.

    Args:
       file (str): a file
       parameters (dict, optional): a map of parameters

    Returns:
        a network
    """
    return Network(_pypowsybl.load_network(file, parameters))


def load_from_string(file_name: str, file_content: str, parameters: dict = {}) -> Network:
    """ Load a network from a string. File content should be in a supported format.

    Args:
       file_name (str): file name
       file_content (str): file content
       parameters (dict, optional): a map of parameters

    Returns:
        a network
    """
    return Network(_pypowsybl.load_network_from_string(file_name, file_content, parameters))
