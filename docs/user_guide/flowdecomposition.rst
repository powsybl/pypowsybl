Running a flow decomposition
============================

.. testsetup:: *

    import pathlib
    import pandas as pd

    import pypowsybl as pp
    
    pd.options.display.max_columns = None
    pd.options.display.expand_frame_repr = False
    import os
    cwd = os.getcwd()
    PROJECT_DIR = pathlib.Path(cwd).parent
    DATA_DIR = PROJECT_DIR.joinpath('data')

You can use the module :mod:`pypowsybl.flowdecomposition` in order to run flow decomposition on networks.
Please check out the examples below.

The general idea of this API is to create a decomposition object.
Then, you can define contingencies if necessary.
Then, you can define XNE and XNEC. XNEC definition requires pre-defined contingencies.
Some pre-defined XNE selection adder functions are available.
All the adder functions will be united when running a flow decomposition.
Finally, you can run the flow decomposition with some flow decomposition and/or load flow parameters.

For detailed documentation of involved classes and methods, please refer to the :mod:`API reference <pypowsybl.flowdecomposition>`.

Start by importing the module:

.. code-block:: python

   import pypowsybl as pp

First example
-------------

To perform a flow decomposition, you need at least a network.  
We will define a flow decomposition object, add some contingencies and some monitored lines.
Those lines will be mapped to the network when running a flow decomposition.  
The flow decomposition computation returns a data frame containing the flow decomposition and the reference values.  
The reference values are the active power flows in AC on the original network and in DC on the compensated network.  
By default, the compensated network is the same as the original network as the loss compensation is not activated by default.  
Here are toy examples that do not reflect reality.  

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> network = pp.network.create_eurostag_tutorial_example1_network()
    >>> branch_ids = ['NHV1_NHV2_1', 'NHV1_NHV2_2']
    >>> flow_decomposition = pp.flowdecomposition.create_decomposition() \
    ...     .add_single_element_contingencies(branch_ids) \
    ...     .add_monitored_elements(branch_ids, branch_ids)
    >>> flow_decomposition_dataframe = flow_decomposition.run(network)
    >>> flow_decomposition_dataframe
                               branch_id contingency_id country1 country2  ac_reference_flow  dc_reference_flow  commercial_flow  x_node_flow  pst_flow  internal_flow  loop_flow_from_be  loop_flow_from_fr
    xnec_id                                                                                                                                                                                                 
    NHV1_NHV2_1              NHV1_NHV2_1                      FR       BE         302.444049              300.0              0.0          0.0       0.0            0.0              300.0                0.0
    NHV1_NHV2_1_NHV1_NHV2_2  NHV1_NHV2_1    NHV1_NHV2_2       FR       BE         610.562161              600.0              0.0          0.0       0.0            0.0              600.0                0.0
    NHV1_NHV2_2              NHV1_NHV2_2                      FR       BE         302.444049              300.0              0.0          0.0       0.0            0.0              300.0                0.0
    NHV1_NHV2_2_NHV1_NHV2_1  NHV1_NHV2_2    NHV1_NHV2_1       FR       BE         610.562161              600.0              0.0          0.0       0.0            0.0              600.0                0.0

Loop flows
----------

Here is another example with imbricated zones.  
This example will highlight loop flows from the peripheral areas.  

.. image:: ../_static/images/flow_decomposition_Loop_Flow.svg
    
