# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Dict
from pypowsybl.loadflow.impl.util import parameters_from_c
from pypowsybl import _pypowsybl
from pypowsybl.loadflow import Parameters as LfParameters
from .increased_violations_parameters import IncreasedViolationsParameters


class Parameters:  # pylint: disable=too-few-public-methods
    """
    Parameters for a security analysis execution.

    All parameters are first read from you configuration file, then overridden with
    the constructor arguments.

    Please note that security analysis providers may not honor all parameters, according to their capabilities.
    For example, some providers will not be able to simulate the voltage control of shunt compensators, etc.
    The exact behaviour of some parameters may also depend on your security analysis provider.
    Please check the documentation of your provider for that information.

    .. currentmodule:: pypowsybl.security

    Args:
        load_flow_parameters: parameters that are common to loadflow and security analysis
        increased_violations_parameters: Define what violations should be considered increased between N and contingency situations
        provider_parameters: Define parameters linked to the security analysis provider
            the names of the existing parameters can be found with method ``get_provider_parameters_names``
    """

    def __init__(self,
                 load_flow_parameters: LfParameters = None,
                 increased_violations_parameters: IncreasedViolationsParameters = None,
                 provider_parameters: Dict[str, str] = None):
        self._init_with_default_values()
        if load_flow_parameters is not None:
            self.load_flow_parameters = load_flow_parameters
        if increased_violations_parameters:
            self._increased_violations = increased_violations_parameters
        if provider_parameters is not None:
            self.provider_parameters = provider_parameters

    @property
    def increased_violations(self) -> IncreasedViolationsParameters:
        """
        Define what violations should be considered increased between N and post-contingency situations
        """
        return self._increased_violations

    def _init_with_default_values(self) -> None:
        default_parameters = _pypowsybl.SecurityAnalysisParameters()
        self.load_flow_parameters = parameters_from_c(default_parameters.loadflow_parameters)
        self._increased_violations = IncreasedViolationsParameters(default_parameters.flow_proportional_threshold,
                                                                   default_parameters.low_voltage_proportional_threshold,
                                                                   default_parameters.low_voltage_absolute_threshold,
                                                                   default_parameters.high_voltage_proportional_threshold,
                                                                   default_parameters.high_voltage_absolute_threshold)
        self.provider_parameters = dict(
            zip(default_parameters.provider_parameters_keys, default_parameters.provider_parameters_values))

    def _to_c_parameters(self) -> _pypowsybl.SecurityAnalysisParameters:
        c_parameters = _pypowsybl.SecurityAnalysisParameters()
        c_parameters.loadflow_parameters = self.load_flow_parameters._to_c_parameters()  # pylint: disable=protected-access
        c_parameters.flow_proportional_threshold = self.increased_violations.flow_proportional_threshold
        c_parameters.low_voltage_proportional_threshold = self.increased_violations.low_voltage_proportional_threshold
        c_parameters.low_voltage_absolute_threshold = self.increased_violations.low_voltage_absolute_threshold
        c_parameters.high_voltage_proportional_threshold = self.increased_violations.high_voltage_proportional_threshold
        c_parameters.high_voltage_absolute_threshold = self.increased_violations.high_voltage_absolute_threshold
        c_parameters.provider_parameters_keys = list(self.provider_parameters.keys())
        c_parameters.provider_parameters_values = list(self.provider_parameters.values())
        return c_parameters

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f", load_flow_parameters={self.load_flow_parameters!r}" \
               f", increased_violations={self.increased_violations!r}" \
               f", provider_parameters={self.provider_parameters!r}" \
               f")"
