=========
Dynawaltz
=========

.. module:: pypowsybl.dynamic

The dynamic module allows to run time domain simulation.

ModelMapping
------------
.. autosummary::
   :toctree: api/

    ModelMapping
    ModelMapping.add_all_dynamic_mappings
    ModelMapping.add_alpha_beta_load
    ModelMapping.add_one_transformer_load
    ModelMapping.add_generator_synchronous_three_windings
    ModelMapping.add_generator_synchronous_three_windings_proportional_regulations
    ModelMapping.add_generator_synchronous_four_windings
    ModelMapping.add_generator_synchronous_four_windings_proportional_regulations
    ModelMapping.add_current_limit_automaton

EventMapping
------------
.. autosummary::
   :toctree: api/

    EventMapping
    EventMapping.get_possible_events
    EventMapping.add_branch_disconnection
    EventMapping.add_injection_disconnection

CurveMapping
------------
.. autosummary::
    :toctree: api/

    CurveMapping
    CurveMapping.add_curve
    CurveMapping.add_curves

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
    SimulationResult.curves
