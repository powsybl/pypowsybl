Running a dynamic simulation with dynawaltz
===========================================

.. currentmodule:: pypowsybl.dynamic

You can use the module :mod:`pypowsybl.dynamic` in order to run time domain simulation on networks.

Start by importing the module:

.. code-block:: python

   import pypowsybl.network as pn
   import pypowsybl.dynamic as dyn

Providers
---------
For now we only support the Dynawaltz integration, provided by the `Dynawo <https://dynawo.github.io>`_ project.


Prerequisites
-------------
The pypowsybl config file (generally located at ~/.itools/config.yaml) must define the dynawaltz section to find your dynawaltz installation and defaults parameters
Here is an example of a simple config.yaml file. It uses the same configurations as in powsybl-dynawatlz.

.. code-block:: yaml+jinja

    dynamic-simulation-default-parameters:
        startTime: 0
        stopTime: 30
    dynawaltz:
        homeDir: PATH_TO_DYNAWO
        debug: true
    dynawaltz-default-parameters:
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
    2. A curve mapping, it records the given values to be watch by the simulation tool. Curves are the output of the simulation
    3. A event mapping, it maps the different events. (their time event is done in configurations file for now)

There is a class for each of these elements.

You will see a lot of arguments called parameterSetId. Dynawaltz use a lot of parameters that will be stored in files.

Pypowsybl will find the path to this file in the powsybl config.yaml in dynawaltz-default-parameters.parametersFile value.

The parameterSetId argument must match an id in this file (generally called models.par).

Simple example
--------------
To run a Dynawaltz simulation:

.. code-block:: python

    import pypowsybl.dynamic as dyn
    import pypowsybl as pp

    # load a network
    network = pp.network.create_eurostag_tutorial_example1_network()

    # dynamic mapping
    model_mapping = dyn.ModelMapping()
    model_mapping.add_base_load("LOAD", "LAB", "LoadAlphaBeta") # and so on

    # events mapping
    events = dyn.EventMapping()
    events.add.add_disconnection("GEN_DISCONNECTION", 10, "GEN")
    events.add_disconnection("LINE_DISCONNECTION", "NHV1_NHV2_1", 10, Side.ONE)

    # curves mapping
    curves = dyn.CurveMapping()
    curves.add_curves("LOAD", ["load_PPu", "load_QPu"])

    # simulations parameters
    start_time = 0
    end_time = 50
    sim = dyn.Simulation()
    # running the simulation
    results = sim.run(network, model_mapping, events, curves, start_time, end_time)
    # getting the results
    results.curves() # dataframe containing the curves mapped