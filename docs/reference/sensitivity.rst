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
    create_dc_analysis
    DcSensitivityAnalysis.run
    set_default_provider
    get_default_provider
    get_provider_names

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

    SensitivityAnalysis.set_branch_flow_factor_matrix
    AcSensitivityAnalysis.set_bus_voltage_factor_matrix
    SensitivityAnalysis.set_zones

In order to create, inspect and manipulate zones, you can use the following methods:

.. autosummary::
   :nosignatures:
   :toctree: api/

    create_empty_zone
    create_country_zone
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
