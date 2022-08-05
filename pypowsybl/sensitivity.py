#
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
from __future__ import annotations
from datetime import datetime
from typing import List as _List, Optional as _Optional, Dict as _Dict, Union as _Union
from enum import Enum as _Enum
import numpy as _np
import pandas as _pd
import pypowsybl._pypowsybl as _pypowsybl
import pypowsybl.loadflow

from pypowsybl import _pypowsybl
from pypowsybl import glsk
from pypowsybl.network import Network as _Network
from pypowsybl.util import ContingencyContainer as _ContingencyContainer
from pypowsybl._pypowsybl import PyPowsyblError as _PyPowsyblError
from pypowsybl.report import Reporter as _Reporter

TO_REMOVE = 'TO_REMOVE'


class Zone:
    def __init__(self, id: str, shift_keys_by_injections_ids: _Dict[str, float] = None):
        self._id = id
        self._shift_keys_by_injections_ids = {} if shift_keys_by_injections_ids is None else shift_keys_by_injections_ids

    @property
    def id(self) -> str:
        return self._id

    @property
    def shift_keys_by_injections_ids(self) -> _Dict[str, float]:
        return self._shift_keys_by_injections_ids

    @property
    def injections_ids(self) -> _List[str]:
        return list(self._shift_keys_by_injections_ids.keys())

    def get_shift_key(self, injection_id: str) -> float:
        shift_key = self._shift_keys_by_injections_ids.get(injection_id)
        if shift_key is None:
            raise _PyPowsyblError(f'Injection {injection_id} not found')
        return shift_key

    def add_injection(self, id: str, key: float = 1) -> None:
        self._shift_keys_by_injections_ids[id] = key

    def remove_injection(self, id: str) -> None:
        del self._shift_keys_by_injections_ids[id]

    def move_injection_to(self, other_zone: Zone, id: str) -> None:
        shift_key = self.get_shift_key(id)
        other_zone.add_injection(id, shift_key)
        self.remove_injection(id)


def create_empty_zone(id: str) -> Zone:
    return Zone(id)


class ZoneKeyType(_Enum):
    GENERATOR_TARGET_P = 0
    GENERATOR_MAX_P = 1
    LOAD_P0 = 2


def create_country_zone(network: _Network, country: str,
                        key_type: ZoneKeyType = ZoneKeyType.GENERATOR_TARGET_P) -> Zone:
    substations = network.get_substations()
    voltage_levels = network.get_voltage_levels()
    if key_type in (ZoneKeyType.GENERATOR_MAX_P, ZoneKeyType.GENERATOR_TARGET_P):
        # join generators, voltage levels and substations to get generators with countries
        generators = network.get_generators()
        generators_with_countries = generators.join(
            voltage_levels[['substation_id']].join(substations[['country']], on=['substation_id']),
            on=['voltage_level_id'])

        # filter generators for specified country
        filtered_generators = generators_with_countries[generators_with_countries['country'] == country]
        shift_keys = filtered_generators.target_p if key_type == ZoneKeyType.GENERATOR_TARGET_P else filtered_generators.max_p
        shift_keys_by_id = dict(zip(filtered_generators.index, shift_keys))
    elif key_type == ZoneKeyType.LOAD_P0:
        # join loads, voltage levels and substations to get generators with countries
        loads = network.get_loads()
        loads_with_countries = loads.join(
            voltage_levels[['substation_id']].join(substations[['country']], on=['substation_id']),
            on=['voltage_level_id'])

        # filter loads for specified country
        filtered_loads = loads_with_countries[loads_with_countries['country'] == country]
        shift_keys_by_id = dict(zip(filtered_loads.index, filtered_loads.p0))
    else:
        raise _PyPowsyblError(f'Unknown key type {key_type}')

    return Zone(country, shift_keys_by_id)


def create_zone_from_injections_and_shift_keys(id: str, injection_index: _List[str], shift_keys: _List[float]) -> Zone:
    """ Create country zone with custom generator name and shift keys
        Args:
            country : Identifier of the zone
            injection_index : IDs of the injection
            shift_keys : shift keys for the generators
        Returns:
            The zone object
    """
    shift_keys_by_id = dict(zip(injection_index, shift_keys))
    return Zone(id, shift_keys_by_id)


