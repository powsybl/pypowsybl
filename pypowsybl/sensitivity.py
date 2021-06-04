#
# Copyright (c) 2021, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
import _pypowsybl

from pypowsybl.loadflow import Parameters
from pypowsybl.network import Network
from pypowsybl.util import ContingencyContainer
from pypowsybl.util import ObjectHandle
from _pypowsybl import PyPowsyblError
from typing import List, Optional, Dict
from enum import Enum
import numpy as np
import pandas as pd


class Zone:
    def __init__(self, id: str, shift_keys_by_injections_id: Dict[str, float] = {}):
        self._id = id
        self._shift_keys_by_injections_id = shift_keys_by_injections_id

    @property
    def id(self):
        return self._id

    @property
    def shift_keys_by_injections_id(self):
        return self._shift_keys_by_injections_id

    @property
    def injections_ids(self):
        return list(self._shift_keys_by_injections_id.keys())

    def get_shift_key(self, injection_id: str):
        shift_key = self._shift_keys_by_injections_id.get(injection_id)
        if shift_key is None:
            raise PyPowsyblError(f'Injection {injection_id} not found')
        return shift_key


def create_empty_zone(id: str) -> Zone:
    return Zone(id)


class ZoneKeyType(Enum):
    GENERATOR_TARGET_P = 0
    GENERATOR_MAX_P = 1
    LOAD_P0 = 2


def create_country_zone(network: Network, country: str, key_type: ZoneKeyType = ZoneKeyType.GENERATOR_TARGET_P) -> Zone:
    substations = network.create_substations_data_frame()
    voltage_levels = network.create_voltage_levels_data_frame()
    if key_type == ZoneKeyType.GENERATOR_MAX_P or key_type == ZoneKeyType.GENERATOR_TARGET_P:
        # join generators, voltage levels and substations to get generators with countries
        generators = network.create_generators_data_frame()
        generators_with_countries = generators.join(
            voltage_levels[['substation_id']].join(substations[['country']], on=['substation_id']),
            on=['voltage_level_id'])

        # filter generators for specified country
        filtered_generators = generators_with_countries[generators_with_countries['country'] == country]
        shift_keys = filtered_generators.target_p if key_type == ZoneKeyType.GENERATOR_TARGET_P else filtered_generators.max_p
        shift_keys_by_id = dict(zip(filtered_generators.index, shift_keys))
    elif key_type == ZoneKeyType.LOAD_P0:
        # join loads, voltage levels and substations to get generators with countries
        loads = network.create_loads_data_frame()
        loads_with_countries = loads.join(
            voltage_levels[['substation_id']].join(substations[['country']], on=['substation_id']),
            on=['voltage_level_id'])

        # filter loads for specified country
        filtered_loads = loads_with_countries[loads_with_countries['country'] == country]
        shift_keys_by_id = dict(zip(filtered_loads.index, filtered_loads.p0))
    else:
        raise PyPowsyblError(f'Unknown key type {key_type}')

    return Zone(country, shift_keys_by_id)


class DcSensitivityAnalysisResult(ObjectHandle):
    """ Represents the result of a DC sensitivity analysis.
    The result contains computed values (so called "reference" values) and sensitivity values
    of requested factors, on the base case and on post contingency states.
    """

    def __init__(self, result_context_ptr, monitored_branches_ids: List[str], elements_ids: List[str], zones_ids: List[str]):
        ObjectHandle.__init__(self, result_context_ptr)
        self.result_context_ptr = result_context_ptr
        self.monitored_branches_ids = monitored_branches_ids
        self.elements_ids = elements_ids
        self.zones_ids = zones_ids

    def get_branch_flows_sensitivity_matrix(self, contingency_id: str = None) -> Optional[pd.DataFrame]:
        """ Get the matrix of branch flows sensitivities on the base case or on the post contingency state depending if
        a contingency ID has been provided.

        Args:
            contingency_id (str, optional): ID of the contingency
        Returns:
            the matrix of branch flows sensitivities
        """
        m = _pypowsybl.get_branch_flows_sensitivity_matrix(self.result_context_ptr, '' if contingency_id is None else contingency_id)
        if m is None:
            return None
        else:
            data = np.array(m, copy=False)
            return pd.DataFrame(data=data, columns=self.monitored_branches_ids, index=self.elements_ids + self.zones_ids)

    def get_reference_flows(self, contingency_id: str = None) -> Optional[pd.DataFrame]:
        """ The branches active power flows on the base case or on the post contingency state depending if
        a contingency ID has been provided.

        Args:
            contingency_id (str, optional): ID of the contingency
        Returns:
            the branches active power flows
        """
        m = _pypowsybl.get_reference_flows(self.result_context_ptr, '' if contingency_id is None else contingency_id)
        if m is None:
            return None
        else:
            data = np.array(m, copy=False)
            return pd.DataFrame(data=data, columns=self.monitored_branches_ids, index=['reference_flows'])


