Sensitivity analysis
====================

.. module:: pypowsybl.sensitivity

The sensitivity analysis module allows to compute the impact of various variations (typically, generation variations),
on other physical values on the network (typically, active power flows or currents on lines).


Run a sensitivity analysis
--------------------------
You can run an AC or DC security analysis using the following methods:

.. autosummary::
   :nosignatures:
   :toctree: api/

    create_ac_analysis
    AcSensitivityAnalysis.run
    AcSensitivityAnalysis.run_adjoint
    create_dc_analysis
    DcSensitivityAnalysis.run
    set_default_provider
    get_default_provider
    get_provider_names


Reverse mode (adjoint / VJP)
----------------------------

:meth:`AcSensitivityAnalysis.run` computes the sensitivity matrix ``S`` column by column, one solve per
monitored function. When what you actually need is the gradient of a single scalar loss ``L`` with respect to
many levers, reverse mode gets there in **one** transpose solve, without ever materialising ``S``: given the
output cotangents ``y_bar = dL/dfunction``, :meth:`AcSensitivityAnalysis.run_adjoint` returns
``theta_bar = S^T . y_bar``. The cost stops depending on the number of levers, which is what makes a
gradient-descent loop over hundreds of controls affordable.

It reuses the AC load flow OpenLoadFlow retains in its network cache, so a load flow must have run on the
network first with ``networkCacheEnabled`` on, and the same load flow parameters must be used for both.

The cotangents are yours to compute: they are the partial derivatives of *your* objective with respect to
each monitored function, in that function's own units, ordered as the factor matrices were declared.

Take a least-squares voltage objective ``L = 0.5 * sum_i (V_i - V_i*)**2``, where ``V_i`` is the solved
voltage of monitored bus *i* and ``V_i*`` the voltage you want there — ``wanted_voltages`` in the code below.
``V_i*`` is a constant of your objective, not a network attribute: in particular it is **not** the
generators' ``target_v``, which is the lever this example differentiates with respect to. The cotangents are
then simply the voltage deviations ``dL/dV_i = V_i - V_i*``, read off the load flow that warmed the cache —
in kV, since that is what ``BUS_VOLTAGE`` is in:

.. doctest::

    >>> import pandas as pd
    >>> import pypowsybl as pp
    >>> from pypowsybl.sensitivity import ContingencyContextType, SensitivityFunctionType, SensitivityVariableType
    >>> network = pp.network.create_ieee14()
    >>> parameters = pp.loadflow.Parameters(distributed_slack=False,
    ...                                     provider_parameters={'networkCacheEnabled': 'true'})
    >>> lf_result = pp.loadflow.run_ac(network, parameters)
    >>> monitored_buses = ['VL4_0', 'VL5_0', 'VL7_0']
    >>> generators = ['B2-G', 'B3-G', 'B6-G']
    >>> analysis = pp.sensitivity.create_ac_analysis()
    >>> analysis.add_factor_matrix(monitored_buses, generators, [], ContingencyContextType.NONE,
    ...                            SensitivityFunctionType.BUS_VOLTAGE,
    ...                            SensitivityVariableType.BUS_TARGET_VOLTAGE, 'objective')
    >>> wanted_voltages = pd.Series({'VL4_0': 138.0, 'VL5_0': 136.0, 'VL7_0': 15.0})
    >>> voltages = network.get_buses().loc[monitored_buses]['v_mag']
    >>> cotangents = voltages - wanted_voltages
    >>> cotangents.round(3)
    id
    VL4_0   -0.614
    VL5_0    1.634
    VL7_0   -0.139
    dtype: float64
    >>> result = analysis.run_adjoint(network, {'objective': cotangents}, parameters)
    >>> result.get_gradient('objective').round(3)
    B2-G    0.405
    B3-G    0.093
    B6-G    2.679
    Name: dL/dvariable, dtype: float64

