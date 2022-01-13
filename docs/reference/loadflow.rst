========
Loadflow
========

.. module:: pypowsybl.loadflow

The loadflow module allows to run AC and DC loadflows.

Run a loadflow
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
   :nosignatures:

    Parameters

.. include it in the toctree
.. toctree::
   :hidden:

   loadflow/parameters

Some enum classes are used in parameters:

.. autosummary::
   :toctree: api/
   :template: autosummary/class_without_members.rst

    VoltageInitMode
    ConnectedComponentMode
    BalanceType

Results
-------

The loadflow result is actually a list of results, one for each component of the network:

.. autosummary::
   :nosignatures:

    ComponentResult

.. include it in the toctree
.. toctree::
   :hidden:

   loadflow/componentresult

Some enum classes are used in results:

.. autosummary::
   :toctree: api/
   :template: autosummary/class_without_members.rst

    ComponentStatus