def create_zones_from_glsk_file(network: _Network, glsk_file: str, instant: datetime) -> _List[Zone]:
    """ Create country zones from glsk file for a given datetime
        Args:
            glsk_file : UCTE glsk file
            instant : timepoint at which to select glsk data
        Returns:
            A list of zones created from glsk file
    """
    glsk_document = glsk.load(glsk_file)
    countries = glsk_document.get_countries()
    zones = []
    for country in countries:
        c_generators = glsk_document.get_points_for_country(network, country, instant)
        c_shift_keys = glsk_document.get_glsk_factors(network, country, instant)
        zone = create_zone_from_injections_and_shift_keys(country, c_generators, c_shift_keys)
        zones.append(zone)
    return zones


class Parameters:  # pylint: disable=too-few-public-methods
    """
    Parameters for a sensitivity analysis execution.

    All parameters are first read from you configuration file, then overridden with
    the constructor arguments.

    Please note that sensitivity providers may not honor all parameters, according to their capabilities.
    The exact behaviour of some parameters may also depend on your sensitivity provider.
    Please check the documentation of your provider for that information.

    .. currentmodule:: pypowsybl.sensitivity

    Args:
        load_flow_parameters: parameters that are common to loadflow and sensitivity analysis
        provider_parameters: Define parameters linked to the sensitivity analysis provider
            the names of the existing parameters can be found with method ``get_provider_parameters_names``
    """

    def __init__(self, load_flow_parameters: pypowsybl.loadflow.Parameters = None,
                 provider_parameters: _Dict[str, str] = None):
        self._init_with_default_values()
        if load_flow_parameters is not None:
            self.load_flow_parameters = load_flow_parameters
        if provider_parameters is not None:
            self.provider_parameters = provider_parameters

    def _init_with_default_values(self) -> None:
        default_parameters = _pypowsybl.SensitivityAnalysisParameters()
        self.load_flow_parameters = pypowsybl.loadflow._parameters_from_c(default_parameters.load_flow_parameters)
        self.provider_parameters = dict(
            zip(default_parameters.provider_parameters_keys, default_parameters.provider_parameters_values))

    def _to_c_parameters(self) -> _pypowsybl.SensitivityAnalysisParameters:
        c_parameters = _pypowsybl.SensitivityAnalysisParameters()
        c_parameters.load_flow_parameters = self.load_flow_parameters._to_c_parameters()
        c_parameters.provider_parameters_keys = list(self.provider_parameters.keys())
        c_parameters.provider_parameters_values = list(self.provider_parameters.values())
        return c_parameters

    def __repr__(self) -> str:
        return f"{self.__class__.__name__}(" \
               f"load_flow_parameters={self.load_flow_parameters}" \
               f", provider_parameters={self.provider_parameters!r}" \
               f")"


class DcSensitivityAnalysisResult:
    """
    Represents the result of a DC sensitivity analysis.

    The result contains computed values (so called "reference" values) and sensitivity values
    of requested factors, on the base case and on post contingency states.
    """

    def __init__(self,
                 result_context_ptr: _pypowsybl.JavaHandle,
                 branches_ids: _Dict[str, _List[str]],
                 branch_data_frame_index: _Dict[str, _List[str]]):
        self._handle = result_context_ptr
        self.result_context_ptr = result_context_ptr
        self.branches_ids = branches_ids
        self.branch_data_frame_index = branch_data_frame_index

    def get_branch_flows_sensitivity_matrix(self, matrix_id: str = 'default', contingency_id: str = None) -> _Optional[
        _pd.DataFrame]:
        """
        Get the matrix of branch flows sensitivities on the base case or on post contingency state.

        If contingency_id is None, returns the base case matrix.

        Args:
            matrix_id:      ID of the matrix
            contingency_id: ID of the contingency
        Returns:
            the matrix of branch flows sensitivities
        """
        matrix = _pypowsybl.get_branch_flows_sensitivity_matrix(self.result_context_ptr, matrix_id,
                                                                '' if contingency_id is None else contingency_id)
        if matrix is None:
            return None

        data = _np.array(matrix, copy=False)

        df = _pd.DataFrame(data=data, columns=self.branches_ids[matrix_id],
                           index=self.branch_data_frame_index[matrix_id])

        # substract second power transfer zone to first one
        i = 0
        while i < len(self.branch_data_frame_index[matrix_id]):
            if self.branch_data_frame_index[matrix_id][i] == TO_REMOVE:
                df.iloc[i - 1] = df.iloc[i - 1] - df.iloc[i]
            i += 1

        # remove rows corresponding to power transfer second zone
        return df.drop([TO_REMOVE], errors='ignore')

    def get_reference_flows(self, matrix_id: str = 'default', contingency_id: str = None) -> _Optional[_pd.DataFrame]:
        """
        The branches active power flows on the base case or on post contingency state.

        Args:
            matrix_id:      ID of the matrix
            contingency_id: ID of the contingency
        Returns:
            the branches active power flows
        """
        matrix= _pypowsybl.get_reference_flows(self.result_context_ptr, matrix_id, '' if contingency_id is None else contingency_id)
        if matrix is None:
            return None
        data = _np.array(matrix, copy=False)
        return _pd.DataFrame(data=data, columns=self.branches_ids[matrix_id], index=['reference_flows'])


