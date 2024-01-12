Running a security analysis
===========================

.. testsetup:: *

    import pypowsybl as pp
    import pandas as pd
    pd.options.display.max_columns = None
    pd.options.display.expand_frame_repr = False

You can use the module :mod:`pypowsybl.security` in order to perform a security analysis on a network.
Please check out the examples below.

For detailed documentation of involved classes and methods, please refer to the :mod:`API reference <pypowsybl.security>`.


AC security analysis
--------------------

To perform a security analysis, you need at least a network and a contingency on this network.
In the result there are violations detected with the initial loadflow on the network.
These violations are collected in pre_contingency_result. The results contain also
the violations created by the contingency, they are collected by contingency in post_contingency_results :

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> network = pp.network.create_eurostag_tutorial_example1_network()
    >>> network.update_loads(id='LOAD', p0=800)
    >>> security_analysis = pp.security.create_analysis()
    >>> security_analysis.add_single_element_contingency('NHV1_NHV2_1', 'First contingency')
    >>> result = security_analysis.run_ac(network)
    >>> result.pre_contingency_result
    PreContingencyResult(, status=CONVERGED, limit_violations=[3])
    >>> result.post_contingency_results
    {'First contingency': PostContingencyResult(contingency_id='First contingency', status=CONVERGED, limit_violations=[3])}
    >>> result.limit_violations
                                  subject_name   limit_type limit_name   limit  acceptable_duration  limit_reduction        value side
    contingency_id    subject_id
                      NHV1_NHV2_1                   CURRENT  permanent   500.0           2147483647              1.0   623.568946  ONE
                      NHV1_NHV2_2                   CURRENT  permanent   500.0           2147483647              1.0   655.409876  TWO
                      VLHV1                     LOW_VOLTAGE              400.0           2147483647              1.0   398.917401
    First contingency NHV1_NHV2_2                   CURRENT        20'  1200.0                   60              1.0  1438.021676  ONE
                      NHV1_NHV2_2                   CURRENT  permanent   500.0           2147483647              1.0  1477.824335  TWO
                      VLHV1                     LOW_VOLTAGE              400.0           2147483647              1.0   392.158685



Adding monitored Elements
^^^^^^^^^^^^^^^^^^^^^^^^^

This feature is used to get information on different element of the network after the loadflow's computations.
Information can be obtained on buses, branches and three windings transformers.

.. testsetup:: security.monitored_elements

    import pandas as pd
    pd.options.display.float_format = '{:,.2f}'.format

.. doctest:: security.monitored_elements
    :options: +NORMALIZE_WHITESPACE

    >>> network = pp.network.create_eurostag_tutorial_example1_network()
    >>> security_analysis = pp.security.create_analysis()
    >>> security_analysis.add_single_element_contingency('NHV1_NHV2_1', 'NHV1_NHV2_1')
    >>> security_analysis.add_single_element_contingency('NGEN_NHV1', 'NGEN_NHV1')
    >>> security_analysis.add_monitored_elements(voltage_level_ids=['VLHV2'])
    >>> security_analysis.add_postcontingency_monitored_elements(branch_ids=['NHV1_NHV2_2'], contingency_ids=['NHV1_NHV2_1', 'NGEN_NHV1'])
    >>> security_analysis.add_postcontingency_monitored_elements(branch_ids=['NHV1_NHV2_1'], contingency_ids='NGEN_NHV1')
    >>> security_analysis.add_precontingency_monitored_elements(branch_ids=['NHV1_NHV2_2'])
    >>> results = security_analysis.run_ac(network)
    >>> results.bus_results
                                                                  v_mag  v_angle
    contingency_id operator_strategy_id voltage_level_id bus_id
                                        VLHV2            NHV2   389.95    -3.51
    NGEN_NHV1                           VLHV2            NHV2   569.04    -1.71
    NHV1_NHV2_1                         VLHV2            NHV2   366.58    -7.50
    >>> results.branch_results
                                                        p1     q1       i1      p2      q2       i2  flow_transfer
    contingency_id operator_strategy_id branch_id
                                        NHV1_NHV2_2 302.44  98.74   456.77 -300.43 -137.19   488.99            NaN
    NGEN_NHV1                           NHV1_NHV2_1 301.06   0.00   302.80 -300.19 -116.60   326.75            NaN
                                        NHV1_NHV2_2 301.06   0.00   302.80 -300.19 -116.60   326.75            NaN
    NHV1_NHV2_1                         NHV1_NHV2_2 610.56 334.06 1,008.93 -601.00 -285.38 1,047.83            NaN



.. testcleanup:: security.monitored_elements

It also possible to get flow transfer on monitored branches in case of N-1 branch contingencies:

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> n = pp.network.create_eurostag_tutorial_example1_network()
    >>> sa = pp.security.create_analysis()
    >>> sa.add_single_element_contingencies(['NHV1_NHV2_1', 'NHV1_NHV2_2'])
    >>> sa.add_monitored_elements(branch_ids=['NHV1_NHV2_1', 'NHV1_NHV2_2'])
    >>> sa_result = sa.run_ac(n)
    >>> sa_result.branch_results
                                                              p1          q1           i1          p2          q2           i2  flow_transfer
    contingency_id operator_strategy_id branch_id
                                        NHV1_NHV2_2  302.444049   98.740275   456.768978 -300.433895 -137.188493   488.992798            NaN
                                        NHV1_NHV2_1  302.444049   98.740275   456.768978 -300.433895 -137.188493   488.992798            NaN
    NHV1_NHV2_2                         NHV1_NHV2_1  610.562154  334.056272  1008.928788 -600.996156 -285.379147  1047.825769       1.018761
    NHV1_NHV2_1                         NHV1_NHV2_2  610.562154  334.056272  1008.928788 -600.996156 -285.379147  1047.825769       1.018761

.. testcleanup:: security.monitored_elements

    pd.options.display.float_format = None
