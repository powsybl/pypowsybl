Running an optimal power flow
=============================

.. currentmodule:: pypowsybl.opf

.. testsetup:: *

    import pypowsybl.network as n
    import pypowsybl.opf as opf

You can use the module :mod:`pypowsybl.opf` to run optimal power flow calculations.

A standard load flow solves for branch flows and bus voltages from specified set points, such as generator active power and voltage targets. Optimal Power Flow (OPF) searches for the best operating point for a given objective, while satisfying the power-flow equations and operational constraints modeled by the OPF implementation.

OPF follows PowSyBl/IIDM conventions for signs and units.

In AC/DC mode, OPF solves a coupled model with AC buses, detailed DC nodes and lines, and voltage source converters (VSCs) linking the AC and DC sides.


Running an OPF
--------------

Start by importing the network and OPF modules:

.. code-block:: python

    import pypowsybl.network as n
    import pypowsybl.opf as opf

Create a network, select an OPF mode, and run the calculation:

.. code-block:: python

    network = n.create_ieee14()

    parameters = opf.OptimalPowerFlowParameters(
        mode=opf.OptimalPowerFlowMode.LOADFLOW,
        solver_type=opf.SolverType.IPOPT,
    )

    success = opf.run_ac(network, parameters)

    if not success:
        raise RuntimeError("OPF did not converge")


Parameters
----------

The calculation is configured with :class:`OptimalPowerFlowParameters`.

This user guide only shows the main execution path. For the complete list of parameters, modes, solver options and default values, see the OPF API reference.


Results
-------

The :func:`run_ac` function returns ``True`` when the calculation converges and ``False`` otherwise.

When the calculation converges, solved values are written back to the network object. This includes the computed operating point as well as the solved power flow results of the network. These values can be read from the usual network dataframes.


AC/DC model overview
--------------------

In AC/DC mode, OPF solves AC and DC equations in the same optimization problem. The model includes:

- AC bus voltage magnitudes and angles;
- AC generated active and reactive powers;
- DC node voltages;
- DC line currents;
- VSC active and reactive powers;
- Converter DC currents.

AC/DC objective function
~~~~~~~~~~~~~~~~~~~~~~~~

In AC/DC mode, the current objective function minimizes DC line current usage:

.. math::

    \min \sum_l \left(I_{1,l}^2 + I_{2,l}^2\right)

where ``I1`` and ``I2`` are the two oriented current variables of each modeled DC line.


DC buses
~~~~~~~~

DC nodes satisfy a current-balance equation:

.. math::

    \sum_i I_i = 0

where the currents are defined as flowing out of the DC node.


DC lines
~~~~~~~~

A DC line between ``dc_node1`` and ``dc_node2`` is modeled with Ohm's law.

For a line resistance ``R`` and node voltages ``V1`` and ``V2``:

.. math::

    I_1 = \frac{V_1 - V_2}{R}

.. math::

    I_2 = \frac{V_2 - V_1}{R}

``I1`` is positive when current flows out of ``dc_node1`` towards the line. ``I2`` is positive when current flows out of ``dc_node2`` towards the line.


Voltage source converters
~~~~~~~~~~~~~~~~~~~~~~~~~

A voltage source converter links one AC bus to two DC nodes.

A VSC can control either:

- the active power on the AC side, with ``P_PCC`` mode;
- the voltage difference between its two DC nodes, with ``V_DC`` mode.

For ``V_DC`` mode, the voltage target is:

.. math::

    V^{target}_{DC} = V_1 - V_2

Converter power balance
~~~~~~~~~~~~~~~~~~~~~~~

The converter DC current ``I`` is oriented from ``dc_node1`` to ``dc_node2``.

The DC-side converter power is:

.. math::

    P_{DC} = I \cdot (V_1 - V_2)

The AC-side converter active power ``P_AC`` follows the converter AC-terminal load convention. ``P_AC > 0`` means the converter absorbs active power from the AC network, and ``P_AC < 0`` means it injects active power into the AC network. For ``P_PCC`` mode, ``target_p`` and the written ``p_ac`` value follow this convention.


The active-power balance of the converter is:

.. math::

    P_{AC} + P_{DC} = P_{loss}

with:

.. math::

    P_{loss} \geq 0


Converter losses are modeled as a function of the DC current magnitude:

.. math::

    P_{loss} = a + b \cdot |I| + c \cdot |I|^2

where:

- \(I\) is the DC current oriented from ``dc_node1`` to ``dc_node2``;
- \(a\) is the idle loss;
- \(b\) is the switching loss coefficient;
- \(c\) is the resistive loss coefficient.


In rectifier mode, power flows from AC to DC:

.. math::

    P_{AC} \gt 0,\quad P_{DC} \lt 0

In inverter mode, power flows from DC to AC:

.. math::

    P_{AC} \lt 0,\quad P_{DC} \gt 0


DC voltage bases
----------------

In AC/DC OPF, networks with several nominal voltages inside the same DC component are rejected before solving.