class AcSensitivityAnalysisResult(DcSensitivityAnalysisResult):
    """
    Represents the result of a AC sensitivity analysis.

    The result contains computed values (so called "reference" values) and sensitivity values
    of requested factors, on the base case and on post contingency states.
    """

    def __init__(self, result_context_ptr: _pypowsybl.JavaHandle, branches_ids: _Dict[str, _List[str]],
                 branch_data_frame_index: _Dict[str, _List[str]],
                 bus_ids: _List[str], target_voltage_ids: _List[str]):
        DcSensitivityAnalysisResult.__init__(self, result_context_ptr, branches_ids, branch_data_frame_index)
        self.bus_ids = bus_ids
        self.target_voltage_ids = target_voltage_ids

    def get_bus_voltages_sensitivity_matrix(self, contingency_id: str = None) -> _Optional[_pd.DataFrame]:
        """
        Get the matrix of bus voltages sensitivities on the base case or on post contingency state.

        Args:
            contingency_id: ID of the contingency
        Returns:
            the matrix of sensitivities
        """
        matrix = _pypowsybl.get_bus_voltages_sensitivity_matrix(self.result_context_ptr,
                                                                '' if contingency_id is None else contingency_id)
        if matrix is None:
            return None

        data = _np.array(matrix, copy=False)
        return _pd.DataFrame(data=data, columns=self.bus_ids, index=self.target_voltage_ids)

    def get_reference_voltages(self, contingency_id: str = None) -> _Optional[_pd.DataFrame]:
        """
        The values of bus voltages on the base case or on post contingency state.

        Args:
            contingency_id: ID of the contingency
        Returns:
            the values of bus voltages
        """
        matrix = _pypowsybl.get_reference_voltages(self.result_context_ptr,
                                                   '' if contingency_id is None else contingency_id)
        if matrix is None:
            return None

        data = _np.array(matrix, copy=False)
        return _pd.DataFrame(data=data, columns=self.bus_ids, index=['reference_voltages'])


