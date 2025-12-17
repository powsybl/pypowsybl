Running a load flow
===================

.. currentmodule:: pypowsybl.loadflow

.. testsetup:: *

    import pypowsybl as pp
    import pypowsybl.loadflow as lf
    import pypowsybl.network as pn

You can use the module :mod:`pypowsybl.loadflow` in order to run load flows on networks.

Start by importing the module:

.. code-block:: python

   import pypowsybl.network as pn
   import pypowsybl.loadflow as lf

Providers
---------
We can get the list of supported load flow implementations (so called providers) and default one:

.. doctest::

    >>> lf.get_provider_names()
    ['DynaFlow', 'OpenLoadFlow']
    >>> lf.get_default_provider()
    'OpenLoadFlow'

By default, load flows are based on the OpenLoadFlow implementation,
fully described `here <https://powsybl.readthedocs.io/projects/powsybl-open-loadflow/en/latest/>`_.
OpenLoadFlow supports AC Newton-Raphson and linear DC calculation methods.

You may also use DynaFlow, provided by the `Dynawo <https://dynawo.github.io>`_ project.
DynaFlow is a new steady-state simulation tool that aims at calculating the steady-state point by using
a simplified time-domain simulation.
Please see configuration instructions `here <https://powsybl.readthedocs.io/projects/powsybl-dynawo/en/latest/>`__.


Parameters
----------

The most important part before running a load flow is knowing the parameters and change them if needed.
Let's have a look at the default ones:

.. doctest::

    >>> lf.Parameters()
    Parameters(voltage_init_mode=UNIFORM_VALUES, transformer_voltage_control_on=False, use_reactive_limits=True, phase_shifter_regulation_on=False, twt_split_shunt_admittance=False, shunt_compensator_voltage_control_on=False, read_slack_bus=True, write_slack_bus=True, distributed_slack=True, balance_type=PROPORTIONAL_TO_GENERATION_P_MAX, dc_use_transformer_ratio=True, countries_to_balance=[], component_mode=<ComponentMode.MAIN_CONNECTED: 0>, hvdc_ac_emulation=True, dc_power_factor=1.0, provider_parameters={})

For more details on each parameter, please refer to the :doc:`API reference </reference/loadflow/parameters>`.

All parameters are also fully described in `Powsybl load flow parameters documentation <https://powsybl.readthedocs.io/projects/powsybl-core/en/stable/simulation/loadflow/configuration.html>`_.

Parameters specific to a provider
---------------------------------

Some parameters are not supported by all load flow providers but specific to only one. These specific
parameters could be specified in a less typed way than common parameters using the `provider_parameters` attribute.

.. warning::
    `provider_parameters` is a dictionary in which all keys and values **must** be a string, even in case of a numeric value:

    * string and integer parameters do not bring much challenge:

      ``provider_parameters={'someStringParam' : 'myStringValue', 'someIntegerParam' : '42'}``

    * for float (double) parameters, use the dot as decimal separator. E notation is also supported:

      ``provider_parameters={'someDoubleParam' : '1.23', 'someOtherDoubleParam' : '4.56E-2'}``

    * for boolean parameters, use either `'True'`, `'true'`, `'False'`, `'false'`:

      ``provider_parameters={'someBooleanParam' : 'true'}``

    * for string list parameters, use the comma as a separator:

      ``provider_parameters={'someStringListParam' : 'value1,value2,value3'}``

We can list supported parameters specific to default provider using:

.. doctest::

    >>> lf.get_provider_parameters_names()
    ['slackBusSelectionMode', 'slackBusesIds', 'lowImpedanceBranchMode', 'voltageRemoteControl', ...]

And get more detailed information about theses parameters, such as parameter description, type, default value if any,
possible values if applicable, using:

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> lf.get_provider_parameters().query('name == "slackBusSelectionMode" or name == "slackBusesIds"')
                                category_key               description         type      default                                possible_values
    name
    slackBusSelectionMode  SlackDistribution  Slack bus selection mode       STRING  MOST_MESHED  [FIRST, MOST_MESHED, NAME, LARGEST_GENERATOR]
    slackBusesIds          SlackDistribution             Slack bus IDs  STRING_LIST

For instance, OLF supports configuration of slack bus from its ID like this:

.. doctest::

    >>> p = lf.Parameters(provider_parameters={'slackBusSelectionMode' : 'NAME', 'slackBusesIds' : 'VLHV2_0'})


AC Load Flow
------------

In order to run an AC loadflow, simply use the :func:`run_ac` method:

.. doctest::

    >>> network = pn.create_eurostag_tutorial_example1_network()
    >>> results = lf.run_ac(network, parameters=lf.Parameters(distributed_slack=False))

