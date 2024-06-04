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

- Specify which buses will have reactive slacks attached in the ACOPF solving.

.. code-block:: python
    import pypowsybl as pp
    import pypowsybl.voltage_initializer as v_init
    params = v_init.VoltageInitializerParameters()
    params.set_reactive_slack_buses_mode(va.VoltageInitializerReactiveSlackBusesMode.NO_GENERATION)

- Specify what is the log level of the AMPL solving.

.. code-block:: python
    import pypowsybl as pp
    import pypowsybl.voltage_initializer as v_init
    params = v_init.VoltageInitializerParameters()
    params.set_log_level_ampl(va.VoltageInitializerLogLevelAmpl.ERROR)
    params.set_log_level_solver(va.VoltageInitializerLogLevelSolver.EVERYTHING)

- Change plausible voltage level limits in ACOPF solving.

.. code-block:: python
    import pypowsybl as pp
    import pypowsybl.voltage_initializer as v_init
    params = v_init.VoltageInitializerParameters()
    params.set_min_plausible_low_voltage_limit(0.45)
    params.set_max_plausible_high_voltage_limit(1.2)

- Tune the threshold defining null values in AMPL.

.. code-block:: python
    import pypowsybl as pp
    import pypowsybl.voltage_initializer as v_init
    params = v_init.VoltageInitializerParameters()
    params.set_min_plausible_active_power_threshold(1)
    params.set_low_impedance_threshold(1e-5)

- Modify the parameters used for the correction of generator limits.

.. code-block:: python
    import pypowsybl as pp
    import pypowsybl.voltage_initializer as v_init
    params = v_init.VoltageInitializerParameters()
    params.set_max_plausible_power_limit(7800)
    params.set_high_active_power_default_limit(950)
    params.set_low_active_power_default_limit(0.5)
    params.set_default_minimal_qp_range(0.45)
    params.set_default_qmax_pmax_ratio(0.45)

- Tune the thresholds used to ignore buses or voltage level limits with nominal voltage lower than them.

.. code-block:: python
    import pypowsybl as pp
    import pypowsybl.voltage_initializer as v_init
    params = v_init.VoltageInitializerParameters()
    params.set_min_nominal_voltage_ignored_bus(0.5)
    params.set_min_nominal_voltage_ignored_voltage_bounds(1)
