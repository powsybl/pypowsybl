Running a short-circuit analysis
================================

You can use the module :mod:`pypowsybl.shortcircuit` in order to perform a shortcircuit analysis on a network.
Please have a look at the examples below.

For detailed documentation of involved classes and methods, please refer to the :mod:`API reference <pypowsybl.shortcircuit>`.

Note that pypowsybl currently does not include a simulator to perform the short-circuit analysis.

Short-circuit analysis
----------------------

The current APIs allow the simulation of three-phase bus faults, where the fault resistance and reactance, if specified, are connected in series to ground.

To perform a short-circuit analysis, you need a network and at least one fault to simulate on that network. The network should have a transient or
subtransient reactance at least on one generator. This reactance is stored in the 'generatorShortCircuit' extension.
The results of the analysis include the calculated currents and voltages on the network after the fault. Depending on parameters,
the results will be given as three-phase magnitude or detailed on each phase.
Optionally, depending on specific parameters of the simulation, the results also include

     - the contributions of each feeder to the short-circuit current
     - a list of all the violations after the fault
     - the voltages, that are higher than a threshold, calculated after the fault on the whole network


Available parameters
--------------------

The parameters available to run a shortcircuit analysis are:

    - with_fortescue_result: indicates whether the currents and voltages are to be given in three-phase magnitude or
      detailed with magnitude and angle on each phase. This parameter also applies to the feeder results and voltage results.
    - with_feeder_result: indicates whether the contributions of each feeder to the short circuit current at the fault
      node should be calculated.
    - with_limit_violations: indicates whether limit violations should be returned after the calculation. If true, a
      list of buses where the calculated shortcircuit current is higher than the maximum admissible current (stored in
      ip_max in the identifiableShortCircuit extension) or lower than the minimum admissible current (stored in ip_min
      in the identifiableShortCircuit extension).
    - with_voltage_result: indicates whether the voltage profile should be calculated on every node of the network
    - min_voltage_drop_proportional_threshold: specifies a threshold for filtering the voltage results.
      Only nodes where the voltage drop due to the short circuit is greater than this property are retained.
    - study_type: specifies the type of short circuit study. It can be SUB_TRANSIENT, TRANSIENT or STEADY_STATE.


+----------------------------------------+---------------+
|*Parameter*                             |*Default value*|
+========================================+===============+
|with_fortescue_result                   | true          |
+----------------------------------------+---------------+
|with_feeder_result                      | true          |
+----------------------------------------+---------------+
|with_limit_violations                   | true          |
+----------------------------------------+---------------+
|with_voltage_result                     | true          |
+----------------------------------------+---------------+
|min_voltage_drop_proportional_threshold | 0             |
+----------------------------------------+---------------+
|study_type                              | TRANSIENT     |
+----------------------------------------+---------------+




Simple example
-------------

.. code-block::

    >>> import pypowsybl as pp
    >>> import pypowsybl.network as pn
    >>> import pandas as pd
    >>> # create a network
    >>> n = pn.create_four_substations_node_breaker_network()
    >>> # sets some short-circuit parameters
    >>> pars = pp.shortcircuit.Parameters(with_feeder_result = False, with_limit_violations = False, study_type = pp.shortcircuit.ShortCircuitStudyType.TRANSIENT)
    >>> # create a short-circuit analysis context
    >>> sc = pp.shortcircuit.create_analysis()
    >>> # create a bus fault on the first two buses
    >>> buses = n.get_buses()
    >>> sc.set_faults(id = ['fault_1', 'fault_2'], element_id = [buses.index[0], buses.index[1]], r = [1, 1], x = [2, 2])
    >>> # perform the short-circuit analysis        
    >>> # results = sc.run(n, pars, 'sc_provider_1')
    >>> # returns the analysis results
    >>> # results.fault_results


