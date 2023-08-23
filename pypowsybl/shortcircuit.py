#
# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# iicense, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from __future__ import annotations  # Necessary for type alias like _DataFrame to work with sphinx

from typing import (
    List as _List,
    Dict as _Dict,
    Optional as _Optional,
)

import pandas as _pd
from numpy.typing import ArrayLike as _ArrayLike
from pandas import DataFrame as _DataFrame
from pypowsybl import _pypowsybl
from pypowsybl._pypowsybl import ShortCircuitFaultType, ShortCircuitStudyType
from pypowsybl.network import Network as _Network
from pypowsybl.report import Reporter as _Reporter
from pypowsybl.util import (
    create_data_frame_from_series_array as _create_data_frame_from_series_array
)
from pypowsybl.utils.dataframes import (
    _get_c_dataframes
)

ShortCircuitStudyType.__module__ = __name__

class Parameters:  # pylint: disable=too-few-public-methods
    """
    Parameters for a short-circuit analysis execution.

    Please check the Powsybl's short-circuit APIs documentation, for detailed information.

    .. currentmodule:: pypowsybl.shortcircuit

    Args:
        with_feeder_result: indicates if the contributions of each feeder to the short circuit current at the fault node should be computed
        with_limit_violations: indicates whether limit violations should be returned after the computation
        study_type: indicates the type of short circuit study. It can be SUB_TRANSIENT, TRANSIENT or STEADY_STATE
    """
    def __init__(self,
                 with_feeder_result: bool = None,
                 with_limit_violations: bool = None,
                 study_type: ShortCircuitStudyType = None,
                 provider_parameters: _Dict[str, str] = None):
        self._init_with_default_values()
        if with_feeder_result is not None:
            self.with_feeder_result = with_feeder_result
        if with_limit_violations is not None:
            self.with_limit_violations = with_limit_violations
        if study_type is not None:
            self.study_type = study_type
        if provider_parameters is not None:
            self.provider_parameters = provider_parameters

    def _init_from_c(self, c_parameters: _pypowsybl.ShortCircuitAnalysisParameters) -> None:
        self.with_feeder_result = c_parameters.with_feeder_result
        self.with_limit_violations = c_parameters.with_limit_violations
        self.study_type = c_parameters.study_type
        self.provider_parameters = dict(zip(c_parameters.provider_parameters_keys, c_parameters.provider_parameters_values))

    def _init_with_default_values(self) -> None:
        self._init_from_c(_pypowsybl.ShortCircuitAnalysisParameters())
        self.with_feeder_result = False
        self.with_limit_violations = False
        self.study_type = ShortCircuitStudyType.TRANSIENT

    def _to_c_parameters(self) -> _pypowsybl.ShortCircuitAnalysisParameters:
        c_parameters = _pypowsybl.ShortCircuitAnalysisParameters()
        c_parameters.with_voltage_result = False
        c_parameters.with_feeder_result = self.with_feeder_result
        c_parameters.with_limit_violations = self.with_limit_violations
        c_parameters.study_type = self.study_type
        c_parameters.with_fortescue_result = False
        c_parameters.min_voltage_drop_proportional_threshold = 0
        c_parameters.provider_parameters_keys = []
        c_parameters.provider_parameters_values = []
        return c_parameters

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"with_feeder_result={self.with_feeder_result!r}" \
               f", with_limit_violations={self.with_limit_violations!r}" \
               f", study_type={self.study_type!r}" \
               f")"

class ShortCircuitAnalysisResult:
    """
    The result of a short-circuit analysis.
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        self._handle = handle

    @property
    def fault_results(self) -> _pd.DataFrame:
        """
        contains the results, for each fault, in a dataframe representation.
        """
        return _create_data_frame_from_series_array(_pypowsybl.get_fault_results(self._handle))

    @property
    def feeder_results(self) -> _pd.DataFrame:
        """
        contains the contributions of each feeder to the short circuit current, in a dataframe representation.
        """
        return _create_data_frame_from_series_array(_pypowsybl.get_feeder_results(self._handle))

    @property
    def limit_violations(self) -> _pd.DataFrame:
        """
        contains a list of all the violations after the fault, in a dataframe representation.
        """
        return _create_data_frame_from_series_array(_pypowsybl.get_short_circuit_limit_violations(self._handle))


class ShortCircuitAnalysis():
    """
    Allows to run a short-circuit analysis on a network.
    """

    def __init__(self, handle: _pypowsybl.JavaHandle):
        self._handle = handle


    def _set_faults(self, fault_type: ShortCircuitFaultType, dfs: _List[_Optional[_DataFrame]],
                    **kwargs: _ArrayLike) -> None:
        metadata = _pypowsybl.get_faults_dataframes_metadata(fault_type)
        c_dfs = _get_c_dataframes(dfs, [metadata], **kwargs)
        _pypowsybl.set_faults(self._handle, c_dfs[0], fault_type)


    def set_faults(self, df: _DataFrame = None, **kwargs: _ArrayLike) -> None:
        """
        Define faults to be analysed in the short-circuit simulation.

        Args:
            df: Attributes as a dataframe.
            kwargs: Attributes as keyword arguments.

        Notes:
            The current implementation allows the simulation of three-phased bus faults, where
            the fault resistance and reactance are connected to the ground in series.

            Data may be provided as a dataframe or as keyword arguments.
            In the latter case, all arguments must have the same length.

            Valid attributes are:

            - **id**: the id of the fault.
            - **element_id**: the id of the bus on which the fault will be simulated (bus/view topology).
            - **r**: The fault resistance to ground, in Ohm.
            - **x**: The fault reactance to ground, in Ohm.

        Examples:

        .. code-block::

            analysis = pypowsybl.shortcircuit.create_analysis()

            # define a single fault as keyword arguments
            analysis.set_faults(id='F1', element_id='Bus1', r= 0, x= 0)

            # or, define multiple faults as keyword arguments
            analysis.set_faults(id=['F1', 'F2'], element_id= [ 'Bus1', 'Bus2'], r= [0, 0], x= [0,0])

            # or, define faults as a dataframe
            analysis.set_faults(pd.DataFrame.from_records(index='id', data=[{'id': 'F1', 'element_id': buses.index[0], 'r': 1, 'x': 2}]))
        """
        self._set_faults(ShortCircuitFaultType.BUS_FAULT, [df], **kwargs)


    def run(self, network: _Network, parameters: Parameters = None,
            provider: str = '', reporter: _Reporter = None) -> ShortCircuitAnalysisResult:
        """ Runs an short-circuit analysis.

        Args:
            network:    Network on which the short-circuit analysis will be computed
            parameters: short-circuit analysis parameters
            provider:   Name of the short-circuit analysis implementation provider to be used.

        Returns:
            A short-circuit analysis result.
        """
        p = parameters._to_c_parameters() if parameters is not None else Parameters()._to_c_parameters() # pylint: disable=protected-access

        return ShortCircuitAnalysisResult(
            _pypowsybl.run_shortcircuit_analysis(self._handle, network._handle, p, provider,
                                                 None if reporter is None else reporter._reporter_model) # pylint: disable=protected-access
            # pylint: disable=protected-access
        )

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


def get_provider_names() -> _List[str]:
    """
    Get list of supported provider names

    Returns:
        the list of supported provider names
    """
    return _pypowsybl.get_shortcircuit_provider_names()


def get_provider_parameters_names(provider: str = '') -> _List[str]:
    """
    Get list of parameters for the specified short-circuit analysis provider.

    If not specified the provider will be the default one.

    Returns:
        the list of provider's parameters
    """
    return _pypowsybl.get_shortcircuit_provider_parameters_names(provider)
