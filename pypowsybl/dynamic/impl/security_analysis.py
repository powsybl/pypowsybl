# Copyright (c) 2026, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import List, Optional, Union

from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import ContingencyContextType
from pypowsybl.network import Network
from pypowsybl.report import ReportNode
from pypowsybl.security import ContingencyContainer, SecurityAnalysisResult
from .model_mapping import ModelMapping
from .event_mapping import EventMapping
from .security_analysis_parameters import DynamicSecurityAnalysisParameters


class DynamicSecurityAnalysis(ContingencyContainer):
    """
    Allows to run a dynamic security analysis on a network, using the Dynawo provider.

    A dynamic security analysis applies the given dynamic models (and optional events) to
    the network and checks security violations on the pre-contingency state and on each
    post-contingency state, using time-domain simulation.
    """

    def __init__(self) -> None:
        ContingencyContainer.__init__(self, _pypowsybl.create_dynamic_security_analysis())

    def run(self,
            network: Network,
            model_mapping: ModelMapping,
            event_mapping: Optional[EventMapping] = None,
            parameters: Optional[DynamicSecurityAnalysisParameters] = None,
            provider: str = '',
            report_node: Optional[ReportNode] = None) -> SecurityAnalysisResult:
        """ Runs a dynamic security analysis.

        Args:
            network:       Network on which the dynamic security analysis will be computed
            model_mapping: Mapping of the network equipments to their dynamic models
            event_mapping: Mapping of the events applied during the simulation
            parameters:    Dynamic security analysis parameters
            provider:      Name of the dynamic security analysis implementation provider to be used,
                           will use default provider if empty.
            report_node:   The reporter to be used to create an execution report, default is None (no report)

        Returns:
            A security analysis result, containing information about violations and monitored elements
        """
        return SecurityAnalysisResult(
            _pypowsybl.run_dynamic_security_analysis(
                self._handle,
                network._handle,  # pylint: disable=protected-access
                model_mapping._handle,  # pylint: disable=protected-access
                None if event_mapping is None else event_mapping._handle,  # pylint: disable=protected-access
                parameters._to_c_parameters() if parameters is not None else _pypowsybl.DynamicSecurityAnalysisParameters(),  # pylint: disable=protected-access
                provider,
                None if report_node is None else report_node._report_node))  # pylint: disable=protected-access

    def add_monitored_elements(self, contingency_context_type: ContingencyContextType = ContingencyContextType.ALL,
                               contingency_ids: Optional[Union[List[str], str]] = None,
                               branch_ids: Optional[List[str]] = None,
                               voltage_level_ids: Optional[List[str]] = None,
                               three_windings_transformer_ids: Optional[List[str]] = None) -> None:
        """ Add elements to be monitored by the dynamic security analysis. The result
        will provide additional information for those elements, like the power and current values.

        Args:
            contingency_context_type: Defines if the elements should be monitored for all state, only N situation
                                      or only specific contingencies
            contingency_ids: list of contingencies for which we want to monitor additional elements
            branch_ids: list of branches to be monitored
            voltage_level_ids: list of voltage levels to be monitored
            three_windings_transformer_ids: list of 3 winding transformers to be monitored
        """
        if contingency_context_type in (ContingencyContextType.ALL, ContingencyContextType.NONE) and contingency_ids:
            raise ValueError('Contingencies list must be empty when defining monitored elements '
                             'for NONE or ALL contingencies')

        if three_windings_transformer_ids is None:
            three_windings_transformer_ids = []
        if branch_ids is None:
            branch_ids = []
        if voltage_level_ids is None:
            voltage_level_ids = []
        if contingency_ids is None:
            contingency_ids = ['']
        elif isinstance(contingency_ids, str):
            contingency_ids = [contingency_ids]

        _pypowsybl.add_dynamic_monitored_elements(self._handle, contingency_context_type, branch_ids, voltage_level_ids,
                                                  three_windings_transformer_ids, contingency_ids)

    def add_precontingency_monitored_elements(self,
                                              branch_ids: Optional[List[str]] = None,
                                              voltage_level_ids: Optional[List[str]] = None,
                                              three_windings_transformer_ids: Optional[List[str]] = None) -> None:
        """ Add elements to be monitored by the dynamic security analysis on precontingency state.

        Args:
            branch_ids: list of branches to be monitored
            voltage_level_ids: list of voltage levels to be monitored
            three_windings_transformer_ids: list of 3 winding transformers to be monitored
        """
        return self.add_monitored_elements(ContingencyContextType.NONE,
                                           branch_ids=branch_ids,
                                           voltage_level_ids=voltage_level_ids,
                                           three_windings_transformer_ids=three_windings_transformer_ids)

    def add_postcontingency_monitored_elements(self, contingency_ids: Union[List[str], str],
                                               branch_ids: Optional[List[str]] = None,
                                               voltage_level_ids: Optional[List[str]] = None,
                                               three_windings_transformer_ids: Optional[List[str]] = None) -> None:
        """ Add elements to be monitored by the dynamic security analysis for specific contingencies.

        Args:
            contingency_ids: list of contingencies for which we want to monitor additional elements
            branch_ids: list of branches to be monitored
            voltage_level_ids: list of voltage levels to be monitored
            three_windings_transformer_ids: list of 3 winding transformers to be monitored
        """
        return self.add_monitored_elements(ContingencyContextType.SPECIFIC, contingency_ids,
                                           branch_ids, voltage_level_ids, three_windings_transformer_ids)