.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> network = pp.network.load(str(DATA_DIR.joinpath('NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct')))
    >>> flow_decomposition = pp.flowdecomposition.create_decomposition().add_monitored_elements(['BLOAD 11 FLOAD 11 1', 'EGEN  11 FGEN  11 1', 'FGEN  11 BGEN  11 1', 'FLOAD 11 ELOAD 11 1'])
    >>> flow_decomposition_dataframe = flow_decomposition.run(network)
    >>> flow_decomposition_dataframe
                                   branch_id contingency_id country1 country2  ac_reference_flow  dc_reference_flow  commercial_flow  x_node_flow  pst_flow  internal_flow  loop_flow_from_be  loop_flow_from_es  loop_flow_from_fr
    xnec_id
    BLOAD 11 FLOAD 11 1  BLOAD 11 FLOAD 11 1                      BE       FR                NaN              200.0     0.000000e+00          0.0       0.0            0.0       0.000000e+00              100.0       1.000000e+02
    EGEN  11 FGEN  11 1  EGEN  11 FGEN  11 1                      ES       FR                NaN              100.0    -8.526513e-14          0.0       0.0            0.0       3.552714e-14              100.0      -3.552714e-14
    FGEN  11 BGEN  11 1  FGEN  11 BGEN  11 1                      FR       BE                NaN              200.0    -1.421085e-13          0.0       0.0            0.0       8.526513e-14              100.0       1.000000e+02
    FLOAD 11 ELOAD 11 1  FLOAD 11 ELOAD 11 1                      FR       ES                NaN              100.0     0.000000e+00          0.0       0.0            0.0       0.000000e+00              100.0       0.000000e+00

On this example, the AC load flow does not converge, the fallback to DC load flow is activated by default.
This means that the AC reference values are NaNs.  
For each line where the AC reference is not a number, the rescaling is disabled to prevent NaN propagation.  

PST flows
---------

Network details
^^^^^^^^^^^^^^^

Here is another example with a more complex network containing a phase-shifting transformer (PST).  
This PST has a non-neutral tap position, thus forcing the flows in a certain direction.  
This example illustrates the flow decomposition with such network element.  

.. image:: ../_static/images/flow_decomposition_PST.svg

