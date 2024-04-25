# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import warnings
from typing import Union

from pypowsybl.network import Network
from pypowsybl.report import ReportNode
from pypowsybl import _pypowsybl
from pypowsybl.loadflow import Parameters as lfParameters
from .parameters import Parameters
from .sensitivity import SensitivityAnalysis
from .dc_sensitivity_analysis_result import DcSensitivityAnalysisResult


class DcSensitivityAnalysis(SensitivityAnalysis):
    """ Represents a DC sensitivity analysis."""

    def __init__(self, handle: _pypowsybl.JavaHandle):
        SensitivityAnalysis.__init__(self, handle)

    def run(self, network: Network, parameters: Union[Parameters, lfParameters] = None,
            provider: str = '', reporter: ReportNode = None, report_node: ReportNode = None) -> DcSensitivityAnalysisResult:
        """ Runs the sensitivity analysis

        Args:
            network:    The network
            parameters: The sensitivity parameters
            provider:   Name of the sensitivity analysis provider
            reporter: deprecated, use report_node instead
            report_node:   the reporter to be used to create an execution report, default is None (no report)

        Returns:
            a sensitivity analysis result
        """
        if reporter is not None:
            warnings.warn("Use of deprecated attribute reporter. Use report_node instead.", DeprecationWarning)
            report_node = reporter
        sensitivity_parameters = Parameters(load_flow_parameters=parameters) if isinstance(parameters,
                                                                                           lfParameters) else parameters
        p: _pypowsybl.SensitivityAnalysisParameters = sensitivity_parameters._to_c_parameters() if sensitivity_parameters is not None else Parameters()._to_c_parameters()  # pylint: disable=protected-access
        return DcSensitivityAnalysisResult(
            _pypowsybl.run_sensitivity_analysis(self._handle, network._handle, True, p, provider,
                                                None if report_node is None else report_node._report_node), # pylint: disable=protected-access
            functions_ids=self.functions_ids, function_data_frame_index=self.function_data_frame_index)
