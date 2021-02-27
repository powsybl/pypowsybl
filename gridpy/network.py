#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _gridpy
from _gridpy import Bus
from _gridpy import Generator
from _gridpy import Load
from _gridpy import ElementType
from gridpy.util import ObjectHandle
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
    def buses(self):
        return _gridpy.get_buses(self.ptr)

    @property
    def generators(self):
        return _gridpy.get_generators(self.ptr)

    @property
    def loads(self):
        return _gridpy.get_loads(self.ptr)

    def open_switch(self, id: str):
        return _gridpy.update_switch_position(self.ptr, id, True)

    def close_switch(self, id: str):
        return _gridpy.update_switch_position(self.ptr, id, False)

    def connect(self, id: str):
        return _gridpy.update_connectable_status(self.ptr, id, True)

    def disconnect(self, id: str):
        return _gridpy.update_connectable_status(self.ptr, id, False)

    def dump(self, file: str, format: str = 'XIIDM'):
        _gridpy.dump_network(self.ptr, file, format)

    def write_single_line_diagram_svg(self, container_id: str, svg_file: str):
        _gridpy.write_single_line_diagram_svg(self.ptr, container_id, svg_file)

    def get_elements_ids(self, element_type: _gridpy.ElementType, nominal_voltages: Set[float] = None, countries: Set[str] = None,
                         main_connected_component: bool = True, main_synchronous_component: bool = True,
                         not_connected_to_same_bus_at_both_sides: bool = False) -> List[str]:
        return _gridpy.get_network_elements_ids(self.ptr, element_type, [] if nominal_voltages is None else list(nominal_voltages),
                                                [] if countries is None else list(countries), main_connected_component, main_synchronous_component,
                                                not_connected_to_same_bus_at_both_sides)

    def create_elements_data_frame(self, element_type: _gridpy.ElementType) -> pd.DataFrame:
        series_array = _gridpy.create_network_elements_series_array(self.ptr, element_type)
        series_dict = {}
        index = None
        for series in series_array:
            if series.type == 0: # string
                if series.name == 'id':
                    index = series.str_data
                else:
                    series_dict[series.name] = series.str_data
            elif series.type == 1: # double
                series_dict[series.name] = series.double_data
            elif series.type == 2:  # int
                series_dict[series.name] = series.int_data
            elif series.type == 3:  # boolean
                series_dict[series.name] = series.boolean_data
            else:
                raise RuntimeError(f'Unsupported series type ${series.type}')
        return pd.DataFrame(series_dict, index = index)

    def create_buses_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_gridpy.ElementType.BUS)

    def create_generators_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_gridpy.ElementType.GENERATOR)

    def create_loads_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_gridpy.ElementType.LOAD)

    def create_lines_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_gridpy.ElementType.LINE)

    def create_2_windings_transformers_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_gridpy.ElementType.TWO_WINDINGS_TRANSFORMER)

    def create_3_windings_transformers_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_gridpy.ElementType.THREE_WINDINGS_TRANSFORMER)

    def create_shunt_compensators_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_gridpy.ElementType.SHUNT_COMPENSATOR)

    def create_dangling_lines_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_gridpy.ElementType.DANGLING_LINE)

    def create_lcc_converter_stations_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_gridpy.ElementType.LCC_CONVERTER_STATION)

    def create_vsc_converter_stations_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_gridpy.ElementType.VSC_CONVERTER_STATION)

    def create_static_var_compensators_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_gridpy.ElementType.STATIC_VAR_COMPENSATOR)


def create_empty(id: str = "Default") -> Network:
    return Network(_gridpy.create_empty_network(id))


def create_ieee14() -> Network:
    return Network(_gridpy.create_ieee14_network())


def create_eurostag_tutorial_example1_network() -> Network:
    return Network(_gridpy.create_eurostag_tutorial_example1_network())


def load(file: str) -> Network:
    return Network(_gridpy.load_network(file))