As we cannot set a PST on an interconnection, we set an equivalent null load called 'BLOAD 11'.

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> network = pp.network.load(str(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct')))
    >>> network.get_generators()
                       name energy_source  target_p   min_p   max_p   min_q   max_q  rated_s reactive_limits_kind  target_v  target_q  voltage_regulator_on regulated_element_id   p   q   i voltage_level_id     bus_id  connected
    id
    FGEN  11_generator              OTHER     100.0 -1000.0  1000.0 -1000.0  1000.0      NaN              MIN_MAX     400.0       0.0                  True   FGEN  11_generator NaN NaN NaN          FGEN  1  FGEN  1_0       True
    BLOAD 12_generator              OTHER     100.0 -1000.0  1000.0 -1000.0  1000.0      NaN              MIN_MAX     400.0       0.0                  True   BLOAD 12_generator NaN NaN NaN          BLOAD 1  BLOAD 1_1       True
    >>> network.get_loads()
                      name       type     p0   q0   p   q   i voltage_level_id     bus_id  connected
    id                                                                                          
    BLOAD 12_load       UNDEFINED  200.0  0.0 NaN NaN NaN          BLOAD 1  BLOAD 1_1       True
    >>> network.get_lines()
                            name    r    x   g1   b1   g2   b2  p1  q1  i1  p2  q2  i2 voltage_level1_id voltage_level2_id    bus1_id    bus2_id  connected1  connected2
    id                                                                                                                                                              
    FGEN  11 BLOAD 12 1       0.5  1.5  0.0  0.0  0.0  0.0 NaN NaN NaN NaN NaN NaN           FGEN  1           BLOAD 1  FGEN  1_0  BLOAD 1_1        True        True
    FGEN  11 BLOAD 11 1       1.0  3.0  0.0  0.0  0.0  0.0 NaN NaN NaN NaN NaN NaN           FGEN  1           BLOAD 1  FGEN  1_0  BLOAD 1_0        True        True
    >>> network.get_buses()
                  name  v_mag  v_angle  connected_component  synchronous_component voltage_level_id
    id                                                                                         
    FGEN  1_0         NaN      NaN                    0                      0          FGEN  1
    BLOAD 1_0         NaN      NaN                    0                      0          BLOAD 1
    BLOAD 1_1         NaN      NaN                    0                      0          BLOAD 1
    >>> network.get_2_windings_transformers()
                            name    r    x       g        b  rated_u1  rated_u2  rated_s  p1  q1  i1  p2  q2  i2 voltage_level1_id voltage_level2_id    bus1_id    bus2_id  connected1  connected2
    id                                                                                                                                                                                        
    BLOAD 11 BLOAD 12 2       0.5  1.5  0.0002  0.00015     400.0     400.0      NaN NaN NaN NaN NaN NaN NaN           BLOAD 1           BLOAD 1  BLOAD 1_1  BLOAD 1_0        True        True
    >>> network.get_phase_tap_changers()
                             tap  low_tap  high_tap  step_count  regulating regulation_mode  regulation_value  target_deadband regulating_bus_id
    id                                                                                                                                      
    BLOAD 11 BLOAD 12 2    0      -16        16          33       False       FIXED_TAP               NaN              NaN  
    
Neutral tap position
^^^^^^^^^^^^^^^^^^^^

Here are the results with neutral tap position.

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> flow_decomposition = pp.flowdecomposition.create_decomposition().add_monitored_elements(['FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 12 1'])
    >>> flow_decomposition_dataframe = flow_decomposition.run(network)
    >>> flow_decomposition_dataframe
                                   branch_id contingency_id country1 country2  ac_reference_flow  dc_reference_flow  commercial_flow  x_node_flow  pst_flow  internal_flow  loop_flow_from_be  loop_flow_from_fr
    xnec_id                                                                                                                                                                                                     
    FGEN  11 BLOAD 11 1  FGEN  11 BLOAD 11 1                      FR       BE          29.003009               25.0        28.999015          0.0      -0.0            0.0          -1.999508          -1.999508
    FGEN  11 BLOAD 12 1  FGEN  11 BLOAD 12 1                      FR       BE          87.009112               75.0        86.997046          0.0       0.0            0.0          -5.998523          -5.998523
    >>> flow_decomposition_dataframe[[c for c in flow_decomposition_dataframe.columns if ("flow" in c and "reference" not in c)]].sum(axis=1)
    xnec_id
    FGEN  11 BLOAD 11 1    25.0
    FGEN  11 BLOAD 12 1    75.0
    dtype: float64

The results are not rescaled to the AC reference by default.

Non neutral tap position
^^^^^^^^^^^^^^^^^^^^^^^^

Here are the results with non-neutral tap position.

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> network = pp.network.load(str(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct')))
    >>> network.update_phase_tap_changers(id="BLOAD 11 BLOAD 12 2", tap=1)
    >>> network.get_phase_tap_changers()
                             tap  low_tap  high_tap  step_count  regulating regulation_mode  regulation_value  target_deadband regulating_bus_id
    id                                                                                                                                      
    BLOAD 11 BLOAD 12 2    1      -16        16          33       False       FIXED_TAP               NaN              NaN                  
    >>> flow_decomposition = pp.flowdecomposition.create_decomposition().add_monitored_elements(['FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 12 1'])
    >>> flow_decomposition_dataframe = flow_decomposition.run(network)
    >>> flow_decomposition_dataframe
                                   branch_id contingency_id country1 country2  ac_reference_flow  dc_reference_flow  commercial_flow  x_node_flow    pst_flow  internal_flow  loop_flow_from_be  loop_flow_from_fr
    xnec_id                                                                                                                                                                                                       
    FGEN  11 BLOAD 11 1  FGEN  11 BLOAD 11 1                      FR       BE         192.390656         188.652703        29.015809          0.0  163.652703            0.0          -2.007905          -2.007905
    FGEN  11 BLOAD 12 1  FGEN  11 BLOAD 12 1                      FR       BE         -76.189072         -88.652703       -87.047428          0.0  163.652703            0.0           6.023714           6.023714
    >>> flow_decomposition_dataframe[[c for c in flow_decomposition_dataframe.columns if ("flow" in c and "reference" not in c)]].sum(axis=1)
    xnec_id
    FGEN  11 BLOAD 11 1    188.652703
    FGEN  11 BLOAD 12 1     88.652703
    dtype: float64



Note that the reference flow on the 2d branch has changed of sign.  
As we use it as reference, all the decomposed flows have also changed of sign.  

Unmerged X node flows
---------------------

To illustrate X node flow, we need a network with unmerged x nodes.
Those x nodes might represent HVDCs, outside countries, etc.
Merged X nodes will not be considered here.

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> network = pp.network.load(DATA_DIR.joinpath('19700101_0000_FO4_UX1.uct'))
    >>> flow_decomposition = pp.flowdecomposition.create_decomposition().add_interconnections_as_monitored_elements()
    >>> flow_decomposition.run(network)
                                                                               branch_id contingency_id country1 country2  ac_reference_flow  dc_reference_flow  commercial_flow  x_node_flow  pst_flow  internal_flow  loop_flow_from_be  loop_flow_from_de  loop_flow_from_fr
    xnec_id
    XBD00011 BD000011 1 + XBD00011 DB000011 1  XBD00011 BD000011 1 + XBD00011 DB000011 1                      BE       DE         121.821917         124.685261       171.516849   -33.155274  2.951653            0.0           0.226369      -8.994903e-09         -16.854336
    XBD00012 BD000011 1 + XBD00012 DB000011 1  XBD00012 BD000011 1 + XBD00012 DB000011 1                      BE       DE         121.821917         124.685261       171.516849   -33.155274  2.951653            0.0           0.226369      -8.994903e-09         -16.854336
    XBF00011 BF000011 1 + XBF00011 FB000011 1  XBF00011 BF000011 1 + XBF00011 FB000011 1                      BE       FR        -775.578124        -764.445217       679.262024   170.472453  7.112098            0.0        -124.052946      -6.713719e-09          31.651588
    XBF00021 BF000021 1 + XBF00021 FB000021 1  XBF00021 BF000021 1 + XBF00021 FB000021 1                      BE       FR        -234.032855        -242.462652       169.385837    44.108499 -0.604396            0.0          62.252842      -1.954504e-09         -32.680130
    XBF00022 BF000021 1 + XBF00022 FB000022 1  XBF00022 BF000021 1 + XBF00022 FB000022 1                      BE       FR        -234.032855        -242.462652       169.385837    44.108499 -0.604396            0.0          62.252842      -1.954504e-09         -32.680130
    XDF00011 DF000011 1 + XDF00011 FD000011 1  XDF00011 DF000011 1 + XDF00011 FD000011 1                      DE       FR       -1156.356167       -1150.629478       906.966302   216.310548 -5.903306            0.0          -0.452738      -2.032061e-08          33.708672


Adder functions
---------------

The flow decomposition algorithm will decompose flow on monitored elements.  
You need to define those elements.  
You can either define those elements with specific ids or with automatic functions.  

The union of selected elements will be decomposed.  
For example, if you select the same branch in the same state two times, it will be decomposed only once.  

Specific adder functions
^^^^^^^^^^^^^^^^^^^^^^^^

Specific adder functions are based on IDs.  
When running the flow decomposition, the IDs will be mapped to the network.  
If an identifiable is not found on the network, a warning will be sent (beware of activated logs) and the corresponding XNEC will be ignored.  

With those adder functions, you can create XNEs and/or XNECs.  
You need to specify contingencies first if required.  
If you try to create a XNEC with an undefined contingency ID, an error will be raised.  

By default, if you add monitored elements with branches and contingencies, it will create all possible valid pairs of branch and states.  
By default, all the states are base case and all contingency states defined.  
You can specify which states you want in the base add monitored element function or use a dedicated pre/post contingency function.  

Here is an example

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> network = pp.network.load(str(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct')))
    >>> flow_decomposition = pp.flowdecomposition.create_decomposition() \
    ... .add_monitored_elements(['FGEN  11 BLOAD 11 1']) \ 
    ... .add_single_element_contingency('FGEN  11 BLOAD 11 1') \
    ... .add_monitored_elements(['FGEN  11 BLOAD 12 1'], ['FGEN  11 BLOAD 11 1']) \ 
    ... .add_multiple_elements_contingency(['FGEN  11 BLOAD 11 1', 'BLOAD 11 BLOAD 12 2']) \
    ... .add_monitored_elements('FGEN  11 BLOAD 12 1', 'FGEN  11 BLOAD 11 1_BLOAD 11 BLOAD 12 2', pp.flowdecomposition.ContingencyContextType.SPECIFIC)
    >>> flow_decomposition.run(network)
                                                                  branch_id                           contingency_id country1 country2  ac_reference_flow  dc_reference_flow  commercial_flow  x_node_flow  pst_flow  internal_flow  loop_flow_from_be  loop_flow_from_fr
    xnec_id                                                                                                                                                                                                                                                              
    FGEN  11 BLOAD 11 1                                 FGEN  11 BLOAD 11 1                                                FR       BE          29.003009               25.0        28.999015          0.0      -0.0            0.0          -1.999508          -1.999508
    FGEN  11 BLOAD 12 1                                 FGEN  11 BLOAD 12 1                                                FR       BE          87.009112               75.0        86.997046          0.0       0.0            0.0          -5.998523          -5.998523
    FGEN  11 BLOAD 12 1_FGEN  11 BLOAD 11 1             FGEN  11 BLOAD 12 1                      FGEN  11 BLOAD 11 1       FR       BE         116.016179              100.0       115.996062          0.0       0.0            0.0          -7.998031          -7.998031
    FGEN  11 BLOAD 12 1_FGEN  11 BLOAD 11 1_BLOAD 1...  FGEN  11 BLOAD 12 1  FGEN  11 BLOAD 11 1_BLOAD 11 BLOAD 12 2       FR       BE         100.034531              100.0       115.996062          0.0       0.0            0.0          -7.998031          -7.998031

See the API reference for more details about how each specific adder works.

Automatic adder functions
^^^^^^^^^^^^^^^^^^^^^^^^^

Automatic adder functions are based on automatic selection processes.  
With those functions, you can create XNEs and/or XNECs.  

Some automatic XNE selection adder functions are available.

5% zonal PTDF criteria
~~~~~~~~~~~~~~~~~~~~~~

This adder function will add all branches in the N state that have a zone-to-zone PTDF greater than 5% or that are interconnections.  
This function adds some non-negligible precomputing to the process.  

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> network = pp.network.load(str(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct')))
    >>> flow_decomposition = pp.flowdecomposition.create_decomposition() \
    ... .add_5perc_ptdf_as_monitored_elements()
    >>> flow_decomposition.run(network)
                                   branch_id contingency_id country1 country2  ac_reference_flow  dc_reference_flow  commercial_flow  x_node_flow  pst_flow  internal_flow  loop_flow_from_be  loop_flow_from_fr
    xnec_id                                                                                                                                                                                                     
    BLOAD 11 BLOAD 12 2  BLOAD 11 BLOAD 12 2                      BE       BE           3.005666              -25.0        28.999015          0.0      -0.0      -1.999508           0.000000          -1.999508
    FGEN  11 BLOAD 11 1  FGEN  11 BLOAD 11 1                      FR       BE          29.003009               25.0        28.999015          0.0      -0.0       0.000000          -1.999508          -1.999508
    FGEN  11 BLOAD 12 1  FGEN  11 BLOAD 12 1                      FR       BE          87.009112               75.0        86.997046          0.0       0.0       0.000000          -5.998523          -5.998523

Interconnections
~~~~~~~~~~~~~~~~

This adder function will add interconnections in the N state.  
Be careful when using this function with large networks.  

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> network = pp.network.load(str(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct')))
    >>> flow_decomposition = pp.flowdecomposition.create_decomposition() \
    ... .add_interconnections_as_monitored_elements()
    >>> flow_decomposition.run(network)
                                   branch_id contingency_id country1 country2  ac_reference_flow  dc_reference_flow  commercial_flow  x_node_flow  pst_flow  internal_flow  loop_flow_from_be  loop_flow_from_fr
    xnec_id                                                                                                                                                                                                     
    FGEN  11 BLOAD 11 1  FGEN  11 BLOAD 11 1                      FR       BE          29.003009               25.0        28.999015          0.0      -0.0            0.0          -1.999508          -1.999508
    FGEN  11 BLOAD 12 1  FGEN  11 BLOAD 12 1                      FR       BE          87.009112               75.0        86.997046          0.0       0.0            0.0          -5.998523          -5.998523

All branches
~~~~~~~~~~~~

This adder function will add all branches in the N state.  
Be careful when using this function with large networks.  

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> network = pp.network.load(str(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct')))
    >>> flow_decomposition = pp.flowdecomposition.create_decomposition() \
    ... .add_all_branches_as_monitored_elements()
    >>> flow_decomposition.run(network)
                                   branch_id contingency_id country1 country2  ac_reference_flow  dc_reference_flow  commercial_flow  x_node_flow  pst_flow  internal_flow  loop_flow_from_be  loop_flow_from_fr
    xnec_id                                                                                                                                                                                                     
    BLOAD 11 BLOAD 12 2  BLOAD 11 BLOAD 12 2                      BE       BE           3.005666              -25.0        28.999015          0.0      -0.0      -1.999508           0.000000          -1.999508
    FGEN  11 BLOAD 11 1  FGEN  11 BLOAD 11 1                      FR       BE          29.003009               25.0        28.999015          0.0      -0.0       0.000000          -1.999508          -1.999508
    FGEN  11 BLOAD 12 1  FGEN  11 BLOAD 12 1                      FR       BE          87.009112               75.0        86.997046          0.0       0.0       0.000000          -5.998523          -5.998523

Mixing adder functions
^^^^^^^^^^^^^^^^^^^^^^

You can mix everything together as you like.

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> network = pp.network.load(str(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct')))
    >>> parameters = pp.flowdecomposition.Parameters(sensitivity_epsilon=pp.flowdecomposition.Parameters.DISABLE_SENSITIVITY_EPSILON)
    >>> flow_decomposition = pp.flowdecomposition.create_decomposition() \
    ... .add_single_element_contingency('FGEN  11 BLOAD 11 1') \
    ... .add_monitored_elements(['FGEN  11 BLOAD 12 1', 'BLOAD 11 BLOAD 12 2'], ['FGEN  11 BLOAD 11 1']) \ 
    ... .add_multiple_elements_contingency(['FGEN  11 BLOAD 11 1', 'BLOAD 11 BLOAD 12 2']) \
    ... .add_postcontingency_monitored_elements('FGEN  11 BLOAD 12 1', 'FGEN  11 BLOAD 11 1_BLOAD 11 BLOAD 12 2') \
    ... .add_interconnections_as_monitored_elements() \
    ... .add_all_branches_as_monitored_elements()
    >>> flow_decomposition.run(network, flow_decomposition_parameters=parameters)
                                                                  branch_id                           contingency_id country1 country2  ac_reference_flow  dc_reference_flow  commercial_flow  x_node_flow  pst_flow  internal_flow  loop_flow_from_be  loop_flow_from_fr
    xnec_id                                                                                                                                                                                                                                                              
    BLOAD 11 BLOAD 12 2                                 BLOAD 11 BLOAD 12 2                                                BE       BE           3.005666              -25.0        28.999015          0.0      -0.0      -1.999508           0.000000          -1.999508
    BLOAD 11 BLOAD 12 2_FGEN  11 BLOAD 11 1             BLOAD 11 BLOAD 12 2                      FGEN  11 BLOAD 11 1       BE       BE          32.000000               -0.0         0.000000          0.0      -0.0       0.000000           0.000000           0.000000
    FGEN  11 BLOAD 11 1                                 FGEN  11 BLOAD 11 1                                                FR       BE          29.003009               25.0        28.999015          0.0      -0.0       0.000000          -1.999508          -1.999508
    FGEN  11 BLOAD 12 1                                 FGEN  11 BLOAD 12 1                                                FR       BE          87.009112               75.0        86.997046          0.0       0.0       0.000000          -5.998523          -5.998523
    FGEN  11 BLOAD 12 1_FGEN  11 BLOAD 11 1             FGEN  11 BLOAD 12 1                      FGEN  11 BLOAD 11 1       FR       BE         116.016179              100.0       115.996062          0.0      -0.0       0.000000          -7.998031          -7.998031
    FGEN  11 BLOAD 12 1_FGEN  11 BLOAD 11 1_BLOAD 1...  FGEN  11 BLOAD 12 1  FGEN  11 BLOAD 11 1_BLOAD 11 BLOAD 12 2       FR       BE         100.034531              100.0       115.996062          0.0       0.0       0.000000          -7.998031          -7.998031

Note: if one of our xnec is missing, it might be caused by a zero MW DC reference flow, you can show them by reducing the sensitivity-epsilon as bone before.  
This will be fixed in next versions.  


Configuration file 
------------------

Inside your config.yml file, you can change the default Configuration of the flow decomposition.  
Here are the available parameters and their default values:

.. code-block::
    :caption: Available parameters and their default values

    flow-decomposition-default-parameters:
        enable-losses-compensation: False
        losses-compensation-epsilon: 1e-5
        sensitivity-epsilon: 1e-5
        rescale-enabled: False
        dc-fallback-enabled-after-ac-divergence: True
        sensitivity-variable-batch-size: 15000

The flow decomposition parameters can be overwritten in Python.  
If you have memory issues, do not hesitate to reduce the `sensitivity-variable-batch-size` parameter.

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> network = pp.network.load(str(DATA_DIR.joinpath('NETWORK_PST_FLOW_WITH_COUNTRIES.uct')))
    >>> parameters = pp.flowdecomposition.Parameters(enable_losses_compensation=True, 
    ... losses_compensation_epsilon=pp.flowdecomposition.Parameters.DISABLE_LOSSES_COMPENSATION_EPSILON, 
    ... sensitivity_epsilon=pp.flowdecomposition.Parameters.DISABLE_SENSITIVITY_EPSILON, 
    ... rescale_enabled=True, 
    ... dc_fallback_enabled_after_ac_divergence=True,
    ... sensitivity_variable_batch_size=1000)
    >>> flow_decomposition = pp.flowdecomposition.create_decomposition().add_monitored_elements(['BLOAD 11 BLOAD 12 2', 'FGEN  11 BLOAD 11 1', 'FGEN  11 BLOAD 12 1'])
    >>> flow_decomposition_dataframe = flow_decomposition.run(network, parameters)
    >>> flow_decomposition_dataframe
                                   branch_id contingency_id country1 country2  ac_reference_flow  dc_reference_flow  commercial_flow  x_node_flow  pst_flow  internal_flow  loop_flow_from_be  loop_flow_from_fr
    xnec_id                                                                                                                                                                                                     
    BLOAD 11 BLOAD 12 2  BLOAD 11 BLOAD 12 2                      BE       BE           3.005666          -28.99635         3.008332          0.0      -0.0      -0.001333           0.000000          -0.001333
    FGEN  11 BLOAD 11 1  FGEN  11 BLOAD 11 1                      FR       BE          29.003009           28.99635        29.005675          0.0       0.0       0.000000          -0.001333          -0.001333
    FGEN  11 BLOAD 12 1  FGEN  11 BLOAD 12 1                      FR       BE          87.009112           86.98905        87.017108          0.0       0.0       0.000000          -0.003998          -0.003998

You can also overwrite the Load flow parameters.

.. doctest::
    :options: +NORMALIZE_WHITESPACE

    >>> network = pp.network.create_eurostag_tutorial_example1_network()
    >>> flow_decomposition_parameters = pp.flowdecomposition.Parameters()
    >>> load_flow_parameters = pp.loadflow.Parameters()
    >>> flow_decomposition = pp.flowdecomposition.create_decomposition().add_monitored_elements(['NHV1_NHV2_1', 'NHV1_NHV2_2'])
    >>> flow_decomposition_dataframe = flow_decomposition.run(network, flow_decomposition_parameters, load_flow_parameters)
    >>> flow_decomposition_dataframe
                   branch_id contingency_id country1 country2  ac_reference_flow  dc_reference_flow  commercial_flow  x_node_flow  pst_flow  internal_flow  loop_flow_from_be  loop_flow_from_fr
    xnec_id                                                                                                                                                                                     
    NHV1_NHV2_1  NHV1_NHV2_1                      FR       BE         302.444049              300.0              0.0          0.0       0.0            0.0              300.0                0.0
    NHV1_NHV2_2  NHV1_NHV2_2                      FR       BE         302.444049              300.0              0.0          0.0       0.0            0.0              300.0                0.0

