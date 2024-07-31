# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Dict

from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import ShortCircuitStudyType, InitialVoltageProfileMode

ShortCircuitStudyType.__module__ = __name__
ShortCircuitStudyType.__name__ = 'ShortCircuitStudyType'
InitialVoltageProfileMode.__module__ = __name__
InitialVoltageProfileMode.__name__ = 'InitialVoltageProfileMode'


class Parameters:  # pylint: disable=too-few-public-methods
    """
    Parameters for a short-circuit analysis execution.

    Please check the PowSyBl short-circuit APIs documentation, for detailed information.

    .. currentmodule:: pypowsybl.shortcircuit

    Args:
        with_fortescue_result: indicates whether the currents and voltages are to be given in three-phase magnitude or
            detailed with magnitude and angle on each phase. This parameter also applies to the feeder results and
            voltage results.
        with_feeder_result: indicates whether the contributions of each feeder to the short-circuit current at the fault
            node should be calculated.
        with_limit_violations: indicates whether limit violations should be returned after the calculation. If true, a
        list of buses where the calculated short-circuit current is higher than the maximum admissible current (stored in
        ip_max in the identifiableShortCircuit extension) or lower than the minimum admissible current (stored in ip_min
        in the identifiableShortCircuit extension).
        with_voltage_result: indicates whether the voltage profile should be calculated on every node of the network
        min_voltage_drop_proportional_threshold: specifies a threshold for filtering the voltage results.
            Only nodes where the voltage drop due to the short circuit is greater than this property are retained.
        study_type: specifies the type of short-circuit study. It can be SUB_TRANSIENT, TRANSIENT or STEADY_STATE.
        initial_voltage_profile_mode: specify how the computation is initialized. It can be NOMINAL, CONFIGURED or PREVIOUS_VALUE
    """

    def __init__(self,
                 with_feeder_result: bool = None,
                 with_limit_violations: bool = None,
                 with_voltage_result: bool = None,
                 min_voltage_drop_proportional_threshold: float = None,
                 study_type: ShortCircuitStudyType = None,
                 provider_parameters: Dict[str, str] = None,
                 with_fortescue_result: bool = None,
                 initial_voltage_profile_mode: InitialVoltageProfileMode = None):
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
        if with_fortescue_result is not None:
            self.with_fortescue_result = with_fortescue_result
        if initial_voltage_profile_mode is not None:
            self.initial_voltage_profile_mode = initial_voltage_profile_mode

    def _init_from_c(self, c_parameters: _pypowsybl.ShortCircuitAnalysisParameters) -> None:
        self.with_feeder_result = c_parameters.with_feeder_result
        self.with_limit_violations = c_parameters.with_limit_violations
        self.with_voltage_result = c_parameters.with_voltage_result
        self.min_voltage_drop_proportional_threshold = c_parameters.min_voltage_drop_proportional_threshold
        self.study_type = c_parameters.study_type
        self.provider_parameters = dict(
            zip(c_parameters.provider_parameters_keys, c_parameters.provider_parameters_values))
        self.with_fortescue_result = c_parameters.with_fortescue_result
        self.initial_voltage_profile_mode = c_parameters.initial_voltage_profile_mode

    def _init_with_default_values(self) -> None:
        self._init_from_c(_pypowsybl.ShortCircuitAnalysisParameters())
        self.with_feeder_result = False
        self.with_limit_violations = False
        self.with_voltage_result = False
        self.min_voltage_drop_proportional_threshold = 0
        self.study_type = ShortCircuitStudyType.TRANSIENT
        self.with_fortescue_result = False
        self.initial_voltage_profile_mode = InitialVoltageProfileMode.NOMINAL

    def _to_c_parameters(self) -> _pypowsybl.ShortCircuitAnalysisParameters:
        c_parameters = _pypowsybl.ShortCircuitAnalysisParameters()
        c_parameters.with_voltage_result = self.with_voltage_result
        c_parameters.with_feeder_result = self.with_feeder_result
        c_parameters.with_limit_violations = self.with_limit_violations
        c_parameters.study_type = self.study_type
        c_parameters.with_fortescue_result = self.with_fortescue_result
        c_parameters.min_voltage_drop_proportional_threshold = self.min_voltage_drop_proportional_threshold
        c_parameters.initial_voltage_profile_mode = self.initial_voltage_profile_mode
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
               f", with_fortescue_result={self.with_fortescue_result!r}" \
               f", initial_voltage_profile_mode={self.initial_voltage_profile_mode!r}" \
               f")"
