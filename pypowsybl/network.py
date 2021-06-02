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


def create_buses_data_frame(self) -> pd.DataFrame:
    """

    Returns:
        The bus data frame.

    Note:
        The resulting dataframe will have the following columns:

          - "v_mag": the voltage magnitude (in pair unit)
          - "v_ang": the voltage angle (in degree)

        This dataframe is index by the name of the buses

    Examples

        .. code-block:: python

            import pypowsybl as pypo
            net = pypo.network.create_ieee14()
            net.create_buses_data_frame()

        It outputs something like:

        ======  =======  =========
         .      `v_mag`   `v_ang`
        ======  =======  =========
        id
        VL1_0   1.060     0.00
        VL2_0   1.045    -4.98
        VL3_0   1.010    -12.72
        VL4_0   1.019    -10.33
        VL5_0   1.020     -8.78
        VL6_0   1.070    -14.22
        VL7_0   1.062    -13.37
        VL8_0   1.090    -13.36
        VL9_0   1.056    -14.94
        VL10_0  1.051    -15.10
        VL11_0  1.057    -14.79
        VL12_0  1.055    -15.07
        VL13_0  1.050    -15.16
        VL14_0  1.036    -16.04
        ======  =======  =========

    """
    return self.create_elements_data_frame(_pypowsybl.ElementType.BUS)


