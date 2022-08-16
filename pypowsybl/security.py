#
# Copyright (c) 2020, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# iicense, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
from typing import Union as _Union, Dict as _Dict, List as _List
import pandas as _pd
import pypowsybl.loadflow
from prettytable import PrettyTable as _PrettyTable
from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import ContingencyResult, LimitViolation, ContingencyContextType
from pypowsybl.network import Network as _Network
from pypowsybl.util import (
    ContingencyContainer as _ContingencyContainer,
    create_data_frame_from_series_array as _create_data_frame_from_series_array
)
from pypowsybl.report import Reporter as _Reporter


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


class IncreasedViolationsParameters:
    """
    Parameters which define what violations should be considered as "increased" between N and post-contingency situations

    Args:
        flow_proportional_threshold: for current and flow violations, if equal to 0.1, the violations which value
                                     have increased of more than 10% between N and post-contingency are considered "increased"
        low_voltage_proportional_threshold: for low voltage violations, if equal to 0.1, the violations which value
                                            have reduced of more than 10% between N and post-contingency are considered "increased"
        low_voltage_absolute_threshold: for low voltage violations, if equal to 1, the violations which value
                                        have reduced of more than 1 kV between N and post-contingency are considered "increased"
        high_voltage_proportional_threshold: for high voltage violations, if equal to 0.1, the violations which value
                                             have increased of more than 10% between N and post-contingency are considered "increased"
        high_voltage_absolute_threshold: for high voltage violations, if equal to 1, the violations which value
                                         have increased of more than 1 kV between N and post-contingency are considered "increased"
    """

    def __init__(self, flow_proportional_threshold: float, low_voltage_proportional_threshold: float,
                 low_voltage_absolute_threshold: float, high_voltage_proportional_threshold: float,
                 high_voltage_absolute_threshold: float):
        self.flow_proportional_threshold = flow_proportional_threshold
        self.low_voltage_proportional_threshold = low_voltage_proportional_threshold
        self.low_voltage_absolute_threshold = low_voltage_absolute_threshold
        self.high_voltage_proportional_threshold = high_voltage_proportional_threshold
        self.high_voltage_absolute_threshold = high_voltage_absolute_threshold

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f", flow_proportional_threshold={self.flow_proportional_threshold!r}" \
               f", low_voltage_proportional_threshold={self.low_voltage_proportional_threshold!r}" \
               f", low_voltage_absolute_threshold={self.low_voltage_absolute_threshold!r}" \
               f", high_voltage_proportional_threshold={self.high_voltage_proportional_threshold!r}" \
               f", high_voltage_absolute_threshold={self.high_voltage_absolute_threshold!r}" \
               f")"


