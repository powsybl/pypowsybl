===================
Voltage Initializer
===================

.. module:: pypowsybl.voltage_initializer

The voltage_initializer module is a tool to initialize voltage of a network before a loadflow and can prevent divergence.

Run the voltage initializer
---------------------------
.. autosummary::
   :toctree: api/

    run

VoltageInitializerParameters : How to parameterize the tool
-----------------------------------------------------------
.. autosummary::
   :toctree: api/

    VoltageInitializerParameters
    VoltageInitializerParameters.add_variable_shunt_compensators
    VoltageInitializerParameters.add_constant_q_generators
    VoltageInitializerParameters.add_variable_two_windings_transformers
    VoltageInitializerParameters.add_specific_low_voltage_limits
    VoltageInitializerParameters.add_specific_high_voltage_limits
    VoltageInitializerParameters.add_specific_voltage_limits
    VoltageInitializerParameters.set_objective
    VoltageInitializerParameters.set_objective_distance
    VoltageInitializerParameters.set_log_level_ampl
    VoltageInitializerParameters.set_log_level_solver
    VoltageInitializerParameters.set_reactive_slack_buses_mode
    VoltageInitializerParameters.set_min_plausible_low_voltage_limit
    VoltageInitializerParameters.set_max_plausible_high_voltage_limit
    VoltageInitializerParameters.set_active_power_variation_rate
    VoltageInitializerParameters.set_min_plausible_active_power_threshold
    VoltageInitializerParameters.set_low_impedance_threshold
    VoltageInitializerParameters.set_min_nominal_voltage_ignored_bus
    VoltageInitializerParameters.set_min_nominal_voltage_ignored_voltage_bounds
    VoltageInitializerParameters.set_max_plausible_power_limit
    VoltageInitializerParameters.set_high_active_power_default_limit
    VoltageInitializerParameters.set_low_active_power_default_limit
    VoltageInitializerParameters.set_default_minimal_qp_range
    VoltageInitializerParameters.set_default_qmax_pmax_ratio
    VoltageInitializerParameters.set_default_variable_scaling_factor
    VoltageInitializerParameters.set_default_constraint_scaling_factor
    VoltageInitializerParameters.set_reactive_slack_variable_scaling_factor
    VoltageInitializerParameters.set_twt_ratio_variable_scaling_factor

VoltageInitializerResults : How to exploit the results
------------------------------------------------------
.. autosummary::
   :toctree: api/

    VoltageInitializerResults
    VoltageInitializerResults.apply_all_modifications
    VoltageInitializerResults.status
    VoltageInitializerResults.indicators
