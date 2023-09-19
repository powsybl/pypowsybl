# Copyright (c) 2023, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from __future__ import annotations
from typing import List, Dict
from pypowsybl import _pypowsybl
from pypowsybl.security import ContingencyContainer
from pypowsybl._pypowsybl import PyPowsyblError
from .zone import Zone

TO_REMOVE = 'TO_REMOVE'


class SensitivityAnalysis(ContingencyContainer):
    """ Base class for sensitivity analysis. Do not instantiate it directly!"""

    def __init__(self, handle: _pypowsybl.JavaHandle):
        ContingencyContainer.__init__(self, handle)
        self.branches_ids: Dict[str, List[str]] = {}
        self.branch_data_frame_index: Dict[str, List[str]] = {}

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

    def _process_variable_ids(self, variables_ids: List) -> tuple:
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

    def add_branch_flow_factor_matrix(self, branches_ids: List[str], variables_ids: List[str],
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

    def add_precontingency_branch_flow_factor_matrix(self, branches_ids: List[str], variables_ids: List[str],
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

    def add_postcontingency_branch_flow_factor_matrix(self, branches_ids: List[str], variables_ids: List[str],
                                                      contingencies_ids: List[str],
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
