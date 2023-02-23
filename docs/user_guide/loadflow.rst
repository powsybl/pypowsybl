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
fully described on `Powsybl website <https://www.powsybl.org/pages/documentation/simulation/powerflow/openlf.html>`_.
OpenLoadFlow supports AC Newton-Raphson and linear DC calculation methods.

You may also use DynaFlow, provided by the `Dynawo <https://dynawo.github.io>`_ project.
DynaFlow is a new steady-state simulation tool that aims at calculating the steady-state point by using
a simplified time-domain simulation.
Please see configuration instructions on `Powsybl website <https://www.powsybl.org/pages/documentation/simulation/powerflow/dynaflow.html>`_.


Parameters
----------

The most important part before running a load flow is knowing the parameters and change them if needed.
Let's have a look at the default ones:

.. doctest::

    >>> lf.Parameters()
    Parameters(voltage_init_mode=UNIFORM_VALUES, transformer_voltage_control_on=False, no_generator_reactive_limits=False, phase_shifter_regulation_on=False, twt_split_shunt_admittance=False, simul_shunt=False, read_slack_bus=True, write_slack_bus=False, distributed_slack=True, balance_type=PROPORTIONAL_TO_GENERATION_P_MAX, dc_use_transformer_ratio=True, countries_to_balance=[], connected_component_mode=<ConnectedComponentMode.MAIN: 0>, provider_parameters={})

For more details on each parameter, please refer to the :doc:`API reference </reference/loadflow/parameters>`.

All parameters are also fully described in `Powsybl loadfow parameter documentation <https://www.powsybl.org/pages/documentation/simulation/powerflow/>`_.

Parameters specific to a provider
---------------------------------

Some parameters are not supported by all load flow providers but specific to only one. These specific
parameters could be specified in a less typed way than common parameters using the `provider_parameters` attribute.

.. warning::
    `provider_parameters` is dictionary and all keys and values have to be a string even in case of a numeric value.

We can list supported parameters specific to default provider using:

.. doctest::

    >>> lf.get_provider_parameters_names()
    ['slackBusSelectionMode', 'slackBusesIds', 'lowImpedanceBranchMode', 'voltageRemoteControl', ...]

And get more detailed informations about theses parameters using:

.. doctest::

    >>> lf.get_provider_parameters().iloc[:2] # doctest: +NORMALIZE_WHITESPACE
                                        description    type      default                                possible_values
    name
    slackBusSelectionMode  Slack bus selection mode  STRING  MOST_MESHED  [FIRST, MOST_MESHED, NAME, LARGEST_GENERATOR]
    slackBusesIds                     Slack bus IDs  STRING

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
    [ComponentResult(connected_component_num=0, synchronous_component_num=0, status=CONVERGED, iteration_count=3, slack_bus_id='VLHV1_0', slack_bus_active_power_mismatch=-606.5596837558763, distributed_active_power=0.0)]

Component results provides general information about the loadflow: was it successful ? how many iterations did
it need ? what's the remaining active power imbalance ? For example, let's have a look at the imbalance
on the main component of the network:

.. doctest::

    >>> results[0].slack_bus_active_power_mismatch
    -606.5596837558763

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

In order to run an AC loadflow, simply use the :func:`run_dc` method.

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

