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
from typing import List, Optional
import numpy as np
import pandas as pd


class DcSensitivityAnalysisResult(ObjectHandle):
    """ Represents the result of a DC sensitivity analysis.
    The result contains computed values (so called "reference" values) and sensitivity values
    of requested factors, on the base case and on post contingency states.
    """

    def __init__(self, result_context_ptr, branches_ids: List[str], injections_or_transformers_ids: List[str]):
        ObjectHandle.__init__(self, result_context_ptr)
        self.result_context_ptr = result_context_ptr
        self.branches_ids = branches_ids
        self.injections_or_transformers_ids = injections_or_transformers_ids

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
            return pd.DataFrame(data=data, columns=self.branches_ids, index=self.injections_or_transformers_ids)

    def get_reference_flows(self, contingency_id: str = None) -> Optional[pd.DataFrame]:
        """ The values of branch flows on the base case or on the post contingency state depending if
        a contingency ID has been provided.

        Args:
            contingency_id (str, optional): ID of the contingency
        Returns:
            the values of branch flows
        """
        m = _pypowsybl.get_reference_flows(self.result_context_ptr, '' if contingency_id is None else contingency_id)
        if m is None:
            return None
        else:
            data = np.array(m, copy=False)
            return pd.DataFrame(data=data, columns=self.branches_ids, index=['reference_flows'])


class AcSensitivityAnalysisResult(DcSensitivityAnalysisResult):
    """ Represents the result of a AC sensitivity analysis.
    The result contains computed values (so called "reference" values) and sensitivity values
    of requested factors, on the base case and on post contingency states.
    """

    def __init__(self, result_context_ptr, branches_ids: List[str], injections_or_transformers_ids: List[str],
                 bus_ids: List[str], target_voltage_ids: List[str]):
        DcSensitivityAnalysisResult.__init__(self, result_context_ptr, branches_ids, injections_or_transformers_ids)
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


class DcSensitivityAnalysis(ContingencyContainer):
    """ Represents a DC sensitivity analysis."""
    def __init__(self, ptr):
        ContingencyContainer.__init__(self, ptr)
        self.branches_ids = None
        self.injections_or_transformers_ids = None

    def set_branch_flow_factor_matrix(self, branches_ids: List[str], injections_or_transformers_ids: List[str]):
        """ Defines branches for which active power flow sensitivities should be computed,
        and to which injections or PST.

        Args:
            branches_ids: IDs of branches for which active power flow sensitivities should be computed
            injections_or_transformers_ids: IDs of injections or PSTs which may impact branch flows,
                                            to which we should compute sensitivities
        """

        _pypowsybl.set_branch_flow_factor_matrix(self.ptr, branches_ids, injections_or_transformers_ids)
        self.branches_ids = branches_ids
        self.injections_or_transformers_ids = injections_or_transformers_ids

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
                                           branches_ids=self.branches_ids, injections_or_transformers_ids=self.injections_or_transformers_ids)


class AcSensitivityAnalysis(ContingencyContainer):
    """ Represents an AC sensitivity analysis."""
    def __init__(self, ptr):
        ContingencyContainer.__init__(self, ptr)
        self.branches_ids = None
        self.injections_or_transformers_ids = None
        self.bus_voltage_ids = None
        self.target_voltage_ids = None

    def set_branch_flow_factor_matrix(self, branches_ids: List[str], injections_or_transformers_ids: List[str]):
        """ Defines branches for which active power flow sensitivities should be computed,
        and to which injections or PST.

        Args:
            branches_ids: IDs of branches for which active power flow sensitivities should be computed
            injections_or_transformers_ids: IDs of injections or PSTs which may impact branch flows,
                                            to which we should compute sensitivities
        """

        _pypowsybl.set_branch_flow_factor_matrix(self.ptr, branches_ids, injections_or_transformers_ids)
        self.branches_ids = branches_ids
        self.injections_or_transformers_ids = injections_or_transformers_ids

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
                                           branches_ids=self.branches_ids, injections_or_transformers_ids=self.injections_or_transformers_ids,
                                           bus_ids=self.bus_voltage_ids, target_voltage_ids=self.target_voltage_ids)


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
