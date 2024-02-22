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
    VoltageInitializerParameters.set_objective
    VoltageInitializerParameters.set_objective_distance

VoltageInitializerResults : How to exploit the results
------------------------------------------------------
.. autosummary::
   :toctree: api/

    VoltageInitializerResults
    VoltageInitializerResults.apply_all_modifications
    VoltageInitializerResults.status
    VoltageInitializerResults.indicators