The ``2.679`` against ``B6-G`` is ``dL/dtarget_v`` for that generator — the derivative of the loss with
respect to its voltage set point, in loss units per kV — and the single transpose solve produced it for
every generator at once. Cotangents passed as a ``pandas.Series`` are aligned on the monitored function ids
rather than taken positionally, so they cannot silently go in shuffled; a plain list is positional.

The run is base case only: an analysis that declares contingencies is refused rather than answered for the
base case, since the two are indistinguishable once returned. A lever outside the solved component — a line
open at both ends, a disconnected shunt — answers ``0.0``, which is what the forward path reports for it,
and a lever that cannot be differentiated at all raises rather than returning a ``NaN`` that would travel
into a gradient step with nothing to trace it back to.


Parameters
----------

The execution of the sensitivity analysis can be customized using sensitivity analysis parameters.

.. autosummary::
   :nosignatures:
   :toctree: api/

    Parameters
    get_provider_parameters_names


Contingencies definition
------------------------

.. autosummary::
   :nosignatures:
   :toctree: api/

    SensitivityAnalysis.add_single_element_contingency
    SensitivityAnalysis.add_multiple_elements_contingency
    SensitivityAnalysis.add_single_element_contingencies

Sensitivities definition
------------------------

You can either define the sensitivities you want to compute by defining individual elements variations,
or by defining zones.

In AC mode, you can define voltage sensitivities, in addition to flows sensitivities.

.. autosummary::
   :nosignatures:
   :toctree: api/

    SensitivityAnalysis.add_branch_flow_factor_matrix
    SensitivityAnalysis.add_precontingency_branch_flow_factor_matrix
    SensitivityAnalysis.add_postcontingency_branch_flow_factor_matrix
    AcSensitivityAnalysis.set_bus_voltage_factor_matrix
    AcSensitivityAnalysis.add_bus_voltage_factor_matrix
    AcSensitivityAnalysis.add_shunt_susceptance_factor_matrix
    AcSensitivityAnalysis.add_branch_admittance_factor_matrix
    AcSensitivityAnalysis.add_svc_pilot_factor_matrix
    SensitivityAnalysis.set_zones

In order to create, inspect and manipulate zones, you can use the following methods:

.. autosummary::
   :nosignatures:
   :toctree: api/

    create_empty_zone
    create_country_zone
    create_zone_from_injections_and_shift_keys
    create_zones_from_glsk_file
    Zone
    Zone.id
    Zone.shift_keys_by_injections_ids
    Zone.injections_ids
    Zone.get_shift_key
    Zone.add_injection
    Zone.remove_injection
    Zone.move_injection_to
    ZoneKeyType


Results
-------

When the security analysis is completed, you can inspect its results:

.. autosummary::
   :nosignatures:
   :toctree: api/

    DcSensitivityAnalysisResult
    DcSensitivityAnalysisResult.get_branch_flows_sensitivity_matrix
    DcSensitivityAnalysisResult.get_reference_flows
    AcSensitivityAnalysisResult
    AcSensitivityAnalysisResult.get_bus_voltages_sensitivity_matrix
    AcSensitivityAnalysisResult.get_reference_voltages
    SensitivityAnalysisResult
    SensitivityAnalysisResult.get_sensitivity_matrix
    SensitivityAnalysisResult.get_reference_matrix
    AcSensitivityAnalysisAdjointResult
    AcSensitivityAnalysisAdjointResult.get_gradient


GLSK UCTE file loading
----------------------

.. module:: pypowsybl.glsk

UCTE GLSK files can be loaded using glsk.load and GLSKDocument, data can be used for zone creation.

.. autosummary::
   :nosignatures:
   :toctree: api/

   load
   GLSKDocument
   GLSKDocument.get_gsk_time_interval_start
   GLSKDocument.get_gsk_time_interval_end
   GLSKDocument.get_countries
   GLSKDocument.get_points_for_country
   GLSKDocument.get_glsk_factors
