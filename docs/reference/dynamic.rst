=========
Dynawaltz
=========

.. module:: pypowsybl.dynamic

The dynamic module allows to run time domain simulation.

ModelMapping
------------
.. autoclass:: ModelMapping
.. automethod:: ModelMapping.add_all_dynamic_mappings
.. automethod:: ModelMapping.add_alpha_beta_load
.. automethod:: ModelMapping.add_one_transformer_load
.. automethod:: ModelMapping.add_generator_synchronous_three_windings
.. automethod:: ModelMapping.add_generator_synchronous_three_windings_proportional_regulations
.. automethod:: ModelMapping.add_generator_synchronous_four_windings
.. automethod:: ModelMapping.add_generator_synchronous_four_windings_proportional_regulations
.. automethod:: ModelMapping.add_current_limit_automaton

EventMapping
------------
.. autoclass:: EventMapping
.. automethod:: EventMapping.add_branch_disconnection
.. automethod:: EventMapping.add_set_point_boolean
.. automethod:: EventMapping.add_event

CurveMapping
------------
.. autoclass:: CurveMapping
.. automethod:: CurveMapping.add_curve
.. automethod:: CurveMapping.add_curves

Simulation
----------
.. autoclass:: Simulation
.. automethod:: Simulation.run

Results
-------
.. autoclass:: SimulationResult
.. automethod:: SimulationResult.status
.. automethod:: SimulationResult.curves
