Short-circuit analysis
======================

.. module:: pypowsybl.shortcircuit


Run a short-circuit analysis
----------------------------

You can run a short-circuit analysis using the following methods:

.. autosummary::
   :nosignatures:
   :toctree: api/

    create_analysis
    ShortCircuitAnalysis.run
    set_default_provider
    get_default_provider
    get_provider_names


Parameters
----------

The execution of the short-circuit analysis can be customized using short-circuit analysis parameters.

.. autosummary::
   :nosignatures:
   :toctree: api/

    Parameters


Define faults
-------------

You can define faults to be simulated with the following methods:

.. autosummary::
   :nosignatures:
   :toctree: api/

    ShortCircuitAnalysis.set_faults


Results
-------

When the short-circuit analysis is completed, you can inspect its results:

.. autosummary::
   :nosignatures:
   :toctree: api/

    ShortCircuitAnalysisResult
    ShortCircuitAnalysisResult.fault_results
    ShortCircuitAnalysisResult.feeder_results
    ShortCircuitAnalysisResult.limit_violations
    ShortCircuitAnalysisResult.short_circuit_bus_results
