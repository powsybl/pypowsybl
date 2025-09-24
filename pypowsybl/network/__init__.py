# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from pypowsybl._pypowsybl import ElementType, ValidationLevel

from .impl.bus_breaker_topology import BusBreakerTopology
from .impl.layout_parameters import LayoutParameters
from .impl.nad_parameters import EdgeInfoType, NadLayoutType, NadParameters
from .impl.nad_profile import NadProfile
from .impl.network import Network
from .impl.network_creation_util import (
    _create_network,
    create_empty,
    create_eurostag_tutorial_example1_network,
    create_eurostag_tutorial_example1_with_more_generators_network,
    create_eurostag_tutorial_example1_with_power_limits_network,
    create_eurostag_tutorial_example1_with_tie_lines_and_areas,
    create_four_substations_node_breaker_network,
    create_four_substations_node_breaker_network_with_extensions,
    create_ieee9,
    create_ieee14,
    create_ieee30,
    create_ieee57,
    create_ieee118,
    create_ieee300,
    create_metrix_tutorial_six_buses_network,
    create_micro_grid_be_network,
    create_micro_grid_nl_network,
    is_loadable,
    load,
    load_from_binary_buffer,
    load_from_binary_buffers,
    load_from_string,
)
from .impl.network_element_modification_util import (
    connect_voltage_level_on_line,
    create_2_windings_transformer_bays,
    create_battery_bay,
    create_coupling_device,
    create_dangling_line_bay,
    create_generator_bay,
    create_lcc_converter_station_bay,
    create_line_bays,
    create_line_on_line,
    create_load_bay,
    create_shunt_compensator_bay,
    create_static_var_compensator_bay,
    create_voltage_level_topology,
    create_vsc_converter_station_bay,
    get_connectables_order_positions,
    get_unused_order_positions_after,
    get_unused_order_positions_before,
    remove_feeder_bays,
    remove_hvdc_lines,
    remove_voltage_levels,
    replace_3_2_windings_transformers_with_3_windings_transformers,
    replace_3_windings_transformers_with_3_2_windings_transformers,
    replace_tee_point_by_voltage_level_on_line,
    revert_connect_voltage_level_on_line,
    revert_create_line_on_line,
)
from .impl.node_breaker_topology import NodeBreakerTopology
from .impl.pandapower_converter import convert_from_pandapower
from .impl.perunit import PerUnitView, per_unit_view
from .impl.sld_parameters import SldParameters
from .impl.svg import Svg
from .impl.util import (
    get_export_formats,
    get_export_parameters,
    get_extensions_information,
    get_extensions_names,
    get_import_formats,
    get_import_parameters,
    get_import_post_processors,
    get_import_supported_extensions,
    get_single_line_diagram_component_library_names,
)
