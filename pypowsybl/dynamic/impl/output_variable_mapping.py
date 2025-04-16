# Copyright (c) 2024, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
# SPDX-License-Identifier: MPL-2.0
#
from typing import List, Union

from pypowsybl import _pypowsybl as _pp
from pypowsybl._pypowsybl import OutputVariableType # pylint: disable=protected-access


class OutputVariableMapping:
    """
    Class to map Curves and Final State Values
    """

    def __init__(self) -> None:
        self._handle = _pp.create_timeseries_mapping()

    def add_dynamic_model_curves(self, dynamic_model_id: str, variables: Union[List[str], str]) -> None:
        """
        Adds curves mapping on a single dynamic model

        :param dynamic_model_id: id of the dynamic model
        :param variables: single element or list of variables names to record
        """
        if isinstance(variables, str):
            variables = [variables]
        self._add_output_variables(dynamic_model_id, variables, True, OutputVariableType.CURVE)

    def add_standard_model_curves(self, static_id: str, variables: Union[List[str], str]) -> None:
        """
        Adds curves mapping on a single network equipment without dynamic model

        :param static_id: id of the network equipment
        :param variables: single element or list of variables names to record
        """
        if isinstance(variables, str):
            variables = [variables]
        self._add_output_variables(static_id, variables, False, OutputVariableType.CURVE)

    def add_dynamic_model_final_state_values(self, dynamic_model_id: str, variables: Union[List[str], str]) -> None:
        """
        Adds final state values mapping on a single dynamic model

        :param dynamic_model_id: id of the dynamic model
        :param variables: single element or list of variables names to record
        """
        if isinstance(variables, str):
            variables = [variables]
        self._add_output_variables(dynamic_model_id, variables, True, OutputVariableType.FINAL_STATE)

    def add_standard_model_final_state_values(self, static_id: str, variables: Union[List[str], str]) -> None:
        """
        Adds final state values mapping on a single network equipment without dynamic model

        :param static_id: id of the network equipment
        :param variables: single element or list of variables names to record
        """
        if isinstance(variables, str):
            variables = [variables]
        self._add_output_variables(static_id, variables, False, OutputVariableType.FINAL_STATE)

    def _add_output_variables(self, element_id: str, variables: List[str], is_dynamic: bool, output_variable_type: OutputVariableType) -> None:
        _pp.add_output_variables(self._handle, element_id, variables, is_dynamic, output_variable_type)