class SensitivityAnalysis(_ContingencyContainer):
    """ Base class for sensitivity analysis. Do not instantiate it directly!"""

    def __init__(self, handle: _pypowsybl.JavaHandle):
        _ContingencyContainer.__init__(self, handle)
        self.branches_ids: _Dict[str, _List[str]] = {}
        self.branch_data_frame_index: _Dict[str, _List[str]] = {}

    def set_zones(self, zones: _List[Zone]) -> None:
        """
        Define zones that will be used in branch flow factor matrix.

        Args:
            zones: a list of zones
        """
        _zones = []
        for zone in zones:
            _zones.append(_pypowsybl.Zone(zone.id, list(zone.shift_keys_by_injections_ids.keys()),
                                          list(zone.shift_keys_by_injections_ids.values())))
        _pypowsybl.set_zones(self._handle, _zones)

    def _process_variable_ids(self, variables_ids: _List) -> tuple:
        flatten_variables_ids = []
        branch_data_frame_index = []
        for variable_id in variables_ids:
            if isinstance(variable_id, str):  # this is an ID
                flatten_variables_ids.append(variable_id)
                branch_data_frame_index.append(variable_id)
            elif isinstance(variable_id, tuple):  # this is a power transfer
                if len(variable_id) != 2:
                    raise _PyPowsyblError('Power transfer factor should be describe with a tuple 2')
                flatten_variables_ids.append(variable_id[0])
                flatten_variables_ids.append(variable_id[1])
                branch_data_frame_index.append(variable_id[0] + ' -> ' + variable_id[1])
                branch_data_frame_index.append(TO_REMOVE)
            else:
                raise _PyPowsyblError(f'Unsupported factor variable type {type(variable_id)}')
        return (flatten_variables_ids, branch_data_frame_index)

    def set_branch_flow_factor_matrix(self, branches_ids: _List[str], variables_ids: _List[str]) -> None:
        """
        .. deprecated:: 0.14.0

        Defines branch active power flow factor matrix, with a list of branches IDs and a list of variables.

        A variable could be:

         - a network element ID: injections, PSTs, dangling lines and HVDC lines are supported
         - a zone ID
         - a couple of zone ID to define a transfer between 2 zones

        Args:
            branches_ids:  IDs of branches for which active power flow sensitivities should be computed
            variables_ids: variables which may impact branch flows,to which we should compute sensitivities
        """
        self.add_branch_flow_factor_matrix(branches_ids, variables_ids)

    def add_branch_flow_factor_matrix(self, branches_ids: _List[str], variables_ids: _List[str],
                                      matrix_id: str = 'default') -> None:
        """
        Defines branch active power flow factor matrix, with a list of branches IDs and a list of variables.

        A variable could be:
         - a network element ID: injections, PSTs, dangling lines and HVDC lines are supported
         - a zone ID
         - a couple of zone ID to define a transfer between 2 zones

        Args:
            branches_ids:  IDs of branches for which active power flow sensitivities should be computed
            variables_ids: variables which may impact branch flows,to which we should compute sensitivities
            matrix_id:     The matrix unique identifier, to be used to retrieve the sensibility value
        """
        (flatten_variables_ids, branch_data_frame_index) = self._process_variable_ids(variables_ids)
        _pypowsybl.add_branch_flow_factor_matrix(self._handle, matrix_id, branches_ids, flatten_variables_ids)
        self.branches_ids[matrix_id] = branches_ids
        self.branch_data_frame_index[matrix_id] = branch_data_frame_index

    def add_precontingency_branch_flow_factor_matrix(self, branches_ids: _List[str], variables_ids: _List[str],
                                                     matrix_id: str = 'default') -> None:
        """
        Defines branch active power flow factor matrix for the base case, with a list of branches IDs and a list of variables.

        A variable could be:
         - a network element ID: injections, PSTs, dangling lines and HVDC lines are supported
         - a zone ID
         - a couple of zone ID to define a transfer between 2 zones

        Args:
            branches_ids:  IDs of branches for which active power flow sensitivities should be computed
            variables_ids: variables which may impact branch flows,to which we should compute sensitivities
            matrix_id:     The matrix unique identifier, to be used to retrieve the sensibility value
        """
        (flatten_variables_ids, branch_data_frame_index) = self._process_variable_ids(variables_ids)

        _pypowsybl.add_precontingency_branch_flow_factor_matrix(self._handle, matrix_id, branches_ids,
                                                                flatten_variables_ids)
        self.branches_ids[matrix_id] = branches_ids
        self.branch_data_frame_index[matrix_id] = branch_data_frame_index

    def add_postcontingency_branch_flow_factor_matrix(self, branches_ids: _List[str], variables_ids: _List[str],
                                                      contingencies_ids: _List[str],
                                                      matrix_id: str = 'default') -> None:
        """
        Defines branch active power flow factor matrix for specific post contingencies states, with a list of branches IDs and a list of variables.

        A variable could be:
         - a network element ID: injections, PSTs, dangling lines and HVDC lines are supported
         - a zone ID
         - a couple of zone ID to define a transfer between 2 zones

        Args:
            branches_ids:      IDs of branches for which active power flow sensitivities should be computed
            variables_ids:     variables which may impact branch flows,to which we should compute sensitivities
            contingencies_ids: List of the IDs of the contingencies to simulate
            matrix_id:         The matrix unique identifier, to be used to retrieve the sensibility value
        """
        (flatten_variables_ids, branch_data_frame_index) = self._process_variable_ids(variables_ids)

        _pypowsybl.add_postcontingency_branch_flow_factor_matrix(self._handle, matrix_id, branches_ids,
                                                                 flatten_variables_ids, contingencies_ids)
        self.branches_ids[matrix_id] = branches_ids
        self.branch_data_frame_index[matrix_id] = branch_data_frame_index