def get_lines(self) -> pd.DataFrame:
    """ Get lines as a ``Pandas`` data frame.

    Returns:
<<<<<<< HEAD
        a lines data frame
||||||| merged common ancestors
        the generator data frame
=======
        the generator data frame.

    Note:
        The resulting dataframe will have the following columns:

          - "energy_source": the energy source used to fuel the generator
          - "target_p": the target active value for the generator (in MW)
          - "max_p": the maximum active value for the generator  (MW)
          - "min_p": the minimum active value for the generator  (MW)
          - "target_v": the target voltage magnitude value for the generator (in pair unit)
          - "target_q": the target reactive value for the generator (in MVAr)
          - "voltage_regulator_on":
          - "p" the actual active production of the generator (Nan if no powerflow has been computed)
          - "q" the actual reactive production of the generator (Nan if no powerflow has been computed)
          - "voltage_level_id": at which substation this generator is connected
          - "bus_id": at which bus this generator is computed

        This dataframe is index by the name of the generators

    Examples

        .. code-block:: python

            import pypowsybl as pypo
            net = pypo.network.create_ieee14()
            net.create_generators_data_frame()

        It outputs something like:

        ====  ============= ========  ====== =======  ========  ========  ====================  ==== ==== ================ ======
         .    energy_source target_p   max_p   min_p  target_v  target_q  voltage_regulator_on   p    q   voltage_level_id bus_id
        ====  ============= ========  ====== =======  ========  ========  ====================  ==== ==== ================ ======
        id
        B1-G         OTHER     232.4  9999.0 -9999.0     1.060     -16.9                  True  NaN  NaN              VL1  VL1_0
        B2-G         OTHER      40.0  9999.0 -9999.0     1.045      42.4                  True  NaN  NaN              VL2  VL2_0
        B3-G         OTHER       0.0  9999.0 -9999.0     1.010      23.4                  True  NaN  NaN              VL3  VL3_0
        B6-G         OTHER       0.0  9999.0 -9999.0     1.070      12.2                  True  NaN  NaN              VL6  VL6_0
        B8-G         OTHER       0.0  9999.0 -9999.0     1.090      17.4                  True  NaN  NaN              VL8  VL8_0
        ====  ============= ========  ====== =======  ========  ========  ====================  ==== ==== ================ ======
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
<<<<<<< HEAD
        a shunt compensators data frame
||||||| merged common ancestors
        the line data frame
=======
        the line data frame

    Note:
        The resulting dataframe will have the following columns:

          - "r": the resistance of the line (TODO unit, pu or not ?)
          - "x": the reactance of the line (TODO unit, pu or not ?)
          - "b1": the susceptance of line at its "1" side (TODO unit, pu or not ?)
          - "g1": the  conductance of line at its "1" side (TODO unit, pu or not ?)
          - "b2": the susceptance of line at its "2" side (TODO unit, pu or not ?)
          - "g2": the  conductance of line at its "2" side (TODO unit, pu or not ?)
          - "p1": the active flow on the line at its "1" side, Nan if no powerlow are computed (in MW)
          - "q1": the reactive flow on the line at its "1" side, Nan if no powerlow are computed  (in MVAr)
          - "p2": the active flow on the line at its "2" side, Nan if no powerlow are computed  (in MW)
          - "q2": the reactive flow on the line at its "2" side, Nan if no powerlow are computed  (in MVAr)
          -  "voltage_level1_id": at which substation the "1" side of the powerline is connected
          -  "voltage_level2_id": at which substation the "2" side of the powerline is connected
          -  "bus1_id": at which bus the "1" side of the powerline is connected
          -  "bus2_id": at which bus the "2" side of the powerline is connected

        This dataframe is index by the name of the powerlines

    Examples

        .. code-block:: python

            import pypowsybl as pypo
            net = pypo.network.create_ieee14()
            net.create_lines_data_frame()

        It outputs something like:

        ========  ========  ========  ===  ====  ===  ==== === === === ==== ================= ================= ======= =======
            .            r         x   g1    b1   g2    b2  p1  q1  p2  q2  voltage_level1_id voltage_level2_id bus1_id bus2_id
        ========  ========  ========  ===  ====  ===  ==== === === === ==== ================= ================= ======= =======
        id
        L1-2-1    0.000194  0.000592  0.0  2.64  0.0  2.64 NaN NaN NaN NaN               VL1               VL2   VL1_0   VL2_0
        L1-5-1    0.000540  0.002230  0.0  2.46  0.0  2.46 NaN NaN NaN NaN               VL1               VL5   VL1_0   VL5_0
        L2-3-1    0.000470  0.001980  0.0  2.19  0.0  2.19 NaN NaN NaN NaN               VL2               VL3   VL2_0   VL3_0
        L2-4-1    0.000581  0.001763  0.0  1.70  0.0  1.70 NaN NaN NaN NaN               VL2               VL4   VL2_0   VL4_0
        L2-5-1    0.000570  0.001739  0.0  1.73  0.0  1.73 NaN NaN NaN NaN               VL2               VL5   VL2_0   VL5_0
        L3-4-1    0.000670  0.001710  0.0  0.64  0.0  0.64 NaN NaN NaN NaN               VL3               VL4   VL3_0   VL4_0
        L4-5-1    0.000134  0.000421  0.0  0.00  0.0  0.00 NaN NaN NaN NaN               VL4               VL5   VL4_0   VL5_0
        L6-11-1   0.000950  0.001989  0.0  0.00  0.0  0.00 NaN NaN NaN NaN               VL6              VL11   VL6_0  VL11_0
        L6-12-1   0.001229  0.002558  0.0  0.00  0.0  0.00 NaN NaN NaN NaN               VL6              VL12   VL6_0  VL12_0
        L6-13-1   0.000661  0.001303  0.0  0.00  0.0  0.00 NaN NaN NaN NaN               VL6              VL13   VL6_0  VL13_0
        L7-8-1    0.000000  0.001762  0.0  0.00  0.0  0.00 NaN NaN NaN NaN               VL7               VL8   VL7_0   VL8_0
        L7-9-1    0.000000  0.001100  0.0  0.00  0.0  0.00 NaN NaN NaN NaN               VL7               VL9   VL7_0   VL9_0
        L9-10-1   0.000318  0.000845  0.0  0.00  0.0  0.00 NaN NaN NaN NaN               VL9              VL10   VL9_0  VL10_0
        L9-14-1   0.001271  0.002704  0.0  0.00  0.0  0.00 NaN NaN NaN NaN               VL9              VL14   VL9_0  VL14_0
        L10-11-1  0.000821  0.001921  0.0  0.00  0.0  0.00 NaN NaN NaN NaN              VL10              VL11  VL10_0  VL11_0
        L12-13-1  0.002209  0.001999  0.0  0.00  0.0  0.00 NaN NaN NaN NaN              VL12              VL13  VL12_0  VL13_0
        L13-14-1  0.001709  0.003480  0.0  0.00  0.0  0.00 NaN NaN NaN NaN              VL13              VL14  VL13_0  VL14_0
        ========  ========  ========  ===  ====  ===  ==== === === === ==== ================= ================= ======= =======
    """
    return self.get_elements(_pypowsybl.ElementType.SHUNT_COMPENSATOR)


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


def _create_battery_network() -> Network:
    return Network(_pypowsybl.create_battery_network())


def _create_dangling_lines_network() -> Network:
    return Network(_pypowsybl.create_dangling_line_network())


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
