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
    params = voltage_initializer.VoltageInitializerParameters()
    n = pp.network.create_eurostag_tutorial_example1_network()
    some_gen_id = n.get_generators().iloc[0].name
    params.add_constant_q_generators([some_gen_id])
    # no shunts in eurostag_tutorial_example1_network
    # some_shunt_id = n.get_shunt_compensators().iloc[0].name
    # params.add_variable_shunt_compensators([n.get_shunt_compensators().iloc[0].name])
    some_2wt_id = n.get_2_windings_transformers().iloc[0].name
    params.add_variable_two_windings_transformers([some_2wt_id])

    params.add_algorithm_param({"foo": "bar", "bar": "bar2"})
    params.set_objective(VoltageInitializerObjective.SPECIFIC_VOLTAGE_PROFILE)

    results = voltage_initializer.run(n, params, True)
    results.apply_all_modification(n)

    print(results.get_status())
    print(results.get_indicators())

