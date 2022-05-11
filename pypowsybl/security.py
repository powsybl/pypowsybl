#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# iicense, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
from typing import Union as _Union, Dict as _Dict, List as _List
import pandas as _pd
from prettytable import PrettyTable as _PrettyTable
import pypowsybl._pypowsybl as _pypowsybl
from pypowsybl._pypowsybl import ContingencyResult, LimitViolation, ContingencyContextType
from pypowsybl.network import Network as _Network
from pypowsybl.util import (
    ContingencyContainer as _ContingencyContainer,
    create_data_frame_from_series_array as _create_data_frame_from_series_array
)
from pypowsybl.loadflow import Parameters


def _contingency_result_repr(self: ContingencyResult) -> str:
    return f"{self.__class__.__name__}(" \
           f"contingency_id={self.contingency_id!r}" \
           f", status={self.status.name}" \
           f", limit_violations=[{len(self.limit_violations)}]" \
           f")"


ContingencyResult.__repr__ = _contingency_result_repr  # type: ignore


def _limit_violation_repr(self: LimitViolation) -> str:
    return f"{self.__class__.__name__}(" \
           f"subject_id={self.subject_id!r}" \
           f", subject_name={self.subject_name!r}" \
           f", limit_type={self.limit_type.name}" \
           f", limit={self.limit!r}" \
           f", limit_name={self.limit_name!r}" \
           f", acceptable_duration={self.acceptable_duration!r}" \
           f", limit_reduction={self.limit_reduction!r}" \
           f", value={self.value!r}" \
           f", side={self.side.name}" \
           f")"


LimitViolation.__repr__ = _limit_violation_repr  # type: ignore


