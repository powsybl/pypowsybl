# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
import warnings
from typing import List
from pandas import DataFrame
from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import (
    ConnectedComponentMode,
    BalanceType,
    VoltageInitMode,
    LoadFlowValidationParameters,
    run_loadflow_validation
)
from pypowsybl.network import Network
from pypowsybl.utils import create_data_frame_from_series_array
from pypowsybl.report import ReportNode
from .component_result import ComponentResult
from .parameters import Parameters
from .validation_result import ValidationResult
from .validation_parameters import ValidationParameters, ValidationType

# enforcing some class metadata on classes imported from C extension,
# in particular for sphinx documentation to work correctly,
# and add some documentation
VoltageInitMode.__module__ = __name__
BalanceType.__module__ = __name__
ConnectedComponentMode.__module__ = __name__


def run_ac(network: Network, parameters: Parameters = None, provider: str = '', reporter: ReportNode = None,
           report_node: ReportNode = None) -> \
        List[ComponentResult]:  # pylint: disable=protected-access
    """
    Run an AC load flow on a network.

    Args:
        network:    a network
        parameters: the load flow parameters
        provider:   the load flow implementation provider, default is the default load flow provider
        reporter: deprecated, use report_node instead
        report_node:   the reporter to be used to create an execution report, default is None (no report)

    Returns:
        A list of component results, one for each component of the network.
    """
    if reporter is not None:
        warnings.warn("Use of deprecated attribute reporter. Use report_node instead.", DeprecationWarning)
        report_node = reporter
    p = parameters._to_c_parameters() if parameters is not None else _pypowsybl.LoadFlowParameters()  # pylint: disable=protected-access
    return [ComponentResult(res) for res in _pypowsybl.run_loadflow(network._handle, False, p, provider,
                                                                    None if report_node is None else report_node._report_node)]  # pylint: disable=protected-access


def run_dc(network: Network, parameters: Parameters = None, provider: str = '', reporter: ReportNode = None,
           report_node: ReportNode = None) -> List[ComponentResult]:  # pylint: disable=protected-access
    """
    Run a DC load flow on a network.

    Args:
        network:    a network
        parameters: the load flow parameters
        provider:   the load flow implementation provider, default is the default load flow provider
        reporter: deprecated, use report_node instead
        report_node:   the reporter to be used to create an execution report, default is None (no report)

    Returns:
        A list of component results, one for each component of the network.
    """
    if reporter is not None:
        warnings.warn("Use of deprecated attribute reporter. Use report_node instead.", DeprecationWarning)
        report_node = reporter
    p = parameters._to_c_parameters() if parameters is not None else _pypowsybl.LoadFlowParameters()  # pylint: disable=protected-access
    return [ComponentResult(res) for res in _pypowsybl.run_loadflow(network._handle, True, p, provider,
                                                                    None if report_node is None else report_node._report_node)]  # pylint: disable=protected-access


def set_default_provider(provider: str) -> None:
    """
    Set the default load flow provider

    Args:
        provider: name of the default load flow provider to set
    """
    _pypowsybl.set_default_loadflow_provider(provider)


def get_default_provider() -> str:
    """
    Get the current default loadflow provider. if nothing is set it is OpenLoadFlow

    Returns:
        the name of the current default loadflow provider
    """
    return _pypowsybl.get_default_loadflow_provider()


def get_provider_names() -> List[str]:
    """
    Get list of supported provider names.

    Returns:
        the list of supported provider names
    """
    return _pypowsybl.get_loadflow_provider_names()


def get_provider_parameters_names(provider: str = None) -> List[str]:
    """
    Get list of parameters for the specified loadflow provider.

    Args:
       provider (str): the provider, if not specified the provider will be the default one.

    Returns:
        the list of provider's parameters
    """
    return _pypowsybl.get_loadflow_provider_parameters_names('' if provider is None else provider)


def get_provider_parameters(provider: str = None) -> DataFrame:
    """
    Supported loadflow specific parameters for a given provider.

    Args:
       provider (str): the provider, if not specified the provider will be the default one.

    Returns:
        loadflow parameters dataframe

    Examples:
       .. doctest::

           >>> parameters = pp.loadflow.get_provider_parameters('OpenLoadFlow')
           >>> parameters['category_key']['slackBusSelectionMode']
           'SlackDistribution'
           >>> parameters['description']['slackBusSelectionMode']
           'Slack bus selection mode'
           >>> parameters['type']['slackBusSelectionMode']
           'STRING'
           >>> parameters['default']['slackBusSelectionMode']
           'MOST_MESHED'
           >>> parameters['possible_values']['slackBusSelectionMode']
           '[FIRST, MOST_MESHED, NAME, LARGEST_GENERATOR]'
    """
    series_array = _pypowsybl.create_loadflow_provider_parameters_series_array('' if provider is None else provider)
    return create_data_frame_from_series_array(series_array)


def run_validation(network: Network, validation_types: List[ValidationType] = None,
                   validation_parameters: ValidationParameters = None) -> ValidationResult:
    """
    Checks that the network data are consistent with AC loadflow equations.

    Args:
        network: The network to be checked.
        validation_types: The types of data to be checked. If None, all types will be checked.
        validation_parameters: The parameters to run the validation with.

    Returns:
        The validation result.
    """
    if validation_types is None:
        validation_types = ValidationType.ALL
    validation_config = validation_parameters.to_c_parameters() if validation_parameters is not None else LoadFlowValidationParameters()
    res_by_type = {}
    for validation_type in validation_types:
        series_array = run_loadflow_validation(network._handle, validation_type, validation_config)
        res_by_type[validation_type] = create_data_frame_from_series_array(series_array)

    return ValidationResult(buses=res_by_type.get(ValidationType.BUSES, None),
                            branch_flows=res_by_type.get(ValidationType.FLOWS, None),
                            generators=res_by_type.get(ValidationType.GENERATORS, None),
                            svcs=res_by_type.get(ValidationType.SVCS, None),
                            shunts=res_by_type.get(ValidationType.SHUNTS, None),
                            twts=res_by_type.get(ValidationType.TWTS, None),
                            t3wts=res_by_type.get(ValidationType.TWTS3W, None))
