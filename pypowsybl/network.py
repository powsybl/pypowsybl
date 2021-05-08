#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import sys
import _pypowsybl
from _pypowsybl import Bus
from _pypowsybl import Generator
from _pypowsybl import Load
from _pypowsybl import PyPowsyblError
from _pypowsybl import ElementType
from pypowsybl.util import ObjectHandle
from pypowsybl.util import create_data_frame_from_series_array
from typing import List
from typing import Set
import pandas as pd

Bus.__repr__ = lambda self: f"{self.__class__.__name__}("\
                            f"id={self.id!r}"\
                            f", v_magnitude={self.v_magnitude!r}"\
                            f", v_angle={self.v_angle!r}"\
                            f", component_num={self.component_num!r}"\
                            f")"

Generator.__repr__ = lambda self: f"{self.__class__.__name__}("\
                            f"id={self.id!r}"\
                            f", target_p={self.target_p!r}"\
                            f", min_p={self.min_p!r}"\
                            f", max_p={self.max_p!r}"\
                            f", nominal_voltage={self.nominal_voltage!r}"\
                            f", country={self.country!r}"\
                            f", bus={self.bus!r}"\
                            f")"

Load.__repr__ = lambda self: f"{self.__class__.__name__}("\
                             f"id={self.id!r}"\
                             f", p0={self.p0!r}"\
                             f", nominal_voltage={self.nominal_voltage!r}"\
                             f", country={self.country!r}"\
                             f", bus={self.bus!r}"\
                             f")"