class DcSensitivityAnalysis(SensitivityAnalysis):
    """ Represents a DC sensitivity analysis."""

    def __init__(self, handle: _pypowsybl.JavaHandle):
        SensitivityAnalysis.__init__(self, handle)

    def run(self, network: _Network, parameters: _Union[Parameters, pypowsybl.loadflow.Parameters] = None,
            provider: str = '', reporter: _Reporter = None) -> DcSensitivityAnalysisResult:
        """ Runs the sensitivity analysis

        Args:
            network:    The network
            parameters: The sensitivity parameters
            provider:   Name of the sensitivity analysis provider

        Returns:
            a sensitivity analysis result
        """
        sensitivity_parameters = Parameters(load_flow_parameters=parameters) if isinstance(parameters, pypowsybl.loadflow.Parameters) else parameters
        p: _pypowsybl.SensitivityAnalysisParameters = sensitivity_parameters._to_c_parameters() if sensitivity_parameters is not None else Parameters()._to_c_parameters()
        return DcSensitivityAnalysisResult(
            _pypowsybl.run_sensitivity_analysis(self._handle, network._handle, True, p, provider,
                                                None if reporter is None else reporter._reporter_model),
            # pylint: disable=protected-access
            branches_ids=self.branches_ids, branch_data_frame_index=self.branch_data_frame_index)


class AcSensitivityAnalysis(SensitivityAnalysis):
    """ Represents an AC sensitivity analysis."""

    def __init__(self, handle: _pypowsybl.JavaHandle):
        SensitivityAnalysis.__init__(self, handle)
        self.bus_voltage_ids: _List[str] = []
        self.target_voltage_ids: _List[str] = []

    def set_bus_voltage_factor_matrix(self, bus_ids: _List[str], target_voltage_ids: _List[str]) -> None:
        """
        Defines buses voltage sensitivities to be computed.

        Args:
            bus_ids:            IDs of buses for which voltage sensitivities should be computed
            target_voltage_ids: IDs of regulating equipments to which we should compute sensitivities
        """
        _pypowsybl.set_bus_voltage_factor_matrix(self._handle, bus_ids, target_voltage_ids)
        self.bus_voltage_ids = bus_ids
        self.target_voltage_ids = target_voltage_ids

    def run(self, network: _Network, parameters: _Union[Parameters, pypowsybl.loadflow.Parameters] = None,
            provider: str = '', reporter: _Reporter = None) -> AcSensitivityAnalysisResult:
        """
        Runs the sensitivity analysis.

        Args:
            network:    The network
            parameters: The sensitivity parameters
            provider:   Name of the sensitivity analysis provider

        Returns:
            a sensitivity analysis result
        """
        sensitivity_parameters = Parameters(load_flow_parameters=parameters) if isinstance(parameters,
                                                                                                       pypowsybl.loadflow.Parameters) else parameters
        p: _pypowsybl.SensitivityAnalysisParameters = sensitivity_parameters._to_c_parameters() if sensitivity_parameters is not None else Parameters()._to_c_parameters()
        return AcSensitivityAnalysisResult(
            _pypowsybl.run_sensitivity_analysis(self._handle, network._handle, False, p, provider,
                                                None if reporter is None else reporter._reporter_model),
            # pylint: disable=protected-access
            branches_ids=self.branches_ids, branch_data_frame_index=self.branch_data_frame_index,
            bus_ids=self.bus_voltage_ids, target_voltage_ids=self.target_voltage_ids)


def create_dc_analysis() -> DcSensitivityAnalysis:
    """
    Creates a new DC sensitivity analysis.

    Returns:
        a new DC sensitivity analysis
    """
    return DcSensitivityAnalysis(_pypowsybl.create_sensitivity_analysis())


def create_ac_analysis() -> AcSensitivityAnalysis:
    """
    Creates a new AC sensitivity analysis.

    Returns:
        a new AC sensitivity analysis
    """
    return AcSensitivityAnalysis(_pypowsybl.create_sensitivity_analysis())


def set_default_provider(provider: str) -> None:
    """
    Set the default sensitivity analysis provider

    Args:
        provider: name of the default sensitivity analysis provider to set
    """
    _pypowsybl.set_default_sensitivity_analysis_provider(provider)


def get_default_provider() -> str:
    """
    Get the current default sensitivity analysis provider.

    Returns:
        the name of the current default sensitivity analysis provider
    """
    return _pypowsybl.get_default_sensitivity_analysis_provider()


def get_provider_names() -> _List[str]:
    """
    Get list of supported provider names

    Returns:
        the list of supported provider names
    """
    return _pypowsybl.get_sensitivity_analysis_provider_names()


def get_provider_parameters_names(provider: str = '') -> _List[str]:
    """
    Get list of parameters for the specified sensitivity analysis provider.

    If not specified the provider will be the default one.

    Returns:
        the list of provider's parameters
    """
    return _pypowsybl.get_sensitivity_analysis_provider_parameters_names(provider)
