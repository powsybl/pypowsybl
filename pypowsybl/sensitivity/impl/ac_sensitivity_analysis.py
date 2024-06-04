# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import warnings
from typing import List, Union
from pypowsybl import _pypowsybl
from pypowsybl.network import Network
from pypowsybl.report import ReportNode
from pypowsybl.loadflow import Parameters as LfParameters
from pypowsybl._pypowsybl import ContingencyContextType, SensitivityFunctionType, SensitivityVariableType
from .ac_sensitivity_analysis_result import AcSensitivityAnalysisResult
from .sensitivity_analysis_result import DEFAULT_MATRIX_ID
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
        .. deprecated:: 1.1.0
          Use :meth:`add_bus_voltage_factor_matrix` instead.

        Defines buses voltage sensitivities to be computed.

        Args:
            bus_ids:            IDs of buses for which voltage sensitivities should be computed
            target_voltage_ids: IDs of regulating equipments to which we should compute sensitivities
       """
        warnings.warn("set_bus_voltage_factor_matrix is deprecated, use add_bus_voltage_factor_matrix instead",
                      DeprecationWarning)
        self.add_bus_voltage_factor_matrix(bus_ids, target_voltage_ids)

    def add_bus_voltage_factor_matrix(self, bus_ids: List[str], target_voltage_ids: List[str], matrix_id: str = DEFAULT_MATRIX_ID) -> None:
        """
        Defines buses voltage sensitivities to be computed.

        Args:
            bus_ids:            IDs of buses for which voltage sensitivities should be computed
            target_voltage_ids: IDs of regulating equipments to which we should compute sensitivities
            matrix_id:          The matrix unique identifier, to be used to retrieve the sensibility value
        """
        self.add_factor_matrix(bus_ids, target_voltage_ids, [], ContingencyContextType.ALL,
                               SensitivityFunctionType.BUS_VOLTAGE, SensitivityVariableType.BUS_TARGET_VOLTAGE, matrix_id)
        self.bus_voltage_ids = bus_ids
        self.target_voltage_ids = target_voltage_ids

    def run(self, network: Network, parameters: Union[Parameters, LfParameters] = None,
            provider: str = '', reporter: ReportNode = None, report_node: ReportNode = None) -> AcSensitivityAnalysisResult:
        """
        Runs the sensitivity analysis.

        Args:
            network:     The network
            parameters:  The sensitivity parameters
            provider:    Name of the sensitivity analysis provider
            reporter:    deprecated, use report_node instead
            report_node: The reporter to be used to create an execution report, default is None (no report)

        Returns:
            a sensitivity analysis result
        """
        if reporter is not None:
            warnings.warn("Use of deprecated attribute reporter. Use report_node instead.", DeprecationWarning)
            report_node = reporter
        sensitivity_parameters = Parameters(load_flow_parameters=parameters) if isinstance(parameters,
                                                                                           LfParameters) else parameters
        p: _pypowsybl.SensitivityAnalysisParameters = sensitivity_parameters._to_c_parameters() if sensitivity_parameters is not None else Parameters()._to_c_parameters()  # pylint: disable=W0212
        return AcSensitivityAnalysisResult(
            _pypowsybl.run_sensitivity_analysis(self._handle, network._handle, False, p, provider, None if report_node is None else report_node._report_node), # pylint: disable=protected-access
            functions_ids=self.functions_ids, function_data_frame_index=self.function_data_frame_index)