class Network(ObjectHandle):
    def __init__(self, ptr):
        ObjectHandle.__init__(self, ptr)

    @property
    def buses(self) -> List[Bus]:
        """ Get buses from the bus/branch view of the network model.

        :return:
        """
        return _pypowsybl.get_buses(self.ptr)

    @property
    def generators(self):
        return _pypowsybl.get_generators(self.ptr)

    @property
    def loads(self):
        return _pypowsybl.get_loads(self.ptr)

    def open_switch(self, id: str):
        return _pypowsybl.update_switch_position(self.ptr, id, True)

    def close_switch(self, id: str):
        return _pypowsybl.update_switch_position(self.ptr, id, False)

    def connect(self, id: str):
        return _pypowsybl.update_connectable_status(self.ptr, id, True)

    def disconnect(self, id: str):
        return _pypowsybl.update_connectable_status(self.ptr, id, False)

    def dump(self, file: str, format: str = 'XIIDM', parameters: dict = {}):
        """Save a network to a file using a specified format.

        :param file: a file
        :type file: str
        :param format: format to save the network
        :type format: str, defaults to 'XIIDM'
        :param parameters: a map of parameters
        :type parameters: dict
        """
        _pypowsybl.dump_network(self.ptr, file, format, parameters)

    def reduce(self, v_min: float = 0, v_max: float = sys.float_info.max, ids: List[str] = [],
               vl_depths: tuple = (), with_dangling_lines: bool = False):
        vls = []
        depths = []
        for v in vl_depths:
            vls.append(v[0])
            depths.append(v[1])
        _pypowsybl.reduce_network(self.ptr, v_min, v_max, ids, vls, depths, with_dangling_lines)

    def write_single_line_diagram_svg(self, container_id: str, svg_file: str):
        _pypowsybl.write_single_line_diagram_svg(self.ptr, container_id, svg_file)

    def get_elements_ids(self, element_type: _pypowsybl.ElementType, nominal_voltages: Set[float] = None, countries: Set[str] = None,
                         main_connected_component: bool = True, main_synchronous_component: bool = True,
                         not_connected_to_same_bus_at_both_sides: bool = False) -> List[str]:
        return _pypowsybl.get_network_elements_ids(self.ptr, element_type, [] if nominal_voltages is None else list(nominal_voltages),
                                                [] if countries is None else list(countries), main_connected_component, main_synchronous_component,
                                                not_connected_to_same_bus_at_both_sides)

    def create_elements_data_frame(self, element_type: _pypowsybl.ElementType) -> pd.DataFrame:
        """ Create a network element ``Pandas`` data frame for a specified element type.

        Args:
            element_type (ElementType): the element type
        Returns:
            the network element data frame for the specified element type
        """
        series_array = _pypowsybl.create_network_elements_series_array(self.ptr, element_type)
        return create_data_frame_from_series_array(series_array)

    def create_buses_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_pypowsybl.ElementType.BUS)

    def create_generators_data_frame(self) -> pd.DataFrame:
        """ Create a generator ``Pandas`` data frame.

        Returns:
            the generator data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.GENERATOR)

    def create_loads_data_frame(self) -> pd.DataFrame:
        """ Create a generator ``Pandas`` data frame.

        Returns:
            the generator data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.LOAD)

    def create_lines_data_frame(self) -> pd.DataFrame:
        """ Create a line ``Pandas`` data frame.

        Returns:
            the line data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.LINE)

    def create_2_windings_transformers_data_frame(self) -> pd.DataFrame:
        """ Create a 2 windings transformer ``Pandas`` data frame.

        Returns:
            the 2 windings transformer data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.TWO_WINDINGS_TRANSFORMER)

    def create_3_windings_transformers_data_frame(self) -> pd.DataFrame:
        """ Create a 3 windings transformer ``Pandas`` data frame.

        Returns:
            the 3 windings transformer data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.THREE_WINDINGS_TRANSFORMER)

    def create_shunt_compensators_data_frame(self) -> pd.DataFrame:
        """ Create a shunt compensator ``Pandas`` data frame.

        Returns:
            the shunt compensator data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.SHUNT_COMPENSATOR)

    def create_dangling_lines_data_frame(self) -> pd.DataFrame:
        """ Create a dangling line ``Pandas`` data frame.

        Returns:
            the dangling line data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.DANGLING_LINE)

    def create_lcc_converter_stations_data_frame(self) -> pd.DataFrame:
        """ Create a LCC converter station ``Pandas`` data frame.

        Returns:
            the LCC converter station data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.LCC_CONVERTER_STATION)

    def create_vsc_converter_stations_data_frame(self) -> pd.DataFrame:
        """ Create a VSC converter station ``Pandas`` data frame.

        Returns:
            the VSC converter station data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.VSC_CONVERTER_STATION)

    def create_static_var_compensators_data_frame(self) -> pd.DataFrame:
        """ Create a static var compensator ``Pandas`` data frame.

        Returns:
            the static var compensator data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.STATIC_VAR_COMPENSATOR)

    def create_voltage_levels_data_frame(self) -> pd.DataFrame:
        """ Create a voltage level ``Pandas`` data frame.

        Returns:
            the voltage level data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.VOLTAGE_LEVEL)

    def create_busbar_sections_data_frame(self) -> pd.DataFrame:
        """ Create a busbar section ``Pandas`` data frame.

        Returns:
            the busbar section data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.BUSBAR_SECTION)

    def create_substations_data_frame(self) -> pd.DataFrame:
        """ Create a substation ``Pandas`` data frame.

        Returns:
            the substation data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.SUBSTATION)

    def create_hvdc_lines_data_frame(self) -> pd.DataFrame:
        """ Create a HVDC line ``Pandas`` data frame.

        Returns:
            the HVDC line data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.HVDC_LINE)

    def create_switches_data_frame(self) -> pd.DataFrame:
        """ Create a switch ``Pandas`` data frame.

        Returns:
            the switch data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.SWITCH)

    def create_ratio_tap_changer_steps_data_frame(self) -> pd.DataFrame:
        """ Create a ratio tap changer step ``Pandas`` data frame.

        Returns:
            the ratio tap changer step data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.RATIO_TAP_CHANGER_STEP)

    def create_phase_tap_changer_steps_data_frame(self) -> pd.DataFrame:
        """ Create a phase tap changer step ``Pandas`` data frame.

        Returns:
            the phase tap changer step data frame
        """
        return self.create_elements_data_frame(_pypowsybl.ElementType.PHASE_TAP_CHANGER_STEP)

    def update_elements_with_data_frame(self, element_type: _pypowsybl.ElementType, df: pd.DataFrame):
        """ Update network elements with a ``Pandas`` data frame for a specified element type.
        The data frame columns are mapped to IIDM element attributes and each row is mapped to an element using the
        index.

        Args:
            element_type (ElementType): the element type
            df (DataFrame): the ``Pandas`` data frame
        """
        for seriesName in df.columns.values:
            series = df[seriesName]
            series_type = _pypowsybl.get_series_type(element_type, seriesName)
            if series_type == 2 or series_type == 3:
                _pypowsybl.update_network_elements_with_int_series(self.ptr, element_type, seriesName, df.index.values,
                                                                   series.values, len(series))
            elif series_type == 1:
                _pypowsybl.update_network_elements_with_double_series(self.ptr, element_type, seriesName,
                                                                      df.index.values,
                                                                      series.values, len(series))
            elif series_type == 0:
                _pypowsybl.update_network_elements_with_string_series(self.ptr, element_type, seriesName,
                                                                      df.index.values,
                                                                      series.values, len(series))
            else:
                raise PyPowsyblError(f'Unsupported series type {series_type}, element type: {element_type}, series_name: {seriesName}')

    def update_switches_with_data_frame(self, df: pd.DataFrame):
        """ Update switches with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements_with_data_frame(_pypowsybl.ElementType.SWITCH, df)

    def update_generators_with_data_frame(self, df: pd.DataFrame):
        """ Update generators with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements_with_data_frame(_pypowsybl.ElementType.GENERATOR, df)

    def update_loads_with_data_frame(self, df: pd.DataFrame):
        """ Update loads with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements_with_data_frame(_pypowsybl.ElementType.LOAD, df)

    def update_dangling_lines_with_data_frame(self, df: pd.DataFrame):
        """ Update dangling lines with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements_with_data_frame(_pypowsybl.ElementType.DANGLING_LINE, df)

    def update_vsc_converter_stations_with_data_frame(self, df: pd.DataFrame):
        """ Update VSC converter stations with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements_with_data_frame(_pypowsybl.ElementType.VSC_CONVERTER_STATION, df)

    def update_static_var_compensators_with_data_frame(self, df: pd.DataFrame):
        """ Update static var compensators with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements_with_data_frame(_pypowsybl.ElementType.STATIC_VAR_COMPENSATOR, df)

    def update_hvdc_lines_with_data_frame(self, df: pd.DataFrame):
        """ Update HVDC lines with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements_with_data_frame(_pypowsybl.ElementType.HVDC_LINE, df)

    def update_2_windings_transformer_with_data_frame(self, df: pd.DataFrame):
        """ Update 2 windings transformer with a ``Pandas`` data frame.

        Args:
            df (DataFrame): the ``Pandas`` data frame
        """
        return self.update_elements_with_data_frame(_pypowsybl.ElementType.TWO_WINDINGS_TRANSFORMER, df)


def create_empty(id: str = "Default") -> Network:
    """ Create an empty network.

    :param id: id of the network, defaults to 'Default'
    :type id: str, optional
    :return: an empty network
    :rtype: Network
    """
    return Network(_pypowsybl.create_empty_network(id))


def create_ieee9() -> Network:
    return Network(_pypowsybl.create_ieee_network(9))


def create_ieee14() -> Network:
    return Network(_pypowsybl.create_ieee_network(14))


def create_ieee30() -> Network:
    return Network(_pypowsybl.create_ieee_network(30))


def create_ieee57() -> Network:
    return Network(_pypowsybl.create_ieee_network(57))


def create_ieee118() -> Network:
    return Network(_pypowsybl.create_ieee_network(118))


def create_ieee300() -> Network:
    return Network(_pypowsybl.create_ieee_network(300))


def create_eurostag_tutorial_example1_network() -> Network:
    return Network(_pypowsybl.create_eurostag_tutorial_example1_network())


def create_four_substations_node_breaker_network() -> Network:
    return Network(_pypowsybl.create_four_substations_node_breaker_network())


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

    :param file: a file
    :type file: str
    :param parameters: a map of parameters
    :type parameters: dict, optional
    :return: a network
    """
    return Network(_pypowsybl.load_network(file, parameters))
