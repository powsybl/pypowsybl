======================
Simulation with Dynawo
======================

.. module:: pypowsybl.dynamic

The dynamic module allows to run time domain simulation.

ModelMapping
------------
.. autosummary::
   :toctree: api/

    ModelMapping
    ModelMapping.add_base_load
    ModelMapping.add_load_one_transformer
    ModelMapping.add_load_one_transformer_tap_changer
    ModelMapping.add_load_two_transformers
    ModelMapping.add_load_two_transformers_tap_changers
    ModelMapping.add_base_generator
    ModelMapping.add_synchronized_generator
    ModelMapping.add_synchronous_generator
    ModelMapping.add_wecc
    ModelMapping.add_grid_forming_converter
    ModelMapping.add_hvdc_p
    ModelMapping.add_hvdc_vsc
    ModelMapping.add_base_transformer
    ModelMapping.add_base_static_var_compensator
    ModelMapping.add_base_line
    ModelMapping.add_base_bus
    ModelMapping.add_infinite_bus
    ModelMapping.add_overload_management_system
    ModelMapping.add_two_levels_overload_management_system
    ModelMapping.add_under_voltage_automation_system
    ModelMapping.add_phase_shifter_i_automation_system
    ModelMapping.add_phase_shifter_p_automation_system
    ModelMapping.add_tap_changer_automation_system
    ModelMapping.add_tap_changer_blocking_automation_system

EventMapping
------------
.. autosummary::
   :toctree: api/

    EventMapping
    EventMapping.add_disconnection
    EventMapping.add_active_power_variation
    EventMapping.add_node_fault

OutputVariableMapping
---------------------
.. autosummary::
    :toctree: api/

    OutputVariableMapping
    OutputVariableMapping.add_dynamic_model_curves
    OutputVariableMapping.add_standard_model_curves
    OutputVariableMapping.add_dynamic_model_final_state_values
    OutputVariableMapping.add_standard_model_final_state_values

Simulation
----------
.. autosummary::
    :toctree: api/

    Simulation
    Simulation.run

Results
-------
.. autosummary::
    :toctree: api/

    SimulationResult
    SimulationResult.status
    SimulationResult.status_text
    SimulationResult.curves
    SimulationResult.final_state_values
    SimulationResult.timeline
