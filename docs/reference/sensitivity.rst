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
network first with ``networkCacheEnabled`` on, and the same load flow parameters must be used for both:

.. code-block:: python

    >>> parameters = pp.loadflow.Parameters(provider_parameters={'networkCacheEnabled': 'true'})
    >>> pp.loadflow.run_ac(network, parameters)
    >>> analysis = pp.sensitivity.create_ac_analysis()
    >>> analysis.add_branch_admittance_factor_matrix(monitored_lines, controllable_lines,
    ...                                             SensitivityFunctionType.BRANCH_ACTIVE_POWER_1)
    >>> gradient = analysis.run_adjoint(network, cotangents, parameters).get_gradient()

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
