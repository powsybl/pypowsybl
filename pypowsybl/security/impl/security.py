# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import warnings
from typing import Union, List
import pypowsybl.loadflow
from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import ContingencyContextType, ConditionType, ViolationType, Side
from pypowsybl._pypowsybl import PostContingencyComputationStatus as ComputationStatus
from pypowsybl.network import Network
from pypowsybl.report import ReportNode
from .parameters import Parameters
from .security_analysis_result import SecurityAnalysisResult
from .contingency_container import ContingencyContainer

ComputationStatus.__name__ = 'ComputationStatus'
ComputationStatus.__module__ = __name__


class SecurityAnalysis(ContingencyContainer):
    """
    Allows to run a security analysis on a network.
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        ContingencyContainer.__init__(self, handle)

    def run_ac(self, network: Network, parameters: Union[Parameters, pypowsybl.loadflow.Parameters] = None,
               provider: str = '', reporter: ReportNode = None, report_node: ReportNode = None) -> SecurityAnalysisResult:
        """ Runs an AC security analysis.

        Args:
            network:    Network on which the security analysis will be computed
            parameters: Security analysis parameters
            provider:   Name of the security analysis implementation provider to be used,
                        will use default provider if empty.
            reporter: deprecated, use report_node instead
            report_node:   the reporter to be used to create an execution report, default is None (no report)

        Returns:
            A security analysis result, containing information about violations and monitored elements
        """
        if reporter is not None:
            warnings.warn("Use of deprecated attribute reporter. Use report_node instead.", DeprecationWarning)
            report_node = reporter
        security_parameters = Parameters(load_flow_parameters=parameters) if isinstance(parameters,
                                                                                        pypowsybl.loadflow.Parameters) else parameters
        p = security_parameters._to_c_parameters() if security_parameters is not None else Parameters()._to_c_parameters()  # pylint: disable=protected-access
        return SecurityAnalysisResult(
            _pypowsybl.run_security_analysis(self._handle, network._handle, p, provider, False,
                                             None if report_node is None else report_node._report_node))  # pylint: disable=protected-access

    def run_dc(self, network: Network, parameters: Union[Parameters, pypowsybl.loadflow.Parameters] = None,
               provider: str = '', reporter: ReportNode = None, report_node: ReportNode = None) -> SecurityAnalysisResult:
        """ Runs a DC security analysis.

        Args:
            network:    Network on which the security analysis will be computed
            parameters: Security analysis parameters
            provider:   Name of the security analysis implementation provider to be used,
                        will use default provider if empty.
            reporter: deprecated, use report_node instead
            report_node:   the reporter to be used to create an execution report, default is None (no report)

        Returns:
            A security analysis result, containing information about violations and monitored elements
        """
        if reporter is not None:
            warnings.warn("Use of deprecated attribute reporter. Use report_node instead.", DeprecationWarning)
            report_node = reporter
        security_parameters = Parameters(load_flow_parameters=parameters) if isinstance(parameters,
                                                                                        pypowsybl.loadflow.Parameters) else parameters
        p = security_parameters._to_c_parameters() if security_parameters is not None else Parameters()._to_c_parameters()  # pylint: disable=protected-access
        return SecurityAnalysisResult(
            _pypowsybl.run_security_analysis(self._handle, network._handle, p, provider, True,
                                             # pylint: disable=protected-access
                                             None if report_node is None else report_node._report_node))  # pylint: disable=protected-access

    def add_monitored_elements(self, contingency_context_type: ContingencyContextType = ContingencyContextType.ALL,
                               contingency_ids: Union[List[str], str] = None,
                               branch_ids: List[str] = None,
                               voltage_level_ids: List[str] = None,
                               three_windings_transformer_ids: List[str] = None) -> None:
        """ Add elements to be monitored by the security analysis. The security analysis result
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
        """ Add elements to be monitored by the security analysis on precontingency state. The security analysis result
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
        """ Add elements to be monitored by the security analysis for specific contingencies.
        The security analysis result will provide additional information for those elements, like the power and current values.

        Args:
            contingency_ids: list of contingencies for which we want to monitor additional elements
            branch_ids: list of branches to be monitored
            voltage_level_ids: list of voltage levels to be monitored
            three_windings_transformer_ids: list of 3 winding transformers to be monitored
        """
        return self.add_monitored_elements(ContingencyContextType.SPECIFIC, contingency_ids,
                                           branch_ids, voltage_level_ids, three_windings_transformer_ids)

    def add_load_active_power_action(self, action_id: str, load_id: str, is_relative: bool, active_power: float) -> None:
        """ Add a load action, modifying the load active power

        Args:
            action_id: unique ID for the action
            load_id: load identifier
            is_relative: whether the active power change specified is absolute, or relative to current load active power
            active_power: the active power change

        """
        _pypowsybl.add_load_active_power_action(self._handle, action_id, load_id, is_relative, active_power)

    def add_load_reactive_power_action(self, action_id: str, load_id: str, is_relative: bool, reactive_power: float) -> None:
        """ Add a load action, modifying the load reactive power

        Args:
            action_id: unique ID for the action
            load_id: load identifier
            is_relative: whether the reactive power change specified is absolute, or relative to current load reactive power
            reactive_power: the reactive power change

        """
        _pypowsybl.add_load_reactive_power_action(self._handle, action_id, load_id, is_relative, reactive_power)

    def add_generator_active_power_action(self, action_id: str, generator_id: str, is_relative: bool, active_power: float) -> None:
        """ Add a generator action, modifying the generator active power

        Args:
            action_id: unique ID for the action
            generator_id: generator identifier
            is_relative: whether the active power change specified is absolute, or relative to current generator active power
            active_power: the active power change

        """
        _pypowsybl.add_generator_active_power_action(self._handle, action_id, generator_id, is_relative, active_power)

    def add_switch_action(self, action_id: str, switch_id: str, open: bool) -> None:
        """ Add a switch action, modifying the switch open/close status

        Args:
            action_id: unique ID for the action
            switch_id: switch identifier
            open: True to open the switch, False to close

        """
        _pypowsybl.add_switch_action(self._handle, action_id, switch_id, open)

    def add_phase_tap_changer_position_action(self, action_id: str, transformer_id: str, is_relative: bool, tap_position: int, side: Side = Side.NONE) -> None:
        """ Add a phase tap changer tap position action, modifying the tap position of the tap changer

        Args:
            action_id: unique ID for the action
            transformer_id: transformer identifier
            is_relative: True means the provided tap_position will be added to the current tap position, False means the provided tap_position will replace the previous one.
            tap_position: The tap position (either a delta if is_relative is true, or the final value if is_relative if false)
            side: Side of the tap changer (for three windings transformers)
        """
        _pypowsybl.add_phase_tap_changer_position_action(self._handle, action_id, transformer_id, is_relative, tap_position, side)

    def add_ratio_tap_changer_position_action(self, action_id: str, transformer_id: str, is_relative: bool, tap_position: int, side: Side = Side.NONE) -> None:
        """ Add a ratio tap changer tap position action, modifying the tap position of the tap changer

        Args:
            action_id: unique ID for the action
            transformer_id: transformer identifier
            is_relative: True means the provided tap_position will be added to the current tap position, False means the provide tap_position will replace the previous one.
            tap_position: The tap position (either a delta if is_relative is true, or the final value if is_relative if false)
            side: Side of the tap changer (for three windings transformers)
        """
        _pypowsybl.add_ratio_tap_changer_position_action(self._handle, action_id, transformer_id, is_relative, tap_position, side)

    def add_shunt_compensator_position_action(self, action_id: str, shunt_id: str, section: int) -> None:
        """ Add a shunt compensator section action, modifying the section of the shunt compensator

        Args:
            action_id: unique ID for the action
            shunt_id: transformer identifier
            section: The new section of the shunt compensator
        """
        _pypowsybl.add_shunt_compensator_position_action(self._handle, action_id, shunt_id, section)

    def add_terminals_connection_action(self, action_id: str, element_id: str, side: Side = Side.NONE, opening: bool = True) -> None:
        """ Add a terminals connection action, connecting/disconnecting one or multiple sides of a network element

        Args:
            action_id: unique ID for the action
            element_id: network element identifier
            side: The side of the element to modify (all if side=None)
            opening: True to open the terminals, False otherwise
        """
        _pypowsybl.add_terminals_connection_action(self._handle, action_id, element_id, side, opening)


    def add_operator_strategy(self, operator_strategy_id: str, contingency_id: str, action_ids: List[str],
                              condition_type: ConditionType = ConditionType.TRUE_CONDITION, violation_subject_ids: List[str] = None,
                              violation_types: List[ViolationType] = None) -> None:
        """ Add an operator strategy to the specified contingency

        Args:
            operator_strategy_id: unique ID for the operator strategy
            contingency_id: the contingency on which the operator strategy applies
            action_ids: the list of actions to be applied as part of the strategy
            condition_type: the type of condition
            violation_subject_ids: identifiers of network elements monitored to apply the operator strategy
            violation_types: type of violations to consider to apply the operator strategy
        """
        if violation_types is None:
            violation_types = []
        if violation_subject_ids is None:
            violation_subject_ids = []
        _pypowsybl.add_operator_strategy(self._handle, operator_strategy_id, contingency_id, action_ids, condition_type, violation_subject_ids, violation_types)
