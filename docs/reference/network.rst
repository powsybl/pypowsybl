=======
Network
=======

.. module:: pypowsybl.network

The Network class is the representation of a power network in pypowsybl, it provides
methods to access and modify underlying network elements data.

.. autoclass:: Network

Network creation
----------------

Following methods may be used to create a new network instance:

.. autosummary::
   :toctree: api/
   :nosignatures:

   load
   load_from_string
   load_from_binary_buffer
   load_from_binary_buffers
   create_empty
   create_ieee9
   create_ieee14
   create_ieee30
   create_ieee57
   create_ieee118
   create_ieee300
   create_eurostag_tutorial_example1_network
   create_eurostag_tutorial_example1_with_power_limits_network
   create_four_substations_node_breaker_network
   create_four_substations_node_breaker_network_with_extensions
   create_micro_grid_be_network
   create_micro_grid_nl_network


Network properties
------------------

.. autosummary::
   :toctree: api/
   :nosignatures:

   Network.id
   Network.name
   Network.source_format
   Network.case_date
   Network.forecast_distance
   Network.per_unit
   Network.nominal_apparent_power


Network elements access
-----------------------

All network elements are accessible as dataframes, using the following getters.

.. note::

   Once obtained, a dataframe has no more relation to the network it originated from.
   In particular, changing a dataframe will not change the underlying network.
   Also, in order to get up-to-date data, for example after a loadflow execution, you will
   need to call again the corresponding getter.

.. autosummary::
   :toctree: api/
   :nosignatures:

   Network.get_2_windings_transformers
   Network.get_3_windings_transformers
   Network.get_aliases
   Network.get_areas
   Network.get_areas_boundaries
   Network.get_areas_voltage_levels
   Network.get_batteries
   Network.get_branches
   Network.get_busbar_sections
   Network.get_buses
   Network.get_current_limits
   Network.get_dangling_lines
   Network.get_generators
   Network.get_hvdc_lines
   Network.get_identifiables
   Network.get_injections
   Network.get_lcc_converter_stations
   Network.get_lines
   Network.get_loads
   Network.get_linear_shunt_compensator_sections
   Network.get_non_linear_shunt_compensator_sections
   Network.get_operational_limits
   Network.get_phase_tap_changer_steps
   Network.get_phase_tap_changers
   Network.get_ratio_tap_changer_steps
   Network.get_ratio_tap_changers
   Network.get_reactive_capability_curve_points
   Network.get_shunt_compensators
   Network.get_static_var_compensators
   Network.get_substations
   Network.get_switches
   Network.get_terminals
   Network.get_voltage_levels
   Network.get_vsc_converter_stations
   Network.get_tie_lines

Bus/Breaker or Node/Breaker topology description of a given voltage level can be retrieved using the following getters:

.. autosummary::
   :toctree: api/
   :nosignatures:

   Network.get_bus_breaker_topology
   Network.get_node_breaker_topology

These getters return an object of the following classes:

.. autosummary::
   :nosignatures:

    BusBreakerTopology
    NodeBreakerTopology

.. include it in the toctree
.. toctree::
   :hidden:

   network/bus_breaker_topology
   network/node_breaker_topology

Network elements update
------------------------

Network elements can be modified using dataframes:

.. autosummary::
   :toctree: api/
   :nosignatures:

   Network.update_2_windings_transformers
   Network.update_3_windings_transformers
   Network.update_areas
   Network.update_batteries
   Network.update_buses
   Network.update_dangling_lines
   Network.update_generators
   Network.update_hvdc_lines
   Network.update_lcc_converter_stations
   Network.update_linear_shunt_compensator_sections
   Network.update_lines
   Network.update_loads
   Network.update_non_linear_shunt_compensator_sections
   Network.update_phase_tap_changers
   Network.update_ratio_tap_changers
   Network.update_shunt_compensators
   Network.update_static_var_compensators
   Network.update_substations
   Network.update_switches
   Network.update_voltage_levels
   Network.update_vsc_converter_stations
   Network.add_elements_properties
   Network.remove_elements_properties
   Network.add_aliases
   Network.remove_aliases


Network elements creation and removal
-------------------------------------

Network elements can be created or removed using the following methods:

.. autosummary::
   :toctree: api/
   :nosignatures:

   Network.create_2_windings_transformers
   Network.create_3_windings_transformers
   Network.create_areas
   Network.add_areas_voltage_levels
   Network.add_areas_boundaries
   Network.create_batteries
   Network.create_busbar_sections
   Network.create_buses
   Network.create_curve_reactive_limits
   Network.create_dangling_lines
   Network.create_generators
   Network.create_hvdc_lines
   Network.create_lcc_converter_stations
   Network.create_lines
   Network.create_loads
   Network.create_minmax_reactive_limits
   Network.create_operational_limits
   Network.create_phase_tap_changers
   Network.create_ratio_tap_changers
   Network.create_shunt_compensators
   Network.create_static_var_compensators
   Network.create_substations
   Network.create_switches
   Network.create_voltage_levels
   Network.create_vsc_converter_stations
   Network.create_tie_lines
   Network.remove_elements
   Network.remove_areas_voltage_levels
   Network.remove_areas_boundaries


Network variants management
---------------------------

Network variants may be used to manage multiple states of the network efficiently.

.. autosummary::
   :toctree: api/
   :nosignatures:

   Network.get_working_variant_id
   Network.clone_variant
   Network.set_working_variant
   Network.remove_variant
   Network.get_variant_ids


Network elements extensions
---------------------------

.. autosummary::
   :toctree: api/
   :nosignatures:

   get_extensions_names
   get_extensions_information
   Network.get_extensions
   Network.create_extensions
   Network.update_extensions
   Network.remove_extensions


Miscellaneous network functions
-------------------------------

.. autosummary::
   :toctree: api/
   :nosignatures:

   Network.reduce
   Network.merge
   Network.get_single_line_diagram
   Network.write_single_line_diagram_svg
   Network.get_matrix_multi_substation_single_line_diagram
   Network.write_matrix_multi_substation_single_line_diagram_svg
   Network.get_network_area_diagram
   Network.write_network_area_diagram_svg
   Network.get_network_area_diagram_displayed_voltage_levels
   Network.disconnect
   Network.connect
   Network.open_switch
   Network.close_switch
   Network.get_validation_level
   Network.validate
   Network.set_min_validation_level


I/O
---

.. autosummary::
   :toctree: api/
   :nosignatures:

   load
   load_from_string
   load_from_binary_buffer
   load_from_binary_buffers
   Network.dump
   Network.dump_to_string
   get_import_formats
   get_import_parameters
   get_import_post_processors
   get_export_formats
   get_export_parameters
   Network.save
   Network.save_to_string
   Network.save_to_binary_buffer


Advanced network modifications
------------------------------

.. autosummary::
   :toctree: api/
   :nosignatures:

   create_2_windings_transformer_bays
   create_line_bays
   create_load_bay
   create_battery_bay
   create_dangling_line_bay
   create_generator_bay
   create_shunt_compensator_bay
   create_static_var_compensator_bay
   create_lcc_converter_station_bay
   create_vsc_converter_station_bay
   create_line_on_line
   revert_create_line_on_line
   connect_voltage_level_on_line
   revert_connect_voltage_level_on_line
   replace_tee_point_by_voltage_level_on_line
   revert_connect_voltage_level_on_line
   create_voltage_level_topology
   create_coupling_device


Utility functions
-----------------

.. autosummary::
   :toctree: api/
   :nosignatures:

    get_connectables_order_positions
    get_unused_order_positions_before
    get_unused_order_positions_after
