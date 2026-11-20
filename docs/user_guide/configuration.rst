Configuration
=============

PyPowSyBl uses the same configuration file as the rest of the PowSyBl framework.
That file is where default implementations and default parameters for load flow,
security analysis, dynamic simulation, and other components are stored.

This page only covers what a Python user needs. The full mechanism (modules,
YAML vs XML, environment-variable overrides) is documented in the
`PowSyBl configuration guide <https://powsybl.readthedocs.io/projects/powsybl-core/en/stable/user/configuration/index.html>`_.

Where is the file?
------------------

By default PyPowSyBl reads:

    ``~/.itools/config.yml``

Create the ``.itools`` directory under your home folder if it does not exist.
The basename is ``config``; a YAML file is tried first, then XML.

Python does not use ``itools.conf`` / ``POWSYBL_HOME``. Those apply to the
Java iTools distribution.

You can disable reading that file (for example in tests) with:

.. code-block:: python

   import pypowsybl as pp
   pp.set_config_read(False)

``pp.is_config_read()`` reports the current setting. Reading the file is
enabled by default.

Example
-------

A minimal YAML file that selects OpenLoadFlow and sets a few default load-flow
parameters:

.. code-block:: yaml

   load-flow:
     default-impl-name: OpenLoadFlow

   load-flow-default-parameters:
     voltageInitMode: UNIFORM_VALUES
     distributedSlack: true
     balanceType: PROPORTIONAL_TO_GENERATION_P_MAX

Module and property names are case-sensitive. The list of modules is in the
`PowSyBl configuration modules <https://powsybl.readthedocs.io/projects/powsybl-core/en/stable/user/configuration/modules.html>`_
page. Load-flow keys are also described in the
`load-flow configuration <https://powsybl.readthedocs.io/projects/powsybl-core/en/stable/simulation/loadflow/configuration.html>`_
page.

Overrides
---------

Constructor arguments on the Python ``Parameters`` classes override values
loaded from the configuration file. For example ``pypowsybl.loadflow.Parameters``
is first filled from the file, then from the arguments you pass.

Environment variables of the form ``MODULE_NAME__PROPERTY_NAME`` also override
the file. See the PowSyBl configuration guide for the naming rules.

Some features still need extra entries in the same file (for example Dynawo
under :doc:`dynamic` and AMPL under :doc:`voltage_initializer`).