class AcSensitivityAnalysisResult(DcSensitivityAnalysisResult):
    """ Represents the result of a AC sensitivity analysis.
    The result contains computed values (so called "reference" values) and sensitivity values
    of requested factors, on the base case and on post contingency states.
    """

    def __init__(self, result_context_ptr, monitored_branches_ids: List[str], elements_ids: List[str],
                 zones_ids: List[str], bus_ids: List[str], target_voltage_ids: List[str]):
        DcSensitivityAnalysisResult.__init__(self, result_context_ptr, monitored_branches_ids, elements_ids, zones_ids)
        self.bus_ids = bus_ids
        self.target_voltage_ids = target_voltage_ids

    def get_bus_voltages_sensitivity_matrix(self, contingency_id: str = None) -> Optional[pd.DataFrame]:
        """ Get the matrix of bus voltages sensitivities on the base case or on the post contingency state depending if
        a contingency ID has been provided.

        Args:
            contingency_id (str, optional): ID of the contingency
        Returns:
            the matrix of sensitivities
        """
        m = _pypowsybl.get_bus_voltages_sensitivity_matrix(self.result_context_ptr, '' if contingency_id is None else contingency_id)
        if m is None:
            return None
        else:
            data = np.array(m, copy=False)
            return pd.DataFrame(data=data, columns=self.bus_ids, index=self.target_voltage_ids)

    def get_reference_voltages(self, contingency_id: str = None) -> Optional[pd.DataFrame]:
        """ The values of bus voltages on the base case or on the post contingency state depending if
        a contingency ID has been provided.

        Args:
            contingency_id (str, optional): ID of the contingency
        Returns:
            the values of bus voltages
        """
        m = _pypowsybl.get_reference_voltages(self.result_context_ptr, '' if contingency_id is None else contingency_id)
        if m is None:
            return None
        else:
            data = np.array(m, copy=False)
            return pd.DataFrame(data=data, columns=self.bus_ids, index=['reference_voltages'])


class SensitivityAnalysis(ContingencyContainer):
    """ Base class for sensitivity analysis. Do not instantiate it directly!"""
    def __init__(self, ptr):
        ContingencyContainer.__init__(self, ptr)
        self.monitored_branches_ids = None
        self.elements_ids = None
        self.zones_ids = None

    def set_branch_flow_factor_matrix(self, monitored_branches_ids: List[str], variables: List):
        """ Defines branch active power flow factor matrix, with a list of monitored branches IDs and a list of variables.
        A variable could be:
         - a network element ID: injections, PSTs, dangling lines and HVDC lines are supported
         - a zone  

        Args:
            monitored_branches_ids: IDs of branches for which active power flow sensitivities should be computed
            variables: variables which may impact branch flows,to which we should compute sensitivities
        """
        elements_ids = []
        zones = []
        zones_ids = []
        for variable in variables:
            if isinstance(variable, str): # this is an element ID
                elements_ids.append(variable)
            elif isinstance(variable, Zone): # this is a zone
                zones.append(_pypowsybl.Zone(variable.id, list(variable.shift_keys_by_injections_id.keys()), list(variable.shift_keys_by_injections_id.values())))
                zones_ids.append(variable.id)
            else:
                raise PyPowsyblError(f'Unsupported factor variable type {type(variable)}')

        _pypowsybl.set_branch_flow_factor_matrix(self.ptr, monitored_branches_ids, elements_ids, zones)
        self.monitored_branches_ids = monitored_branches_ids
        self.elements_ids = elements_ids
        self.zones_ids = zones_ids


class DcSensitivityAnalysis(SensitivityAnalysis):
    """ Represents a DC sensitivity analysis."""
    def __init__(self, ptr):
        SensitivityAnalysis.__init__(self, ptr)

    def run(self, network: Network, parameters: Parameters = Parameters(), provider: str = 'OpenSensitivityAnalysis') -> DcSensitivityAnalysisResult:
        """ Runs the sensitivity analysis

        Args:
            network (Network): The network
            parameters (Parameters, optional): The load flow parameters
            provider (str, optional): Name of the sensitivity analysis provider

        Returns:
            a sensitivity analysis result
        """
        return DcSensitivityAnalysisResult(_pypowsybl.run_sensitivity_analysis(self.ptr, network.ptr, True, parameters, provider),
                                           monitored_branches_ids=self.monitored_branches_ids, elements_ids=self.elements_ids,
                                           zones_ids=self.zones_ids)


class AcSensitivityAnalysis(SensitivityAnalysis):
    """ Represents an AC sensitivity analysis."""
    def __init__(self, ptr):
        SensitivityAnalysis.__init__(self, ptr)
        self.bus_voltage_ids = None
        self.target_voltage_ids = None

    def set_bus_voltage_factor_matrix(self, bus_ids: List[str], target_voltage_ids: List[str]):
        """ Defines buses for which voltage sensitivities should be computed,
        and to which regulating equipments.

        Args:
            bus_ids: IDs of buses for which voltage sensitivities should be computed
            target_voltage_ids: IDs of regulating equipments to which we should compute sensitivities
        """
        _pypowsybl.set_bus_voltage_factor_matrix(self.ptr, bus_ids, target_voltage_ids)
        self.bus_voltage_ids = bus_ids
        self.target_voltage_ids = target_voltage_ids

    def run(self, network: Network, parameters: Parameters = Parameters(), provider: str = 'OpenSensitivityAnalysis') -> AcSensitivityAnalysisResult:
        """ Runs the sensitivity analysis

        Args:
            network (Network): The network
            parameters (Parameters, optional): The load flow parameters
            provider (str, optional): Name of the sensitivity analysis provider

        Returns:
            a sensitivity analysis result
        """
        return AcSensitivityAnalysisResult(_pypowsybl.run_sensitivity_analysis(self.ptr, network.ptr, False, parameters, provider),
                                           monitored_branches_ids=self.monitored_branches_ids, elements_ids=self.elements_ids,
                                           zones_ids=self.zones_ids, bus_ids=self.bus_voltage_ids, target_voltage_ids=self.target_voltage_ids)


def create_dc_analysis() -> DcSensitivityAnalysis:
    """ Creates a new DC sensitivity analysis.

    Returns:
        a new DC sensitivity analysis
    """
    return DcSensitivityAnalysis(_pypowsybl.create_sensitivity_analysis())


def create_ac_analysis() -> AcSensitivityAnalysis:
    """ Creates a new AC sensitivity analysis.

    Returns:
        a new AC sensitivity analysis
    """
    return AcSensitivityAnalysis(_pypowsybl.create_sensitivity_analysis())
