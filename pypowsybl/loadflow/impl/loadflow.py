#
# Copyright (c) 2020-2022, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import (
    List as _List
)
from pandas import DataFrame as _DataFrame

from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import (
    ConnectedComponentMode,
    BalanceType,
    VoltageInitMode
)
from pypowsybl.network import Network as _Network
from pypowsybl.util import create_data_frame_from_series_array as _create_data_frame_from_series_array
from pypowsybl.report import Reporter as _Reporter
from loadflow.impl.component_result import ComponentResult
from loadflow.impl.parameters import Parameters

# enforcing some class metadata on classes imported from C extension,
# in particular for sphinx documentation to work correctly,
# and add some documentation
VoltageInitMode.__module__ = __name__
BalanceType.__module__ = __name__
ConnectedComponentMode.__module__ = __name__



def _parameters_from_c(c_parameters: _pypowsybl.LoadFlowParameters) -> Parameters:
    """
    Converts C struct to python parameters (bypassing python constructor)
    """
    res = Parameters.__new__(Parameters)
    res._init_from_c(c_parameters)
    return res


def run_ac(network: _Network, parameters: Parameters = None, provider: str = '', reporter: _Reporter = None) -> _List[ComponentResult]:
    """
    Run an AC loadflow on a network.

    Args:
        network:    a network
        parameters: the loadflow parameters
        provider:   the loadflow implementation provider, default is the default loadflow provider
        reporter:   the reporter to be used to create an execution report, default is None (no report)

    Returns:
        A list of component results, one for each component of the network.
    """
    p = parameters._to_c_parameters() if parameters is not None else _pypowsybl.LoadFlowParameters()
    return [ComponentResult(res) for res in _pypowsybl.run_loadflow(network._handle, False, p, provider, None if reporter is None else reporter._reporter_model)] # pylint: disable=protected-access


def run_dc(network: _Network, parameters: Parameters = None, provider: str = '', reporter: _Reporter = None) -> _List[ComponentResult]:
    """
    Run a DC loadflow on a network.

    Args:
        network:    a network
        parameters: the loadflow parameters
        provider:   the loadflow implementation provider, default is the default loadflow provider
        reporter:   the reporter to be used to create an execution report, default is None (no report)

    Returns:
        A list of component results, one for each component of the network.
    """
    p = parameters._to_c_parameters() if parameters is not None else _pypowsybl.LoadFlowParameters()
    return [ComponentResult(res) for res in _pypowsybl.run_loadflow(network._handle, True, p, provider, None if reporter is None else reporter._reporter_model)]  # pylint: disable=protected-access


def set_default_provider(provider: str) -> None:
    """
    Set the default loadflow provider

    Args:
        provider: name of the default loadflow provider to set
    """
    _pypowsybl.set_default_loadflow_provider(provider)


def get_default_provider() -> str:
    """
    Get the current default loadflow provider. if nothing is set it is OpenLoadFlow

    Returns:
        the name of the current default loadflow provider
    """
    return _pypowsybl.get_default_loadflow_provider()


def get_provider_names() -> _List[str]:
    """
    Get list of supported provider names.

    Returns:
        the list of supported provider names
    """
    return _pypowsybl.get_loadflow_provider_names()


def get_provider_parameters_names(provider: str = None) -> _List[str]:
    """
    Get list of parameters for the specified loadflow provider.

    Args:
       provider (str): the provider, if not specified the provider will be the default one.

    Returns:
        the list of provider's parameters
    """
    return _pypowsybl.get_loadflow_provider_parameters_names('' if provider is None else provider)


def get_provider_parameters(provider: str = None) -> _DataFrame:
    """
    Supported loadflow specific parameters for a given provider.

    Args:
       provider (str): the provider, if not specified the provider will be the default one.

    Returns:
        loadflow parameters dataframe

    Examples:
       .. doctest::

           >>> parameters = pp.loadflow.get_provider_parameters('OpenLoadFlow')
           >>> parameters['description']['slackBusSelectionMode']
           'Slack bus selection mode'
           >>> parameters['type']['slackBusSelectionMode']
           'STRING'
           >>> parameters['default']['slackBusSelectionMode']
           'MOST_MESHED'
    """
    series_array = _pypowsybl.create_loadflow_provider_parameters_series_array('' if provider is None else provider)
    return _create_data_frame_from_series_array(series_array)
