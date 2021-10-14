========
Loadflow
========

.. module:: pypowsybl.loadflow

The loadflow module allows to run AC and DC loadflows.

Running a loadflow
------------------

.. autosummary::
   :toctree: api/
   :nosignatures:

   run_ac
   run_dc

Parameters
----------

The execution of the loadflow can be customized using loadflow parameters.

.. autosummary::
   :toctree: api/
   :nosignatures:

   Parameters

Results
-------

The loadflow result is actually a list of results, one for each component of the network:

.. autosummary::
   :toctree: api/
   :nosignatures:

    ComponentResult
