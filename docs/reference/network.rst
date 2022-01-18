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
   create_empty
   create_ieee9
   create_ieee14
   create_ieee30
   create_ieee57
   create_ieee118
   create_ieee300
   create_eurostag_tutorial_example1_network
   create_four_substations_node_breaker_network
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
   Network.get_batteries
   Network.get_busbar_sections
   Network.get_buses
   Network.get_current_limits
   Network.get_dangling_lines
   Network.get_generators
   Network.get_hvdc_lines
   Network.get_lcc_converter_stations
   Network.get_lines
   Network.get_loads
   Network.get_linear_shunt_compensator_sections
   Network.get_non_linear_shunt_compensator_sections
   Network.get_phase_tap_changer_steps
   Network.get_phase_tap_changers
   Network.get_ratio_tap_changer_steps
   Network.get_ratio_tap_changers
   Network.get_reactive_capability_curve_points
   Network.get_shunt_compensators
   Network.get_static_var_compensators
   Network.get_substations
   Network.get_switches
   Network.get_voltage_level_topology
   Network.get_voltage_levels
   Network.get_vsc_converter_stations


Network elements  update
------------------------

Network elements can be modified using dataframes:

.. autosummary::
   :toctree: api/
   :nosignatures:

   Network.update_2_windings_transformers
   Network.update_batteries
   Network.update_buses
   Network.update_dangling_lines
   Network.update_elements
   Network.update_generators
   Network.update_hvdc_lines
   Network.update_linear_shunt_compensator_sections
   Network.update_lines
   Network.update_loads
   Network.update_non_linear_shunt_compensator_sections
   Network.update_phase_tap_changers
   Network.update_ratio_tap_changers
   Network.update_shunt_compensators
   Network.update_static_var_compensators
   Network.update_switches
   Network.update_vsc_converter_stations


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


Miscellaneous network functions
-------------------------------

.. autosummary::
   :toctree: api/
   :nosignatures:

   Network.reduce
   Network.merge
   Network.get_single_line_diagram
   Network.write_single_line_diagram_svg
   Network.disconnect
   Network.connect
   Network.open_switch
   Network.close_switch

I/O
---

.. autosummary::
   :toctree: api/
   :nosignatures:

   load
   load_from_string
   Network.dump
   Network.dump_to_string
   get_import_formats
   get_import_parameters
   get_export_formats
   get_export_parameters
