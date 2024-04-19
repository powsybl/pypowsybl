Run the voltage initializer
===========================================

.. currentmodule:: pypowsybl.voltage_initializer

.. testsetup:: *

    import pypowsybl as pp
    import pypowsybl.voltage_initializer as v_init


Prerequisites
-------------

For now the voltage initializer tool rely on Ampl and Knitro. the binary knitroampl must be in your PATH.

The pypowsybl config file (generally located at ~/.itools/config.yaml) must define the ampl section to find your dynawaltz installation and defaults parameters
Here is an example of a simple config.yaml file.

.. code-block:: yaml+jinja

    ampl:
        homeDir: PATH_TO_AMPL

Quick start
-------------

Here is a simple starting example:

.. code-block:: python

    import pypowsybl as pp
    import pypowsybl.voltage_initializer as v_init
    params = v_init.VoltageInitializerParameters()
    n = pp.network.create_eurostag_tutorial_example1_network()
    some_gen_id = n.get_generators().iloc[0].name
    params.add_constant_q_generators([some_gen_id])
    some_2wt_id = n.get_2_windings_transformers().iloc[0].name
    params.add_variable_two_windings_transformers([some_2wt_id])

    params.set_objective(VoltageInitializerObjective.SPECIFIC_VOLTAGE_PROFILE)

    results = v_init.run(n, params)
    results.apply_all_modification(n)

    print(results.status())
    print(results.indicators())


Available settings in the VoltageInitializerParameters class
-------------

- Specify which parameters will be variable or fixed in the ACOPF solving

.. code-block:: python

    import pypowsybl as pp
    import pypowsybl.voltage_initializer as v_init
    params = v_init.VoltageInitializerParameters()
    n = pp.network.create_eurostag_tutorial_example1_network()
    some_gen_id = n.get_generators().iloc[0].name
    some_2wt_id = n.get_2_windings_transformers().iloc[0].name
    some_shunt_id = n.get_shunt_compensators().iloc[0].name
    params.add_constant_q_generators([some_gen_id])
    params.add_variable_two_windings_transformers([some_2wt_id])
    params.add_variable_shunt_compensators([some_shunt_id])


- Override the network voltage limits:

.. code-block:: python

    import pypowsybl as pp
    import pypowsybl.voltage_initializer as v_init
    params = v_init.VoltageInitializerParameters()
    params.add_specific_low_voltage_limits([("vl_id_1", False, 380)])
    params.add_specific_high_voltage_limits([("vl_id_2", False, 420)])

.. code-block:: python

    import pypowsybl as pp
    import pypowsybl.voltage_initializer as v_init
    params = v_init.VoltageInitializerParameters()
    params.add_specificvoltage_limits({"vl_id": (0.5, 1.2)})


- Specify the objective function and the objective distance

.. code-block:: python

    import pypowsybl as pp
    import pypowsybl.voltage_initializer as v_init
    params = v_init.VoltageInitializerParameters()
    params.set_objective(va.VoltageInitializerObjective.SPECIFIC_VOLTAGE_PROFILE)
    params.set_objective_distance(1.3)


- Tune scaling factors applied before ACOPF solving:

.. code-block:: python

    import pypowsybl as pp
    import pypowsybl.voltage_initializer as v_init
    params = v_init.VoltageInitializerParameters()
    params.set_default_variable_scaling_factor(1.1)
    params.set_default_constraint_scaling_factor(0.9)
    params.set_reactive_slack_variable_scaling_factor(0.15)
    params.set_twt_ratio_variable_scaling_factor(0.002)