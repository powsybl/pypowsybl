# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from enum import IntEnum
from typing import Dict, Optional
from pypowsybl import _pypowsybl


class CalculationType(IntEnum):
    """ Type of margin calculation. """
    GLOBAL_MARGIN = 0
    LOCAL_MARGIN = 1


class LoadModelsRule(IntEnum):
    """ Rule defining which loads are modelled during the margin calculation. """
    ALL_LOADS = 0
    TARGETED_LOADS = 1


class MarginCalculationParameters:  # pylint: disable=too-many-instance-attributes
    """
    Parameters for a margin calculation execution.

    All parameters are first read from your configuration file, then overridden with
    the constructor arguments.

    .. currentmodule:: pypowsybl.dynamic

    Args:
        start_time: instant of time at which the simulation begins, in seconds
        stop_time: instant of time at which the simulation ends, in seconds
        margin_calculation_start_time: instant of time at which the margin calculation begins, in seconds
        load_increase_start_time: instant of time at which the load increase begins, in seconds
        load_increase_stop_time: instant of time at which the load increase ends, in seconds
        contingencies_start_time: instant of time at which the contingencies are applied, in seconds
        calculation_type: type of margin calculation (GLOBAL_MARGIN or LOCAL_MARGIN)
        accuracy: accuracy of the margin calculation, as a percentage of the load increase
        load_models_rule: rule defining which loads are modelled (ALL_LOADS or TARGETED_LOADS)
        debug_dir: directory where debug files are dumped, none by default
        provider_parameters: parameters linked to the Dynawo provider
    """

    def __init__(self, start_time: Optional[float] = None,
                 stop_time: Optional[float] = None,
                 margin_calculation_start_time: Optional[float] = None,
                 load_increase_start_time: Optional[float] = None,
                 load_increase_stop_time: Optional[float] = None,
                 contingencies_start_time: Optional[float] = None,
                 calculation_type: Optional[CalculationType] = None,
                 accuracy: Optional[int] = None,
                 load_models_rule: Optional[LoadModelsRule] = None,
                 debug_dir: Optional[str] = None,
                 provider_parameters: Optional[Dict[str, str]] = None):
        self._init_with_default_values()
        if start_time is not None:
            self.start_time = start_time
        if stop_time is not None:
            self.stop_time = stop_time
        if margin_calculation_start_time is not None:
            self.margin_calculation_start_time = margin_calculation_start_time
        if load_increase_start_time is not None:
            self.load_increase_start_time = load_increase_start_time
        if load_increase_stop_time is not None:
            self.load_increase_stop_time = load_increase_stop_time
        if contingencies_start_time is not None:
            self.contingencies_start_time = contingencies_start_time
        if calculation_type is not None:
            self.calculation_type = calculation_type
        if accuracy is not None:
            self.accuracy = accuracy
        if load_models_rule is not None:
            self.load_models_rule = load_models_rule
        if debug_dir is not None:
            self.debug_dir = debug_dir
        if provider_parameters is not None:
            self.provider_parameters = provider_parameters

    def _init_with_default_values(self) -> None:
        default_parameters = _pypowsybl.MarginCalculationParameters()
        self.start_time = default_parameters.start_time
        self.stop_time = default_parameters.stop_time
        self.margin_calculation_start_time = default_parameters.margin_calculation_start_time
        self.load_increase_start_time = default_parameters.load_increase_start_time
        self.load_increase_stop_time = default_parameters.load_increase_stop_time
        self.contingencies_start_time = default_parameters.contingencies_start_time
        self.calculation_type = CalculationType(default_parameters.calculation_type)
        self.accuracy = default_parameters.accuracy
        self.load_models_rule = LoadModelsRule(default_parameters.load_models_rule)
        self.debug_dir = default_parameters.debug_dir
        self.provider_parameters = dict(
            zip(default_parameters.provider_parameters_keys, default_parameters.provider_parameters_values))

    def _to_c_parameters(self) -> _pypowsybl.MarginCalculationParameters:
        c_parameters = _pypowsybl.MarginCalculationParameters()
        c_parameters.start_time = self.start_time
        c_parameters.stop_time = self.stop_time
        c_parameters.margin_calculation_start_time = self.margin_calculation_start_time
        c_parameters.load_increase_start_time = self.load_increase_start_time
        c_parameters.load_increase_stop_time = self.load_increase_stop_time
        c_parameters.contingencies_start_time = self.contingencies_start_time
        c_parameters.calculation_type = int(self.calculation_type)
        c_parameters.accuracy = self.accuracy
        c_parameters.load_models_rule = int(self.load_models_rule)
        c_parameters.debug_dir = self.debug_dir
        c_parameters.provider_parameters_keys = list(self.provider_parameters.keys())
        c_parameters.provider_parameters_values = list(self.provider_parameters.values())
        return c_parameters
