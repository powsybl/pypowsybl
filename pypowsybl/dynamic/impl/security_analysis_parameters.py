# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Dict, Optional
from pypowsybl import _pypowsybl


class DynamicSecurityAnalysisParameters:  # pylint: disable=too-few-public-methods
    """
    Parameters for a dynamic security analysis execution.

    All parameters are first read from your configuration file, then overridden with
    the constructor arguments.

    .. currentmodule:: pypowsybl.dynamic

    Args:
        start_time: instant of time at which the simulation begins, in seconds
        stop_time: instant of time at which the simulation ends, in seconds
        contingencies_start_time: instant of time at which the contingencies are applied, in seconds
        debug_dir: directory where debug files are dumped, none by default
        provider_parameters: parameters linked to the Dynawo provider
    """

    def __init__(self, start_time: Optional[float] = None,
                 stop_time: Optional[float] = None,
                 contingencies_start_time: Optional[float] = None,
                 debug_dir: Optional[str] = None,
                 provider_parameters: Optional[Dict[str, str]] = None):
        self._init_with_default_values()
        if start_time is not None:
            self.start_time = start_time
        if stop_time is not None:
            self.stop_time = stop_time
        if contingencies_start_time is not None:
            self.contingencies_start_time = contingencies_start_time
        if debug_dir is not None:
            self.debug_dir = debug_dir
        if provider_parameters is not None:
            self.provider_parameters = provider_parameters

    def _init_with_default_values(self) -> None:
        default_parameters = _pypowsybl.DynamicSecurityAnalysisParameters()
        self.start_time = default_parameters.start_time
        self.stop_time = default_parameters.stop_time
        self.contingencies_start_time = default_parameters.contingencies_start_time
        self.debug_dir = default_parameters.debug_dir
        self.provider_parameters = dict(
            zip(default_parameters.provider_parameters_keys, default_parameters.provider_parameters_values))

    def _to_c_parameters(self) -> _pypowsybl.DynamicSecurityAnalysisParameters:
        c_parameters = _pypowsybl.DynamicSecurityAnalysisParameters()
        c_parameters.start_time = self.start_time
        c_parameters.stop_time = self.stop_time
        c_parameters.contingencies_start_time = self.contingencies_start_time
        c_parameters.debug_dir = self.debug_dir
        c_parameters.provider_parameters_keys = list(self.provider_parameters.keys())
        c_parameters.provider_parameters_values = list(self.provider_parameters.values())
        return c_parameters