The result is composed of a list of component results, one for each connected component of the network
included in the computation:

.. doctest::

    >>> results
    [ComponentResult(connected_component_num=0, synchronous_component_num=0, status=CONVERGED, status_text=Converged, iteration_count=3, reference_bus_id='VLHV1_0', slack_bus_results=[SlackBusResult(id='VLHV1_0', active_power_mismatch=-606.5596837558767)], distributed_active_power=0.0)]

Component results provides general information about the loadflow execution: was it successful? How many iterations did
it need? What is the remaining active power imbalance? For example, let's have a look at the imbalance
on the main component of the network:

.. doctest::

    >>> results[0].slack_bus_results[0].active_power_mismatch
    -606.5596837558767

Then, the main output of the loadflow is actually the updated data in the network itself:
all voltages and flows are now updated with the computed values. For example you can have a look at
the voltage magnitudes (rounded to 2 digits here):

.. doctest::

    >>> network.get_buses().v_mag.round(2)
    id
    VLGEN_0      24.50
    VLHV1_0     400.62
    VLHV2_0     388.33
    VLLOAD_0    146.90
    Name: v_mag, dtype: float64


DC Load Flow
------------

In order to run a DC loadflow, simply use the :func:`run_dc` method.

For that example, we will use a distributed slack, with imbalance distributed on generators,
proportional to their maximum power. We also choose to ignore transformer ratios in the DC equations:

.. doctest::

    >>> parameters = lf.Parameters(dc_use_transformer_ratio=False, distributed_slack=True,
    ...                            balance_type=lf.BalanceType.PROPORTIONAL_TO_GENERATION_P_MAX)

Then let's create our test network and run the DC load flow:

.. doctest::

    >>> network = pn.create_eurostag_tutorial_example1_network()
    >>> results = lf.run_dc(network, parameters)

We can finally retrieve the computed flows on lines:

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> network.get_lines()[['p1', 'p2']]
                    p1     p2
    id
    NHV1_NHV2_1  300.0 -300.0
    NHV1_NHV2_2  300.0 -300.0

Reports
------------

Reports contain detailed computation information. To see those reports, pass a report_node argument to the run command.

.. code-block:: python

    >>> report_node = pp.report.ReportNode()
    >>> network = pn.create_eurostag_tutorial_example1_network()
    >>> results = lf.run_ac(network, parameters, report_node=report_node)
    >>> print(report_node)
    +
       + Load flow on network 'sim1'
          + Network CC0 SC0
             + Network info
                Network has 4 buses and 4 branches
                Network balance: active generation=1214.0 MW, active load=600.0 MW, reactive generation=0.0 MVar, reactive load=200.0 MVar
                Angle reference bus: VLHV1_0
                Slack bus: VLHV1_0
             + Outer loop DistributedSlack
                + Outer loop iteration 1
		   Slack bus active power (-606.5596837558763 MW) distributed in 1 distribution iteration(s)
                + Outer loop iteration 2
		   Slack bus active power (-1.8792855272990572 MW) distributed in 1 distribution iteration(s)
             Outer loop VoltageMonitoring
             Outer loop ReactiveLimits
             Outer loop DistributedSlack
             Outer loop VoltageMonitoring
             Outer loop ReactiveLimits
             AC load flow completed successfully (solverStatus=CONVERGED, outerloopStatus=STABLE)
    <BLANKLINE>
	     
Asynchronous API
----------------

An asynchronous API based on Python's asyncio has been added for AC loadflow calculations (DC loadflow support will be added in a future release). The following example demonstrates how to:
 - Load a network
 - Create two identical variants from the initial state
 - Run AC loadflow calculations on both variants concurrently
 - Wait for both results and display their convergence status

Both loadflow calculations are executed in parallel using Python's asyncio API.


.. caution::
   The network has to be loaded using a special parameter `allow_variant_multi_thread_access` to `True` to be able
   to work on multiple variants of a same network concurrently using different threads.

.. doctest::

    >>> import asyncio
    >>> async def run_2_lf():
    ...     lf1 = lf.run_ac_async(network, "variant1")
    ...     lf2 = lf.run_ac_async(network, "variant2")
    ...     results = await asyncio.gather(lf1, lf2)
    ...     print(results[0][0].status)
    ...     print(results[1][0].status)
    >>> network = pn.create_ieee14(allow_variant_multi_thread_access=True)
    >>> network.clone_variant("InitialState", "variant1")
    >>> network.clone_variant("InitialState", "variant2")
    >>> asyncio.run(run_2_lf())
    ComponentStatus.CONVERGED
    ComponentStatus.CONVERGED
