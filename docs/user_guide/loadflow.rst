Load Flow
=========

.. testsetup:: *

    import pypowsybl as pp

You can use the module ``pypowsybl.loadflow`` in order to run load flows on networks. By default, load flows are based on the OpenLoadFlow implementation, fully described on `Powsybl website <https://www.powsybl.org/pages/documentation/simulation/powerflow/openlf.html>`_. OpenLoadFlow supports AC Newton-Raphson and linear DC calculation methods.

Parameters
**********

The most important part before running a load flow is, after importing a network, knowing the parameters and change it if needed. Let's have a look at the default ones:

.. doctest::

    >>> network = pp.network.create_eurostag_tutorial_example1_network()
    >>> import pypowsybl.loadflow
    >>> pp.loadflow.Parameters()
    LoadFlowParameters(voltage_init_mode=UNIFORM_VALUES, transformer_voltage_control_on=False, no_generator_reactive_limits=False, phase_shifter_regulation_on=False, twt_split_shunt_admittance=False, simul_shunt=False, read_slack_bus=False, write_slack_bus=False, distributed_slack=True, balance_type=PROPORTIONAL_TO_GENERATION_P_MAX, dc_use_transformer_ratio=True, countries_to_balance=[], connected_component_mode=<ConnectedComponentMode.MAIN: 0>)


All parameters are fully described in `Powsybl loadfow parameter documentation <https://www.powsybl.org/pages/documentation/simulation/powerflow/>`_.

- *voltage_init_mode* represents the starting point mode: use ``pp.loadflow.VoltageInitMode.`` and then UNIFORM_VALUES for a flat start, and DC_VALUES for a DC load flow based starting point.
- The *transformer_voltage_control_on* attribute set to FALSE means that two or three windings transformers with ratio tap changers are not controlling voltage. The initial tap position is used for the resolution.
- *twt_split_shunt_admittance* refers to the modelling of transformer legs. If you want to split the conductance and the susceptance in two, one at each side of the serie impedance, use TRUE.
- The *simul_shunt* attribute set to FALSE means that shunt compensator are not controlling voltage. Note that OpenLoadFlow does not support this feature yet.
- The *read_slack_bus* parameter set to TRUE means that the slack bus is read in the network through an dedicate extension. Prefer FALSE if you want to use the most meshed one. The slack bus selector is configured in the OpenLoadFlow specific parameters.
- The *write_slack_bus* parameter set to FALSE means that the slack bus found and used by the load flow engine is not written as an extension inside the network.
- *distributed_slack* set to TRUE means that the active power slack is distributed, on loads or on generators.
- *balance_type* is an enum used in case of distributed slack. Use ``pp.loadflow.BalanceType.`` followed by PROPORTIONAL_TO_LOAD to distribute slack on loads,  PROPORTIONAL_TO_GENERATION_P_MAX or PROPORTIONAL_TO_GENERATION_P to distribute on generators.
- The *dc_use_transformer_ratio* parameter is used only for DC load flows to include ratios in the equation system.
- *countries_to_balance* allows to model slack distribution on some countries, use [] if the slack is distributed on the whole network.
- And then, the *connected_component_mode* parameter set to ``pp.loadflow.ConnectedComponentMode.`` followed by MAIN computes flows only on the main connected component. Prefer ALL for a run on all connected component.

AC Load Flow
************

.. doctest::

    >>> network = pp.network.create_eurostag_tutorial_example1_network()
    >>> import pypowsybl.loadflow
    >>> parameters = pp.loadflow.Parameters(distributed_slack=False)
    >>> results = pp.loadflow.run_ac(network, parameters)
    >>> results
    [LoadFlowComponentResult(connected_component_num=0, synchronous_component_num=0, status=CONVERGED, iteration_count=3, slack_bus_id='VLHV1_0', slack_bus_active_power_mismatch=-606.5596837558763)]
    >>> results[0].slack_bus_active_power_mismatch
    -606.5596837558763

.. doctest::

    >>> network = pp.network.create_four_substations_node_breaker_network()
    >>> import pypowsybl.loadflow
    >>> parameters = pp.loadflow.Parameters(balance_type=pp.loadflow.BalanceType.PROPORTIONAL_TO_GENERATION_P_MAX, distributed_slack=True)
    >>> results = pp.loadflow.run_ac(network, parameters)
    >>> network.get_buses().v_mag
    id
    S1VL1_0    224.764350
    S1VL2_0    400.000000
    S2VL1_0    408.846146
    S3VL1_0    400.000000
    S4VL1_0    400.000000
    Name: v_mag, dtype: float64

If you want more logs:

.. doctest::

    >>> pp.set_debug_mode(True)


DC Load Flow
************

.. doctest::

    >>> network = pp.network.create_eurostag_tutorial_example1_network()
    >>> import pypowsybl.loadflow
    >>> parameters = pp.loadflow.Parameters(dc_use_transformer_ratio=False, distributed_slack=True, balance_type=pp.loadflow.BalanceType.PROPORTIONAL_TO_GENERATION_P_MAX)
    >>> results = pp.loadflow.run_dc(network, parameters)
    >>> network.get_lines().p1
    id
    NHV1_NHV2_1    300.0
    NHV1_NHV2_2    300.0
    Name: p1, dtype: float64
    >>> network.get_lines().p2
    id
    NHV1_NHV2_1   -300.0
    NHV1_NHV2_2   -300.0
    Name: p2, dtype: float64
    >>> network.get_buses().v_angle
    id
    VLGEN_0      2.643659
    VLHV1_0      0.000000
    VLHV2_0     -3.928173
    VLLOAD_0   -10.115696
    Name: v_angle, dtype: float64

