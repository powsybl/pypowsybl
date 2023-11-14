# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import Union, List
import pypowsybl.loadflow
from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import ContingencyContextType, ConditionType, ViolationType
from pypowsybl._pypowsybl import PostContingencyComputationStatus as ComputationStatus
from pypowsybl.network import Network
from pypowsybl.report import Reporter
from .parameters import Parameters
from .security_analysis_result import SecurityAnalysisResult
from .contingency_container import ContingencyContainer

ComputationStatus.__name__ = 'ComputationStatus'
ComputationStatus.__module__ = __name__


class SecurityAnalysis(ContingencyContainer):
    """
    Allows to run a sensitivity analysis on a network.
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        ContingencyContainer.__init__(self, handle)

    def run_ac(self, network: Network, parameters: Union[Parameters, pypowsybl.loadflow.Parameters] = None,
               provider: str = '', reporter: Reporter = None) -> SecurityAnalysisResult:
        """ Runs an AC sensitivity analysis.

        Args:
            network:    Network on which the sensitivity analysis will be computed
            parameters: Security analysis parameters
            provider:   Name of the sensitivity analysis implementation provider to be used,
                        will use default provider if empty.

        Returns:
            A sensitivity analysis result, containing information about violations and monitored elements
        """
        security_parameters = Parameters(load_flow_parameters=parameters) if isinstance(parameters,
                                                                                        pypowsybl.loadflow.Parameters) else parameters
        p = security_parameters._to_c_parameters() if security_parameters is not None else Parameters()._to_c_parameters()  # pylint: disable=protected-access
        return SecurityAnalysisResult(
            _pypowsybl.run_security_analysis(self._handle, network._handle, p, provider, False,
                                             None if reporter is None else reporter._reporter_model))  # pylint: disable=protected-access

    def run_dc(self, network: Network, parameters: Union[Parameters, pypowsybl.loadflow.Parameters] = None,
               provider: str = '', reporter: Reporter = None) -> SecurityAnalysisResult:
        """ Runs an DC sensitivity analysis.

        Args:
            network:    Network on which the sensitivity analysis will be computed
            parameters: Security analysis parameters
            provider:   Name of the sensitivity analysis implementation provider to be used,
                        will use default provider if empty.

        Returns:
            A sensitivity analysis result, containing information about violations and monitored elements
        """
        security_parameters = Parameters(load_flow_parameters=parameters) if isinstance(parameters,
                                                                                        pypowsybl.loadflow.Parameters) else parameters
        p = security_parameters._to_c_parameters() if security_parameters is not None else Parameters()._to_c_parameters()  # pylint: disable=protected-access
        return SecurityAnalysisResult(
            _pypowsybl.run_security_analysis(self._handle, network._handle, p, provider, True,
                                             # pylint: disable=protected-access
                                             None if reporter is None else reporter._reporter_model))  # pylint: disable=protected-access

    def add_monitored_elements(self, contingency_context_type: ContingencyContextType = ContingencyContextType.ALL,
                               contingency_ids: Union[List[str], str] = None,
                               branch_ids: List[str] = None,
                               voltage_level_ids: List[str] = None,
                               three_windings_transformer_ids: List[str] = None) -> None:
        """ Add elements to be monitored by the sensitivity analysis. The sensitivity analysis result
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

        _pypowsybl.add_monitored_elements(self._handle, contingency_context_type, branch_ids, voltage_level_ids,
                                          three_windings_transformer_ids, contingency_ids)

    def add_precontingency_monitored_elements(self,
                                              branch_ids: List[str] = None,
                                              voltage_level_ids: List[str] = None,
                                              three_windings_transformer_ids: List[str] = None) -> None:
        """ Add elements to be monitored by the sensitivity analysis on precontingency state. The sensitivity analysis result
        will provide additional information for those elements, like the power and current values.

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
                                               branch_ids: List[str] = None,
                                               voltage_level_ids: List[str] = None,
                                               three_windings_transformer_ids: List[str] = None) -> None:
        """ Add elements to be monitored by the sensitivity analysis for specific contingencies.
        The sensitivity analysis result will provide additional information for those elements, like the power and current values.

        Args:
            contingency_ids: list of contingencies for which we want to monitor additional elements
            branch_ids: list of branches to be monitored
            voltage_level_ids: list of voltage levels to be monitored
            three_windings_transformer_ids: list of 3 winding transformers to be monitored
        """
        return self.add_monitored_elements(ContingencyContextType.SPECIFIC, contingency_ids,
                                           branch_ids, voltage_level_ids, three_windings_transformer_ids)

    def add_load_active_power_action(self, action_id: str, load_id: str, is_relative: bool, active_power: float) -> None:
        """
        """
        _pypowsybl.add_load_active_power_action(self._handle, action_id, load_id, is_relative, active_power)

    def add_load_reactive_power_action(self, action_id: str, load_id: str, is_relative: bool, reactive_power: float) -> None:
        """
        """
        _pypowsybl.add_load_reactive_power_action(self._handle, action_id, load_id, is_relative, reactive_power)

    def add_generator_active_power_action(self, action_id: str, generator_id: str, is_relative: bool, active_power: float) -> None:
        """
        """
        _pypowsybl.add_generator_active_power_action(self._handle, action_id, generator_id, is_relative, active_power)

    def add_switch_action(self, action_id: str, switch_id: str, open: bool) -> None:
        """
        """
        _pypowsybl.add_switch_action(self._handle, action_id, switch_id, open)

    def add_operator_strategy(self, operator_strategy_id: str, contingency_id: str, action_ids: List[str],
                              condition_type: ConditionType = ConditionType.TRUE_CONDITION, violation_subject_ids: List[str] = None,
                              violation_types: List[ViolationType] = None) -> None:
        """
        """
        if violation_types is None:
            violation_types = []
        if violation_subject_ids is None:
            violation_subject_ids = []
        _pypowsybl.add_operator_strategy(self._handle, operator_strategy_id, contingency_id, action_ids, condition_type, violation_subject_ids, violation_types)
