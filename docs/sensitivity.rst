Sensitivity analysis
====================

You can use the module ``pypowsybl.sensitivity`` in order to perform sensitivity analysis on a network.

DC sensitivity analysis
-----------------------

To perform a sensitivity analysis, you first need to define "factors" you want to compute.
What we call a factor is the dependency of a function, typically the active power flow on a branch, to
a variable, typically the active power injection of a generator, a load or a phase shifter.

To make the definition of those factors easier, ``pypowsybl`` provides a method to define the branches for
which the flow sensitivity should be computed, and for which injections or phase shifters. We obtain a matrix of sensitivities
as a result:

.. doctest::

    >>> import pypowsybl as pp
    >>> network = pp.network.create_eurostag_tutorial_example1_network()
    >>> analysis = pp.sensitivity.create_dc_analysis()
    >>> analysis.set_branch_flow_factor_matrix(branches_ids=['NHV1_NHV2_1', 'NHV1_NHV2_2'], variables_ids=['LOAD'])
    >>> result = analysis.run(network)
    >>> result.get_reference_flows()
                     NHV1_NHV2_1  NHV1_NHV2_2
    reference_flows        300.0        300.0
    >>> result.get_branch_flows_sensitivity_matrix()
          NHV1_NHV2_1  NHV1_NHV2_2
    LOAD         -0.5         -0.5

Zone to slack sensitivity
^^^^^^^^^^^^^^^^^^^^^^^^^

We illustrate this feature with a simple network where we have 4 countries (FR, DE, NL and BE) and 3 buses per countries. A zone is a group of weighted injections. With this network, we can create zones based on countries. The country attribute is defined in the network at the substation level through attribute `Country`.

First, we create a zone containing all generators of DE network with a shift key equals to generators' active power targets. To compute the sensitivity of an injection increase from this zone to the slack bus, we first create load flow parameters in order to disabled slack distribution. Note that this example is based on sensitivity analysis with DC approximation. In the following lines, we ask for the DE zone sensitivity on the border line BBE2AA1 FFRAA1 1.

.. code-block:: python

     >>> n = pp.network.load('simple-eu.uct')
     >>> zone_de = pp.sensitivity.create_country_zone(n, 'DE')
     >>> params = pp.loadflow.Parameters(distributed_slack=False)
     >>> sa = pp.sensitivity.create_dc_analysis()
     >>> sa.set_zones([zone_de])
     >>> sa.set_branch_flow_factor_matrix(['BBE2AA1  FFR3AA1  1'], ['DE'])
     >>> results = sa.run(n, params)
     >>> m = results.get_branch_flows_sensitivity_matrix()
              BBE2AA1  FFR3AA1  1
     DE             -0.45182


1 MW increase on DE zone and 1 MW decrease on slack bus injection is responsible of a variation of -0.45182 MW on border line BBE2AA1 FFRAA1 1.

Zone to zone sensitivity
^^^^^^^^^^^^^^^^^^^^^^^^

This feature is better known as Power Transfer Distribution Factor (PTDF).

In the following example, we compute the sensitivity of a active power transfer from FR zone to DE zone on the border line 'BBE2AA1 FFRAA1 1', through two zone to slack sensitivity calculations.

.. code-block:: python

     >>> zone_fr = pp.sensitivity.create_country_zone(n, 'FR')
     >>> zone_de = pp.sensitivity.create_country_zone(n, 'DE')
     >>> params = pp.loadflow.Parameters(distributed_slack=False)
     >>> sa = pp.sensitivity.create_dc_analysis()
     >>> sa.set_zones([zone_fr, zone_de])
     >>> sa.set_branch_flow_factor_matrix(['BBE2AA1  FFR3AA1  1'], ['FR', 'DE'])
     >>> results = sa.run(n, params)
     >>> m = results.get_branch_flows_sensitivity_matrix()
              BBE2AA1  FFR3AA1  1
     FR            -0.708461
     DE            -0.451820

1 MW active power transfer from FR zone to DE zone will be responsible of a variation of -0.256641 MW (indeed -0.708461 MW - (-0.451820 MW)) on the border line BBE2AA1 FFRAA1 1.

Let's obtain that directly. In the following example, we create four zones based on countries FR, DE, BE and NL. After a sensitivity analysis where we should set the zones, we are able to ask for a FR zone to slack sensitivity, a FR to DE zone to zone sensitivity, a DE to FR zone to zone sensitivity and a NL zone to slack sensitivity, on the border lines 'BBE2AA1  FFR3AA1  1' and 'FFR2AA1  DDE3AA1  1'.

.. code-block:: python

     >>> zone_fr = pp.sensitivity.create_country_zone(n, 'FR')
     >>> zone_de = pp.sensitivity.create_country_zone(n, 'DE')
     >>> zone_be = pp.sensitivity.create_country_zone(n, 'BE')
     >>> zone_nl = pp.sensitivity.create_country_zone(n, 'NL')
     >>> params = pp.loadflow.Parameters(distributed_slack=False)
     >>> sa = pp.sensitivity.create_dc_analysis()
     >>> sa.set_zones([zone_fr, zone_de, zone_be, zone_nl])
     >>> sa.set_branch_flow_factor_matrix(['BBE2AA1  FFR3AA1  1', 'FFR2AA1  DDE3AA1  1'], ['FR', ('FR', 'DE'), ('DE', 'FR'), 'NL'])
     >>> result = sa.run(n, params)
     >>> m = result.get_branch_flows_sensitivity_matrix()
               BBE2AA1  FFR3AA1  1  FFR2AA1  DDE3AA1  1
     FR                  -0.708461             0.291539
     FR -> DE            -0.256641             0.743359
     DE -> FR             0.256641            -0.743359
     NL                  -0.225206            -0.225206


