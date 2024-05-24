# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from __future__ import annotations

import warnings
from typing import List, Dict

from pypowsybl import _pypowsybl
from pypowsybl.security import ContingencyContainer
from pypowsybl._pypowsybl import PyPowsyblError, ContingencyContextType, SensitivityFunctionType, SensitivityVariableType
from .sensitivity_analysis_result import DEFAULT_MATRIX_ID, TO_REMOVE
from .zone import Zone


class SensitivityAnalysis(ContingencyContainer):
    """ Base class for sensitivity analysis. Do not instantiate it directly!"""

    def __init__(self, handle: _pypowsybl.JavaHandle):
        ContingencyContainer.__init__(self, handle)
        self.functions_ids: Dict[str, List[str]] = {}
        self.function_data_frame_index: Dict[str, List[str]] = {}

    def set_zones(self, zones: List[Zone]) -> None:
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

    @staticmethod
    def _process_variable_ids(variables_ids: List) -> tuple:
        flatten_variables_ids = []
        branch_data_frame_index = []
        for variable_id in variables_ids:
            if isinstance(variable_id, str):  # this is an ID
                flatten_variables_ids.append(variable_id)
                branch_data_frame_index.append(variable_id)
            elif isinstance(variable_id, tuple):  # this is a power transfer
                if len(variable_id) != 2:
                    raise PyPowsyblError('Power transfer factor should be describe with a tuple 2')
                flatten_variables_ids.append(variable_id[0])
                flatten_variables_ids.append(variable_id[1])
                branch_data_frame_index.append(variable_id[0] + ' -> ' + variable_id[1])
                branch_data_frame_index.append(TO_REMOVE)
            else:
                raise PyPowsyblError(f'Unsupported factor variable type {type(variable_id)}')
        return (flatten_variables_ids, branch_data_frame_index)

    def set_branch_flow_factor_matrix(self, branches_ids: List[str], variables_ids: List[str]) -> None:
        """
        .. deprecated:: 0.14.0
          Use :meth:`add_branch_flow_factor_matrix` instead.

        Defines branch active power flow factor matrix, with a list of branches IDs and a list of variables.

        A variable could be:

         - a network element ID: injections, PSTs, dangling lines and HVDC lines are supported
         - a zone ID
         - a couple of zone ID to define a transfer between 2 zones

        Args:
            branches_ids:  IDs of branches for which active power flow sensitivities should be computed
            variables_ids: variables which may impact branch flows,to which we should compute sensitivities
        """
        warnings.warn("set_branch_flow_factor_matrix is deprecated, use add_branch_flow_factor_matrix instead",
                      DeprecationWarning)
        self.add_branch_flow_factor_matrix(branches_ids, variables_ids)

    def add_branch_flow_factor_matrix(self, branches_ids: List[str], variables_ids: List[str],
                                      matrix_id: str = DEFAULT_MATRIX_ID) -> None:
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
        self.add_factor_matrix(branches_ids, variables_ids, [], ContingencyContextType.ALL,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, SensitivityVariableType.AUTO_DETECT, matrix_id)

    def add_precontingency_branch_flow_factor_matrix(self, branches_ids: List[str], variables_ids: List[str],
                                                     matrix_id: str = DEFAULT_MATRIX_ID) -> None:
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
        self.add_factor_matrix(branches_ids, variables_ids, [], ContingencyContextType.NONE,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, SensitivityVariableType.AUTO_DETECT, matrix_id)

    def add_postcontingency_branch_flow_factor_matrix(self, branches_ids: List[str], variables_ids: List[str],
                                                      contingencies_ids: List[str],
                                                      matrix_id: str = DEFAULT_MATRIX_ID) -> None:
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
        self.add_factor_matrix(branches_ids, variables_ids, contingencies_ids, ContingencyContextType.SPECIFIC,
                               SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, SensitivityVariableType.AUTO_DETECT, matrix_id)

    def add_factor_matrix(self, functions_ids: List[str], variables_ids: List[str], contingencies_ids: List[str],
                          contingency_context_type: ContingencyContextType,
                          sensitivity_function_type: SensitivityFunctionType,
                          sensitivity_variable_type: SensitivityVariableType = SensitivityVariableType.AUTO_DETECT,
                          matrix_id: str = DEFAULT_MATRIX_ID) -> None:
        """
        Defines branch active power factor matrix, with a list of branches IDs and a list of variables.

        A variable could be:
         - a network element ID: injections, PSTs, dangling lines and HVDC lines are supported
         - a zone ID
         - a couple of zone ID to define a transfer between 2 zones

        sensitivity_function_type can be:
         - BRANCH_ACTIVE_POWER_1
         - BRANCH_CURRENT_1
         - BRANCH_REACTIVE_POWER_1
         - BRANCH_ACTIVE_POWER_2
         - BRANCH_CURRENT_2
         - BRANCH_REACTIVE_POWER_2
         - BRANCH_ACTIVE_POWER_3
         - BRANCH_CURRENT_3
         - BRANCH_REACTIVE_POWER_3
         - BUS_REACTIVE_POWER
         - BUS_VOLTAGE

        sensitivity_variable_type can be:
         - INJECTION_ACTIVE_POWER
         - INJECTION_REACTIVE_POWER
         - TRANSFORMER_PHASE
         - BUS_TARGET_VOLTAGE
         - HVDC_LINE_ACTIVE_POWER
         - TRANSFORMER_PHASE_1
         - TRANSFORMER_PHASE_2
         - TRANSFORMER_PHASE_3

        Args:
            functions_ids:              functions for which the sensitivities for the sensitivity_function_type should be computed
            variables_ids:              variables which may impact functions,to which we should compute sensitivities
            contingencies_ids:          List of the IDs of the contingencies to simulate
            contingency_context_type:   the contingency context type it could be ALL, NONE or SPECIFIC
            sensitivity_function_type:  the function type of sensitivity to compute
            sensitivity_variable_type:  the variable type of sensitivity to compute, automatically guessed (best effort) if value is AUTO_DETECT
            matrix_id:                  The matrix unique identifier, to be used to retrieve the sensibility value
        """
        (flatten_variables_ids, function_data_frame_index) = self._process_variable_ids(variables_ids)
        _pypowsybl.add_factor_matrix(self._handle, matrix_id, functions_ids,
                                     flatten_variables_ids, contingencies_ids, contingency_context_type,
                                     sensitivity_function_type, sensitivity_variable_type)
        self.functions_ids[matrix_id] = functions_ids
        self.function_data_frame_index[matrix_id] = function_data_frame_index
