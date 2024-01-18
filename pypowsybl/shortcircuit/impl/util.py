# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import List

from pandas import DataFrame

from pypowsybl import _pypowsybl
from util import create_data_frame_from_series_array
from .short_circuit_analysis import ShortCircuitAnalysis
from ...utils import _create_c_dataframe


def create_analysis() -> ShortCircuitAnalysis:
    """ Creates a short-circuit analysis object, which can be used to run a short-circuit analysis on a network

    Examples:
        .. code-block::

            >>> analysis = pypowsybl.shortcircuit.create_analysis()
            >>> analysis.set_faults(id='F1', element_id='Bus1', r= 1, x= 2)
            >>> res = analysis.run(network, parameters, provider_name)

    Returns:
        A short-circuit analysis object.
    """
    return ShortCircuitAnalysis(_pypowsybl.create_shortcircuit_analysis())


def set_default_provider(provider: str) -> None:
    """
    Set the default short-circuit analysis provider.

    Args:
        provider: name of the default short-circuit analysis provider to set
    """
    _pypowsybl.set_default_shortcircuit_analysis_provider(provider)


def get_default_provider() -> str:
    """
    Get the current default short-circuit analysis provider.

    Returns:
        the name of the current default short-circuit analysis provider
    """
    return _pypowsybl.get_default_shortcircuit_analysis_provider()


def get_provider_names() -> List[str]:
    """
    Get list of supported provider names

    Returns:
        the list of supported provider names
    """
    return _pypowsybl.get_shortcircuit_provider_names()


def get_provider_parameters_names(provider: str = '') -> List[str]:
    """
    Get list of parameters for the specified short-circuit analysis provider.

    If not specified the provider will be the default one.

    Returns:
        the list of provider's parameters
    """
    return _pypowsybl.get_shortcircuit_provider_parameters_names(provider)


def convert_to_three_phase_value(df: DataFrame) -> DataFrame:
    """

    Take a dataframe containing data on symmetrical components and converts it to data on the three phases.

    """
    metadata = #TODO
    c_df = _create_c_dataframe(df, metadata)
    series_array = _pypowsybl.convert_fortescue_value_to_three_phase(c_df)
    return create_data_frame_from_series_array(series_array)