Shift keys modification
^^^^^^^^^^^^^^^^^^^^^^^

When we create a zone from a country, the default behaviour is to use the generator active power target as weight. It means that:

.. code-block:: python

     >>> zone_de = pp.sensitivity.create_country_zone(n, 'DE')

is totally equivalent to:

.. code-block:: python

     >>> zone_de = pp.sensitivity.create_country_zone(n, 'DE', pp.sensitivity.ZoneKeyType.GENERATOR_TARGET_P)

There are two additional modes, using generator maximum active power or load active power target, as illustrated in the following lines:

.. code-block:: python

     >>> zone_de = pp.sensitivity.create_country_zone(n, 'DE', pp.sensitivity.ZoneKeyType.GENERATOR_MAX_P)
     >>> zone_de = pp.sensitivity.create_country_zone(n, 'DE', pp.sensitivity.ZoneKeyType.LOAD_P0)

You can display the keys with:

.. code-block:: python

     >>> zone_de = pp.sensitivity.create_country_zone(n, 'DE')
     >>> zone_de.shift_keys_by_injections_ids
     {'DDE1AA1 _generator': 2500.0,
      'DDE2AA1 _generator': 2000.0,
      'DDE3AA1 _generator': 1500.0}

Note that keys are not normalized here.

Zone modification
^^^^^^^^^^^^^^^^^

You can change a zone perimeter. In the following example, we imagine that the bus 'DDE3AA1' moves from DE zone to FR zone.

.. code-block:: python

     >>> zone_fr = pp.sensitivity.create_country_zone(n, 'FR')
     >>> zone_fr.injections_ids
     ['FFR1AA1 _generator',
      'FFR2AA1 _generator',
      'FFR3AA1 _generator']
     >>> zone_de = pp.sensitivity.create_country_zone(n, 'DE')
     >>> zone_de.injections_ids
     ['DDE1AA1 _generator',
      'DDE2AA1 _generator',
      'DDE3AA1 _generator']
     >>> zone_de.move_injection_to(zone_fr, 'DDE3AA1 _generator')
     >>> zone_fr.injections_ids
     ['FFR1AA1 _generator',
      'FFR2AA1 _generator',
      'FFR3AA1 _generator',
      'DDE3AA1 _generator']
     >>> zone_de.injections_ids
     ['DDE1AA1 _generator',
      'DDE2AA1 _generator']

If we rerun the sensitivity calculation, we found that 1 MW active power transfer from FR zone to DE zone will be responsible of a variation of -0.239337 MW (previously -0.256641 MW) on the border line 'BBE2AA1 FFRAA1 1'. Changing the monitored branch could be relevant in that use case to simulate that borders have moved.

We can also create an zone totally empty and transfer injections from other country zones to this new one.

.. code-block:: python

     >>> zone_fict = pp.sensitivity.create_empty_zone('FICT')
     >>> zone_fr.move_injection_to(zone_fict, 'DDE3AA1 _generator')
     >>> zone_fict.injections_ids
     ['DDE3AA1 _generator']

Other kind of sensitivities
^^^^^^^^^^^^^^^^^^^^^^^^^^^

PyPowSyBl allows to compute more that PTDF. In addition to injections and zones you configure the sensitivity matrix with:

- a phase shifter ID to compute the sensitivity of a phase shifting on a branch, feature also called Phase Shift Distribution Factor (PSDF)
- a HVDC line ID if you want to see the effect of an increase of the active power set point on a other branch (better known as DCDF). Note that in that case, the HVDC line must be explicitly described in the network through `HvdcLine` object. If the HVDC line is modeled with two injections because the HVDC line is not explicitly modeled (as in network coming from UCTE format), you have to put both injection ids and make the difference between the sensitivity results.

.. code-block:: python

     >>> sa.set_branch_flow_factor_matrix(['BBE2AA1  FFR3AA1  1'], [zone, injection_id, transformer_id, hvdc_id])

AC sensitivity analysis
-----------------------

It's possible to perform an AC sensitivity analysis almost in the same way, just use ``create_ac_analysis`` instead of
``create_dc_analysis``:

.. doctest::

    >>> analysis = pp.sensitivity.create_ac_analysis()

Additionally, AC sensitivity analysis allows to compute voltage sensitivities. You just need to define
the list of buses for which you want to compute the sensitivity, and a list of regulating equipments
(generators, transformers, etc.):

.. doctest::

    >>> analysis = pp.sensitivity.create_ac_analysis()
    >>> analysis.set_bus_voltage_factor_matrix(bus_ids=['VLHV1_0', 'VLLOAD_0'], target_voltage_ids=['GEN'])
    >>> result = analysis.run(network)
    >>> result.get_bus_voltages_sensitivity_matrix()
           VLHV1_0  VLLOAD_0
    GEN  17.629602   7.89637

Post-contingency analysis
-------------------------

In previous paragraphs, sensitivities were only computed on pre-contingency situation. Additionally, you can compute sensitivities on post-contingency situations, by adding contingency definitions to your analysis:

.. doctest::

    >>> analysis = pp.sensitivity.create_dc_analysis()
    >>> analysis.set_branch_flow_factor_matrix(branches_ids=['NHV1_NHV2_1', 'NHV1_NHV2_2'], variables_ids=['LOAD'])
    >>> analysis.add_single_element_contingency('NHV1_NHV2_1')
    >>> result = analysis.run(network)
    >>> result.get_reference_flows('NHV1_NHV2_1')
                     NHV1_NHV2_1  NHV1_NHV2_2
    reference_flows          0.0        600.0
    >>> result.get_branch_flows_sensitivity_matrix('NHV1_NHV2_1')
          NHV1_NHV2_1  NHV1_NHV2_2
    LOAD          0.0         -1.0
