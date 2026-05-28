Running an optimal power flow
=============================

.. currentmodule:: pypowsybl.opf

.. testsetup:: *

    import pypowsybl.network as n
    import pypowsybl.opf as opf

You can use the module :mod:`pypowsybl.opf` to run optimal power flow calculations.

Optimal Power Flow (OPF) adds optimization to a standard load-flow calculation. A standard load flow computes branch flows and bus voltages from specified set points, such as generator active power and voltage targets. OPF searches for the best operating point for a given objective, while satisfying the power-flow equations and operational constraints modeled by the OPF implementation.

Running an OPF
--------------

Start by importing the module:

.. code-block:: python

    import pypowsybl.network as n
    import pypowsybl.opf as opf

Then create a network, configure the OPF parameters, and run the calculation:

.. code-block:: python

    network = n.create_ieee14()

    parameters = opf.OptimalPowerFlowParameters(
        mode=opf.OptimalPowerFlowMode.LOADFLOW,
        solver_type=opf.SolverType.IPOPT,
    )

    success = opf.run_ac(network, parameters)

    if not success:
        raise RuntimeError("OPF did not converge")

The :func:`run_ac` function returns ``True`` when the OPF converges and ``False`` otherwise.

Parameters
----------

Use :class:`OptimalPowerFlowParameters` to configure the OPF.

The main parameters are:

- ``mode``: OPF mode to run
- ``solver_type``: solver used by the OPF
- ``solver_options``: solver-specific options

Parameters can be passed in the constructor or configured with fluent methods:

.. code-block:: python

    parameters = (
        opf.OptimalPowerFlowParameters()
        .with_mode(opf.OptimalPowerFlowMode.REDISPATCHING)
        .with_solver_type(opf.SolverType.IPOPT)
        .with_solver_option("max_iter", 50)
    )

OPF modes
---------

The available modes are defined by :class:`OptimalPowerFlowMode`.

- ``LOADFLOW``: OPF based on a standard AC load-flow formulation
- ``REDISPATCHING``: OPF with a redispatching-oriented objective and current-limit constraints
- ``ACDC``: OPF including detailed DC components and associated validation rules

Solvers
-------

The available solvers are defined by :class:`SolverType`.

- ``IPOPT``
- ``KNITRO``

Select the solver with the ``solver_type`` parameter.

Results
-------

Run the OPF with :func:`run_ac`.

The function returns a boolean convergence status:

- ``True``: the OPF converged
- ``False``: the OPF did not converge

When the OPF succeeds, the network is updated in place with the computed operating point.

Sign conventions
================

OPF follows PowSyBl/IIDM sign conventions.

- Loads: positive active and reactive powers are consumption.
- Generator and battery targets: positive active power means injection into the bus.
- Terminal flows use the load sign convention: positive means the equipment absorbs power.
- For a branch, ``p_i,j`` is positive when active power flows out of bus ``i``.

- For a VSC in ``P_PCC`` mode, ``target_p`` and the written ``p_ac`` value follow the converter AC-terminal load sign convention.

  - ``target_p > 0`` and ``p_ac > 0`` mean the converter absorbs active power from the AC network.
  - ``target_p < 0`` and ``p_ac < 0`` mean the converter injects active power into the AC network.
  - The same applies for reactive power.

- Converter balance follows::

    P_AC + P_DC = P_loss

  with ``P_loss >= 0``.

- The converter DC-side power is oriented as::

    P_DC = I * (V(dc_node1) - V(dc_node2))

- For a VSC in rectifier mode:

  - ``P_AC > 0``: the converter absorbs active power from the AC network.
  - ``P_DC < 0``: the converter injects active power into the DC network.

- For a VSC in inverter mode:

  - ``P_AC < 0``: the converter injects active power into the AC network.
  - ``P_DC > 0``: the converter absorbs active power from the DC network.

- For DC voltage control, ``target_v_dc`` means::

    V(dc_node1) - V(dc_node2) = target_v_dc

  Therefore, a positive ``target_v_dc`` means ``V(dc_node1) > V(dc_node2)``.

- Internally, the converter DC current variable ``I`` is oriented from ``dc_node1`` to ``dc_node2``.

  - ``I > 0`` means current flows from ``dc_node1`` to ``dc_node2``.
  - ``I < 0`` means current flows from ``dc_node2`` to ``dc_node1``.

- The written DC-side values ``p_dc1`` and ``p_dc2`` are currently computed from that oriented current and the solved DC node voltages::

    p_dc1 = I * V(dc_node1)
    p_dc2 = I * V(dc_node2)

  These values are oriented voltage-current products used for write-back and diagnostics.
  They should not be interpreted independently as terminal powers in load sign convention.

- The meaningful oriented DC-side power difference is::

    P_DC = p_dc1 - p_dc2 = I * (V(dc_node1) - V(dc_node2))

  This difference is the value consistent with the converter balance ``P_AC + P_DC = P_loss``.

Per-unit conventions
--------------------

OPF works internally in per-unit.

- Active and reactive powers use the nominal apparent power base
- AC voltages use the nominal voltage of the voltage level
- In AC/DC OPF, all DC nodes in the same detailed DC connected component must have the same nominal voltage, to avoid subtracting DC voltages defined on different bases
