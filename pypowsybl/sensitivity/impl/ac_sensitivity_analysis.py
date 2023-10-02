# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import List, Union
from pypowsybl import _pypowsybl
from pypowsybl.network import Network
from pypowsybl.report import Reporter
from pypowsybl.loadflow import Parameters as LfParameters
from .ac_sensitivity_analysis_result import AcSensitivityAnalysisResult
from .sensitivity import SensitivityAnalysis
from .parameters import Parameters


class AcSensitivityAnalysis(SensitivityAnalysis):
    """ Represents an AC sensitivity analysis."""

    def __init__(self, handle: _pypowsybl.JavaHandle):
        SensitivityAnalysis.__init__(self, handle)
        self.bus_voltage_ids: List[str] = []
        self.target_voltage_ids: List[str] = []

    def set_bus_voltage_factor_matrix(self, bus_ids: List[str], target_voltage_ids: List[str]) -> None:
        """
        Defines buses voltage sensitivities to be computed.

        Args:
            bus_ids:            IDs of buses for which voltage sensitivities should be computed
            target_voltage_ids: IDs of regulating equipments to which we should compute sensitivities
        """
        _pypowsybl.set_bus_voltage_factor_matrix(self._handle, bus_ids, target_voltage_ids)
        self.bus_voltage_ids = bus_ids
        self.target_voltage_ids = target_voltage_ids

    def run(self, network: Network, parameters: Union[Parameters, LfParameters] = None,
            provider: str = '', reporter: Reporter = None) -> AcSensitivityAnalysisResult:
        """
        Runs the sensitivity analysis.

        Args:
            network:    The network
            parameters: The sensitivity parameters
            provider:   Name of the sensitivity analysis provider

        Returns:
            a sensitivity analysis result
        """
        sensitivity_parameters = Parameters(load_flow_parameters=parameters) if isinstance(parameters,
                                                                                           LfParameters) else parameters
        p: _pypowsybl.SensitivityAnalysisParameters = sensitivity_parameters._to_c_parameters() if sensitivity_parameters is not None else Parameters()._to_c_parameters()  # pylint: disable=W0212
        return AcSensitivityAnalysisResult(
            _pypowsybl.run_sensitivity_analysis(self._handle, network._handle, False, p, provider,
                                                None if reporter is None else reporter._reporter_model),
            # pylint: disable=protected-access
            branches_ids=self.branches_ids, branch_data_frame_index=self.branch_data_frame_index,
            bus_ids=self.bus_voltage_ids, target_voltage_ids=self.target_voltage_ids)
