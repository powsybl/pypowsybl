Sensitivity analysis
====================

You can use the module ``pypowsybl.sensitivity_analysis`` in order to perform sensitivity analysis on a network.

DC sensitivity analysis
-----------------------

To perform a sensitivity analysis, you first need to define "factors" you want to compute.
What we call a factor is the dependency of a function, typically the active power flow on a branch, to
a variable, typically the active power injection ofr a generator.

To make the definition of those factors easier, ``pypowsybl`` provides a method to define the branches for
which the flow sensitivity should be computed, and for which injections. We obtain a matrix of sensitivities
as a result:

.. doctest::

    >>> import pypowsybl as pp
    >>> import pypowsybl.network as pn
    >>> import pypowsybl.sensitivity_analysis as sa
    >>> network = pn.create_eurostag_tutorial_example1_network()
    >>> analysis = sa.create()
    >>> analysis.set_branch_flow_factor_matrix(branches_ids=['NHV1_NHV2_1', 'NHV1_NHV2_2'], injections_or_transformers_ids=['LOAD'])
    >>> result = analysis.run_dc(network)
    >>> result.get_reference_flows()
                     NHV1_NHV2_1  NHV1_NHV2_2
    reference_flows        300.0        300.0
    >>> result.get_branch_flows_sensitivity_matrix()
          NHV1_NHV2_1  NHV1_NHV2_2
    LOAD         -0.5         -0.5


AC sensitivity analysis
-----------------------

It's possible to perform an AC sensitivity analysis almost in the same way, just call ``run_ac`` instead:

.. doctest::

    >>> result = analysis.run_ac(network)

Additionally, AC sensitivity analysis allows to compute voltage sensitivities. You just need to define
the list of buses for which you want to compute the sensitivity, and a list of regulating equipments
(generators, transformers, ...):

.. doctest::

    >>> analysis = sa.create()
    >>> analysis.set_bus_voltage_factor_matrix(bus_ids=['VLHV1_0', 'VLLOAD_0'], target_voltage_ids=['GEN'])
    >>> result = analysis.run_ac(network)
    >>> result.get_bus_voltages_sensitivity_matrix()
           VLHV1_0  VLLOAD_0
    GEN  17.629602   7.89637

Post-contingency analysis
-------------------------

In previous paragraphs, sensitivities were only computed on N situation.
Additionally, you can compute sensitivities on post-contingency situations, by adding
contingency definitions to your analysis:

.. doctest::

    >>> analysis = sa.create()
    >>> analysis.set_branch_flow_factor_matrix(branches_ids=['NHV1_NHV2_1', 'NHV1_NHV2_2'], injections_or_transformers_ids=['LOAD'])
    >>> analysis.add_single_element_contingency('NHV1_NHV2_1')
    >>> result = analysis.run_dc(network)
    >>> result.get_post_contingency_reference_flows('NHV1_NHV2_1')
                     NHV1_NHV2_1  NHV1_NHV2_2
    reference_flows          0.0        600.0
    >>> result.get_post_contingency_branch_flows_sensitivity_matrix('NHV1_NHV2_1')
          NHV1_NHV2_1  NHV1_NHV2_2
    LOAD          0.0         -1.0
