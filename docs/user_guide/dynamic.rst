Running a dynamic simulation with dynawo
===========================================

.. currentmodule:: pypowsybl.dynamic

You can use the module :mod:`pypowsybl.dynamic` in order to run time domain simulation on networks.

Start by importing the module:

.. code-block:: python

   import pypowsybl.network as pn
   import pypowsybl.dynamic as dyn

Providers
---------
For now we only support the Dynawo simulator integration, provided by the `Dynawo <https://dynawo.github.io>`_ project.


Prerequisites
-------------
The pypowsybl config file (generally located at ~/.itools/config.yaml) must define the dynawo section to find your dynawo installation and defaults parameters
Here is an example of a simple config.yaml file. It uses the same configurations as in powsybl-dynawo.

.. code-block:: yaml+jinja

    dynamic-simulation-default-parameters:
        startTime: 0
        stopTime: 30
    dynawo:
        homeDir: PATH_TO_DYNAWO
        debug: true
    dynawo-simulation-default-parameters:
        parametersFile: ./models.par
        network.parametersFile: ./network.par
        network.parametersId: "1"
        solver.type: IDA
        solver.parametersFile: ./solver.par
        solver.parametersId: "1"


Parameters
----------

To make a dynamic simulation, you need multiple things:

    1. A dynamic mapping, it links the static elements (generators, loads, lines) to their dynamic behavior (alpha beta load)
    2. A event mapping, it maps the different events. (e.g equipment disconnection)
    3. A curve mapping, it records the given values to be watch by the simulation tool. Curves are the output of the simulation

There is a class for each of these elements.

You will see a lot of arguments called parameterSetId. Dynawo simulator use a lot of parameters that will be stored in files.

Pypowsybl will find the path to this file in the powsybl config.yaml in dynawo-simulation-default-parameters.parametersFile value.

The parameterSetId argument must match an id in this file (generally called models.par).

Simple example
--------------
To run a Dynawo simulation:

.. code-block:: python

    import pypowsybl.dynamic as dyn
    import pypowsybl as pp

    # load a network
    network = pp.network.create_eurostag_tutorial_example1_network()

    # dynamic mapping
    model_mapping = dyn.ModelMapping()
    model_mapping.add_base_load(static_id='LOAD',
                                parameter_set_id='LAB',
                                dynamic_model_id='DM_LOAD',
                                model_name='LoadAlphaBeta') # and so on

    # events mapping
    event_mapping = dyn.EventMapping()
    event_mapping.add.add_disconnection(static_id='GEN', start_time=10)
    event_mapping.add_disconnection(static_id='NHV1_NHV2_1', start_time=10, disconnect_only='ONE')

    # curves mapping
    variables_mapping = dyn.OutputVariableMapping()
    variables_mapping.add_dynamic_model_curves("DM_LOAD", ["load_PPu", "load_QPu"])
    variables_mapping.add_standard_model_final_state_values('NGEN', 'Upu_value', False) # and so on

    # simulations parameters
    start_time = 0
    end_time = 50
    sim = dyn.Simulation()
    # running the simulation
    results = sim.run(network, model_mapping, event_mapping, variables_mapping, start_time, end_time)
    # getting the results
    results.status()
    results.curves() # dataframe containing the mapped curves