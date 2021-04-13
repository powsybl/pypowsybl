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
from _pypowsybl import ElementType
from pypowsybl.util import ObjectHandle
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

    def dump(self, file: str, format: str = 'XIIDM'):
        _pypowsybl.dump_network(self.ptr, file, format)

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
        series_array = _pypowsybl.create_network_elements_series_array(self.ptr, element_type)
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
        return self.create_elements_data_frame(_pypowsybl.ElementType.BUS)

    def create_generators_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_pypowsybl.ElementType.GENERATOR)

    def create_loads_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_pypowsybl.ElementType.LOAD)

    def create_lines_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_pypowsybl.ElementType.LINE)

    def create_2_windings_transformers_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_pypowsybl.ElementType.TWO_WINDINGS_TRANSFORMER)

    def create_3_windings_transformers_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_pypowsybl.ElementType.THREE_WINDINGS_TRANSFORMER)

    def create_shunt_compensators_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_pypowsybl.ElementType.SHUNT_COMPENSATOR)

    def create_dangling_lines_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_pypowsybl.ElementType.DANGLING_LINE)

    def create_lcc_converter_stations_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_pypowsybl.ElementType.LCC_CONVERTER_STATION)

    def create_vsc_converter_stations_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_pypowsybl.ElementType.VSC_CONVERTER_STATION)

    def create_static_var_compensators_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_pypowsybl.ElementType.STATIC_VAR_COMPENSATOR)

    def create_hvdc_lines_data_frame(self) -> pd.DataFrame:
        return self.create_elements_data_frame(_pypowsybl.ElementType.HVDC_LINE)


def create_empty(id: str = "Default") -> Network:
    return Network(_pypowsybl.create_empty_network(id))


def create_ieee14() -> Network:
    return Network(_pypowsybl.create_ieee14_network())


def create_eurostag_tutorial_example1_network() -> Network:
    return Network(_pypowsybl.create_eurostag_tutorial_example1_network())


def load(file: str) -> Network:
    return Network(_pypowsybl.load_network(file))
