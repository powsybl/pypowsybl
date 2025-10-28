# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Dict, Optional
from pypowsybl.loadflow import Parameters as LfParameters
from pypowsybl.loadflow.impl.util import parameters_from_c
from pypowsybl import _pypowsybl


class Parameters:  # pylint: disable=too-few-public-methods
    """
    Parameters for a sensitivity analysis execution.

    All parameters are first read from you configuration file, then overridden with
    the constructor arguments.

    Please note that sensitivity providers may not honor all parameters, according to their capabilities.
    The exact behaviour of some parameters may also depend on your sensitivity provider.
    Please check the documentation of your provider for that information.

    .. currentmodule:: pypowsybl.sensitivity

    Args:
        load_flow_parameters: parameters that are common to loadflow and sensitivity analysis
        provider_parameters: Define parameters linked to the sensitivity analysis provider
            the names of the existing parameters can be found with method ``get_provider_parameters_names``
    """

    def __init__(self, load_flow_parameters: Optional[LfParameters] = None,
                 provider_parameters: Optional[Dict[str, str]] = None,
                 flow_flow_sensitivity_value_threshold: float = None,
                 voltage_voltage_sensitivity_value_threshold: float = None,
                 flow_voltage_sensitivity_value_threshold: float = None,
                 angle_flow_sensitivity_value_threshold: float = None,):
        self._init_with_default_values()
        if load_flow_parameters is not None:
            self.load_flow_parameters = load_flow_parameters
        if provider_parameters is not None:
            self.provider_parameters = provider_parameters
        if flow_flow_sensitivity_value_threshold is not None:
            self.flow_flow_sensitivity_value_threshold = flow_flow_sensitivity_value_threshold
        if voltage_voltage_sensitivity_value_threshold is not None:
            self.voltage_voltage_sensitivity_value_threshold = voltage_voltage_sensitivity_value_threshold
        if flow_voltage_sensitivity_value_threshold is not None:
            self.flow_voltage_sensitivity_value_threshold = flow_voltage_sensitivity_value_threshold
        if angle_flow_sensitivity_value_threshold is not None:
            self.angle_flow_sensitivity_value_threshold = angle_flow_sensitivity_value_threshold

    def _init_with_default_values(self) -> None:
        default_parameters = _pypowsybl.SensitivityAnalysisParameters()
        self.flow_flow_sensitivity_value_threshold = default_parameters.flow_flow_sensitivity_value_threshold
        self.voltage_voltage_sensitivity_value_threshold = default_parameters.voltage_voltage_sensitivity_value_threshold
        self.flow_voltage_sensitivity_value_threshold = default_parameters.flow_voltage_sensitivity_value_threshold
        self.angle_flow_sensitivity_value_threshold = default_parameters.angle_flow_sensitivity_value_threshold
        self.load_flow_parameters = parameters_from_c(default_parameters.loadflow_parameters)
        self.provider_parameters = dict(
            zip(default_parameters.provider_parameters_keys, default_parameters.provider_parameters_values))

    def _to_c_parameters(self) -> _pypowsybl.SensitivityAnalysisParameters:
        c_parameters = _pypowsybl.SensitivityAnalysisParameters()
        c_parameters.flow_flow_sensitivity_value_threshold = self.flow_flow_sensitivity_value_threshold
        c_parameters.voltage_voltage_sensitivity_value_threshold = self.voltage_voltage_sensitivity_value_threshold
        c_parameters.flow_voltage_sensitivity_value_threshold = self.flow_voltage_sensitivity_value_threshold
        c_parameters.angle_flow_sensitivity_value_threshold = self.angle_flow_sensitivity_value_threshold
        c_parameters.loadflow_parameters = self.load_flow_parameters._to_c_parameters()  # pylint: disable=protected-access
        c_parameters.provider_parameters_keys = list(self.provider_parameters.keys())
        c_parameters.provider_parameters_values = list(self.provider_parameters.values())
        return c_parameters

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"flow_flow_sensitivity_value_threshold={self.flow_flow_sensitivity_value_threshold}" \
               f", voltage_voltage_sensitivity_value_threshold={self.voltage_voltage_sensitivity_value_threshold}" \
               f", flow_voltage_sensitivity_value_threshold={self.flow_voltage_sensitivity_value_threshold}" \
               f", angle_flow_sensitivity_value_threshold={self.angle_flow_sensitivity_value_threshold}" \
               f", load_flow_parameters={self.load_flow_parameters}" \
               f", provider_parameters={self.provider_parameters!r}" \
               f")"
