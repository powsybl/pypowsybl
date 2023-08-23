Running a short-circuit analysis
================================

You can use the module :mod:`pypowsybl.shortcircuit` in order to perform a shortcircuit analysis on a network.
Please check out the examples below.

For detailed documentation of involved classes and methods, please refer to the :mod:`API reference <pypowsybl.shortcircuit>`.

Note that, currently, no simulator is integrated in pypowsybl to perform the short-circuit analysis.

Short-circuit analysis
----------------------

The current APIs allow the simulation of three-phased bus faults, where the fault resistance and reactance are connected to the ground in series.

To perform a short-circuit analysis, you need a network and at least a fault to simulate on this network.
The results of the analysis contain the computed current and voltages on the network after the fault, in three-phased magnitude.
Optionally, depending on specific parameters for the simulation, the results contain also

     - the contributions of each feeder to the short circuit current  (parameter with_feeder_result)
     - a list of all the violations after the fault (parameter with_limit_violations)


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


