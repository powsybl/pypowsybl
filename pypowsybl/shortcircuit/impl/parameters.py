# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Dict

from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import ShortCircuitFaultType, ShortCircuitStudyType

ShortCircuitStudyType.__module__ = __name__
ShortCircuitStudyType.__name__ = 'ShortCircuitStudyType'


class Parameters:  # pylint: disable=too-few-public-methods
    """
    Parameters for a short-circuit analysis execution.

    Please check the Powsybl's short-circuit APIs documentation, for detailed information.

    .. currentmodule:: pypowsybl.shortcircuit

    Args:
        with_feeder_result: indicates if the contributions of each feeder to the short circuit current at the fault node should be computed
        with_limit_violations: indicates whether limit violations should be returned after the computation
        with_voltage_result: indicates if the voltage profile should be computed on every node of the network
        min_voltage_drop_proportional_threshold: indicates a threshold to filter the voltage results. Only nodes where the voltage drop due to the short circuit is greater than this property are retained
        study_type: indicates the type of short circuit study. It can be SUB_TRANSIENT, TRANSIENT or STEADY_STATE
    """

    def __init__(self,
                 with_feeder_result: bool = None,
                 with_limit_violations: bool = None,
                 with_voltage_result: bool = None,
                 min_voltage_drop_proportional_threshold: float = None,
                 study_type: ShortCircuitStudyType = None,
                 provider_parameters: Dict[str, str] = None):
        self._init_with_default_values()
        if with_feeder_result is not None:
            self.with_feeder_result = with_feeder_result
        if with_limit_violations is not None:
            self.with_limit_violations = with_limit_violations
        if with_voltage_result is not None:
            self.with_voltage_result = with_voltage_result
        if min_voltage_drop_proportional_threshold is not None:
            self.min_voltage_drop_proportional_threshold = min_voltage_drop_proportional_threshold
        if study_type is not None:
            self.study_type = study_type
        if provider_parameters is not None:
            self.provider_parameters = provider_parameters

    def _init_from_c(self, c_parameters: _pypowsybl.ShortCircuitAnalysisParameters) -> None:
        self.with_feeder_result = c_parameters.with_feeder_result
        self.with_limit_violations = c_parameters.with_limit_violations
        self.with_voltage_result = c_parameters.with_voltage_result
        self.min_voltage_drop_proportional_threshold = c_parameters.min_voltage_drop_proportional_threshold
        self.study_type = c_parameters.study_type
        self.provider_parameters = dict(
            zip(c_parameters.provider_parameters_keys, c_parameters.provider_parameters_values))

    def _init_with_default_values(self) -> None:
        self._init_from_c(_pypowsybl.ShortCircuitAnalysisParameters())
        self.with_feeder_result = False
        self.with_limit_violations = False
        self.with_voltage_result = False
        self.min_voltage_drop_proportional_threshold = 0
        self.study_type = ShortCircuitStudyType.TRANSIENT

    def _to_c_parameters(self) -> _pypowsybl.ShortCircuitAnalysisParameters:
        c_parameters = _pypowsybl.ShortCircuitAnalysisParameters()
        c_parameters.with_voltage_result = self.with_voltage_result
        c_parameters.with_feeder_result = self.with_feeder_result
        c_parameters.with_limit_violations = self.with_limit_violations
        c_parameters.study_type = self.study_type
        c_parameters.with_fortescue_result = False
        c_parameters.min_voltage_drop_proportional_threshold = self.min_voltage_drop_proportional_threshold
        c_parameters.provider_parameters_keys = []
        c_parameters.provider_parameters_values = []
        return c_parameters

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"with_feeder_result={self.with_feeder_result!r}" \
               f", with_limit_violations={self.with_limit_violations!r}" \
               f", with_voltage_result={self.with_voltage_result!r}" \
               f", min_voltage_drop_proportional_threshold={self.min_voltage_drop_proportional_threshold!r}" \
               f", study_type={self.study_type!r}" \
               f")"