class SecurityAnalysisResult:
    """
    The result of a security analysis.
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        self._handle = handle
        results = _pypowsybl.get_security_analysis_result(self._handle)
        self._post_contingency_results = {}
        for result in results:
            if result.contingency_id:
                self._post_contingency_results[result.contingency_id] = result
            else:
                self._pre_contingency_result = result
        self._limit_violations = _create_data_frame_from_series_array(_pypowsybl.get_limit_violations(self._handle))

    @property
    def pre_contingency_result(self) -> ContingencyResult:
        """
        Result for the pre-contingency state.
        """
        return self._pre_contingency_result

    @property
    def post_contingency_results(self) -> _Dict[str, ContingencyResult]:
        """
        Results for the contingencies, as a dictionary contingency ID -> result.
        """
        return self._post_contingency_results

    def find_post_contingency_result(self, contingency_id: str) -> ContingencyResult:
        """
        Result for the specified contingency.

        Returns:
            Result for the specified contingency.
        """
        result = self._post_contingency_results[contingency_id]
        if not result:
            raise KeyError(f'Contingency {contingency_id} not found')
        return result

    def get_table(self) -> _PrettyTable:
        table = _PrettyTable()
        table.field_names = ["Contingency ID", "Status", "Equipment ID", "Equipment name", "Limit type", "Limit",
                             "Limit name", "Acceptable duration", "Limit reduction", "Value", "Side"]
        for contingency_id, post_contingency_result in self._post_contingency_results.items():
            table.add_row([contingency_id, post_contingency_result.status.name, '', '', '', '', '', '', '', '', ''])
            for limit_violation in post_contingency_result.limit_violations:
                table.add_row(['', '',
                               limit_violation.subject_id,
                               limit_violation.subject_name,
                               limit_violation.limit_type.name,
                               f'{limit_violation.limit:.1f}',
                               limit_violation.limit_name,
                               limit_violation.acceptable_duration,
                               limit_violation.limit_reduction,
                               f'{limit_violation.value:.1f}',
                               limit_violation.side.name])
        return table

    @property
    def limit_violations(self) -> _pd.DataFrame:
        """
        All limit violations in a dataframe representation.
        """
        return self._limit_violations

    @property
    def branch_results(self) -> _pd.DataFrame:
        """
        Results (P, Q, I) for monitored branches.
        """
        return _create_data_frame_from_series_array(_pypowsybl.get_branch_results(self._handle))

    @property
    def bus_results(self) -> _pd.DataFrame:
        """
        Bus results (voltage angle and magnitude) for monitored voltage levels.
        """
        return _create_data_frame_from_series_array(_pypowsybl.get_bus_results(self._handle))

    @property
    def three_windings_transformer_results(self) -> _pd.DataFrame:
        """
        Results (P, Q, I) for monitored three winding transformers.
        """
        return _create_data_frame_from_series_array(_pypowsybl.get_three_windings_transformer_results(self._handle))


class SecurityAnalysis(_ContingencyContainer):
    """
    Allows to run a security analysis on a network.
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        _ContingencyContainer.__init__(self, handle)

    def run_ac(self, network: _Network, parameters: Parameters = None,
               provider: str = '') -> SecurityAnalysisResult:
        """ Runs an AC security analysis.

        Args:
            network:    Network on which the security analysis will be computed
            parameters: Security analysis parameters
            provider:   Name of the security analysis implementation provider to be used,
                        will use default provider if empty.

        Returns:
            A security analysis result, containing information about violations and monitored elements
        """
        p = parameters._to_c_parameters() if parameters is not None else _pypowsybl.LoadFlowParameters()
        return SecurityAnalysisResult(
            _pypowsybl.run_security_analysis(self._handle, network._handle, p, provider, False))

    def run_dc(self, network: _Network, parameters: Parameters = None,
               provider: str = '') -> SecurityAnalysisResult:
        """ Runs an DC security analysis.

        Args:
            network:    Network on which the security analysis will be computed
            parameters: Security analysis parameters
            provider:   Name of the security analysis implementation provider to be used,
                        will use default provider if empty.

        Returns:
            A security analysis result, containing information about violations and monitored elements
        """
        p = parameters._to_c_parameters() if parameters is not None else _pypowsybl.LoadFlowParameters()
        return SecurityAnalysisResult(
            _pypowsybl.run_security_analysis(self._handle, network._handle, p, provider, True))

    def add_monitored_elements(self, contingency_context_type: ContingencyContextType = ContingencyContextType.ALL,
                               contingency_ids: _Union[_List[str], str] = None,
                               branch_ids: _List[str] = None,
                               voltage_level_ids: _List[str] = None,
                               three_windings_transformer_ids: _List[str] = None) -> None:
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
                                              branch_ids: _List[str] = None,
                                              voltage_level_ids: _List[str] = None,
                                              three_windings_transformer_ids: _List[str] = None) -> None:
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

    def add_postcontingency_monitored_elements(self, contingency_ids: _Union[_List[str], str],
                                               branch_ids: _List[str] = None,
                                               voltage_level_ids: _List[str] = None,
                                               three_windings_transformer_ids: _List[str] = None) -> None:
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


def create_analysis() -> SecurityAnalysis:
    """ Creates a security analysis objet, which can be used to run a security analysis on a network

    Examples:
        .. code-block::

            >>> analysis = pypowsybl.security.create_analysis()
            >>> analysis.add_single_element_contingencies(['line 1', 'line 2'])
            >>> res = analysis.run_ac(network)

    Returns:
        A security analysis object, which allows to run a security analysis on a network.
    """
    return SecurityAnalysis(_pypowsybl.create_security_analysis())

def set_default_provider(provider: str) -> None:
    """
    Set the default security analysis provider

    Args:
        provider: name of the default security analysis provider to set
    """
    _pypowsybl.set_default_security_analysis_provider(provider)

def get_default_provider() -> str:
    """
    Get the current default security analysis provider. if nothing is set it is OpenSecurityAnalysis

    Returns:
        the name of the current default security analysis provider
    """
    return _pypowsybl.get_default_security_analysis_provider()

def get_provider_names() -> _List[str]:
    """
    Get list of supported provider names

    Returns:
        the list of supported provider names
    """
    return _pypowsybl.get_security_analysis_provider_names()