class Parameters:  # pylint: disable=too-few-public-methods
    """
    Parameters for a security analysis execution.

    All parameters are first read from you configuration file, then overridden with
    the constructor arguments.

    Please note that security analysis providers may not honor all parameters, according to their capabilities.
    For example, some providers will not be able to simulate the voltage control of shunt compensators, etc.
    The exact behaviour of some parameters may also depend on your security analysis provider.
    Please check the documentation of your provider for that information.

    .. currentmodule:: pypowsybl.security

    Args:
        load_flow_parameters: parameters that are common to loadflow and security analysis
        increased_violations_parameters: Define what violations should be considered increased between N and contingency situations
        provider_parameters: Define parameters linked to the security analysis provider
            the names of the existing parameters can be found with method ``get_provider_parameters_names``
    """

    def __init__(self,
                 load_flow_parameters: pypowsybl.loadflow.Parameters = None,
                 increased_violations_parameters: IncreasedViolationsParameters = None,
                 provider_parameters: _Dict[str, str] = None):
        self._init_with_default_values()
        if load_flow_parameters is not None:
            self.load_flow_parameters = load_flow_parameters
        if increased_violations_parameters:
            self._increased_violations = increased_violations_parameters
        if provider_parameters is not None:
            self.provider_parameters = provider_parameters

    @property
    def increased_violations(self) -> IncreasedViolationsParameters:
        """
        Define what violations should be considered increased between N and post-contingency situations
        """
        return self._increased_violations

    def _init_with_default_values(self) -> None:
        default_parameters = _pypowsybl.SecurityAnalysisParameters()
        self.load_flow_parameters = pypowsybl.loadflow._parameters_from_c(default_parameters.load_flow_parameters)
        self._increased_violations = IncreasedViolationsParameters(default_parameters.flow_proportional_threshold,
                                                                   default_parameters.low_voltage_proportional_threshold,
                                                                   default_parameters.low_voltage_absolute_threshold,
                                                                   default_parameters.high_voltage_proportional_threshold,
                                                                   default_parameters.high_voltage_absolute_threshold)
        self.provider_parameters = dict(zip(default_parameters.provider_parameters_keys, default_parameters.provider_parameters_values))

    def _to_c_parameters(self) -> _pypowsybl.SecurityAnalysisParameters:
        c_parameters = _pypowsybl.SecurityAnalysisParameters()
        c_parameters.load_flow_parameters = self.load_flow_parameters._to_c_parameters()
        c_parameters.flow_proportional_threshold = self.increased_violations.flow_proportional_threshold
        c_parameters.low_voltage_proportional_threshold = self.increased_violations.low_voltage_proportional_threshold
        c_parameters.low_voltage_absolute_threshold = self.increased_violations.low_voltage_absolute_threshold
        c_parameters.high_voltage_proportional_threshold = self.increased_violations.high_voltage_proportional_threshold
        c_parameters.high_voltage_absolute_threshold = self.increased_violations.high_voltage_absolute_threshold
        c_parameters.provider_parameters_keys = list(self.provider_parameters.keys())
        c_parameters.provider_parameters_values = list(self.provider_parameters.values())
        return c_parameters

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f", load_flow_parameters={self.load_flow_parameters!r}" \
               f", increased_violations={self.increased_violations!r}" \
               f", provider_parameters={self.provider_parameters!r}" \
               f")"


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

    def run_ac(self, network: _Network, parameters: _Union[Parameters, pypowsybl.loadflow.Parameters] = None,
               provider: str = '', reporter: _Reporter = None) -> SecurityAnalysisResult:
        """ Runs an AC security analysis.

        Args:
            network:    Network on which the security analysis will be computed
            parameters: Security analysis parameters
            provider:   Name of the security analysis implementation provider to be used,
                        will use default provider if empty.

        Returns:
            A security analysis result, containing information about violations and monitored elements
        """
        security_parameters = Parameters(load_flow_parameters=parameters) if isinstance(parameters, pypowsybl.loadflow.Parameters) else parameters
        p = security_parameters._to_c_parameters() if security_parameters is not None else Parameters()._to_c_parameters()
        return SecurityAnalysisResult(
            _pypowsybl.run_security_analysis(self._handle, network._handle, p, provider, False, None if reporter is None else reporter._reporter_model)) # pylint: disable=protected-access

    def run_dc(self, network: _Network, parameters: _Union[Parameters, pypowsybl.loadflow.Parameters] = None,
               provider: str = '', reporter: _Reporter = None) -> SecurityAnalysisResult:
        """ Runs an DC security analysis.

        Args:
            network:    Network on which the security analysis will be computed
            parameters: Security analysis parameters
            provider:   Name of the security analysis implementation provider to be used,
                        will use default provider if empty.

        Returns:
            A security analysis result, containing information about violations and monitored elements
        """
        security_parameters = Parameters(load_flow_parameters=parameters) if isinstance(parameters, pypowsybl.loadflow.Parameters) else parameters
        p = security_parameters._to_c_parameters() if security_parameters is not None else Parameters()._to_c_parameters()
        return SecurityAnalysisResult(
            _pypowsybl.run_security_analysis(self._handle, network._handle, p, provider, True, None if reporter is None else reporter._reporter_model)) # pylint: disable=protected-access

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
    Set the default security analysis provider.

    Args:
        provider: name of the default security analysis provider to set
    """
    _pypowsybl.set_default_security_analysis_provider(provider)


def get_default_provider() -> str:
    """
    Get the current default security analysis provider.

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


def get_provider_parameters_names(provider: str = '') -> _List[str]:
    """
    Get list of parameters for the specified security analysis provider.

    If not specified the provider will be the default one.

    Returns:
        the list of provider's parameters
    """
    return _pypowsybl.get_security_analysis_provider_parameters_names(provider)
